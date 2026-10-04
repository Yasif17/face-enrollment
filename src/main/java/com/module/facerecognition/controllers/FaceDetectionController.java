package com.module.facerecognition.controllers;


import com.module.facerecognition.ai.ArcFaceEmbedder;
import com.module.facerecognition.ai.FaceDetection;
import com.module.facerecognition.entities.EnrollmentSession;
import com.module.facerecognition.entities.Person;
import com.module.facerecognition.services.*;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

import com.module.facerecognition.ai.FaceAligner;

@RestController
@RequestMapping("/api/faces")
public class FaceDetectionController {

    private final ScrfdDetectionService detectionService;
    private final FaceAligner faceAligner;
    private final ArcFaceEmbedder embedder;
    private final FaceEnrollmentService enrollmentService;
    private final FaceRecognitionService faceRecognitionService;
    private final LivenessDetectionService livenessDetectionService;
    private final EnrollmentSessionService enrollmentSessionService;
    private final LiveLivenessService liveLivenessService;

    public FaceDetectionController(
            ScrfdDetectionService detectionService,
            FaceAligner faceAligner,
            ArcFaceEmbedder embedder,
            FaceEnrollmentService enrollmentService,
            FaceRecognitionService faceRecognitionService,
            LivenessDetectionService livenessDetectionService,
            EnrollmentSessionService enrollmentSessionService,
            LiveLivenessService liveLivenessService) {

        this.detectionService = detectionService;

        this.faceAligner = faceAligner;
        this.embedder = embedder;
        this.enrollmentService = enrollmentService;
        this.faceRecognitionService = faceRecognitionService;
        this.livenessDetectionService = livenessDetectionService;
        this.enrollmentSessionService = enrollmentSessionService;
        this.liveLivenessService = liveLivenessService;
    }

    @PostMapping("/detect")
    public ResponseEntity<?> detectFace(
            @RequestParam("image")
            MultipartFile image) throws IOException {

        List<FaceDetection> faces =
                detectionService.detectFaces(image);

        List<Map<String, Object>> results =
                new ArrayList<>();

        for (FaceDetection face : faces) {

            Map<String, Object> data =
                    new LinkedHashMap<>();

            data.put(
                    "confidence",
                    face.getConfidence()
            );

            Map<String, Object> box =
                    new LinkedHashMap<>();

            box.put("x", face.getBoundingBox().x);
            box.put("y", face.getBoundingBox().y);
            box.put("width", face.getBoundingBox().width);
            box.put("height", face.getBoundingBox().height);

            data.put("box", box);

            var landmarks =
                    face.getLandmarks();

            List<Map<String, Double>> points =
                    new ArrayList<>();

            for (var point : landmarks) {

                points.add(
                        Map.of(
                                "x", point.x,
                                "y", point.y
                        )
                );
            }

            data.put("landmarks", points);

            results.add(data);
        }

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "facesDetected",
                results.size()
        );

        response.put(
                "faces",
                results
        );

