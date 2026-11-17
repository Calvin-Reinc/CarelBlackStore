<%@ page contentType="text/html;charset=UTF-8" %>
<%
    if (session == null || session.getAttribute("adminUser") == null) {
        response.sendRedirect("login.html");
        return;
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Carel Black - Admin Registration</title>

    <!-- Bootstrap 5 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
          rel="stylesheet"
          integrity="sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH"
          crossorigin="anonymous">

    <!-- Google Fonts: Exile -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Exile&display=swap" rel="stylesheet">

    <style>
        html, body {
            background-color: #ffffff;
            color: #111111;
            margin: 0;
        }

        .auth-wrapper {
            min-height: 100vh;
            min-height: 100dvh;
        }

        .auth-card {
            width: 100%;
            max-width: 400px;
            background-color: #ffffff;
            border: 1px solid #111111;
            border-radius: 0;
            box-shadow: 0 12px 32px rgba(0, 0, 0, 0.06);
        }

        .brand-logo {
            width: clamp(56px, 14vw, 76px);
            height: auto;
            background-color: transparent;
            display: block;
            margin-inline: auto;
        }

        .brand-title {
            font-family: "Exile", system-ui, -apple-system, sans-serif;
            font-size: clamp(2rem, 6vw, 2.75rem);
            line-height: 1.05;
            letter-spacing: 0.02em;
            color: #111111;
        }

        .form-label {
            font-size: 0.875rem;
            font-weight: 500;
            color: #111111;
        }

        .form-control {
            border-radius: 0;
            border: 1px solid #ced4da;
            min-height: 44px;
            color: #111111;
        }

        .form-control:focus {
            border-color: #111111;
            box-shadow: 0 0 0 0.2rem rgba(17, 17, 17, 0.12);
        }

        .alert {
            border-radius: 0;
            font-size: 0.9rem;
        }

        .btn-register {
            width: 100%;
            min-height: 48px;
            border-radius: 0;
            font-weight: 500;
            letter-spacing: 0.04em;
            color: #ffffff;
            border: 1px solid #111111;
            background-color: #111111;
            background-image: linear-gradient(135deg, #111111 0%, #111111 44%, #6c757d 72%, #000000 100%);
            background-size: 260% 260%;
            background-position: 0% 50%;
            transition: background-position 0.35s ease, border-color 0.3s ease, transform 0.2s ease, box-shadow 0.25s ease;
        }

        .btn-register:hover:not(:disabled),
        .btn-register:focus-visible:not(:disabled) {
            color: #ffffff;
            border-color: #343a40;
            background-position: 100% 50%;
            transform: translateY(-1px);
            box-shadow: 0 8px 20px rgba(0, 0, 0, 0.16);
        }

        .btn-register:disabled {
            opacity: 0.7;
            cursor: wait;
            color: #ffffff;
        }

        .auth-link {
            color: #111111;
            font-weight: 500;
            text-decoration: underline;
            text-underline-offset: 3px;
        }

        .auth-link:hover {
            color: #6c757d;
        }
    </style>
</head>
<body class="bg-white">

    <main class="auth-wrapper d-flex align-items-center justify-content-center px-3 py-4">
        <div class="auth-card p-4 p-sm-5">
            <div class="text-center mb-4">
                <img src="assets/img/logo.png"
                     alt="Carel Black Logo"
                     class="brand-logo img-fluid mb-2">
                <h1 class="brand-title mb-1">Carel Black</h1>
                <p class="text-muted small mb-0">Create Admin Account</p>
            </div>

            <div id="alertBox" class="alert d-none" role="alert"></div>

            <form id="registerForm">
                <div class="mb-3">
                    <label for="username" class="form-label">Username</label>
                    <input type="text"
                           class="form-control"
                           id="username"
                           required
                           autocomplete="username">
                </div>

                <div class="mb-3">
                    <label for="password" class="form-label">Password</label>
                    <input type="password"
                           class="form-control"
                           id="password"
                           required
                           autocomplete="new-password">
                </div>

                <div class="mb-4">
                    <label for="confirmPassword" class="form-label">Confirm Password</label>
                    <input type="password"
                           class="form-control"
                           id="confirmPassword"
                           required
                           autocomplete="new-password">
                </div>

                <button type="submit" id="submitBtn" class="btn btn-register">
                    Register
                </button>
            </form>

            <div class="text-center mt-4">
                <small class="text-muted d-block mb-2">
                    Already have an account? <a href="login.html" class="auth-link">Sign In</a>
                </small>
                <small>
                    <a href="dashboard.jsp" class="text-muted text-decoration-none">&larr; Back to Dashboard</a>
                </small>
            </div>
        </div>
    </main>

    <script>
        document.getElementById('registerForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            const username = document.getElementById('username').value.trim();
            const password = document.getElementById('password').value;
            const confirmPassword = document.getElementById('confirmPassword').value;
            const alertBox = document.getElementById('alertBox');
            const submitBtn = document.getElementById('submitBtn');

            // Reset alert state
            alertBox.className = 'alert d-none';

            // Client-side validation check
            if (password !== confirmPassword) {
                alertBox.textContent = 'Passwords do not match.';
                alertBox.className = 'alert alert-danger';
                return;
            }

            submitBtn.disabled = true;
            submitBtn.textContent = 'Registering...';

            try {
                const response = await fetch('api/auth/register', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username, password })
                });

                if (response.ok) {
                    alertBox.textContent = 'Registration successful! Redirecting to login...';
                    alertBox.className = 'alert alert-success';
                    setTimeout(() => {
                        window.location.href = 'login.html';
                    }, 1500);
                } else {
                    const errData = await response.json().catch(() => ({ error: 'Registration failed' }));
                    alertBox.textContent = errData.error || 'Could not complete registration.';
                    alertBox.className = 'alert alert-danger';
                    submitBtn.disabled = false;
                    submitBtn.textContent = 'Register';
                }
            } catch (err) {
                alertBox.textContent = 'Unable to connect to the server. Please check your network.';
                alertBox.className = 'alert alert-danger';
                submitBtn.disabled = false;
                submitBtn.textContent = 'Register';
            }
        });
    </script>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"
            integrity="sha384-YvpcrYf0tY3lHB60NNkmXc5s9fDVZLESaAA55NDzOxhy9GkcIdslK1eN7N6jIeHz"
            crossorigin="anonymous"></script>
</body>
</html>