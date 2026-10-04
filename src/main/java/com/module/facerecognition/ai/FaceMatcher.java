package com.module.facerecognition.ai;

import org.springframework.stereotype.Component;

@Component
public class FaceMatcher {

    public double cosineSimilarity(
            float[] embedding1,
            float[] embedding2) {

        if (embedding1 == null || embedding2 == null) {
            throw new IllegalArgumentException(
                    "Embeddings cannot be null"
            );
        }

        if (embedding1.length != embedding2.length) {
            throw new IllegalArgumentException(
                    "Embedding dimensions must match"
            );
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < embedding1.length; i++) {

            dotProduct +=
                    embedding1[i] * embedding2[i];

            norm1 +=
                    embedding1[i] * embedding1[i];

            norm2 +=
                    embedding2[i] * embedding2[i];
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            throw new IllegalArgumentException(
                    "Cannot compare zero embeddings"
            );
        }

        return dotProduct /
                (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}