package com.module.facerecognition.services;

import com.module.facerecognition.ai.ArcFaceEmbedder;
import com.module.facerecognition.ai.FaceAligner;
import com.module.facerecognition.ai.FaceDetection;
import com.module.facerecognition.auth.entities.AppUser;
import com.module.facerecognition.auth.security.CurrentUserService;
import com.module.facerecognition.entities.Attendance;
import com.module.facerecognition.entities.FaceEmbedding;
import com.module.facerecognition.entities.Person;
import com.module.facerecognition.exceptions.FaceAlreadyRegisteredException;
import com.module.facerecognition.repositories.FaceEmbeddingRepository;
import com.module.facerecognition.repositories.PersonRepository;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FaceRecognitionService {

    private final ScrfdDetectionService detectionService;
    private final FaceAligner faceAligner;
    private final ArcFaceEmbedder embedder;

    private final PersonRepository personRepository;
    private final FaceEmbeddingRepository faceEmbeddingRepository;

    private final ObjectMapper objectMapper;
    private final AttendanceService attendanceService;

    private final CurrentUserService currentUserService;


    /*
     * Recognition threshold.
     *
     * Keep your existing value for now.
     * We can calibrate this later using real samples.
     */
    private static final double RECOGNITION_THRESHOLD = 0.55;


    /*
     * Same-person duplicate sample.
     *
     * This prevents saving practically the same
     * image/face multiple times.
     */
    private static final double DUPLICATE_SAMPLE_THRESHOLD = 0.98;


    /*
     * Cross-person duplicate protection.
     *
     * If a newly enrolling face is too similar to
     * an already registered face, enrollment is rejected.
     *
     * This is deliberately separate from the 0.98
     * same-person sample threshold.
     */
    private static final double DUPLICATE_FACE_THRESHOLD = 0.85;


    public FaceRecognitionService(
            ScrfdDetectionService detectionService,
            FaceAligner faceAligner,
            ArcFaceEmbedder embedder,
            PersonRepository personRepository,
            FaceEmbeddingRepository faceEmbeddingRepository,
            ObjectMapper objectMapper,
            AttendanceService attendanceService,
            CurrentUserService currentUserService) {

        this.detectionService = detectionService;
        this.faceAligner = faceAligner;
        this.embedder = embedder;
        this.personRepository = personRepository;
        this.faceEmbeddingRepository = faceEmbeddingRepository;
        this.objectMapper = objectMapper;
        this.attendanceService = attendanceService;
        this.currentUserService = currentUserService;
    }


    /*
     * =========================================================
     * AUTHENTICATED USER FACE ENROLLMENT
     * =========================================================
     *
     * The user is obtained from JWT.
     *
     * The frontend does NOT send:
     *
     *     name
     *     personId
     *
     * This prevents one user from enrolling a face
     * under another person's name.
     */
    @Transactional
    public Person enrollForCurrentUser(
            MultipartFile image)
            throws Exception {

        validateImage(image);


        /*
         * Get logged-in user from JWT.
         */
        AppUser user =
                currentUserService.getCurrentUser();

        Person existingPerson =
                personRepository.findByUserId(user.getId())
                        .orElse(null);

        if (existingPerson != null
                && !existingPerson.getFaceEmbeddings().isEmpty()) {

            throw new IllegalStateException(
                    "Face is already enrolled for this account."
            );
        }


        /*
         * Generate 512-dimensional ArcFace embedding.
         */
        float[] newEmbedding =
                generateEmbedding(image);


        /*
         * -----------------------------------------------------
         * STEP 1
         * Check whether this face already belongs to
         * another enrolled person.
         * -----------------------------------------------------
         */

        List<FaceEmbedding> allEmbeddings =
                faceEmbeddingRepository.findAll();

        for (FaceEmbedding existing :
                allEmbeddings) {

            float[] storedEmbedding =
                    objectMapper.readValue(
                            existing.getEmbedding(),
                            float[].class
                    );


            double similarity =
                    cosineSimilarity(
                            newEmbedding,
                            storedEmbedding
                    );


            /*
             * We deliberately do not tell the client
             * which account/person matched.
             */
            if (similarity >=
                    DUPLICATE_FACE_THRESHOLD) {

                throw new FaceAlreadyRegisteredException();
            }
        }


        /*
         * -----------------------------------------------------
         * STEP 2
         * Check whether this user already has a Person.
         * -----------------------------------------------------
         */

        Person person =
                personRepository
                        .findByUserId(user.getId())
                        .orElse(null);


        /*
         * If no Person exists, create one.
         */
        if (person == null) {

            person =
                    new Person(
                            user.getName()
                    );

            person.setUser(user);

            person =
                    personRepository.save(person);
        }


        /*
         * -----------------------------------------------------
         * STEP 3
         * Store embedding.
         * -----------------------------------------------------
         */

        String json =
                objectMapper.writeValueAsString(
                        newEmbedding
                );


        FaceEmbedding faceEmbedding =
                new FaceEmbedding(json);


        person.addFaceEmbedding(
                faceEmbedding
        );


        personRepository.save(person);


        return person;
    }


    /*
     * =========================================================
     * OLD METHOD
     * =========================================================
     *
     * We are keeping this temporarily so other existing code
     * does not break.
     *
     * IMPORTANT:
     * Do NOT expose this method through a public controller
     * endpoint anymore.
     */
    @Deprecated
    @Transactional
    public Person enroll(
            String name,
            MultipartFile image)
            throws Exception {

        validateName(name);
        validateImage(image);

        float[] embedding =
                generateEmbedding(image);

        String json =
                objectMapper.writeValueAsString(
                        embedding
                );

        Person person =
                new Person(name.trim());

        person =
                personRepository.save(person);

        FaceEmbedding faceEmbedding =
                new FaceEmbedding(json);

        person.addFaceEmbedding(
                faceEmbedding
        );

        personRepository.save(person);

        return person;
    }


    /*
     * =========================================================
     * ADD ANOTHER FACE SAMPLE
     * =========================================================
     */

    @Transactional
    public FaceEmbedding addFaceEmbedding(
            Long personId,
            MultipartFile image)
            throws Exception {

        validateImage(image);

        Person person =
                personRepository.findById(personId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Person not found: "
                                                + personId
                                )
                        );


        float[] newEmbedding =
                generateEmbedding(image);


        List<FaceEmbedding> existingEmbeddings =
                person.getFaceEmbeddings();


        for (FaceEmbedding existing :
                existingEmbeddings) {

            float[] storedEmbedding =
                    objectMapper.readValue(
                            existing.getEmbedding(),
                            float[].class
                    );


            double similarity =
                    cosineSimilarity(
                            newEmbedding,
                            storedEmbedding
                    );


            if (similarity >=
                    DUPLICATE_SAMPLE_THRESHOLD) {

                throw new IllegalArgumentException(
                        "This face sample is already enrolled " +
                                "for this person."
                );
            }
        }


        String json =
                objectMapper.writeValueAsString(
                        newEmbedding
                );


        FaceEmbedding faceEmbedding =
                new FaceEmbedding(json);


        person.addFaceEmbedding(
                faceEmbedding
        );


        personRepository.save(person);


        return faceEmbedding;
    }


    /*
     * =========================================================
     * RECOGNITION
     * =========================================================
     */

    @Transactional(readOnly = true)
    public MatchResult recognize(
            MultipartFile image)
            throws Exception {

        validateImage(image);


        float[] currentEmbedding =
                generateEmbedding(image);


        List<Person> people =
                personRepository
                        .findAllWithFaceEmbeddings();


        if (people.isEmpty()) {

            throw new IllegalStateException(
                    "No enrolled persons found"
            );
        }


        Person bestPerson = null;

        double bestSimilarity = -1.0;

        Long bestEmbeddingId = null;


        for (Person person :
                people) {

            for (FaceEmbedding faceEmbedding :
                    person.getFaceEmbeddings()) {

                float[] storedEmbedding =
                        objectMapper.readValue(
                                faceEmbedding.getEmbedding(),
                                float[].class
                        );


                double similarity =
                        cosineSimilarity(
                                currentEmbedding,
                                storedEmbedding
                        );


                System.out.println(
                        "Person ID: "
                                + person.getId()
                                + " | Name: "
                                + person.getName()
                                + " | Embedding ID: "
                                + faceEmbedding.getId()
                                + " | Similarity: "
                                + similarity
                );


                if (similarity > bestSimilarity) {

                    bestSimilarity =
                            similarity;

                    bestPerson =
                            person;

                    bestEmbeddingId =
                            faceEmbedding.getId();
                }
            }
        }


        boolean recognized =
                bestPerson != null
                        && bestSimilarity >=
                        RECOGNITION_THRESHOLD;


        System.out.println(
                "BEST MATCH -> Person ID: "
                        + (
                        bestPerson != null
                                ? bestPerson.getId()
                                : "NONE"
                )
                        + " | Similarity: "
                        + bestSimilarity
                        + " | Embedding ID: "
                        + bestEmbeddingId
        );


        return new MatchResult(
                recognized
                        ? bestPerson
                        : null,
                bestSimilarity
        );
    }


    /*
     * =========================================================
     * RECOGNITION + ATTENDANCE
     * =========================================================
     */

    @Transactional
    public AttendanceVerificationResult
    recognizeAndMarkAttendance(
            MultipartFile image)
            throws Exception {

        MatchResult result =
                recognize(image);


        if (result.person() == null) {

            return new AttendanceVerificationResult(
                    false,
                    null,
                    null,
                    result.similarity(),
                    null
            );
        }


        Attendance attendance =
                attendanceService.markPresent(
                        result.person()
                );


        return new AttendanceVerificationResult(
                true,
                result.person().getId(),
                result.person().getName(),
                result.similarity(),
                attendance.getAttendanceTime()
        );
    }


    /*
     * =========================================================
     * EMBEDDING GENERATION
     * =========================================================
     *
     * THIS PART USES YOUR ACTUAL METHODS.
     *
     * MultipartFile
     *      ↓
     * OpenCV Mat
     *      ↓
     * SCRFD
     *      ↓
     * FaceDetection
     *      ↓
     * landmarks
     *      ↓
     * FaceAligner
     *      ↓
     * ArcFace
     */

    private float[] generateEmbedding(
            MultipartFile image)
            throws Exception {


        byte[] bytes =
                image.getBytes();


        Mat input =
                Imgcodecs.imdecode(
                        new MatOfByte(bytes),
                        Imgcodecs.IMREAD_COLOR
                );


        if (input.empty()) {

            throw new IOException(
                    "Could not decode image"
            );
        }


        try {

            /*
             * YOUR ACTUAL METHOD:
             *
             * detectFaces(...)
             */
            List<FaceDetection> faces =
                    detectionService.detectFaces(
                            image
                    );


            /*
             * Exactly one face.
             */

            if (faces.isEmpty()) {

                throw new IllegalArgumentException(
                        "No face detected"
                );
            }


            if (faces.size() > 1) {

                throw new IllegalArgumentException(
                        "Multiple faces detected. " +
                                "Only one face is allowed."
                );
            }


            FaceDetection face =
                    faces.get(0);


            /*
             * YOUR ACTUAL FaceAligner METHOD:
             *
             * align(Mat, Point[])
             */

            Mat aligned =
                    faceAligner.align(
                            input,
                            face.getLandmarks()
                    );


            try {

                /*
                 * YOUR ACTUAL ArcFace METHOD:
                 *
                 * generateEmbedding(Mat)
                 */
                return embedder.generateEmbedding(
                        aligned
                );

            } finally {

                aligned.release();
            }

        } finally {

            input.release();
        }
    }


    /*
     * =========================================================
     * IMAGE VALIDATION
     * =========================================================
     */

    private void validateImage(
            MultipartFile image) {

        if (image == null
                || image.isEmpty()) {

            throw new IllegalArgumentException(
                    "Image is required"
            );
        }


        long maxSize =
                5L * 1024L * 1024L;


        if (image.getSize() > maxSize) {

            throw new IllegalArgumentException(
                    "Image is too large. " +
                            "Maximum size is 5 MB."
            );
        }
    }


    /*
     * =========================================================
     * NAME VALIDATION
     * =========================================================
     */

    private void validateName(
            String name) {

        if (name == null
                || name.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Person name is required"
            );
        }


        if (name.trim().length() > 100) {

            throw new IllegalArgumentException(
                    "Person name cannot exceed 100 characters"
            );
        }
    }


    /*
     * =========================================================
     * COSINE SIMILARITY
     * =========================================================
     */

    private double cosineSimilarity(
            float[] a,
            float[] b) {

        if (a == null || b == null) {
            return -1.0;
        }


        if (a.length != b.length) {
            return -1.0;
        }


        double dot = 0.0;

        double normA = 0.0;

        double normB = 0.0;


        for (int i = 0;
             i < a.length;
             i++) {

            dot +=
                    a[i] * b[i];

            normA +=
                    a[i] * a[i];

            normB +=
                    b[i] * b[i];
        }


        if (normA == 0.0
                || normB == 0.0) {

            return -1.0;
        }


        return dot /
                (
                        Math.sqrt(normA)
                                *
                                Math.sqrt(normB)
                );
    }


    /*
     * =========================================================
     * RESULT RECORDS
     * =========================================================
     */

    public record MatchResult(
            Person person,
            double similarity) {
    }


    public record AttendanceVerificationResult(
            boolean recognized,
            Long personId,
            String name,
            double similarity,
            LocalDateTime attendanceTime) {
    }
}