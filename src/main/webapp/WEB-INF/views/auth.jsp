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

    <title>Face Attendance - Authentication</title>



    <style>
        * {
            box-sizing: border-box;
        }

        body {
            margin: 0;
            min-height: 100vh;
            font-family: Arial, sans-serif;
            background: #f4f6f8;
            display: flex;
            justify-content: center;
            align-items: center;
        }

        .container {
            width: 400px;
            background: white;
            padding: 30px;
            border-radius: 12px;
            box-shadow: 0 5px 25px rgba(0, 0, 0, 0.1);
        }

        h1 {
            text-align: center;
            margin-bottom: 25px;
        }

        .tabs {
            display: flex;
            margin-bottom: 20px;
        }

        .tabs button {
            width: 50%;
            padding: 12px;
            border: none;
            cursor: pointer;
            background: #eee;
        }

        .tabs button.active {
            background: #222;
            color: white;
        }

        .form {
            display: none;
        }

        .form.active {
            display: block;
        }

        input {
            width: 100%;
            padding: 12px;
            margin-bottom: 15px;
            border: 1px solid #ccc;
            border-radius: 6px;
        }

        .submit-btn {
            width: 100%;
            padding: 12px;
            border: none;
            border-radius: 6px;
            background: #222;
            color: white;
            cursor: pointer;
        }

        .submit-btn:hover {
            background: #444;
        }

        #message {
            margin-top: 15px;
            text-align: center;
            font-size: 14px;
        }

        .token-box {
            margin-top: 15px;
            padding: 10px;
            background: #f1f1f1;
            word-break: break-all;
            font-size: 12px;
            display: none;
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

    <h1>Face Attendance</h1>

    <div class="tabs">

        <button id="loginTab"
                class="active"
                onclick="showLogin()">
            Login
        </button>

        <button id="registerTab"
                onclick="showRegister()">
            Register
        </button>

    </div>


    <!-- LOGIN -->

    <div id="loginForm" class="form active">

        <input
                type="email"
                id="loginEmail"
                placeholder="Email"
        >

        <input
                type="password"
                id="loginPassword"
                name="password"
                autocomplete="current-password"
                placeholder="Password"
        >

        <button
                class="submit-btn"
                onclick="login()">
            Login
        </button>

    </div>


    <!-- REGISTER -->

    <div id="registerForm" class="form">

        <input
                type="text"
                id="registerName"
                placeholder="Full name"
        >

        <input
                type="email"
                id="registerEmail"
                placeholder="Email"
        >

        <input
                type="password"
                id="registerPassword"
                name="password"
                autocomplete="new-password"
                placeholder="Password (minimum 8 characters)"
        >

        <button
                class="submit-btn"
                onclick="register()">
            Create Account
        </button>

    </div>


    <div id="message"></div>

    <div id="tokenBox" class="token-box"></div>

</div>


<script>

    function showLogin() {

        document
            .getElementById("loginForm")
            .classList.add("active");

        document
            .getElementById("registerForm")
            .classList.remove("active");

        document
            .getElementById("loginTab")
            .classList.add("active");

        document
            .getElementById("registerTab")
            .classList.remove("active");
    }


    function showRegister() {

        document
            .getElementById("registerForm")
            .classList.add("active");

        document
            .getElementById("loginForm")
            .classList.remove("active");

        document
            .getElementById("registerTab")
            .classList.add("active");

        document
            .getElementById("loginTab")
            .classList.remove("active");
    }


    async function register() {

        const name =
            document.getElementById("registerName").value;

        const email =
            document.getElementById("registerEmail").value;

        const password =
            document.getElementById("registerPassword").value;


        if (!name || !email || !password) {

            showMessage(
                "Please fill all fields.",
                true
            );

            return;
        }


        try {

            const response =
                await fetch("/api/auth/register", {

                    method: "POST",

                    headers: {
                        "Content-Type":
                            "application/json"
                    },

                    body: JSON.stringify({
                        name: name,
                        email: email,
                        password: password
                    })
                });


            const data =
                await response.json();


            if (!response.ok) {

                showMessage(
                    data.message ||
                    "Registration failed.",
                    true
                );

                return;
            }


            saveAuthentication(data,"/live-enroll");

            showMessage(
                "Registration successful.",
                false
            );


        } catch (error) {

            showMessage(
                "Server error.",
                true
            );

            console.error(error);
        }
    }


    async function login() {

        const email =
            document.getElementById("loginEmail").value;

        const password =
            document.getElementById("loginPassword").value;

        // TEMPORARY DEBUG
        console.log("LOGIN EMAIL:", email);
        console.log("LOGIN PASSWORD LENGTH:", password.length);

        if (!email || !password) {

            showMessage(
                "Please enter email and password.",
                true
            );

            return;
        }

        try {

            const payload = {
                email: email,
                password: password
            };

            // TEMPORARY DEBUG
            console.log(
                "LOGIN PAYLOAD PASSWORD LENGTH:",
                payload.password.length
            );

            const response =
                await fetch("/api/auth/login", {

                    method: "POST",

                    headers: {
                        "Content-Type":
                            "application/json"
                    },

                    body: JSON.stringify(payload)
                });

            const data =
                await response.json();

            if (!response.ok) {

                showMessage(
                    data.message ||
                    "Login failed.",
                    true
                );

                return;
            }

            saveAuthentication(data,"/live");

            showMessage(
                "Login successful.",
                false
            );

        } catch (error) {

            showMessage(
                "Server error.",
                true
            );

            console.error(error);
        }
    }


    function saveAuthentication(data,redirectUrl) {

        localStorage.setItem(
            "jwt",
            data.token
        );

        localStorage.setItem(
            "userId",
            data.userId
        );

        localStorage.setItem(
            "userName",
            data.name
        );

        localStorage.setItem(
            "userEmail",
            data.email
        );


        document.getElementById("tokenBox")
            .style.display = "block";

        document.getElementById("tokenBox")
            .innerText =
            "JWT saved successfully.";


        setTimeout(() => {

            window.location.href =
                redirectUrl;

        }, 800);
    }


    function showMessage(message, error) {

        const element =
            document.getElementById("message");

        element.innerText = message;

        element.style.color =
            error ? "red" : "green";
    }

</script>

</body>
</html>