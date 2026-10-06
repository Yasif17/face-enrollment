<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="true" %>
<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport"
          content="width=device-width, initial-scale=1.0">

    <title>Live Face Attendance</title>

    <link rel="manifest" href="/manifest.json">

    <meta name="theme-color" content="#0f172a">
    <meta name="mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-status-bar-style" content="default">
    <meta name="apple-mobile-web-app-title" content="Face Attendance">

    <style>

        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            min-height: 100vh;

            font-family: "Segoe UI",
            Arial,
            sans-serif;

            background: radial-gradient(
                    circle at top,
                    #1e3a8a 0%,
                    #0f172a 45%,
                    #020617 100%
            );

            color: #ffffff;

            padding: 20px;
        }

        .page {
            width: 100%;
            max-width: 760px;
            margin: 0 auto;
        }

        /* =========================
           HEADER
        ========================= */

        .header {
            text-align: center;
            margin: 25px 0 22px;
        }

        #video {
            transform: scaleX(-1) !important;
            transform-origin: center;
        }

        .header-icon {
            width: 58px;
            height: 58px;

            margin: 0 auto 12px;

            display: flex;
            align-items: center;
            justify-content: center;

            border-radius: 18px;

            background: rgba(59, 130, 246, 0.15);

            border: 1px solid rgba(96, 165, 250, 0.3);

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

        /* =========================
           CAMERA
        ========================= */

        .camera-card {
            background: rgba(15, 23, 42, 0.88);
            border: 1px solid rgba(148, 163, 184, 0.18);
            border-radius: 26px;
            padding: 10px;
            box-shadow: 0 24px 60px rgba(0, 0, 0, 0.40),
            0 0 0 1px rgba(255, 255, 255, 0.02);
            backdrop-filter: blur(16px);
        }

        .camera-wrapper {
            position: relative;
            width: min(360px, 90vw);
            aspect-ratio: 1 / 1;
            margin: 0 auto;
            overflow: hidden;
            border-radius: 50%;
            background: #000;
        }

        video {
            display: block;
            width: 100%;
            height: 100%;
            object-fit: cover;
            border-radius: 50%;
            background: #000;
        }

        canvas {
            display: none;
        }

        /* =========================
           CAMERA OVERLAY
        ========================= */

        .camera-overlay {
            position: absolute;

            inset: 0;

            pointer-events: none;

            display: flex;

            align-items: center;

            justify-content: center;
        }

        .face-guide {
            position: absolute;
            top: 50%;
            left: 50%;
            width: 92%;
            aspect-ratio: 1;
            transform: translate(-50%, -50%);
            border: 2px solid rgba(255, 255, 255, 0.75);
            border-radius: 50%;
            box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.08),
            0 0 24px rgba(255, 255, 255, 0.12);
            pointer-events: none;
        }

        .face-guide::after {
            content: "";

            position: absolute;

            left: 8%;

            width: 84%;

            height: 2px;

            top: 10%;

            background: rgba(255, 255, 255, 0.65);

            box-shadow:
                    0 0 12px rgba(56, 189, 248, 0.45);

            animation: scanLine 2.8s ease-in-out infinite;
        }

        @keyframes scanLine {
            0%, 100% {
                top: 10%;
                opacity: 0.35;
            }

            50% {
                top: 88%;
                opacity: 0.85;
            }
        }

        .success-state .face-guide::after,
        .error-state .face-guide::after {
            display: none;
        }

        .camera-indicator {
            position: absolute;

            top: 14px;
            left: 14px;

            display: inline-flex;
            align-items: center;
            gap: 6px;

            padding: 7px 12px;

            border-radius: 999px;

            background: rgba(15, 23, 42, 0.78);

            border: 1px solid rgba(148, 163, 184, 0.25);

            backdrop-filter: blur(10px);

            color: #e2e8f0;

            font-size: 12px;

            font-weight: 600;

            letter-spacing: 0.2px;

            box-shadow: 0 6px 18px rgba(0, 0, 0, 0.25);

            transition: background 0.25s ease,
            border-color 0.25s ease,
            color 0.25s ease,
            box-shadow 0.25s ease;
        }

        .camera-indicator::first-letter {
            color: #22c55e;
        }

        .camera-indicator.active {
            color: #86efac;

            background: rgba(22, 101, 52, 0.35);

            border-color: rgba(74, 222, 128, 0.35);

            box-shadow: 0 0 18px rgba(34, 197, 94, 0.18);
        }

        .camera-indicator.verified {
            color: #67e8f9;

            background: rgba(8, 145, 178, 0.25);

            border-color: rgba(34, 211, 238, 0.35);

            box-shadow: 0 0 20px rgba(34, 211, 238, 0.20);
        }

        /* =========================
           STATUS
        ========================= */

        .status-card {
            margin-top: 18px;

            padding: 20px;

            border-radius: 20px;

            background: rgba(30, 41, 59, 0.78);

            border: 1px solid rgba(148, 163, 184, 0.16);

            text-align: center;

            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.16);

            backdrop-filter: blur(12px);

            transition: border-color 0.25s ease,
            background 0.25s ease;
        }

        .status-label {
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

            transition: color 0.25s ease,
            opacity 0.25s ease;
        }

        #status.success {
            color: #4ade80;
        }

        #status.warning {
            color: #fbbf24;
        }

        #status.error {
            color: #fb7185;
        }

        /* =========================
           PROGRESS
        ========================= */

        .progress-section {
            margin-top: 18px;

            padding: 18px;

            border-radius: 20px;

            background: rgba(15, 23, 42, 0.82);

            border: 1px solid rgba(148, 163, 184, 0.16);

            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.18);
        }

        .progress-header {
            display: flex;

            justify-content: space-between;

            align-items: center;

            margin-bottom: 11px;
        }

        .progress-title {
            color: #cbd5e1;

            font-size: 14px;

            font-weight: 600;
        }

        #progress {
            min-width: 48px;

            font-size: 14px;

            font-weight: 800;

            color: #67e8f9;

            text-align: right;

            font-variant-numeric: tabular-nums;
        }

        .progress-track {
            width: 100%;

            height: 11px;

            background: rgba(30, 41, 59, 0.95);

            border: 1px solid rgba(148, 163, 184, 0.08);

            border-radius: 999px;

            overflow: hidden;

            box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.35);
        }

        #progressBar {
            width: 0%;

            height: 100%;

            border-radius: 999px;

            background: linear-gradient(
                    90deg,
                    #38bdf8,
                    #22c55e
            );

            box-shadow: 0 0 12px rgba(34, 197, 94, 0.25);

            transition: width 0.4s ease;
        }

        .progress-steps {
            display: flex;

            justify-content: space-between;

            margin-top: 10px;

            font-size: 11px;

            font-weight: 600;

            color: #64748b;
        }

        .progress-steps span {
            transition: color 0.25s ease,
            transform 0.25s ease;
        }

        /* =========================
           CONTROLS
        ========================= */

        .controls {
            margin-top: 18px;
        }

        #startButton {
            width: 100%;

            padding: 15px 18px;

            border: 1px solid rgba(255, 255, 255, 0.12);

            border-radius: 15px;

            font-size: 16px;

            font-weight: 700;

            cursor: pointer;

            color: #ffffff;

            background:
                    linear-gradient(
                            135deg,
                            #2563eb,
                            #06b6d4
                    );

            box-shadow:
                    0 10px 28px rgba(37, 99, 235, 0.28);

            transition:
                    transform 0.2s ease,
                    opacity 0.2s ease,
                    box-shadow 0.2s ease,
                    filter 0.2s ease;
        }

        #startButton:disabled {
            opacity: 0.6;

            cursor: not-allowed;
        }

        #startButton:not(:disabled):hover {
            filter: brightness(1.08);

            box-shadow:
                    0 12px 32px rgba(37, 99, 235, 0.36);

            transform: translateY(-1px);
        }

        #startButton:active {
            transform: scale(0.98);
        }

        /* =========================
           RESULT CARD
        ========================= */

        .result-card {
            display: none;

            margin-top: 18px;

            padding: 26px 22px;

            border-radius: 22px;

            text-align: center;

            background: linear-gradient(
                    145deg,
                    rgba(34, 197, 94, 0.14),
                    rgba(15, 23, 42, 0.72)
            );

            border: 1px solid rgba(74, 222, 128, 0.35);

            box-shadow: 0 16px 40px rgba(0, 0, 0, 0.22),
            0 0 30px rgba(34, 197, 94, 0.08);

            backdrop-filter: blur(14px);

            animation: resultAppear 0.45s ease-out;
        }

        .result-icon {
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

            box-shadow: 0 0 25px rgba(34, 197, 94, 0.16);
        }

        .result-card h2 {
            color: #4ade80;

            font-size: 21px;

            font-weight: 750;

            margin-bottom: 8px;
        }

        .result-card p {
            color: #a7b5c7;

            font-size: 14px;

            line-height: 1.6;

            max-width: 520px;

            margin: 0 auto;
        }

        @keyframes resultAppear {
            from {
                opacity: 0;
                transform: translateY(10px) scale(0.98);
            }

            to {
                opacity: 1;
                transform: translateY(0) scale(1);
            }
        }

        .success-state .face-guide {
            border-color: #22c55e;

            box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.12),
            0 0 30px rgba(34, 197, 94, 0.5);
        }

        .error-state .face-guide {
            border-color: #fb7185;
        }

        /* =========================
           INFO CARDS
        ========================= */

        .info-section {
            display: grid;

            grid-template-columns:
        repeat(3, 1fr);

            gap: 12px;

            margin-top: 18px;
        }

        .info-card {
            padding: 17px 10px;

            text-align: center;

            border-radius: 18px;

            background: rgba(15, 23, 42, 0.78);

            border: 1px solid rgba(148, 163, 184, 0.14);

            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.14);

            backdrop-filter: blur(10px);

            transition: transform 0.2s ease,
            border-color 0.2s ease;
        }

        .info-card h3 {
            font-size: 10px;

            text-transform: uppercase;

            letter-spacing: 0.9px;

            font-weight: 700;

            color: #64748b;

            margin-bottom: 8px;
        }

        .info-card p {
            font-size: 22px;

            font-weight: 800;

            color: #67e8f9;

            font-variant-numeric: tabular-nums;
        }

        /* =========================
           RECORDING
        ========================= */

        .recording {
            animation: pulse 1.5s ease-in-out infinite;
        }

        @keyframes pulse {

            0%, 100% {
                opacity: 1;
            }

            50% {
                opacity: 0.55;
            }

        }

        /* =========================
           MOBILE
        ========================= */

        @media (max-width: 600px) {

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
                border-radius: 50%;
            }

            .face-guide {
                width: 92%;
                height: auto;
                aspect-ratio: 1;
            }

            #status {
                font-size: 17px;
            }

            .status-card,
            .progress-section {
                padding: 15px;
            }

            .info-section {
                gap: 7px;
            }

            .info-card {
                padding: 13px 5px;
            }

            .info-card p {
                font-size: 19px;
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

            .camera-indicator {
                top: 10px;
                left: 10px;

                padding: 6px 10px;

                font-size: 11px;
            }

            .face-guide {
                width: 92%;
                aspect-ratio: 1 / 1;
                height: auto;
            }

            .result-card {
                padding: 22px 16px;
            }

            .result-card h2 {
                font-size: 19px;
            }

            #startButton {
                padding: 14px;
            }

        }

    </style>
