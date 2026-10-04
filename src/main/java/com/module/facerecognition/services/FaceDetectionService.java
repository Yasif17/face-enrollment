package com.module.facerecognition.services;

import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class FaceDetectionService {

    private final CascadeClassifier faceDetector;

    public FaceDetectionService() {

        // Load OpenCV
        OpenCV.loadLocally();

        // Load Haar Cascade model
        faceDetector = new CascadeClassifier(
                "src/main/resources/models/haarcascade_frontalface_default.xml"
        );

        if (faceDetector.empty()) {
            throw new RuntimeException("Failed to load face detection model");
        }
    }

    public int detectFaces(MultipartFile file) throws IOException {

        // Create temporary file
        Path tempFile = Files.createTempFile("face-", ".jpg");

        // Save uploaded image
        file.transferTo(tempFile.toFile());

        // Read image using OpenCV
        Mat image = Imgcodecs.imread(tempFile.toString());

        if (image.empty()) {
            throw new RuntimeException("Could not read image");
        }

        // Store detected faces
        MatOfRect faces = new MatOfRect();

        // Detect faces
        faceDetector.detectMultiScale(
                image,
                faces,
                1.1,
                5
        );

        // Delete temporary file
        Files.deleteIfExists(tempFile);

        return faces.toArray().length;
    }
}