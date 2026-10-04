package com.module.facerecognition.services;

import ai.onnxruntime.OrtException;
import com.module.facerecognition.ai.FaceDetection;
import com.module.facerecognition.ai.ScrfdFaceDetector;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class ScrfdDetectionService {

    private final ScrfdFaceDetector detector;

    public ScrfdDetectionService(ScrfdFaceDetector detector) {
        this.detector = detector;
    }

    public List<FaceDetection> detectFaces(
            MultipartFile file) throws IOException {

        // Convert uploaded image to byte array
        byte[] bytes = file.getBytes();

        // Convert bytes to OpenCV Mat
        Mat image = Imgcodecs.imdecode(
                new MatOfByte(bytes),
                Imgcodecs.IMREAD_COLOR
        );

        if (image.empty()) {
            throw new IOException("Could not decode image");
        }

        try {
            return detector.detect(image);
        } catch (OrtException e) {
            throw new RuntimeException(e);
        } finally {
            image.release();
        }
    }
}