</head>


<script>

    /*
     * PWA SERVICE WORKER
     */

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

</script>


<body>

<div class="page">

    <!-- HEADER -->

    <header class="header">

        <div class="header-icon">
            📷
        </div>

        <h1>
            Live Face Attendance
        </h1>

        <p class="subtitle">
            Secure biometric attendance with
            real-time liveness verification
        </p>

    </header>


    <!-- CAMERA -->

    <div class="camera-card" id="cameraCard">

        <div class="camera-wrapper">

            <video
                    id="video"
                    autoplay
                    muted
                    playsinline>
            </video>

            <div class="camera-overlay">

                <div class="face-guide"></div>

            </div>

            <div
                    class="camera-indicator"
                    id="cameraIndicator">

                ● Camera inactive

            </div>

        </div>


        <!-- STATUS -->

        <div class="status-card">

            <div class="status-label">
                Liveness & Recognition
            </div>

            <div id="status">
                Camera not started
            </div>

        </div>


        <!-- PROGRESS -->

        <div class="progress-section">

            <div class="progress-header">

                <span class="progress-title">
                    Liveness Progress
                </span>

                <span id="progress">
                    0%
                </span>

            </div>

            <div class="progress-track">

                <div id="progressBar"></div>

            </div>

            <div class="progress-steps">

                <span>
                    Start
                </span>

                <span>
                    50%
                </span>

                <span>
                    Complete
                </span>

            </div>

        </div>


        <!-- BUTTON -->

        <div class="controls">

            <button id="startButton">
                📷 Start Camera
            </button>

        </div>

        <button
                id="antiSpoofButton"
                type="button"
                disabled
                style="
            width: 100%;
            margin-top: 10px;
            padding: 14px;
            border: 1px solid rgba(255,255,255,0.12);
            border-radius: 15px;
            font-size: 15px;
            font-weight: 700;
            cursor: pointer;
            color: #ffffff;
            background: linear-gradient(135deg, #7c3aed, #ec4899);
        ">
            🛡️ Test Anti-Spoof
        </button>

        <div
                id="antiSpoofResult"
                style="
            margin-top: 12px;
            text-align: center;
            font-size: 14px;
            font-weight: 600;
            color: #cbd5e1;
        ">
        </div>


        <!-- RESULT -->

        <div
                class="result-card"
                id="resultCard">

            <div class="result-icon">
                ✓
            </div>

            <h2>
                Attendance Verified
            </h2>

            <p id="resultMessage">
                Your attendance has been successfully recorded.
            </p>

        </div>

    </div>


    <!-- INFO -->

    <div class="info-section">

        <div class="info-card">

            <h3>
                Required Movement
            </h3>

            <p>
                5
            </p>

        </div>


        <div class="info-card">

            <h3>
                Movements
            </h3>

            <p id="movementCount">
                0
            </p>

        </div>


        <div class="info-card">

            <h3>
                Frames
            </h3>

            <p id="frameCount">
                0
            </p>

        </div>

    </div>

</div>


<script>

    const video =
        document.getElementById("video");

    const canvas =
        document.createElement("canvas");

    const startButton =
        document.getElementById("startButton");

    const antiSpoofButton =
        document.getElementById("antiSpoofButton");

    const antiSpoofResult =
        document.getElementById("antiSpoofResult");

    const statusText =
        document.getElementById("status");

    const progressText =
        document.getElementById("progress");

    const progressBar =
        document.getElementById("progressBar");

    const movementCountText =
        document.getElementById("movementCount");

    const frameCountText =
        document.getElementById("frameCount");

    const cameraIndicator =
        document.getElementById("cameraIndicator");

    const cameraCard =
        document.getElementById("cameraCard");

    const resultCard =
        document.getElementById("resultCard");

    const resultMessage =
        document.getElementById("resultMessage");


    let stream = null;
    let timer = null;
    let processing = false;
    let livenessSessionId = null;
    let livenessPassedHandled = false;


    startButton.addEventListener(
        "click",
        startCamera
    );

    /*
     * START CAMERA
     */

    antiSpoofButton.addEventListener(
        "click",
        testAntiSpoof
    );


    async function testAntiSpoof() {

        if (!stream || video.readyState < 2) {

            antiSpoofResult.innerText =
                "❌ Start the camera first.";

            return;
        }

        try {

            antiSpoofButton.disabled = true;

            antiSpoofResult.innerText =
                "🔍 Checking anti-spoof...";

            const testCanvas =
                document.createElement("canvas");

            testCanvas.width = 320;
            testCanvas.height = 320;

            const ctx =
                testCanvas.getContext("2d");

            drawCenteredSquare(video, ctx, 320);

            const blob =
                await new Promise(resolve => {

                    testCanvas.toBlob(
                        resolve,
                        "image/jpeg",
                        0.80
                    );

                });

            if (!blob) {
                throw new Error(
                    "Could not capture camera frame"
                );
            }

            const formData =
                new FormData();

            formData.append(
                "image",
                blob,
                "antispoof-test.jpg"
            );

            const response =
                await fetch(
                    "/api/antispoof/check",
                    {
                        method: "POST",
                        body: formData
                    }
                );

            const result =
                await response.json();

            console.log(
                "ANTI-SPOOF RESULT:",
                result
            );

            if (result.status === "NO_FACE") {

                antiSpoofResult.innerText =
                    "❌ No face detected";

            } else if (
                result.status === "MULTIPLE_FACES"
            ) {

                antiSpoofResult.innerText =
                    "❌ Multiple faces detected";

            } else if (
                result.status === "OK"
            ) {

                antiSpoofResult.innerText =
                    "🛡️ Class 0: "
                    + Number(result.class0).toFixed(4)
                    + " | Class 1: "
                    + Number(result.class1).toFixed(4);

            } else {

                antiSpoofResult.innerText =
                    "⚠️ "
                    + (result.message || "Test failed");
            }

        } catch (error) {

            console.error(
                "ANTI-SPOOF ERROR:",
                error
            );

            antiSpoofResult.innerText =
                "❌ Anti-spoof test failed: "
                + error.message;

        } finally {

            antiSpoofButton.disabled = false;
        }
    }

    async function startCamera() {

        try {

            livenessPassedHandled = false;

            resultCard.style.display =
                "none";

            cameraCard.classList.remove(
                "success-state",
                "error-state"
            );

            progressBar.style.width =
                "0%";

            progressText.innerText =
                "0%";

            movementCountText.innerText =
                "0";

            frameCountText.innerText =
                "0";


            // Start liveness session
            const sessionResponse =
                await fetch(
                    "/api/liveness/start",
                    {
                        method: "POST"
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


            stream =
                await navigator
                    .mediaDevices
                    .getUserMedia({

                        video: {

                            width: {
                                ideal: 640
                            },

                            height: {
                                ideal: 480
                            },

                            facingMode: "user"

                        },

                        audio: false

                    });


            video.srcObject =
                stream;

            await video.play();

            antiSpoofButton.disabled = false;


            cameraIndicator.innerText =
                "● Camera active";


            statusText.innerText =
                "Camera ready - "
                + session.challenge;

            statusText.classList.add(
                "recording"
            );


            startButton.disabled = true;

            startButton.innerText =
                "⏳ Processing...";


            startFrameProcessing();


        } catch (error) {

            console.error(error);


            cameraIndicator.innerText =
                "● Camera unavailable";


            statusText.innerText =
                "Camera permission denied or camera unavailable.";

            statusText.classList.remove(
                "success",
                "warning"
            );

            statusText.classList.add(
                "error"
            );

        }

    }


    /*
     * FRAME PROCESSING
     */

    function startFrameProcessing() {

        if (timer !== null) {
            return;
        }


        timer =
            setInterval(
                captureAndSendFrame,
                200
            );


        console.log(
            "Frame processing started"
        );

    }


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


        processing = true;


        try {

            canvas.width = 320;
            canvas.height = 320;


            const ctx =
                canvas.getContext(
                    "2d",
                    {
                        alpha: false
                    }
                );


            drawCenteredSquare(video, ctx, 320);


            const blob =
                await new Promise(
                    resolve => {

                        canvas.toBlob(
                            resolve,
                            "image/jpeg",
                            0.62
                        );

                    }
                );


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


            const response =
                await fetch(
                    "/api/liveness/frame",
                    {
                        method: "POST",
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
                    1000 * performance.now()
                    - requestStart
                );


            console.log(
                "Liveness response:",
                result,
                "Request time:",
                requestTime + " ms"
            );


            updateStatus(result);


        } catch (error) {

            console.error(
                "Frame processing error:",
                error
            );


            statusText.innerText =
                "⚠️ Frame processing error";


        } finally {

            processing = false;

        }

    }

    /* Match the square source area visible in the circular camera preview. */
    function drawCenteredSquare(source, context, outputSize) {
        const side = Math.min(source.videoWidth, source.videoHeight);
        const sourceX = (source.videoWidth - side) / 2;
        const sourceY = (source.videoHeight - side) / 2;

        context.drawImage(
            source,
            sourceX,
            sourceY,
            side,
            side,
            0,
            0,
            outputSize,
            outputSize
        );
    }


    /*
     * UPDATE UI
     */

    async function verifyLiveFrame() {

        if (!stream || !video || video.readyState < 2) {
            statusText.textContent =
                "❌ Camera frame is not ready";
            return;
        }

        try {

            statusText.textContent =
                "🔍 Recognizing face...";

            const canvas =
                document.createElement("canvas");

            canvas.width = 320;
            canvas.height = 320;

            const ctx =
                canvas.getContext("2d");

            drawCenteredSquare(video, ctx, 320);

            const blob =
                await new Promise(resolve => {

                    canvas.toBlob(
                        resolve,
                        "image/jpeg",
                        0.80
                    );

                });

            if (!blob) {
                throw new Error(
                    "Could not capture webcam frame"
                );
            }

            const formData =
                new FormData();

            formData.append(
                "image",
                blob,
                "live-face.jpg"
            );

            formData.append(
                "sessionId",
                livenessSessionId
            );

            const response =
                await fetch(
                    "/api/faces/live-verify",
                    {
                        method: "POST",
                        body: formData
                    }
                );

            const result =
                await response.json();

            console.log(
                "LIVE RECOGNITION RESULT:",
                result
            );

            if (result.recognized) {

                statusText.innerText =
                    "✓ Attendance marked PRESENT for "
                    + result.name;

                statusText.classList.remove(
                    "warning",
                    "error"
                );

                statusText.classList.add(
                    "success"
                );

                cameraIndicator.innerText =
                    "● Verified";

                cameraCard.classList.add(
                    "success-state"
                );

                resultCard.style.display =
                    "block";

                resultMessage.innerText =
                    "Attendance recorded successfully. "
                    + "Welcome, "
                    + result.name
                    + "! Similarity: "
                    + Number(result.similarity)
                        .toFixed(3);

            } else {

                statusText.innerText =
                    "❌ Face not recognized";

                statusText.classList.remove(
                    "success",
                    "warning"
                );

                statusText.classList.add(
                    "error"
                );

                cameraCard.classList.add(
                    "error-state"
                );

                resultCard.style.display =
                    "block";

                resultMessage.innerText =
                    "Liveness passed, but the face "
                    + "was not recognized. Similarity: "
                    + Number(result.similarity || 0)
                        .toFixed(3);

            }

        } catch (error) {

            console.error(
                "LIVE RECOGNITION ERROR:",
                error
            );

            statusText.innerText =
                "❌ Recognition failed";

            statusText.classList.remove(
                "success",
                "warning"
            );

            statusText.classList.add(
                "error"
            );

            resultCard.style.display =
                "block";

            resultMessage.innerText =
                error.message;

        } finally {

            stopCamera(false);

            startButton.disabled = false;

            startButton.innerText =
                "📷 Start Camera";

        }
    }


    async function updateStatus(result) {

        if (!result) {
            return;
        }

        let displayChallenge = result.challenge;

        if (displayChallenge === "TURN_LEFT") {
            displayChallenge = "TURN_RIGHT";
        } else if (displayChallenge === "TURN_RIGHT") {
            displayChallenge = "TURN_LEFT";
        }


        /*
         * Progress from backend
         */

        if (typeof result.progress === "number") {

            const progress =
                Math.max(
                    0,
                    Math.min(
                        100,
                        result.progress
                    )
                );


            progressText.innerText =
                progress + "%";


            progressBar.style.width =
                progress + "%";


            movementCountText.innerText =
                Math.floor(progress / 20);
        }


        if (result.status === "NO_FACE") {

            statusText.innerText =
                "🔍 No face detected";

            statusText.classList.remove(
                "success",
                "error"
            );

            statusText.classList.add(
                "warning"
            );

            return;

        }


        if (
            result.status ===
            "MULTIPLE_FACES"
        ) {

            statusText.innerText =
                "❌ Multiple faces detected";

            statusText.classList.remove(
                "success",
                "warning"
            );

            statusText.classList.add(
                "error"
            );

            return;

        }


        if (
            result.status ===
            "LOW_CONFIDENCE"
        ) {

            statusText.innerText =
                "🔍 Face not clear";

            statusText.classList.remove(
                "success",
                "error"
            );

            statusText.classList.add(
                "warning"
            );

            return;

        }


        if (
            result.status ===
            "INVALID_LANDMARKS" ||
            result.status ===
            "INVALID_FACE"
        ) {

            statusText.innerText =
                "⚠️ "
                + result.status;

            console.log(
                "Rejected:",
                result.status,
                result
            );

            return;

        }


        if (
            result.status ===
            "CALIBRATING"
        ) {

            statusText.innerText =
                "📐 Keep your head straight...";

            statusText.classList.remove(
                "success",
                "error"
            );

            statusText.classList.add(
                "warning"
            );

            return;

        }


        /*
         * SUCCESS
         */

        if (result.livenessPassed) {

            if (timer !== null) {
                clearInterval(timer);
                timer = null;

                console.log(
                    "Frame processing stopped after liveness pass."
                );
            }

            progressBar.style.width = "100%";
            progressText.textContent = "100%";

            status.textContent =
                "✅ LIVENESS PASSED — Recognizing...";

            cameraIndicator.textContent =
                "● Verified";

            if (!livenessPassedHandled) {

                livenessPassedHandled = true;

                await verifyLiveFrame();
            }

            return;
        }


        /*
         * Normal challenge
         */

        statusText.innerText =
            "👉 "
            + displayChallenge
            + " | Progress: "
            + result.progress
            + "%";

        statusText.classList.remove(
            "success",
            "error"
        );

        statusText.classList.add(
            "warning"
        );

    }


    /*
     * STOP CAMERA
     */

    function stopCamera(
        clearStatus = true
    ) {

        if (timer !== null) {

            clearInterval(timer);

            timer = null;

        }


        if (stream !== null) {

            stream
                .getTracks()
                .forEach(track => {

                    track.stop();

                });

            stream = null;

        }


        video.srcObject = null;

        antiSpoofButton.disabled = true;

        processing = false;


        if (clearStatus) {

            cameraIndicator.innerText =
                "● Camera inactive";

            startButton.disabled = false;

            startButton.innerText =
                "📷 Start Camera";

        }

    }
</script>

</body>
</html>