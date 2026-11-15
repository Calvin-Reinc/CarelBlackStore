<%-- 
    Document   : login
    Created on : Nov 14, 2026, 9:13:08 PM
    Author     : squid
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CarelStock - Login</title>
    <!-- Bootstrap 5 CSS CDN -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css">
</head>
<body class="bg-light d-flex align-items-center justify-content-center vh-100">

    <div class="card shadow-sm p-4 border-0" style="width: 100%; max-width: 400px;">
        <div class="text-center mb-4">
            <i class="bi bi-box-seam text-primary fs-1"></i>
            <h3 class="fw-bold mt-2">CarelStock</h3>
            <p class="text-muted">Sign in to manage inventory</p>
        </div>

        <!-- Error Alert Banner -->
        <div id="errorAlert" class="alert alert-danger d-none" role="alert"></div>

        <form id="loginForm">
            <div class="mb-3">
                <label for="username" class="form-label">Username</label>
                <input type="text" class="form-control" id="username" required autocomplete="username">
            </div>
            <div class="mb-3">
                <label for="password" class="form-label">Password</label>
                <input type="password" class="form-control" id="password" required autocomplete="current-password">
            </div>
            <button type="submit" class="btn btn-primary w-100 py-2 fw-semibold">Sign In</button>
        </form>
    </div>

    <!-- Client-Side JavaScript for Authentication -->
    <script>
        document.getElementById('loginForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const username = document.getElementById('username').value.trim();
            const password = document.getElementById('password').value;
            const errorAlert = document.getElementById('errorAlert');

            // Reset error banner
            errorAlert.classList.add('d-none');
            errorAlert.textContent = '';

            try {
                const response = await fetch('${pageContext.request.contextPath}/api/auth/login', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    credentials: 'include', // Ensure session cookie is accepted and stored
                    body: JSON.stringify({ username, password })
                });

                if (response.ok) {
                    // Redirect to dashboard on successful authentication
                    window.location.href = 'dashboard.jsp';
                } else {
                    const data = await response.json();
                    errorAlert.textContent = data.error || 'Invalid credentials. Please try again.';
                    errorAlert.classList.remove('d-none');
                }
            } catch (error) {
                console.error('Login request failed:', error);
                errorAlert.textContent = 'Network error. Could not connect to the server.';
                errorAlert.classList.remove('d-none');
            }
        });
    </script>
</body>
</html>
