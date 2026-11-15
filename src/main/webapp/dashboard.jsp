<%-- 
    Document   : dashboard
    Created on : Nov 14, 2026, 9:12:32 PM
    Author     : squid
--%>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CarelStock - Admin Dashboard</title>
    <!-- Bootstrap 5 CSS CDN -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css">
</head>
<body class="bg-light">

    <!-- Navigation Bar -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark shadow-sm">
        <div class="container-fluid">
            <a class="navbar-brand fw-bold" href="#"><i class="bi bi-box-seam me-2"></i>CarelStock Admin</a>
            <div class="d-flex align-items-center">
                <span class="text-light me-3" id="welcomeUser">Welcome, Admin</span>
                <button class="btn btn-outline-light btn-sm" id="logoutBtn"><i class="bi bi-box-arrow-right me-1"></i>Logout</button>
            </div>
        </div>
    </nav>

    <!-- Main Container -->
    <div class="container my-4">
        <div class="row mb-4">
            <div class="col-12">
                <h2>Dashboard Overview</h2>
                <p class="text-muted">Real-time inventory metrics and stock health.</p>
            </div>
        </div>

        <!-- Metric Cards Row -->
        <div class="row g-4 mb-4">
            <div class="col-md-4">
                <div class="card border-0 shadow-sm bg-primary text-white">
                    <div class="card-body">
                        <h6 class="card-title text-uppercase">Total Products</h6>
                        <h2 class="display-6 fw-bold" id="totalProducts">-</h2>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card border-0 shadow-sm bg-success text-white">
                    <div class="card-body">
                        <h6 class="card-title text-uppercase">Inventory Value</h6>
                        <h2 class="display-6 fw-bold" id="totalValue">$0.00</h2>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card border-0 shadow-sm bg-warning text-dark">
                    <div class="card-body">
                        <h6 class="card-title text-uppercase">Total Categories</h6>
                        <h2 class="display-6 fw-bold" id="totalCategories">-</h2>
                    </div>
                </div>
            </div>
        </div>

        <!-- Quick Actions / Management Section -->
        <div class="row">
            <div class="col-12">
                <div class="card border-0 shadow-sm">
                    <div class="card-body">
                        <h5 class="card-title mb-3">Quick Navigation</h5>
                        <div class="d-flex gap-2">
                            <a href="products.jsp" class="btn btn-outline-primary"><i class="bi bi-tags me-1"></i>Manage Products</a>
                            <a href="categories.jsp" class="btn btn-outline-secondary"><i class="bi bi-folder me-1"></i>Manage Categories</a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Client-Side JavaScript -->
    <script>
        // Fetch dashboard metrics on load
        window.addEventListener('DOMContentLoaded', async () => {
            try {
                const response = await fetch('${pageContext.request.contextPath}/api/dashboard', {
                    method: 'GET',
                    credentials: 'include' // Sends session cookie
                });

                if (response.status === 401) {
                    // Redirect to login if unauthorized
                    window.location.href = 'login.jsp';
                    return;
                }

                if (response.ok) {
                    const data = await response.json();
                    document.getElementById('totalProducts').textContent = data.total_products || 0;
                    document.getElementById('totalValue.').textContent = '$' + (data.total_inventory_value || 0).toFixed(2);
                    document.getElementById('totalCategories').textContent = data.total_categories || 0;
                }
            } catch (error) {
                console.error('Failed to load dashboard metrics:', error);
            }
        });

        // Handle Logout
        document.getElementById('logoutBtn').addEventListener('click', async () => {
            const response = await fetch('${pageContext.request.contextPath}/api/auth/logout', {
                method: 'POST',
                credentials: 'include'
            });
            if (response.ok) {
                window.location.href = 'login.jsp';
            }
        });
    </script>
</body>
</html>