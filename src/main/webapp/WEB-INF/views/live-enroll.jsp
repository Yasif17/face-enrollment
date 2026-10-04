<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Live Face Enrollment</title>

    <link rel="manifest" href="/manifest.json">

    <meta name="theme-color" content="#0f172a">
    <meta name="mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-status-bar-style" content="default">
    <meta name="apple-mobile-web-app-title" content="Face Attendance">

    <style>
        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            font-family: "Segoe UI", Arial, sans-serif;
            min-height: 100vh;
            background:
                    radial-gradient(
                            circle at top,
                            #1e3a8a 0%,
                            #0f172a 45%,
                            #020617 100%
                    );
            color: #fff;
            padding: 20px;
        }

        .page {
            width: 100%;
            max-width: 760px;
            margin: 0 auto;
        }

        .header {
            text-align: center;
            margin: 25px 0 22px;
        }

        .header-icon {
            width: 58px;
            height: 58px;
            margin: 0 auto 12px;
            display: flex;
            align-items: center;
            justify-content: center;
            border-radius: 18px;
            background: rgba(59, 130, 246, .15);
            border: 1px solid rgba(96, 165, 250, .3);
            font-size: 28px;
        }

        h1 {
            font-size: 30px;
            margin-bottom: 8px;
        }

        .subtitle {
            color: #94a3b8;
            font-size: 15px;
            line-height: 1.5;
        }

        .camera-card {
            background: rgba(15, 23, 42, 0.88);
            border: 1px solid rgba(148, 163, 184, 0.18);
            border-radius: 26px;
            padding: 10px;
            box-shadow:
                    0 24px 60px rgba(0, 0, 0, 0.40),
                    0 0 0 1px rgba(255, 255, 255, 0.02);
            backdrop-filter: blur(16px);
        }

        .camera-wrapper {
            position: relative;
            overflow: hidden;
            border-radius: 20px;
            background: #000;
            box-shadow:
                    inset 0 0 0 1px rgba(255, 255, 255, 0.06);
        }

        video {
            display: block;
            width: 100%;
            aspect-ratio: 4 / 3;
            object-fit: cover;
            background: #000;
        }

        .camera-overlay {
            position: absolute;
            inset: 0;
            pointer-events: none;
            display: flex;
            align-items: center;
            justify-content: center;
        }

        .face-guide {
            width: 46%;
            aspect-ratio: 1 / 1.2;
            height: auto;
            border: 2px solid rgba(255, 255, 255, 0.75);
            border-radius: 48% 48% 45% 45%;
            box-shadow:
                    0 0 0 9999px rgba(0, 0, 0, 0.08),
                    0 0 24px rgba(255, 255, 255, 0.12);
            transition:
                    border-color 0.3s ease,
                    box-shadow 0.3s ease;
        }

        .camera-status {
            position: absolute;
            top: 14px;
            left: 14px;
            display: inline-flex;
            align-items: center;
            padding: 7px 12px;
            border-radius: 999px;
            background: rgba(15, 23, 42, 0.78);
            border: 1px solid rgba(148, 163, 184, 0.25);
            backdrop-filter: blur(10px);
            color: #e2e8f0;
            font-size: 12px;
            font-weight: 600;
            letter-spacing: 0.2px;
            box-shadow:
                    0 6px 18px rgba(0, 0, 0, 0.25);
        }
        .instruction-card {
            margin-top: 18px;
            padding: 20px;
            text-align: center;
            border-radius: 20px;
            background: rgba(30, 41, 59, 0.78);
            border: 1px solid rgba(148, 163, 184, 0.16);
            box-shadow:
                    0 10px 30px rgba(0, 0, 0, 0.16);
            backdrop-filter: blur(12px);
        }

        .instruction-label {
            font-size: 11px;
            text-transform: uppercase;
            letter-spacing: 1.6px;
            font-weight: 700;
            color: #94a3b8;
            margin-bottom: 9px;
        }

        #status {
            font-size: 18px;
            font-weight: 700;
            line-height: 1.5;
            color: #f8fafc;
            min-height: 27px;
            transition:
                    color 0.25s ease,
                    opacity 0.25s ease;
        }

        .progress-section {
            margin-top: 18px;
            padding: 18px;
            border-radius: 20px;
            background: rgba(15, 23, 42, 0.82);
            border: 1px solid rgba(148, 163, 184, 0.16);
            box-shadow:
                    0 10px 30px rgba(0, 0, 0, 0.18);
        }

        .progress-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 10px;
        }

        .progress-title {
            color: #cbd5e1;
            font-size: 14px;
            font-weight: 600;
        }

        #progressText {
            font-size: 14px;
            font-weight: 700;
            color: #38bdf8;
        }

        .progress-track {
            width: 100%;
            height: 11px;
            background: rgba(30, 41, 59, 0.95);
            border: 1px solid rgba(148, 163, 184, 0.08);
            border-radius: 999px;
            overflow: hidden;
            box-shadow:
                    inset 0 1px 3px rgba(0, 0, 0, 0.35);
        }

        #progressFill {
            width: 0%;
            height: 100%;
            border-radius: 999px;
            background: linear-gradient(
                    90deg,
                    #38bdf8,
                    #22c55e
            );
            box-shadow:
                    0 0 12px rgba(34, 197, 94, 0.25);
            transition: width 0.4s ease;
        }

        .progress-steps {
            display: flex;
            justify-content: space-between;
            margin-top: 10px;
            color: #64748b;
            font-size: 11px;
            font-weight: 600;
        }

        .progress-steps span {
            transition:
                    color 0.25s ease,
                    transform 0.25s ease;
        }

        .progress-steps span.active {
            color: #4ade80;
            font-weight: 700;
            transform: translateY(-1px);
        }

        .progress-steps span.active {
            color: #22c55e;
            font-weight: 700;
        }

        .controls {
            display: flex;
            gap: 12px;
            margin-top: 18px;
        }

        button {
            flex: 1;
            padding: 14px 18px;
            border: none;
            border-radius: 14px;
            font-size: 15px;
            font-weight: 700;
            cursor: pointer;
            transition:
                    transform .2s ease,
                    opacity .2s ease;
        }

        button:active {
            transform: scale(.98);
        }

        .start-btn {
            color: #fff;
            background:
                    linear-gradient(
                            135deg,
                            #2563eb,
                            #06b6d4
                    );
            border: 1px solid rgba(255, 255, 255, 0.12);
            box-shadow:
                    0 10px 28px rgba(37, 99, 235, 0.28);
            transition:
                    transform 0.2s ease,
                    opacity 0.2s ease,
                    box-shadow 0.2s ease,
                    filter 0.2s ease;
        }

        .start-btn:hover {
            filter: brightness(1.08);
            box-shadow:
                    0 12px 32px rgba(37, 99, 235, 0.36);
            transform: translateY(-1px);
        }

        .stop-btn {
            color: #e2e8f0;
            background: rgba(30, 41, 59, 0.9);
            border: 1px solid rgba(148, 163, 184, 0.20);
            transition:
                    transform 0.2s ease,
                    background 0.2s ease,
                    border-color 0.2s ease;
        }

        .stop-btn:hover {
            background: rgba(51, 65, 85, 0.95);
            border-color: rgba(148, 163, 184, 0.35);
        }
        .success-card {
            display: none;
            margin-top: 18px;
            padding: 26px 22px;
            text-align: center;
            border-radius: 22px;
            background:
                    linear-gradient(
                            145deg,
                            rgba(34, 197, 94, 0.14),
                            rgba(15, 23, 42, 0.72)
                    );
            border: 1px solid rgba(74, 222, 128, 0.35);
            box-shadow:
                    0 16px 40px rgba(0, 0, 0, 0.22),
                    0 0 30px rgba(34, 197, 94, 0.08);
            backdrop-filter: blur(14px);
            animation: enrollmentSuccess 0.45s ease-out;
        }

        .success-icon {
            width: 64px;
            height: 64px;
            margin: 0 auto 14px;
            display: flex;
            align-items: center;
            justify-content: center;
            border-radius: 50%;
            background: rgba(34, 197, 94, 0.14);
            border: 1px solid rgba(74, 222, 128, 0.3);
            color: #4ade80;
            font-size: 31px;
            box-shadow:
                    0 0 25px rgba(34, 197, 94, 0.16);
        }

        .success-card h2 {
            color: #4ade80;
            font-size: 21px;
            font-weight: 750;
            margin-bottom: 8px;
        }

        .success-card p {
            color: #a7b5c7;
            font-size: 14px;
            line-height: 1.6;
            max-width: 520px;
            margin: 0 auto;
        }

        .success-state .face-guide {
            border-color: #22c55e;
            box-shadow:
                    0 0 0 9999px rgba(0, 0, 0, .12),
                    0 0 30px rgba(34, 197, 94, .5);
        }

        @keyframes enrollmentSuccess {
            from {
                opacity: 0;
                transform: translateY(10px) scale(0.98);
            }

            to {
                opacity: 1;
                transform: translateY(0) scale(1);
            }
        }

        @media(max-width:600px) {

            body {
                padding: 12px;
            }

            .header {
                margin-top: 12px;
            }

            h1 {
                font-size: 24px;
            }

            .subtitle {
                font-size: 13px;
            }

            .camera-card {
                padding: 8px;
                border-radius: 18px;
            }

            .camera-wrapper {
                border-radius: 14px;
            }

            .face-guide {
                width: 56%;
                aspect-ratio: 1 / 1.2;
                height: auto;
            }

            #status {
                font-size: 17px;
            }

            .instruction-card,
            .progress-section {
                padding: 15px;
            }

            .controls {
                flex-direction: column;
            }

            button {
                width: 100%;
            }

            .page {
                max-width: 100%;
            }

            .header-icon {
                width: 52px;
                height: 52px;
                border-radius: 16px;
                font-size: 25px;
            }

            .camera-status {
                top: 10px;
                left: 10px;
                padding: 6px 10px;
                font-size: 11px;
            }

            .success-card {
                padding: 22px 16px;
            }

            .success-card h2 {
                font-size: 19px;
            }

            button {
                padding: 14px;
            }

        }
    </style>
