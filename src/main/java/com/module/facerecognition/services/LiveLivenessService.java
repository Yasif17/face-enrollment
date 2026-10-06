package com.module.facerecognition.services;

import com.module.facerecognition.ai.FaceDetection;
import com.module.facerecognition.auth.security.CurrentUserService;
import org.opencv.core.Point;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LiveLivenessService {

    private final ScrfdDetectionService detectionService;
    private final EyeStateModelService eyeStateModelService;

    private final ConcurrentHashMap<String, LivenessSession> sessions =
            new ConcurrentHashMap<>();
    private final CurrentUserService currentUserService;

    public LiveLivenessService(
            ScrfdDetectionService detectionService,
            CurrentUserService currentUserService,
            EyeStateModelService eyeStateModelService) {

        this.detectionService = detectionService;
        this.currentUserService = currentUserService;
        this.eyeStateModelService = eyeStateModelService;
    }

    // =========================================================
    // START LIVENESS SESSION
    // =========================================================

    public StartResult start() {

        String sessionId =
                UUID.randomUUID().toString();

        LivenessSession session =
                new LivenessSession();

        sessions.put(sessionId, session);

        return new StartResult(
                sessionId,
                session.challenge
        );
    }

    // =========================================================
    // PROCESS CAMERA FRAME
    // =========================================================

    public FrameResult processFrame(
            String sessionId,
            MultipartFile image)
            throws Exception {

        LivenessSession session =
                sessions.get(sessionId);

        if (session == null) {

            throw new IllegalArgumentException(
                    "Invalid liveness session"
            );
        }

        // =====================================================
        // SCRFD FACE DETECTION
        // =====================================================

        long detectionStart =
                System.currentTimeMillis();

        List<FaceDetection> faces =
                detectionService.detectFaces(image);

        long detectionTime =
                System.currentTimeMillis()
                        - detectionStart;

        System.out.println(
                "SCRFD detection = "
                        + detectionTime
                        + " ms"
        );

        // -----------------------------------------------------
        // NO FACE
        // -----------------------------------------------------

        if (faces.isEmpty()) {

            return new FrameResult(
                    false,
                    "NO_FACE",
                    session.challenge,
                    session.progress,
                    false
            );
        }

        // -----------------------------------------------------
        // MULTIPLE FACES
        // -----------------------------------------------------

        if (faces.size() > 1) {

            return new FrameResult(
                    false,
                    "MULTIPLE_FACES",
                    session.challenge,
                    session.progress,
                    false
            );
        }

        FaceDetection face =
                faces.get(0);

        // =====================================================
        // LANDMARK VALIDATION
        // =====================================================

        Point[] landmarks =
                face.getLandmarks();

        if (landmarks == null ||
                landmarks.length != 5) {

            return new FrameResult(
                    false,
                    "INVALID_LANDMARKS",
                    session.challenge,
                    session.progress,
                    false
            );
        }

        EyeStateModelService.EyeProbabilities eyeState =
                eyeStateModelService.classify(image, face);

        /*
         * SCRFD landmarks:
         *
         * 0 = left eye
         * 1 = right eye
         * 2 = nose
         * 3 = left mouth
         * 4 = right mouth
         */

        Point leftEye =
                landmarks[0];

        Point rightEye =
                landmarks[1];

        Point nose =
                landmarks[2];

        boolean blinkDetected =
                updateBlinkChallenge(session, eyeState);

        // =====================================================
        // HEAD TURN MEASUREMENT
        // =====================================================

        /*
         * Calculate horizontal distance
         * between the two eyes.
         */
        double eyeDistance =
                Math.abs(
                        rightEye.x -
                                leftEye.x
                );

        if (eyeDistance < 1.0) {

            return new FrameResult(
                    false,
                    "INVALID_FACE",
                    session.challenge,
                    session.progress,
                    false
            );
        }

        /*
         * Calculate center between the eyes.
         */
        double eyeCenterX =
                (leftEye.x +
                        rightEye.x) / 2.0;

        /*
         * Calculate horizontal position
         * of nose relative to the eyes.
         *
         * This is scale-independent.
         */
        double noseOffset =
                (nose.x -
                        eyeCenterX)
                        / eyeDistance;

        session.lastNoseOffset =
                noseOffset;

        System.out.println(
                "Challenge = "
                        + session.challenge
                        + " | EyeCenter = "
                        + eyeCenterX
                        + " | Nose = "
                        + nose.x
                        + " | NoseOffset = "
                        + noseOffset
        );

        // =====================================================
        // CALIBRATION
        // =====================================================

        /*
         * User keeps the head straight.
         *
         * Collect 7 samples.
         */
        if (session.calibrationSamples.size() < 7) {

            session.calibrationSamples.add(
                    noseOffset
            );

            System.out.println(
                    "Calibration sample "
                            + session.calibrationSamples.size()
                            + "/7"
            );

            return new FrameResult(
                    true,
                    "CALIBRATING",
                    "KEEP_STRAIGHT",
                    0,
                    false
            );
        }

        // =====================================================
        // CALCULATE MEDIAN BASELINE
        // =====================================================

        if (!session.calibrationComplete) {

            List<Double> sortedSamples =
                    new ArrayList<>(
                            session.calibrationSamples
                    );

            Collections.sort(
                    sortedSamples
            );

            int middle =
                    sortedSamples.size() / 2;

            session.neutralNoseOffset =
                    sortedSamples.get(middle);

            session.calibrationComplete =
                    true;

            System.out.println(
                    "Calibration values = "
                            + sortedSamples
            );

            System.out.println(
                    "Neutral baseline = "
                            + session.neutralNoseOffset
            );
        }

        // =====================================================
        // CALCULATE MOVEMENT
        // =====================================================

        /*
         * Positive movement:
         *     nose moved toward right
         *
         * Negative movement:
         *     nose moved toward left
         */
        double movement =
                noseOffset -
                        session.neutralNoseOffset;

        System.out.println(
                "Challenge = "
                        + session.challenge
                        + " | NoseOffset = "
                        + noseOffset
                        + " | Neutral = "
                        + session.neutralNoseOffset
                        + " | Movement = "
                        + movement
        );

        // =====================================================
        // TURN LEFT
        // =====================================================

        if (session.challenge.equals("TURN_LEFT")) {

            /*
             * Negative movement means
             * head moved toward left.
             */
            if (movement < -0.12) {

                session.leftConfirmations++;

                System.out.println(
                        "LEFT confirmation = "
                                + session.leftConfirmations
                );
            }

            /*
             * We intentionally DO NOT reset the
             * confirmation when the user briefly
             * returns toward center.
             *
             * This makes the live camera more tolerant
             * to SCRFD landmark fluctuations.
             */

            if (session.leftConfirmations >= 3) {

                session.challenge =
                        "TURN_RIGHT";

                session.progress =
                        50;

                session.leftConfirmations =
                        0;

                System.out.println(
                        "================================="
                );

                System.out.println(
                        "LEFT TURN PASSED"
                );

                System.out.println(
                        "Now ask user to TURN RIGHT"
                );

                System.out.println(
                        "================================="
                );
            }
        }

        // =====================================================
        // TURN RIGHT
        // =====================================================

        else if (session.challenge.equals("TURN_RIGHT")) {

            /*
             * Positive movement means
             * head moved toward right.
             */
            if (movement > 0.12) {

                session.rightConfirmations++;

                System.out.println(
                        "RIGHT confirmation = "
                                + session.rightConfirmations
                );
            }

            /*
             * Don't reset on a temporary
             * landmark fluctuation.
             */

            if (session.rightConfirmations >= 3) {

                session.challenge = "BLINK";
                session.progress = 80;

                session.rightConfirmations =
                        0;

                System.out.println(
                        "================================="
                );

                System.out.println(
                        "HEAD TURNS PASSED - NOW ASK USER TO BLINK"
                );

                System.out.println(
                        "================================="
                );
            }
        }

        // =====================================================
        // RETURN RESPONSE
        // =====================================================

        return new FrameResult(
                true,
                "FACE_DETECTED",
                session.challenge,
                session.progress,
                session.livenessPassed,
                blinkDetected,
                eyeState.leftOpen(),
                eyeState.rightOpen()
        );
    }

    private boolean updateBlinkChallenge(
            LivenessSession session,
            EyeStateModelService.EyeProbabilities eyeState) {

        if (!"BLINK".equals(session.challenge)) {
            return false;
        }

        boolean bothOpen =
                eyeState.leftOpen() >= 0.65f &&
                eyeState.rightOpen() >= 0.65f;
        boolean bothClosed =
                eyeState.leftOpen() <= 0.35f &&
                eyeState.rightOpen() <= 0.35f;

        if (bothOpen && !session.blinkArmed) {
            session.blinkArmed = true;
            return false;
        }

        if (session.blinkArmed && bothClosed) {
            session.eyesClosed = true;
            return false;
        }

        if (session.blinkArmed && session.eyesClosed && bothOpen) {
            session.challenge = "COMPLETED";
            session.progress = 100;
            session.livenessPassed = true;
            session.blinkArmed = false;
            session.eyesClosed = false;
            return true;
        }

        return false;
    }

    // =========================================================
// CHECK LIVENESS
// =========================================================

    public boolean isLivenessPassed(String sessionId) {

        LivenessSession session =
                sessions.get(sessionId);

        if (session == null) {
            return false;
        }

        return session.livenessPassed;
    }

    public void clearSession(String sessionId) {
        sessions.remove(sessionId);
    }

    // =========================================================
    // START RESULT
    // =========================================================

    public record StartResult(
            String sessionId,
            String challenge
    ) {
    }

    // =========================================================
    // FRAME RESULT
    // =========================================================

    public record FrameResult(
            boolean faceDetected,
            String status,
            String challenge,
            int progress,
            boolean livenessPassed,
            boolean blinkDetected,
            float leftEyeOpenProbability,
            float rightEyeOpenProbability
    ) {
        public FrameResult(
                boolean faceDetected,
                String status,
                String challenge,
                int progress,
                boolean livenessPassed) {
            this(faceDetected, status, challenge, progress, livenessPassed, false, 0.0f, 0.0f);
        }
    }



    // =========================================================
    // LIVENESS SESSION
    // =========================================================

    private static class LivenessSession {

        private String challenge =
                "TURN_LEFT";

        private int progress =
                0;

        private boolean livenessPassed =
                false;

        private boolean blinkArmed = false;
        private boolean eyesClosed = false;

        /*
         * Last calculated nose position.
         */
        private double lastNoseOffset =
                0.0;

        /*
         * Calibration samples.
         */
        private final List<Double> calibrationSamples =
                new ArrayList<>();

        /*
         * Median neutral nose position.
         */
        private double neutralNoseOffset =
                0.0;

        /*
         * Indicates calibration is finished.
         */
        private boolean calibrationComplete =
                false;

        /*
         * LEFT confirmation count.
         */
        private int leftConfirmations =
                0;

        /*
         * RIGHT confirmation count.
         */
        private int rightConfirmations =
                0;
        
        private Long userId;
    }
}
