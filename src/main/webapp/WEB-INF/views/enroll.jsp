<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<!DOCTYPE html>
<html>
<head>

    <meta charset="UTF-8">

    <link rel="manifest" href="/manifest.json">

    <meta name="theme-color" content="#222222">

    <meta name="mobile-web-app-capable"
          content="yes">

    <meta name="apple-mobile-web-app-capable"
          content="yes">

    <meta name="apple-mobile-web-app-status-bar-style"
          content="default">

    <meta name="apple-mobile-web-app-title"
          content="Face Attendance">

    <title>Face Enrollment</title>

    <style>

        body {
            font-family: Arial, sans-serif;
            background: #0f172a;
            color: white;
            text-align: center;
            padding: 30px;
        }

        .container {
            max-width: 700px;
            margin: auto;
            background: #1e293b;
            padding: 30px;
            border-radius: 12px;
        }

        input {
            width: 90%;
            padding: 12px;
            margin: 8px;
            border-radius: 6px;
            border: none;
        }

        button {
            padding: 12px 20px;
            margin: 10px;
            border: none;
            border-radius: 6px;
            cursor: pointer;
        }

        #video {
            width: 640px;
            max-width: 100%;
            margin-top: 20px;
            border-radius: 10px;
            background: black;
        }

        .status {
            margin-top: 20px;
            padding: 15px;
            background: #334155;
            border-radius: 8px;
        }

        .success {
            color: #22c55e;
        }

        .error {
            color: #ef4444;
        }

        .session {
            word-break: break-all;
            margin-top: 15px;
        }

    </style>

</head>

<script>

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

<div class="container">

    <h1>Face Enrollment</h1>

    <!-- LOGIN SECTION -->

    <div id="loginSection">

        <h2>Login</h2>

        <input
                id="email"
                type="email"
                placeholder="Email"
        >

        <input
                id="password"
                type="password"
                placeholder="Password"
        >

        <br>

        <button onclick="login()">
            Login
        </button>

    </div>


    <!-- ENROLLMENT SECTION -->

    <div id="enrollmentSection"
         style="display:none;">

        <h2>Live Face Enrollment</h2>

        <p style="color: #94a3b8; margin-bottom: 20px;">
            Complete liveness verification to securely enroll your face.
        </p>

        <button onclick="startEnrollment()">
            Start Enrollment
        </button>

        <div
                id="session"
                class="session">
        </div>

        <br>

        <button
                id="cameraButton"
                onclick="startCamera()">
            📷 Start Camera
        </button>

        <button
                id="stopButton"
                onclick="stopCamera()"
                style="display:none;">
            ■ Stop Camera
        </button>

        <br>

        <video
                id="video"
                autoplay
                playsinline
                style="display:none;">
        </video>

        <!-- PROGRESS -->

        <div
                id="progressSection"
                style="display:none; margin-top: 20px; padding: 15px; background: #334155; border-radius: 8px;">

            <div style="display: flex; justify-content: space-between; margin-bottom: 10px;">

                <span style="color: #cbd5e1; font-size: 14px; font-weight: 600;">
                    Liveness Progress
                </span>

                <span
                        id="progressText"
                        style="color: #38bdf8; font-size: 14px; font-weight: 700;">
                    0%
                </span>

            </div>

            <div
                    style="width: 100%; height: 10px; background: #1e293b; border-radius: 20px; overflow: hidden;">

                <div
                        id="progressFill"
                        style="width: 0%; height: 100%; border-radius: 20px; background: linear-gradient(90deg, #38bdf8, #22c55e); transition: width 0.4s ease;">
                </div>

            </div>

            <div
                    style="display: flex; justify-content: space-between; margin-top: 9px; color: #64748b; font-size: 11px;">

                <span>Start</span>

                <span>50%</span>

                <span>Complete</span>

            </div>

        </div>

        <!-- SUCCESS CARD -->

        <div
                id="successCard"
                style="display:none; margin-top: 20px; padding: 24px; text-align: center; border-radius: 20px; background: rgba(34, 197, 94, 0.10); border: 1px solid rgba(34, 197, 94, 0.35);">

            <div
                    style="width: 58px; height: 58px; margin: 0 auto 12px; display: flex; align-items: center; justify-content: center; border-radius: 50%; background: rgba(34, 197, 94, 0.15); color: #22c55e; font-size: 30px;">
                ✓
            </div>

            <h2 style="color: #4ade80; margin-bottom: 6px;">
                Face Enrolled Successfully
            </h2>

            <p style="color: #94a3b8; font-size: 14px;">
                Your face has been securely enrolled with liveness verification.
            </p>

        </div>

        <div
                id="status"
                class="status">

            Please login first.

        </div>

    </div>

