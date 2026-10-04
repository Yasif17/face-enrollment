package com.module.facerecognition.exceptions;

public class FaceAlreadyRegisteredException extends RuntimeException {
    public FaceAlreadyRegisteredException(String message) {
        super(message);
    }

    public FaceAlreadyRegisteredException() {
        super("Face already registered");
    }

}
