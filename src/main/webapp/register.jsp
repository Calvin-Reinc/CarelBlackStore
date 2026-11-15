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
    <title>CarelStock - Admin Registration</title>
    <!-- Bootstrap 5 CDN -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light d-flex align-items-center justify-content-center vh-100">

    <div class="card shadow-sm p-4" style="width: 100%; max-width: 400px;">
        <div class="text-center mb-4">
            <h2 class="h3 fw-bold text-dark">CarelStock</h2>
            <p class="text-muted">Create Admin Account</p>
        </div>

        <div id="alertBox" class="alert d-none" role="alert"></div>

        <form id="registerForm">
            <div class="mb-3">
                <label for="username" class="form-label">Username</label>
                <input type="text" class="form-control" id="username" required autocomplete="username">
            </div>
            <div class="mb-3">
                <label for="password" class="form-label">Password</label>
                <input type="password" class="form-control" id="password" required autocomplete="new-password">
            </div>
            <div class="mb-3">
                <label for="confirmPassword" class="form-label">Confirm Password</label>
                <input type="password" class="form-control" id="confirmPassword" required autocomplete="new-password">
            </div>
            <button type="submit" class="btn btn-success w-100 py-2">Register</button>
        </form>

        <div class="text-center mt-3">
            <small class="text-muted">Already have an account? <a href="login.html" class="text-decoration-none">Sign In</a></small>
        </div>
    </div>

    <script>
        document.getElementById('registerForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            const username = document.getElementById('username').value.trim();
            const password = document.getElementById('password').value;
            const confirmPassword = document.getElementById('confirmPassword').value;
            const alertBox = document.getElementById('alertBox');

            // Reset alert state
            alertBox.className = 'alert d-none';

            // Client-side validation check
            if (password !== confirmPassword) {
                alertBox.textContent = 'Passwords do not match.';
                alertBox.className = 'alert alert-danger';
                return;
            }

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
                }
            } catch (err) {
                alertBox.textContent = 'Unable to connect to the server. Please check your network.';
                alertBox.className = 'alert alert-danger';
            }
        });
    </script>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>