</head>

<body>

<div class="page">

    <header class="header">

        <div class="header-icon">👤</div>

        <h1>Live Face Enrollment</h1>

        <p class="subtitle">
            Complete the liveness verification to securely enroll your face.
        </p>

    </header>

    <div class="camera-card">

        <div class="camera-wrapper">

            <video
                    id="video"
                    autoplay
                    muted
                    playsinline
                    width="480"
                    height="360">
            </video>

            <div class="camera-overlay">
                <div class="face-guide"></div>
            </div>

            <div
                    class="camera-status"
                    id="cameraIndicator">
                ● Camera inactive
            </div>

        </div>

        <div class="instruction-card">

            <div class="instruction-label">
                Liveness Instruction
            </div>

            <div id="status">
                Camera stopped
            </div>

        </div>

        <div class="progress-section">

            <div class="progress-header">

            <span class="progress-title">
                Liveness Progress
            </span>

                <span id="progressText">
                0%
            </span>

            </div>

            <div class="progress-track">
                <div id="progressFill"></div>
            </div>

            <div class="progress-steps">

            <span id="step0" class="active">
                Start
            </span>

                <span id="step50">
                50%
            </span>

                <span id="step100">
                Complete
            </span>

            </div>

        </div>

        <div class="controls">

            <button
                    class="start-btn"
                    onclick="startCamera()">
                📷 Start Camera
            </button>

            <button
                    class="stop-btn"
                    onclick="stopCamera()">
                ■ Stop Camera
            </button>

        </div>

        <div class="success-card" id="successCard">

            <div class="success-icon">
                ✓
            </div>

            <h2>
                Liveness Verified
            </h2>

            <p>
                Your live face verification was completed successfully.
            </p>

        </div>

    </div>

