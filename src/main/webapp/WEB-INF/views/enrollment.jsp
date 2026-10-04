<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<!DOCTYPE html>
<html>

<head>

    <meta charset="UTF-8">

    <title>Face Enrollment</title>

    <style>

        * {
            box-sizing: border-box;
        }

        body {
            margin: 0;
            font-family: Arial, sans-serif;
            background: #f4f6f8;
            text-align: center;
        }

        .container {
            width: 600px;
            max-width: 95%;
            margin: 40px auto;
            background: white;
            padding: 25px;
            border-radius: 12px;
            box-shadow: 0 5px 25px rgba(0,0,0,0.1);
        }

        video {
            width: 480px;
            max-width: 100%;
            border-radius: 10px;
            background: black;
        }

        button {
            margin: 10px;
            padding: 12px 20px;
            border: none;
            border-radius: 6px;
            cursor: pointer;
            background: #222;
            color: white;
        }

        #status {
            margin: 20px;
            font-size: 18px;
        }

        #progress {
            width: 100%;
            height: 10px;
            background: #ddd;
            border-radius: 5px;
            overflow: hidden;
        }

        #progressBar {
            width: 0%;
            height: 100%;
            background: #222;
        }

    </style>

</head>


<body>

<div class="container">

    <h1>Face Enrollment</h1>

    <p id="userInfo"></p>

    <button onclick="startEnrollment()">
        Start Face Enrollment
    </button>

    <br>

    <video
            id="video"
            autoplay
            playsinline>
    </video>

    <canvas
            id="canvas"
            width="480"
            height="360"
            style="display:none;">
    </canvas>

    <div id="status">
        Click Start Face Enrollment
    </div>

    <div id="progress">

        <div id="progressBar"></div>

    </div>

    <br>

    <button onclick="logout()">
        Logout
    </button>

</div>


<script>

    const token =
        localStorage.getItem("jwt");


    /*
     * Make sure user is logged in.
     */

    if (!token) {

        window.location.href = "/";

    }


    const userName =
        localStorage.getItem("userName");

    const userEmail =
        localStorage.getItem("userEmail");


    document.getElementById("userInfo")
        .innerText =
        "Logged in as: "
        + userName
        + " ("
        + userEmail
        + ")";


    let enrollmentSessionId = null;

    let livenessSessionId = null;

    let stream = null;

    let processing = false;


    async function startEnrollment() {

        setStatus(
            "Starting authenticated enrollment..."
        );


        try {

            /*
             * IMPORTANT:
             *
             * This request is protected.
             *
             * Therefore this tests whether
             * Spring Security accepts our JWT.
             */

            const response =
                await fetch(
                    "/api/faces/enrollment/start",
                    {

                        method: "POST",

                        headers: {

                            "Authorization":
                                "Bearer " + token

                        }

                    }
                );


            if (response.status === 401 ||
                response.status === 403) {

                setStatus(
                    "Authentication failed: HTTP "
                    + response.status
                );

                console.error(
                    "JWT rejected by backend"
                );

                return;
            }


            if (!response.ok) {

                const text =
                    await response.text();

                setStatus(
                    "Enrollment start failed."
                );

                console.error(text);

                return;
            }


            const data =
                await response.json();


            enrollmentSessionId =
                data.sessionId;


            setStatus(
                "Enrollment session created. Starting camera..."
            );


            await startCamera();


            /*
             * Start the live liveness session.
             */

            const livenessResponse =
                await fetch(
                    "/api/liveness/start"
                );


            if (!livenessResponse.ok) {

                setStatus(
                    "Could not start liveness."
                );

                return;
            }


            const livenessData =
                await livenessResponse.json();


            livenessSessionId =
                livenessData.sessionId;


            setStatus(
                "Camera started. Follow the instructions."
            );


            processFrames();


        } catch (error) {

            console.error(error);

            setStatus(
                "Server error. Check console."
            );
        }
    }


    async function startCamera() {

        stream =
            await navigator.mediaDevices
                .getUserMedia({

                    video: {

                        width: {
                            ideal: 480
                        },

                        height: {
                            ideal: 360
                        },

                        facingMode: "user"

                    },

                    audio: false

                });


        document
            .getElementById("video")
            .srcObject = stream;
    }


    async function processFrames() {

        if (!stream ||
            !livenessSessionId) {

            return;
        }


        if (processing) {

            requestAnimationFrame(
                processFrames
            );

            return;
        }


        processing = true;


        try {

            const video =
                document.getElementById("video");

            const canvas =
                document.getElementById("canvas");

            const context =
                canvas.getContext("2d");


            context.drawImage(
                video,
                0,
                0,
                canvas.width,
                canvas.height
            );


            const blob =
                await new Promise(resolve => {

                    canvas.toBlob(
                        resolve,
                        "image/jpeg",
                        0.75
                    );

                });


            const formData =
                new FormData();


            formData.append(
                "sessionId",
                livenessSessionId
            );


            formData.append(
                "image",
                blob,
                "frame.jpg"
            );


            const response =
                await fetch(
                    "/api/liveness/frame",
                    {

                        method: "POST",

                        body: formData

                    }
                );


            const data =
                await response.json();


            setStatus(
                data.status
                + " | "
                + data.challenge
            );


            document
                .getElementById("progressBar")
                .style.width =
                data.progress + "%";


            if (data.livenessPassed) {

                setStatus(
                    "Liveness passed."
                );

                stopCamera();

                return;
            }


        } catch (error) {

            console.error(error);

        } finally {

            processing = false;

        }


        setTimeout(
            processFrames,
            100
        );
    }


    function stopCamera() {

        if (stream) {

            stream
                .getTracks()
                .forEach(
                    track => track.stop()
                );

            stream = null;
        }
    }


    function setStatus(message) {

        document
            .getElementById("status")
            .innerText = message;
    }


    function logout() {

        localStorage.removeItem("jwt");
        localStorage.removeItem("userId");
        localStorage.removeItem("userName");
        localStorage.removeItem("userEmail");

        stopCamera();

        window.location.href = "/";
    }

</script>

</body>

</html>