</div>


<script>

    let token = localStorage.getItem("jwt") || localStorage.getItem("token");

    let enrollmentSessionId = null;
    let livenessSessionId = null;
    let cameraStream = null;
    let timer = null;
    let processing = false;

    window.addEventListener("DOMContentLoaded", () => {
        if (token) {
            document.getElementById("loginSection").style.display = "none";
            document.getElementById("enrollmentSection").style.display = "block";
            setStatus("Logged in. Click Start Enrollment.", "success");
        }
    });

    /*
     * LOGIN
     */

    async function login() {

        const email =
            document.getElementById("email").value;

        const password =
            document.getElementById("password").value;


        if (!email || !password) {

            setStatus(
                "Please enter email and password.",
                "error"
            );

            return;
        }


        try {

            const response =
                await fetch("/api/auth/login", {

                    method: "POST",

                    headers: {
                        "Content-Type":
                            "application/json"
                    },

                    body: JSON.stringify({
                        email: email,
                        password: password
                    })

                });


            const data =
                await response.json();


            if (!response.ok) {

                setStatus(
                    data.message ||
                    "Login failed.",
                    "error"
                );

                return;
            }


            /*
             * Save JWT
             */

            token = data.token;

            localStorage.setItem(
                "jwt",
                token
            );
            localStorage.setItem(
                "token",
                token
            );


            /*
             * Hide login
             */

            document.getElementById(
                "loginSection"
            ).style.display = "none";


            /*
             * Show enrollment
             */

            document.getElementById(
                "enrollmentSection"
            ).style.display = "block";


            setStatus(
                "Login successful. Now click Start Enrollment.",
                "success"
            );


        } catch (error) {

            console.error(error);

            setStatus(
                "Could not connect to server.",
                "error"
            );
        }

    }


    /*
     * START ENROLLMENT
     */

    async function startEnrollment() {

        if (!token) {
            token =
                localStorage.getItem("jwt") ||
                localStorage.getItem("token");
        }

        if (!token) {
            setStatus("Please login first.", "error");
            return;
        }

        try {
            setStatus("Starting enrollment...", "");

            const response =
                await fetch(
                    "/api/faces/enrollment/start",
                    {
                        method: "POST",
                        headers: {
                            "Authorization": "Bearer " + token
                        }
                    }
                );

            const data = await response.json();

            console.log("Enrollment response:", data);

            if (!response.ok) {
                setStatus(data.message || "Could not start enrollment.", "error");
                return;
            }

            enrollmentSessionId = data.sessionId;

            document.getElementById("session").innerHTML =
                "<strong>Session ID:</strong><br>" + enrollmentSessionId;

            setStatus(
                "Enrollment session started. Click Start Camera to begin liveness verification.",
                "success"
            );

        } catch (error) {
            console.error(error);
            setStatus("Error starting enrollment.", "error");
        }
    }


    /*
     * START CAMERA
     */

    async function startCamera() {

        if (!enrollmentSessionId) {
            setStatus("Start enrollment first.", "error");
            return;
        }

        try {
            // Start liveness session
            const sessionResponse =
                await fetch("/api/liveness/start", {
                    method: "POST"
                });

            if (!sessionResponse.ok) {
                throw new Error("Liveness start failed: " + sessionResponse.status);
            }

            const session = await sessionResponse.json();
            livenessSessionId = session.sessionId;

            console.log("Liveness session:", session);

            // Open webcam
            cameraStream =
                await navigator.mediaDevices.getUserMedia({
                    video: {
                        width: 640,
                        height: 480,
                        facingMode: "user"
                    },
                    audio: false
                });

            const video = document.getElementById("video");
            video.srcObject = cameraStream;
            video.style.display = "block";

            await video.play();

            document.getElementById("cameraButton").style.display = "none";
            document.getElementById("stopButton").style.display = "inline-block";
            document.getElementById("progressSection").style.display = "block";

            setStatus("Camera ready - " + session.challenge, "success");

            // Start frame processing
            startFrameProcessing();

        } catch (error) {
            console.error(error);
            setStatus("Could not access camera. Please allow camera permission.", "error");
        }
    }


    /*
     * STOP CAMERA
     */

    function stopCamera() {

        if (timer !== null) {
            clearInterval(timer);
            timer = null;
        }

        if (cameraStream !== null) {
            cameraStream.getTracks().forEach(track => track.stop());
            cameraStream = null;
        }

        const video = document.getElementById("video");
        video.srcObject = null;
        video.style.display = "none";

        document.getElementById("cameraButton").style.display = "inline-block";
        document.getElementById("stopButton").style.display = "none";

        processing = false;
        livenessSessionId = null;

        setStatus("Camera stopped.", "");
    }


    /*
     * FRAME PROCESSING
     */

    function startFrameProcessing() {

        if (timer !== null) {
            return;
        }

        timer = setInterval(captureAndSendFrame, 300);
        console.log("Frame processing started");
    }


    async function captureAndSendFrame() {

        if (processing) {
            return;
        }

        if (!cameraStream) {
            return;
        }

        if (!livenessSessionId) {
            return;
        }

        const video = document.getElementById("video");

        if (video.readyState < 2) {
            return;
        }

        if (video.videoWidth === 0 || video.videoHeight === 0) {
            return;
        }

        processing = true;

        try {

            const canvas = document.createElement("canvas");
            canvas.width = 480;
            canvas.height = 360;

            const ctx = canvas.getContext("2d", { alpha: false });
            ctx.drawImage(video, 0, 0, 480, 360);

            const blob = await new Promise(resolve => {
                canvas.toBlob(resolve, "image/jpeg", 0.70);
            });

            if (!blob) {
                console.error("Could not create JPEG");
                return;
            }

            const formData = new FormData();
            formData.append("image", blob, "live-frame.jpg");
            formData.append("sessionId", livenessSessionId);

            const response = await fetch("/api/liveness/frame", {
                method: "POST",
                body: formData
            });

            if (!response.ok) {
                throw new Error("Frame API returned " + response.status);
            }

            const result = await response.json();
            console.log("Liveness response:", result);

            updateStatus(result);

        } catch (error) {
            console.error("Frame processing error:", error);
            setStatus("⚠️ Frame processing error", "error");
        } finally {
            processing = false;
        }

    }


    /*
     * UPDATE STATUS
     */

    function updateStatus(result) {

        if (!result) {
            return;
        }

        if (typeof result.progress === "number") {
            const progress = Math.max(0, Math.min(100, result.progress));
            document.getElementById("progressText").innerText = progress + "%";
            document.getElementById("progressFill").style.width = progress + "%";
        }

        if (result.status === "NO_FACE") {
            setStatus("🔍 No face detected", "warning");
            return;
        }

        if (result.status === "MULTIPLE_FACES") {
            setStatus("❌ Multiple faces detected", "error");
            return;
        }

        if (result.status === "CALIBRATING") {
            setStatus("📐 Keep your head straight...", "warning");
            return;
        }

        if (result.livenessPassed) {
            setStatus("✅ LIVENESS PASSED - Enrolling...", "success");
            stopCamera();
            completeEnrollment();
            return;
        }

        setStatus("👉 " + result.challenge + " | Progress: " + result.progress + "%", "warning");
    }


    /*
     * COMPLETE ENROLLMENT
     */

    async function completeEnrollment() {

        try {

            setStatus("Capturing face for enrollment...", "success");

            // Capture final frame
            const video = document.getElementById("video");
            const canvas = document.createElement("canvas");
            canvas.width = video.videoWidth;
            canvas.height = video.videoHeight;

            const ctx = canvas.getContext("2d");
            ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

            const blob = await new Promise(resolve => {
                canvas.toBlob(resolve, "image/jpeg", 0.9);
            });

            const formData = new FormData();
            formData.append("image", blob, "enrollment.jpg");
            formData.append("enrollmentSessionId", enrollmentSessionId);
            formData.append("livenessSessionId", livenessSessionId);

            const response = await fetch("/api/faces/enrollment/complete", {
                method: "POST",
                headers: {
                    "Authorization": "Bearer " + token
                },
                body: formData
            });

            const data = await response.json();

            console.log("Enrollment result:", data);

            if (!response.ok) {
                setStatus(data.message || "Enrollment failed.", "error");
                return;
            }

            document.getElementById("successCard").style.display = "block";
            setStatus("Face enrolled successfully!", "success");

            // Clear sessions
            enrollmentSessionId = null;
            livenessSessionId = null;
            document.getElementById("session").innerHTML = "";

        } catch (error) {
            console.error(error);
            setStatus("Enrollment request failed.", "error");
        }

    }


    /*
     * STATUS
     */

    function setStatus(message, type) {

        const status =
            document.getElementById("status");


        status.innerText =
            message;


        status.className =
            "status " + type;

    }

</script>

</body>
</html>
