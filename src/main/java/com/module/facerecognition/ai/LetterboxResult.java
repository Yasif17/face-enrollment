package com.module.facerecognition.ai;

import org.opencv.core.Mat;

public class LetterboxResult {

    private final Mat image;
    private final double scale;
    private final double padX;
    private final double padY;

    public LetterboxResult(
            Mat image,
            double scale,
            double padX,
            double padY) {

        this.image = image;
        this.scale = scale;
        this.padX = padX;
        this.padY = padY;
    }

    public Mat getImage() {
        return image;
    }

    public double getScale() {
        return scale;
    }

    public double getPadX() {
        return padX;
    }

    public double getPadY() {
        return padY;
    }
}