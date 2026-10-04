package com.module.facerecognition.services;

import com.module.facerecognition.ai.ArcFaceEmbedder;
import com.module.facerecognition.ai.FaceAligner;
import com.module.facerecognition.ai.FaceDetection;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class FaceEnrollmentService {

    private final ScrfdDetectionService detectionService;
    private final FaceAligner faceAligner;
    private final ArcFaceEmbedder embedder;

    private float[] enrolledEmbedding;

    public FaceEnrollmentService(
            ScrfdDetectionService detectionService,
            FaceAligner faceAligner,
            ArcFaceEmbedder embedder) {

        this.detectionService =
                detectionService;

        this.faceAligner =
                faceAligner;

        this.embedder =
                embedder;
    }

    public void enroll(
            MultipartFile image)
            throws Exception {

        enrolledEmbedding =
                generateEmbedding(image);
    }

    public double verify(
            MultipartFile image)
            throws Exception {

        if (enrolledEmbedding == null) {

            throw new IllegalStateException(
                    "No face has been enrolled"
            );
        }

        float[] currentEmbedding =
                generateEmbedding(image);

        return cosineSimilarity(
                enrolledEmbedding,
                currentEmbedding
        );
    }

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

            List<FaceDetection> faces =
                    detectionService.detectFaces(image);

            if (faces.isEmpty()) {

                throw new IllegalArgumentException(
                        "No face detected"
                );
            }

            /*
             * POC:
             * use the first detected face.
             */
            FaceDetection face =
                    faces.get(0);

            Mat aligned =
                    faceAligner.align(
                            input,
                            face.getLandmarks()
                    );

            try {

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

    private double cosineSimilarity(
            float[] a,
            float[] b) {

        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {

            dot += a[i] * b[i];

            normA += a[i] * a[i];

            normB += b[i] * b[i];
        }

        return dot /
                (Math.sqrt(normA) *
                        Math.sqrt(normB));
    }
}