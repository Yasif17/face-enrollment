package com.module.facerecognition.controllers;


import com.module.facerecognition.ai.FaceDetection;

import com.module.facerecognition.services.FaceAntiSpoofService;
import com.module.facerecognition.services.ScrfdDetectionService;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect2d;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/antispoof")
public class FaceAntiSpoofController {

    private final ScrfdDetectionService detectionService;
    private final FaceAntiSpoofService antiSpoofService;

    public FaceAntiSpoofController(
            ScrfdDetectionService detectionService,
            FaceAntiSpoofService antiSpoofService) {

        this.detectionService = detectionService;
        this.antiSpoofService = antiSpoofService;
    }

    @PostMapping("/check")
    public ResponseEntity<?> check(
            @RequestParam("image") MultipartFile image)
            throws Exception {

        List<FaceDetection> faces =
                detectionService.detectFaces(image);

        if (faces.isEmpty()) {
            return ResponseEntity.ok(
                    java.util.Map.of(
                            "status", "NO_FACE"
                    )
            );
        }

        if (faces.size() > 1) {
            return ResponseEntity.ok(
                    java.util.Map.of(
                            "status", "MULTIPLE_FACES"
                    )
            );
        }

        FaceDetection face = faces.get(0);

        byte[] bytes = image.getBytes();

        Mat frame = Imgcodecs.imdecode(
                new MatOfByte(bytes),
                Imgcodecs.IMREAD_COLOR
        );

        try {

            Rect2d box = face.getBoundingBox();

            int x = Math.max(0, (int) box.x);
            int y = Math.max(0, (int) box.y);

            int width = Math.min(
                    (int) box.width,
                    frame.cols() - x
            );

            int height = Math.min(
                    (int) box.height,
                    frame.rows() - y
            );

            Mat faceCrop = new Mat(
                    frame,
                    new org.opencv.core.Rect(
                            x,
                            y,
                            width,
                            height
                    )
            );

            try {

                float[] scores =
                        antiSpoofService.predict(faceCrop);

                return ResponseEntity.ok(
                        java.util.Map.of(
                                "status", "OK",
                                "class0", scores[0],
                                "class1", scores[1]
                        )
                );

            } finally {
                faceCrop.release();
            }

        } finally {
            frame.release();
        }
    }
}