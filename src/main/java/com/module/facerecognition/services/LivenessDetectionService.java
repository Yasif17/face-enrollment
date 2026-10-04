package com.module.facerecognition.services;

import com.module.facerecognition.ai.FaceDetection;
import org.opencv.core.Point;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class LivenessDetectionService {

    private final ScrfdDetectionService detectionService;

    // Liveness detection thresholds
    private static final int MIN_FRAMES_REQUIRED = 10;
    private static final double MOVEMENT_THRESHOLD = 2.0; // Minimum pixel movement (reduced for sensitivity)
    private static final double BLINK_THRESHOLD = 0.20; // Eye aspect ratio threshold for blink (reduced for sensitivity)
    private static final int REQUIRED_BLINKS = 1; // Minimum blinks required
    private static final int REQUIRED_MOVEMENT_FRAMES = 5; // Minimum frames with movement

    // Session storage for tracking liveness
    private static class LivenessSession {
        List<Point[]> landmarkHistory = new ArrayList<>();
        List<Double> earHistory = new ArrayList<>();
        int blinkCount = 0;
        int movementFrameCount = 0;
        boolean eyesClosed = false;
    }

    private final java.util.Map<String, LivenessSession> sessions = new java.util.concurrent.ConcurrentHashMap<>();

    public LivenessDetectionService(ScrfdDetectionService detectionService) {
        this.detectionService = detectionService;
    }

    public LivenessResult analyzeFrame(String sessionId, MultipartFile image) throws Exception {
        List<FaceDetection> faces = detectionService.detectFaces(image);

        if (faces.isEmpty()) {
            return new LivenessResult(false, false, false, 0, 0, 0, "No face detected");
        }

        if (faces.size() > 1) {
            return new LivenessResult(false, false, false, 0, 0, 0, "Multiple faces detected");
        }

        FaceDetection face = faces.get(0);
        Point[] landmarks = face.getLandmarks();

        LivenessSession session = sessions.computeIfAbsent(sessionId, k -> new LivenessSession());

        // Calculate eye aspect ratio for blink detection
        double ear = calculateEyeAspectRatio(landmarks);
        session.earHistory.add(ear);

        // Detect blink
        boolean blinked = detectBlink(session, ear);

        // Detect movement
        boolean moved = detectMovement(session, landmarks);

        session.landmarkHistory.add(landmarks);

        // Check if liveness requirements are met
        boolean isLive = session.landmarkHistory.size() >= MIN_FRAMES_REQUIRED
                && session.blinkCount >= REQUIRED_BLINKS
                && session.movementFrameCount >= REQUIRED_MOVEMENT_FRAMES;

        return new LivenessResult(
                isLive,
                moved,
                blinked,
                session.landmarkHistory.size(),
                session.blinkCount,
                session.movementFrameCount,
                isLive ? "Liveness verified" : "Collecting more frames..."
        );
    }

    public void clearSession(String sessionId) {
        sessions.remove(sessionId);
    }

    private double calculateEyeAspectRatio(Point[] landmarks) {
        // With 5-point landmarks: left eye (0), right eye (1), nose (2), left mouth (3), right mouth (4)
        // Approximate EAR using eye landmarks
        Point leftEye = landmarks[0];
        Point rightEye = landmarks[1];

        // Simple approximation: distance between eyes as baseline
        double eyeDistance = Math.sqrt(
                Math.pow(rightEye.x - leftEye.x, 2) +
                Math.pow(rightEye.y - leftEye.y, 2)
        );

        // Use vertical distance from eyes to nose as proxy for eye opening
        Point nose = landmarks[2];
        double leftEyeToNose = Math.sqrt(
                Math.pow(nose.x - leftEye.x, 2) +
                Math.pow(nose.y - leftEye.y, 2)
        );
        double rightEyeToNose = Math.sqrt(
                Math.pow(nose.x - rightEye.x, 2) +
                Math.pow(nose.y - rightEye.y, 2)
        );

        // Normalize EAR
        double ear = (leftEyeToNose + rightEyeToNose) / (2 * eyeDistance);
        return ear;
    }

    private boolean detectBlink(LivenessSession session, double currentEar) {
        boolean blinked = false;

        if (currentEar < BLINK_THRESHOLD && !session.eyesClosed) {
            // Eye just closed
            session.eyesClosed = true;
        } else if (currentEar >= BLINK_THRESHOLD && session.eyesClosed) {
            // Eye just opened - blink detected
            session.eyesClosed = false;
            session.blinkCount++;
            blinked = true;
        }

        return blinked;
    }

    private boolean detectMovement(LivenessSession session, Point[] currentLandmarks) {
        if (session.landmarkHistory.isEmpty()) {
            return false;
        }

        Point[] previousLandmarks = session.landmarkHistory.get(session.landmarkHistory.size() - 1);
        double totalMovement = 0.0;

        for (int i = 0; i < currentLandmarks.length; i++) {
            double dx = currentLandmarks[i].x - previousLandmarks[i].x;
            double dy = currentLandmarks[i].y - previousLandmarks[i].y;
            totalMovement += Math.sqrt(dx * dx + dy * dy);
        }

        double avgMovement = totalMovement / currentLandmarks.length;

        if (avgMovement > MOVEMENT_THRESHOLD) {
            session.movementFrameCount++;
            return true;
        }

        return false;
    }

    public record LivenessResult(
            boolean isLive,
            boolean movementDetected,
            boolean blinkDetected,
            int framesProcessed,
            int blinkCount,
            int movementFrameCount,
            String message
    ) {
    }
}
