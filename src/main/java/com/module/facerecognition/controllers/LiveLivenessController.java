package com.module.facerecognition.controllers;

import com.module.facerecognition.services.LiveLivenessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/liveness")
public class LiveLivenessController {

    private final LiveLivenessService livenessService;

    public LiveLivenessController(
            LiveLivenessService livenessService) {

        this.livenessService = livenessService;
    }

    @PostMapping("/start")
    public ResponseEntity<?> start() {

        return ResponseEntity.ok(
                livenessService.start()
        );
    }

    @PostMapping("/frame")
    public ResponseEntity<?> processFrame(
            @RequestParam("sessionId") String sessionId,
            @RequestParam("image") MultipartFile image)
            throws Exception {

        return ResponseEntity.ok(
                livenessService.processFrame(
                        sessionId,
                        image
                )
        );
    }

}
