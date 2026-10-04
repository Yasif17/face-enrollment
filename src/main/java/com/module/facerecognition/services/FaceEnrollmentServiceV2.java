//package com.module.facerecognition.services;
//
//
//import com.module.facerecognition.ai.ArcFaceEmbedder;
//import com.module.facerecognition.ai.FaceAligner;
//import com.module.facerecognition.ai.FaceMatcher;
//import com.module.facerecognition.auth.entities.AppUser;
//import com.module.facerecognition.auth.security.CurrentUserService;
//import com.module.facerecognition.entities.FaceEmbedding;
//import com.module.facerecognition.entities.Person;
//import com.module.facerecognition.exceptions.FaceAlreadyRegisteredException;
//import com.module.facerecognition.repositories.FaceEmbeddingRepository;
//import com.module.facerecognition.repositories.PersonRepository;
//import jakarta.transaction.Transactional;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.util.List;
//
//@Service
//public class FaceEnrollmentServiceV2 {
//
//    private static final double DUPLICATE_FACE_THRESHOLD = 0.75;
//
//    private final CurrentUserService currentUserService;
//    private final PersonRepository personRepository;
//    private final FaceEmbeddingRepository faceEmbeddingRepository;
//
//    private final ScrfdDetectionService scrfdDetectionService;
//    private final FaceAligner faceAligner;
//    private final ArcFaceEmbedder arcFaceEmbedder;
//
//    public FaceEnrollmentServiceV2(
//            CurrentUserService currentUserService,
//            PersonRepository personRepository,
//            FaceEmbeddingRepository faceEmbeddingRepository,
//            ScrfdDetectionService scrfdDetectionService,
//            FaceAligner faceAligner,
//            ArcFaceEmbedder arcFaceEmbedder
//    ) {
//        this.currentUserService = currentUserService;
//        this.personRepository = personRepository;
//        this.faceEmbeddingRepository = faceEmbeddingRepository;
//        this.scrfdDetectionService = scrfdDetectionService;
//        this.faceAligner = faceAligner;
//        this.arcFaceEmbedder = arcFaceEmbedder;
//    }
//
//    @Transactional
//    public EnrollmentResult enroll(MultipartFile image) {
//
//        AppUser user =
//                currentUserService.getCurrentUser();
//
//        if (image == null || image.isEmpty()) {
//            throw new IllegalArgumentException(
//                    "Image is required"
//            );
//        }
//
//        float[] embedding =
//                generateEmbedding(image);
//
//        /*
//         * IMPORTANT:
//         *
//         * Compare this face against every existing
//         * enrolled face.
//         *
//         * We do NOT tell the attacker which user
//         * owns the matching face.
//         */
//
//        List<FaceEmbedding> existing =
//                faceEmbeddingRepository.findAll();
//
//        for (FaceEmbedding existingEmbedding : existing) {
//
//            float[] stored =
//                    parseEmbedding(
//                            existingEmbedding.getEmbedding()
//                    );
//
//            double similarity =
//                    FaceMatcher.cosineSimilarity(
//                            embedding,
//                            stored
//                    );
//
//            if (similarity >= DUPLICATE_FACE_THRESHOLD) {
//
//                throw new FaceAlreadyRegisteredException();
//            }
//        }
//
//        Person person =
//                personRepository
//                        .findByUserId(user.getId())
//                        .orElseGet(() ->
//                                createPerson(user)
//                        );
//
//        FaceEmbedding faceEmbedding =
//                new FaceEmbedding(
//                        serializeEmbedding(embedding)
//                );
//
//        person.addFaceEmbedding(faceEmbedding);
//
//        personRepository.save(person);
//
//        return new EnrollmentResult(
//                true,
//                person.getId(),
//                user.getName(),
//                "Face enrolled successfully"
//        );
//    }
//
//    private Person createPerson(AppUser user) {
//
//        Person person =
//                new Person(user.getName());
//
//        person.setUser(user);
//
//        return person;
//    }
//
//    private float[] generateEmbedding(
//            MultipartFile image
//    ) {
//
//        // Reuse your existing SCRFD → alignment → ArcFace pipeline.
//
//        var detections =
//                scrfdDetectionService.detect(image);
//
//        if (detections.size() != 1) {
//
//            throw new IllegalArgumentException(
//                    "Exactly one face must be visible"
//            );
//        }
//
//        var detection =
//                detections.get(0);
//
//        var aligned =
//                faceAligner.align(
//                        image,
//                        detection
//                );
//
//        return arcFaceEmbedder.generateEmbedding(
//                aligned
//        );
//    }
//
//    private String serializeEmbedding(
//            float[] embedding
//    ) {
//
//        StringBuilder builder =
//                new StringBuilder();
//
//        for (int i = 0; i < embedding.length; i++) {
//
//            if (i > 0) {
//                builder.append(",");
//            }
//
//            builder.append(embedding[i]);
//        }
//
//        return builder.toString();
//    }
//
//    private float[] parseEmbedding(
//            String value
//    ) {
//
//        String[] parts =
//                value.split(",");
//
//        float[] result =
//                new float[parts.length];
//
//        for (int i = 0; i < parts.length; i++) {
//
//            result[i] =
//                    Float.parseFloat(parts[i]);
//        }
//
//        return result;
//    }
//
//    public record EnrollmentResult(
//            boolean success,
//            Long personId,
//            String name,
//            String message
//    ) {
//    }
//}