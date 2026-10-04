package com.module.facerecognition.ai;

import org.opencv.core.Point;
import org.opencv.core.Rect2d;

public class FaceDetection {

    private final Rect2d boundingBox;
    private final float confidence;
    private final Point[] landmarks;

    public FaceDetection(
            Rect2d boundingBox,
            float confidence,
            Point[] landmarks) {

        this.boundingBox = boundingBox;
        this.confidence = confidence;
        this.landmarks = landmarks;
    }

    public Rect2d getBoundingBox() {
        return boundingBox;
    }

    public float getConfidence() {
        return confidence;
    }

    public Point[] getLandmarks() {
        return landmarks;
    }
}