</div>

<script>

    function getAuthHeaders() {
        const token = localStorage.getItem("jwt");

        if (!token) {
            throw new Error("JWT token not found. Please login again.");
        }

        return {
            "Authorization": "Bearer " + token
        };
    }

    if ("serviceWorker" in navigator) {

        window.addEventListener("load", () => {

            navigator.serviceWorker
                .register("/sw.js")
                .then(registration => {

                    console.log(
                        "Service Worker registered:",
                        registration.scope
                    );

                })
                .catch(error => {

                    console.error(
                        "Service Worker registration failed:",
                        error
                    );

                });

        });

    }


    /* =========================
       DOM REFERENCES
       ========================= */

    const video =
        document.getElementById("video");

    const status =
        document.getElementById("status");

    const progressFill =
        document.getElementById("progressFill");

    const progressText =
        document.getElementById("progressText");

    const cameraIndicator =
        document.getElementById("cameraIndicator");

    const successCard =
        document.getElementById("successCard");

    const step0 =
        document.getElementById("step0");

    const step50 =
        document.getElementById("step50");

    const step100 =
        document.getElementById("step100");


    /* =========================
       STATE
       ========================= */

    let stream = null;

    let timer = null;

    let processing = false;

    let livenessSessionId = null;

    let enrollmentSessionId = null;

    /*
     * Prevents enrollment completion
     * from being submitted more than once.
     */
    let enrollmentCompleting = false;


    /* =========================
       START CAMERA
       ========================= */

    async function startCamera() {

        try {

            /*
             * Stop any previous camera/session.
             */
            stopCamera(false);

            /*
             * Allow a completely new attempt.
             */
            enrollmentCompleting = false;

            /*
             * Reset UI.
             */
            successCard.style.display = "none";

            document
                .querySelector(".camera-card")
                .classList
                .remove("success-state");

            progressFill.style.width = "0%";

            progressText.innerText = "0%";

            step0.classList.add("active");

            step50.classList.remove("active");

            step100.classList.remove("active");

            status.innerText =
                "Starting liveness...";

            cameraIndicator.innerText =
                "● Starting camera";


            /* =========================
               START ENROLLMENT SESSION
               ========================= */

            const enrollmentResponse = await fetch(
                "/api/faces/enrollment/start",
                {
                    method: "POST",
                    headers: getAuthHeaders()
                }
            );

            if (!enrollmentResponse.ok) {

                throw new Error(
                    "Enrollment session start failed: "
                    + enrollmentResponse.status
                );

            }

            const enrollmentSession =
                await enrollmentResponse.json();

            enrollmentSessionId =
                enrollmentSession.sessionId;

            console.log(
                "Enrollment session:",
                enrollmentSession
            );


            /* =========================
               START LIVENESS SESSION
               ========================= */

            const sessionResponse = await fetch(
                "/api/liveness/start",
                {
                    method: "POST",
                    headers: getAuthHeaders()
                }
            );

            if (!sessionResponse.ok) {

                throw new Error(
                    "Liveness start failed: "
                    + sessionResponse.status
                );

            }

            const session =
                await sessionResponse.json();

            livenessSessionId =
                session.sessionId;

            console.log(
                "Liveness session:",
                session
            );


            /* =========================
               OPEN WEBCAM
               ========================= */

            stream =
                await navigator.mediaDevices.getUserMedia({

                    video: {

                        width: {
                            ideal: 320
                        },

                        height: {
                            ideal: 240
                        },

                        facingMode: "user"

                    },

                    audio: false

                });


            video.srcObject = stream;

            await video.play();


            console.log(
                "Video:",
                video.videoWidth,
                "x",
                video.videoHeight
            );


            cameraIndicator.innerText =
                "● Camera active";

            status.innerText =
                "Camera ready - "
                + session.challenge;


            startFrameProcessing();


        } catch (error) {

            console.error(
                "Camera start error:",
                error
            );

            cameraIndicator.innerText =
                "● Camera error";

            status.innerText =
                "❌ Camera error: "
                + error.message;

        }

    }


    /* =========================
       FRAME PROCESSING
       ========================= */

    function startFrameProcessing() {

        if (timer !== null) {
            return;
        }

        timer =
            setInterval(
                captureAndSendFrame,
                300
            );

        console.log(
            "Frame processing started"
        );

    }


    /* =========================
       CAPTURE + SEND FRAME
       ========================= */

    async function captureAndSendFrame() {

        if (processing) {
            return;
        }

        if (!stream) {
            return;
        }

        if (!livenessSessionId) {
            return;
        }

        if (video.readyState < 2) {

            console.log(
                "Video not ready"
            );

            return;
        }

        if (
            video.videoWidth === 0 ||
            video.videoHeight === 0
        ) {

            console.log(
                "Video dimensions unavailable"
            );

            return;
        }


        /*
         * Lock frame processing.
         *
         * IMPORTANT:
         * We now keep this locked until
         * updateStatus() and enrollment
         * completion are finished.
         */
        processing = true;


        try {

            const canvas =
                document.createElement("canvas");

            canvas.width = 480;

            canvas.height = 360;


            const ctx =
                canvas.getContext(
                    "2d",
                    {
                        alpha: false
                    }
                );


            ctx.drawImage(
                video,
                0,
                0,
                480,
                360
            );


            const blob =
                await new Promise(resolve => {

                    canvas.toBlob(
                        resolve,
                        "image/jpeg",
                        0.70
                    );

                });


            if (!blob) {

                console.error(
                    "Could not create JPEG"
                );

                return;
            }


            console.log(
                "Captured frame:",
                blob.size,
                "bytes"
            );


            const formData =
                new FormData();

            formData.append(
                "image",
                blob,
                "live-frame.jpg"
            );

            formData.append(
                "sessionId",
                livenessSessionId
            );


            const requestStart =
                performance.now();


            const response = await fetch(
                "/api/liveness/frame",
                {
                    method: "POST",
                    headers: getAuthHeaders(),
                    body: formData
                }
            );


            if (!response.ok) {

                throw new Error(
                    "Frame API returned "
                    + response.status
                );

            }


            const result =
                await response.json();


            const requestTime =
                Math.round(
                    performance.now()
                    - requestStart
                );


            console.log(
                "Liveness response:",
                result,
                "Request time:",
                requestTime + " ms"
            );


            /*
             * IMPORTANT FIX:
             *
             * Await updateStatus().
             *
             * Previously this was:
             *
             * updateStatus(result);
             *
             * which allowed processing=false
             * while enrollment was still running.
             */
            await updateStatus(result);


        } catch (error) {

            console.error(
                "Frame processing error:",
                error
            );

            status.innerText =
                "⚠️ Frame processing error";


        } finally {

            /*
             * Release the frame lock only after
             * updateStatus/enrollment finishes.
             */
            processing = false;

        }

    }


    /* =========================
       UPDATE LIVENESS STATUS
       ========================= */

    async function updateStatus(result) {

        if (!result) {
            return;
        }


        /* =========================
           PROGRESS
           ========================= */

        if (
            typeof result.progress === "number"
        ) {

            const progress =
                Math.max(
                    0,
                    Math.min(
                        100,
                        result.progress
                    )
                );


            progressFill.style.width =
                progress + "%";


            progressText.innerText =
                progress + "%";


            if (progress >= 50) {

                step50.classList.add(
                    "active"
                );

            }


            if (progress >= 100) {

                step100.classList.add(
                    "active"
                );

            }

        }


        /* =========================
           NORMAL LIVENESS STATES
           ========================= */

        if (
            result.status === "NO_FACE"
        ) {

            status.innerText =
                "🔍 No face detected";

            return;
        }


        if (
            result.status === "MULTIPLE_FACES"
        ) {

            status.innerText =
                "❌ Multiple faces detected";

            return;
        }


        if (
            result.status === "LOW_CONFIDENCE"
        ) {

            status.innerText =
                "🔍 Face not clear";

            return;
        }


        if (
            result.status === "INVALID_LANDMARKS" ||
            result.status === "INVALID_FACE"
        ) {

            status.innerText =
                "⚠️ " + result.status;

            console.log(
                "Rejected:",
                result.status,
                result
            );

            return;
        }


        if (
            result.status === "CALIBRATING"
        ) {

            status.innerText =
                "📐 Keep your head straight...";

            return;
        }


        /* =========================
           LIVENESS PASSED
           ========================= */

        if (result.livenessPassed) {

            /*
             * Safety guard.
             *
             * If another frame somehow reports
             * livenessPassed while enrollment is
             * already being completed, ignore it.
             */
            if (enrollmentCompleting) {

                console.log(
                    "Enrollment completion already running."
                );

                return;
            }


            /*
             * STOP the repeating 300 ms timer.
             *
             * This is important because once
             * liveness has passed, we don't need
             * any more liveness frames.
             */
            if (timer !== null) {

                clearInterval(timer);

                timer = null;

                console.log(
                    "Frame processing stopped after liveness pass."
                );

            }


            /* =========================
               SUCCESS UI
               ========================= */

            progressFill.style.width =
                "100%";

            progressText.innerText =
                "100%";

            step0.classList.add(
                "active"
            );

            step50.classList.add(
                "active"
            );

            step100.classList.add(
                "active"
            );


            status.innerText =
                "✅ LIVENESS PASSED — Completing enrollment...";


            cameraIndicator.innerText =
                "● Verified";


            document
                .querySelector(".camera-card")
                .classList
                .add("success-state");


            /*
             * Complete enrollment exactly once.
             */
            await completeEnrollment();

            return;
        }


        /* =========================
           CHALLENGE STATUS
           ========================= */

        status.innerText =
            "👉 "
            + result.challenge
            + " | Progress: "
            + result.progress
            + "%";

    }


    /* =========================
       COMPLETE ENROLLMENT
       ========================= */

    async function completeEnrollment() {

        /*
         * Double protection.
         */
        if (enrollmentCompleting) {

            console.log(
                "Enrollment completion already in progress."
            );

            return;
        }


        /*
         * Lock completion.
         */
        enrollmentCompleting = true;


        /* =========================
           SESSION VALIDATION
           ========================= */

        if (!enrollmentSessionId) {

            enrollmentCompleting = false;

            status.innerText =
                "❌ Enrollment session missing";

            return;
        }


        if (!livenessSessionId) {

            enrollmentCompleting = false;

            status.innerText =
                "❌ Liveness session missing";

            return;
        }


        try {

            status.innerText =
                "🔐 Saving your face securely...";


            /* =========================
               VERIFY CAMERA FRAME
               ========================= */

            if (
                !stream ||
                video.readyState < 2 ||
                video.videoWidth === 0 ||
                video.videoHeight === 0
            ) {

                throw new Error(
                    "Camera frame is not available"
                );

            }


            /* =========================
               CAPTURE FINAL LIVE FRAME
               ========================= */

            const canvas =
                document.createElement("canvas");

            canvas.width = 480;

            canvas.height = 360;


            const ctx =
                canvas.getContext(
                    "2d",
                    {
                        alpha: false
                    }
                );


            ctx.drawImage(
                video,
                0,
                0,
                480,
                360
            );


            const blob =
                await new Promise(resolve => {

                    canvas.toBlob(
                        resolve,
                        "image/jpeg",
                        0.85
                    );

                });


            if (!blob) {

                throw new Error(
                    "Could not capture enrollment image"
                );

            }


            /* =========================
               BUILD FORM DATA
               ========================= */

            const formData =
                new FormData();


            formData.append(
                "enrollmentSessionId",
                enrollmentSessionId
            );


            formData.append(
                "livenessSessionId",
                livenessSessionId
            );


            formData.append(
                "image",
                blob,
                "live-enrollment.jpg"
            );


            /* =========================
               COMPLETE ENROLLMENT API
               ========================= */

            const response = await fetch(
                "/api/faces/enrollment/complete",
                {
                    method: "POST",
                    headers: getAuthHeaders(),
                    body: formData
                }
            );


            const result =
                await response.json();


            console.log(
                "Enrollment result:",
                result
            );


            if (
                !response.ok ||
                !result.success
            ) {

                throw new Error(
                    result.error ||
                    result.message ||
                    "Face enrollment failed"
                );

            }


            /* =========================
               ENROLLMENT SUCCESS
               ========================= */

            status.innerText =
                "✅ Face enrollment completed";


            cameraIndicator.innerText =
                "● Enrollment complete";


            successCard.style.display =
                "block";


            successCard
                .querySelector("h2")
                .innerText =
                "Face Enrolled Successfully";


            successCard
                .querySelector("p")
                .innerText =
                result.message ||
                "Your face has been securely enrolled.";


            document
                .querySelector(".camera-card")
                .classList
                .add("success-state");


            /*
             * Enrollment is now finished.
             *
             * This stops the webcam and clears
             * the liveness session.
             */
            stopCamera(false);

            setTimeout(() => {
                window.location.href = "/live";
            }, 1000);


        } catch (error) {

            console.error(
                "Enrollment error:",
                error
            );


            /*
             * Allow a new attempt after failure.
             */
            enrollmentCompleting = false;


            status.innerText =
                "❌ Enrollment failed";


            cameraIndicator.innerText =
                "● Enrollment error";


            successCard.style.display =
                "block";


            successCard
                .querySelector("h2")
                .innerText =
                "Enrollment Failed";


            successCard
                .querySelector("p")
                .innerText =
                error.message;


            document
                .querySelector(".camera-card")
                .classList
                .remove("success-state");

        }

    }


    /* =========================
       STOP CAMERA
       ========================= */

    function stopCamera(
        clearStatus = true
    ) {

        /*
         * Stop frame timer.
         */
        if (timer !== null) {

            clearInterval(timer);

            timer = null;

        }


        /*
         * Stop webcam tracks.
         */
        if (stream !== null) {

            stream
                .getTracks()
                .forEach(track => {
                    track.stop();
                });

            stream = null;

        }


        /*
         * Detach video.
         */
        video.srcObject = null;


        /*
         * Release processing lock.
         */
        processing = false;


        /*
         * Liveness session must not be reused
         * after the camera is stopped.
         */
        livenessSessionId = null;


        if (clearStatus) {

            cameraIndicator.innerText =
                "● Camera inactive";

            status.innerText =
                "Camera stopped";

        }

    }

</script>

</body>
</html>