        return ResponseEntity.ok(response);
    }


    @PostMapping(
            value = "/align",
            produces = MediaType.IMAGE_JPEG_VALUE
    )
    public ResponseEntity<byte[]> alignFace(
            @RequestParam("image")
            MultipartFile image) throws IOException {

        byte[] bytes = image.getBytes();

        Mat input = Imgcodecs.imdecode(
                new MatOfByte(bytes),
                Imgcodecs.IMREAD_COLOR
        );

        if (input.empty()) {
            throw new IOException(
                    "Could not decode image"
            );
        }

        try {

            List<FaceDetection> faces =
                    detectionService.detectFaces(image);

            if (faces.isEmpty()) {
                return ResponseEntity.badRequest()
                        .build();
            }

            FaceDetection face =
                    faces.get(0);

            Mat aligned =
                    faceAligner.align(
                            input,
                            face.getLandmarks()
                    );

            try {

                MatOfByte output =
                        new MatOfByte();

                boolean encoded =
                        Imgcodecs.imencode(
                                ".jpg",
                                aligned,
                                output
                        );

                if (!encoded) {
                    return ResponseEntity.internalServerError()
                            .build();
                }

                return ResponseEntity.ok(
                        output.toArray()
                );

            } finally {
                aligned.release();
            }

        } finally {
            input.release();
        }
    }

    @PostMapping("/embedding")
    public ResponseEntity<?> generateEmbedding(
            @RequestParam("image")
            MultipartFile image) throws Exception {

        byte[] bytes = image.getBytes();

        Mat input = Imgcodecs.imdecode(
                new MatOfByte(bytes),
                Imgcodecs.IMREAD_COLOR
        );

        if (input.empty()) {
            throw new IOException(
                    "Could not decode image"
            );
        }

        try {

            List<FaceDetection> faces =
                    detectionService.detectFaces(image);

            if (faces.isEmpty()) {

                return ResponseEntity.badRequest()
                        .body(
                                Map.of(
                                        "error",
                                        "No face detected"
                                )
                        );
            }

            /*
             * For now, we use the first face.
             */
            FaceDetection face =
                    faces.get(0);

            /*
             * Align face to 112x112.
             */
            Mat aligned =
                    faceAligner.align(
                            input,
                            face.getLandmarks()
                    );

            try {

                /*
                 * Generate 512-D ArcFace embedding.
                 */
                float[] embedding =
                        embedder.generateEmbedding(
                                aligned
                        );

                return ResponseEntity.ok(
                        Map.of(
                                "embeddingSize",
                                embedding.length,
                                "embedding",
                                embedding
                        )
                );

            } finally {
                aligned.release();
            }

        } finally {
            input.release();
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(
            @RequestParam("image")
            MultipartFile image) throws Exception {

        double similarity =
                enrollmentService.verify(image);

        return ResponseEntity.ok(
                Map.of(
                        "similarity",
                        similarity
                )
        );
    }

    @PostMapping("/enroll-db")
    public ResponseEntity<?> enrollDatabase(
            @RequestParam("name")
            String name,

            @RequestParam("sessionId")
            String sessionId,

            @RequestParam("image")
            MultipartFile image)
            throws Exception {

        // Verify liveness before enrollment
        var livenessResult =
                livenessDetectionService.analyzeFrame(
                        sessionId,
                        image
                );

        if (!livenessResult.isLive()) {
            return ResponseEntity.badRequest()
                    .body(
                            Map.of(
                                    "error",
                                    "Liveness verification failed",
                                    "message",
                                    livenessResult.message(),
                                    "framesProcessed",
                                    livenessResult.framesProcessed(),
                                    "blinkCount",
                                    livenessResult.blinkCount(),
                                    "movementFrameCount",
                                    livenessResult.movementFrameCount()
                            )
                    );
        }

        Person person =
                faceRecognitionService.enroll(
                        name,
                        image
                );

        // Clear session after successful enrollment
        livenessDetectionService.clearSession(sessionId);

        return ResponseEntity.ok(
                Map.of(
                        "id",
                        person.getId(),

                        "name",
                        person.getName(),

                        "message",
                        "Face enrolled successfully with liveness verification"
                )
        );
    }

    @PostMapping("/recognize")
    public ResponseEntity<?> recognize(
            @RequestParam("image") MultipartFile image) throws Exception {

        FaceRecognitionService.MatchResult result =
                faceRecognitionService.recognize(image);

        if (result.person() == null) {

            return ResponseEntity.ok(Map.of(
                    "recognized", false,
                    "name", "UNKNOWN",
                    "similarity", result.similarity()
            ));
        }

        return ResponseEntity.ok(Map.of(
                "recognized", true,
                "personId", result.person().getId(),
                "name", result.person().getName(),
                "similarity", result.similarity()
        ));
    }

    @PostMapping("/live-frame")
    public ResponseEntity<?> analyzeLiveFrame(
            @RequestParam("image") MultipartFile image)
            throws IOException {

        List<FaceDetection> faces =
                detectionService.detectFaces(image);

        if (faces.isEmpty()) {

            return ResponseEntity.ok(
                    Map.of(
                            "facesDetected", 0
                    )
            );
        }

        FaceDetection face = faces.get(0);

        List<Map<String, Double>> landmarks =
                Arrays.stream(face.getLandmarks())
                        .map(point -> Map.of(
                                "x", point.x,
                                "y", point.y
                        ))
                        .toList();

        return ResponseEntity.ok(
                Map.of(
                        "facesDetected", faces.size(),
                        "confidence", face.getConfidence(),
                        "landmarks", landmarks
                )
        );
    }


    @PostMapping("/live-verify")
    public ResponseEntity<?> liveVerify(
            @RequestParam("sessionId") String sessionId,
            @RequestParam("image") MultipartFile image) throws Exception {

        // =========================================================
        // STEP 1
        // Verify that the liveness session has actually passed.
        // This prevents direct photo-upload bypass.
        // =========================================================

        boolean livenessPassed =
                liveLivenessService.isLivenessPassed(sessionId);

        if (!livenessPassed) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "recognized", false,
                                    "attendanceMarked", false,
                                    "message",
                                    "Liveness verification has not passed"
                            )
                    );
        }


        // =========================================================
        // STEP 2
        // Recognize the face and mark attendance.
        //
        // Existing service already performs:
        // SCRFD
        //   ↓
        // Face Alignment
        //   ↓
        // ArcFace
        //   ↓
        // Face matching
        //   ↓
        // AttendanceService.markPresent()
        // =========================================================

        FaceRecognitionService.AttendanceVerificationResult result =
                faceRecognitionService
                        .recognizeAndMarkAttendance(image);


        // =========================================================
        // STEP 3
        // Clear the liveness session after recognition attempt.
        //
        // This makes the liveness proof effectively one-time.
        // =========================================================

        liveLivenessService.clearSession(sessionId);


        // =========================================================
        // STEP 4
        // Unknown / not recognized
        // =========================================================

        if (!result.recognized()) {

            return ResponseEntity.ok(
                    Map.of(
                            "recognized", false,
                            "name", "UNKNOWN",
                            "similarity", result.similarity(),
                            "attendanceMarked", false
                    )
            );
        }


        // =========================================================
        // STEP 5
        // Recognized + attendance result
        // =========================================================

        return ResponseEntity.ok(
                Map.of(
                        "recognized", true,
                        "personId", result.personId(),
                        "name", result.name(),
                        "similarity", result.similarity(),
                        "attendanceMarked", true,
                        "status", "PRESENT",
                        "attendanceTime", result.attendanceTime()
                )
        );
    }

    @PostMapping("/persons/{personId}/embeddings")
    public ResponseEntity<?> addFaceEmbedding(
            @PathVariable Long personId,
            @RequestParam("image") MultipartFile image)
            throws Exception {

        var embedding =
                faceRecognitionService.addFaceEmbedding(
                        personId,
                        image
                );

        return ResponseEntity.ok(
                Map.of(
                        "personId", personId,
                        "embeddingId", embedding.getId(),
                        "message",
                        "Face sample added successfully"
                )
        );
    }

    @PostMapping("/liveness/check")
    public ResponseEntity<?> checkLiveness(
            @RequestParam("sessionId") String sessionId,
            @RequestParam("image") MultipartFile image)
            throws Exception {

        var result =
                livenessDetectionService.analyzeFrame(
                        sessionId,
                        image
                );

        return ResponseEntity.ok(
                Map.of(
                        "isLive", result.isLive(),
                        "movementDetected", result.movementDetected(),
                        "blinkDetected", result.blinkDetected(),
                        "framesProcessed", result.framesProcessed(),
                        "blinkCount", result.blinkCount(),
                        "movementFrameCount", result.movementFrameCount(),
                        "message", result.message()
                )
        );
    }

    @PostMapping("/liveness/reset")
    public ResponseEntity<?> resetLivenessSession(
            @RequestParam("sessionId") String sessionId) {

        livenessDetectionService.clearSession(sessionId);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Liveness session reset"
                )
        );
    }

    @PostMapping("/enroll")
    public ResponseEntity<?> enroll(
            @RequestParam("image") MultipartFile image) throws Exception {

        enrollmentService.enroll(image);

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message", "Face enrolled successfully"
                )
        );
    }

    @PostMapping("/enrollment/start")
    public ResponseEntity<?> startEnrollment() {

        EnrollmentSession session =
                enrollmentSessionService.start();

        return ResponseEntity.ok(
                Map.of(
                        "sessionId",
                        session.getId(),

                        "expiresAt",
                        session.getExpiresAt()
                )
        );
    }

    @PostMapping("/enrollment/complete")
    public ResponseEntity<?> completeEnrollment(

            @RequestParam("enrollmentSessionId")
            String enrollmentSessionId,

            @RequestParam("livenessSessionId")
            String livenessSessionId,

            @RequestParam("image")
            MultipartFile image)

            throws Exception {

        // =========================================================
        // STEP 1
        // Verify authenticated user's enrollment session
        // =========================================================

        enrollmentSessionService.getActive(
                enrollmentSessionId
        );


        // =========================================================
        // STEP 2
        // Verify server-side liveness
        // =========================================================

        boolean livenessPassed =
                liveLivenessService.isLivenessPassed(
                        livenessSessionId
                );

        if (!livenessPassed) {

            return ResponseEntity.badRequest()
                    .body(
                            Map.of(
                                    "success", false,
                                    "error",
                                    "Liveness verification has not passed"
                            )
                    );
        }


        // =========================================================
        // STEP 3
        // Generate ArcFace embedding
        // + duplicate check
        // + save FaceEmbedding
        // =========================================================

        Person person =
                faceRecognitionService
                        .enrollForCurrentUser(image);


        // =========================================================
        // STEP 4
        // Mark enrollment session completed
        // =========================================================

        enrollmentSessionService.complete(
                enrollmentSessionId
        );


        // =========================================================
        // STEP 5
        // Remove liveness session
        // =========================================================

        liveLivenessService.clearSession(
                livenessSessionId
        );


        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "personId", person.getId(),
                        "name", person.getName(),
                        "message",
                        "Face enrolled successfully"
                )
        );
    }

}