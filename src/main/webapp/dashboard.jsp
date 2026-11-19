<%@ page contentType="text/html;charset=UTF-8" %>
<%
    if (session == null || session.getAttribute("adminUser") == null) {
        response.sendRedirect("login.html");
        return;
    }

    Object adminAttr = session.getAttribute("adminUser");
    String adminUsername = "Admin";
    if (adminAttr instanceof String) {
        adminUsername = (String) adminAttr;
    } else if (adminAttr != null) {
        try {
            Object val = adminAttr.getClass().getMethod("getUsername").invoke(adminAttr);
            if (val != null) adminUsername = String.valueOf(val);
        } catch (Exception ignored) {
            adminUsername = String.valueOf(adminAttr);
        }
    }
    String safeAdminUsername = adminUsername
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
%>
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>flying Jvck - Admin Dashboard</title>

    <!-- Bootstrap 5 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
          rel="stylesheet"
          integrity="sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH"
          crossorigin="anonymous">
    <!-- Bootstrap Icons -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
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

        .top-header {
            display: grid;
            grid-template-columns: 1fr auto 1fr;
            align-items: center;
            height: 64px;
            background-color: #ffffff;
            border-bottom: 1px solid #e5e7eb;
            position: sticky;
            top: 0;
            z-index: 1020;
        }

        .header-logo {
            height: 38px;
            width: auto;
            background-color: transparent;
            display: block;
        }

        .brand-font {
            font-family: "Exile", system-ui, -apple-system, sans-serif;
            letter-spacing: 0.02em;
        }

        .card,
        .modal-content,
        .offcanvas,
        .form-control,
        .form-select,
        .btn,
        .alert,
        .badge {
            border-radius: 0 !important;
        }

        .card {
            border: 1px solid #e5e7eb;
            box-shadow: 0 6px 20px rgba(0, 0, 0, 0.03);
        }

        .modal-content {
            border: 1px solid #111111;
        }

        .form-control:focus,
        .form-select:focus {
            border-color: #111111;
            box-shadow: 0 0 0 0.2rem rgba(17, 17, 17, 0.12);
        }

        .btn-carel {
            color: #ffffff;
            border: 1px solid #111111;
            background-color: #111111;
            background-image: linear-gradient(135deg, #111111 0%, #111111 44%, #6c757d 72%, #000000 100%);
            background-size: 260% 260%;
            background-position: 0% 50%;
            transition: background-position 0.35s ease, border-color 0.3s ease, transform 0.15s ease;
        }

        .btn-carel:hover:not(:disabled),
        .btn-carel:focus-visible:not(:disabled) {
            color: #ffffff;
            border-color: #343a40;
            background-position: 100% 50%;
            transform: translateY(-1px);
        }

        .btn-carel:disabled {
            opacity: 0.65;
            color: #ffffff;
        }

        .menu-nav-link {
            color: #111111;
            padding: 0.7rem 1rem;
            border: 1px solid transparent;
            text-decoration: none;
            display: flex;
            align-items: center;
            font-weight: 500;
            transition: background-color 0.2s ease, border-color 0.2s ease;
        }

        .menu-nav-link:hover {
            background-color: #f8f9fa;
            border-color: #dee2e6;
            color: #111111;
        }

        .menu-nav-link.active {
            background-color: #111111;
            color: #ffffff;
            border-color: #111111;
        }

        .product-thumb {
            width: 2.75rem;
            height: 2.75rem;
            object-fit: cover;
            border: 1px solid #dee2e6;
            background-color: #f8f9fa;
            display: block;
        }

        .product-thumb-placeholder {
            width: 2.75rem;
            height: 2.75rem;
            border: 1px solid #dee2e6;
            background-color: #f8f9fa;
            display: flex;
            align-items: center;
            justify-content: center;
            color: #adb5bd;
        }

        .product-link {
            color: #111111;
            font-weight: 500;
            text-decoration: none;
        }

        .product-link:hover {
            text-decoration: underline;
            text-underline-offset: 3px;
        }

        .modal-image-preview {
            width: 88px;
            height: 88px;
            object-fit: cover;
            border: 1px solid #dee2e6;
            background-color: #f8f9fa;
        }

        .table thead th {
            font-size: 0.8rem;
            text-transform: uppercase;
            letter-spacing: 0.04em;
            font-weight: 600;
        }

        .msg-cell {
            max-width: 360px;
            white-space: pre-wrap;
            word-break: break-word;
        }
    </style>
</head>
<body class="bg-white">

    <!-- Top Header: Left Hamburger | Center Logo | Right Account -->
    <header class="top-header px-3 px-md-4">
        <div class="d-flex justify-content-start">
            <button type="button"
                    class="btn btn-outline-dark btn-sm d-inline-flex align-items-center justify-content-center"
                    style="width: 40px; height: 40px;"
                    data-bs-toggle="offcanvas"
                    data-bs-target="#menuOffcanvas"
                    aria-controls="menuOffcanvas"
                    aria-label="Open navigation menu">
                <i class="bi bi-list fs-4"></i>
            </button>
        </div>

        <div class="d-flex justify-content-center">
            <a href="dashboard.jsp" class="d-inline-flex align-items-center text-decoration-none" title="Admin Dashboard">
                <img src="assets/img/logo.png"
                     onerror="if (!this.dataset.fb) { this.dataset.fb = '1'; this.src = 'assets/img/Artwork.png'; }"
                     alt="flying Jvck Logo"
                     class="header-logo">
            </a>
        </div>

        <div class="d-flex justify-content-end">
            <button type="button"
                    class="btn btn-outline-dark btn-sm d-inline-flex align-items-center gap-2 px-3"
                    style="height: 40px;"
                    data-bs-toggle="modal"
                    data-bs-target="#accountModal"
                    aria-label="Open account window">
                <i class="bi bi-person-circle fs-6"></i>
                <span class="d-none d-sm-inline">Account</span>
            </button>
        </div>
    </header>

    <!-- Left Offcanvas Navigation Window -->
    <div class="offcanvas offcanvas-start" tabindex="-1" id="menuOffcanvas" aria-labelledby="menuOffcanvasLabel">
        <div class="offcanvas-header border-bottom">
            <div class="d-flex align-items-center gap-2">
                <img src="assets/img/logo.png"
                     onerror="if (!this.dataset.fb) { this.dataset.fb = '1'; this.src = 'assets/img/Artwork.png'; }"
                     alt="Logo"
                     style="height: 44px; width: auto;">
                <h5 class="offcanvas-title brand-font fs-3 mb-0" id="menuOffcanvasLabel">Admin</h5>
            </div>
            <button type="button" class="btn-close" data-bs-dismiss="offcanvas" aria-label="Close"></button>
        </div>
        <div class="offcanvas-body d-flex flex-column p-3">
            <p class="text-uppercase text-muted small fw-semibold mb-2 px-2">Store Management</p>
            <nav class="d-flex flex-column gap-1">
                <a class="menu-nav-link active" href="#" onclick="switchTab('products', event)" id="navProducts">
                    <i class="bi bi-box-seam me-3"></i> Products
                </a>
                <a class="menu-nav-link" href="#" onclick="switchTab('categories', event)" id="navCategories">
                    <i class="bi bi-tags me-3"></i> Categories
                </a>
                <a class="menu-nav-link" href="#" onclick="switchTab('stock', event)" id="navStock">
                    <i class="bi bi-clipboard-data me-3"></i> Stock Management
                </a>
                <a class="menu-nav-link" href="#" onclick="switchTab('orders', event)" id="navOrders">
                    <i class="bi bi-receipt me-3"></i> Orders
                </a>
            </nav>

            <hr class="my-3">

            <p class="text-uppercase text-muted small fw-semibold mb-2 px-2">Community</p>
            <nav class="d-flex flex-column gap-1">
                <a class="menu-nav-link" href="#" onclick="switchTab('subscribers', event)" id="navSubscribers">
                    <i class="bi bi-envelope-check me-3"></i> Subscribers
                </a>
                <a class="menu-nav-link" href="#" onclick="switchTab('messages', event)" id="navMessages">
                    <i class="bi bi-chat-left-text me-3"></i> Messages
                </a>
            </nav>

            <hr class="my-3">

            <p class="text-uppercase text-muted small fw-semibold mb-2 px-2">Admin</p>
            <a class="menu-nav-link" href="register.jsp">
                <i class="bi bi-person-plus me-3"></i> Register New User
            </a>
        </div>
    </div>

    <!-- Right Account / Profile Settings & Logout Window -->
    <div class="modal fade" id="accountModal" tabindex="-1" aria-labelledby="accountModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-sm">
            <div class="modal-content">
                <div class="modal-header border-bottom">
                    <h5 class="modal-title fs-6 fw-semibold" id="accountModalLabel">
                        <i class="bi bi-person-circle me-2"></i>Account
                    </h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-4">
                    <div class="mb-3">
                        <span class="text-uppercase text-muted d-block" style="font-size: 0.72rem; letter-spacing: 0.05em;">Signed in as</span>
                        <div class="fs-5 fw-semibold text-dark"><%= safeAdminUsername %></div>
                        <span class="badge bg-dark mt-1">Administrator</span>
                    </div>

                    <div class="border p-3 bg-light mb-4">
                        <div class="fw-semibold small mb-1">
                            <i class="bi bi-gear me-1"></i> Profile Settings
                        </div>
                        <p class="text-muted small mb-0">
                            Account editing &amp; password management are not enabled yet.
                        </p>
                    </div>

                    <div class="d-grid gap-2">
                        <a href="register.jsp" class="btn btn-outline-dark btn-sm py-2">
                            <i class="bi bi-person-plus me-1"></i> Register New Admin
                        </a>
                        <button type="button" class="btn btn-carel btn-sm py-2" onclick="logout()">
                            <i class="bi bi-box-arrow-right me-1"></i> Logout
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Main Content Area -->
    <main class="container-xl px-3 px-md-4 py-4">
        <div class="d-flex justify-content-between flex-wrap align-items-center gap-2 pb-3 mb-4 border-bottom">
            <h1 class="h3 fw-semibold mb-0" id="pageTitle">Product Management</h1>
            <div id="alertContainer"></div>
        </div>

        <!-- PRODUCTS SECTION -->
        <div id="productsSection" class="content-section">
            <div class="card mb-4">
                <div class="card-body">
                    <div class="row g-2 align-items-center">
                        <div class="col-12 col-sm-auto">
                            <input type="number" id="searchProductId" class="form-control" placeholder="Search by Product ID...">
                        </div>
                        <div class="col-auto">
                            <button type="button" class="btn btn-outline-dark" onclick="searchProductById()">Search</button>
                            <button type="button" class="btn btn-outline-secondary" onclick="loadProducts()">Reset</button>
                        </div>
                        <div class="col-12 col-sm-auto ms-sm-auto d-grid d-sm-block">
                            <button type="button" class="btn btn-carel" onclick="openProductModal('add')">
                                <i class="bi bi-plus-lg me-1"></i> Add Product
                            </button>
                        </div>
                    </div>
                </div>
            </div>

            <div class="card">
                <div class="card-body p-0 table-responsive">
                    <table class="table table-hover align-middle mb-0">
                        <thead class="table-dark">
                            <tr>
                                <th class="ps-3">Image</th>
                                <th>ID</th>
                                <th>Name</th>
                                <th>Description</th>
                                <th>Price</th>
                                <th>Stock</th>
                                <th>Category</th>
                                <th class="pe-3 text-end">Actions</th>
                            </tr>
                        </thead>
                        <tbody id="productsTableBody"></tbody>
                    </table>
                </div>
            </div>
        </div>

        <!-- CATEGORIES SECTION -->
        <div id="categoriesSection" class="content-section" style="display: none;">
            <div class="card mb-4">
                <div class="card-body text-end">
                    <button type="button" class="btn btn-carel" onclick="openCategoryModal()">
                        <i class="bi bi-plus-lg me-1"></i> Add Category
                    </button>
                </div>
            </div>
            <div class="card">
                <div class="card-body p-0 table-responsive">
                    <table class="table table-hover align-middle mb-0">
                        <thead class="table-dark">
                            <tr>
                                <th class="ps-3">ID</th>
                                <th>Category Name</th>
                                <th>Description</th>
                                <th class="pe-3 text-end">Actions</th>
                            </tr>
                        </thead>
                        <tbody id="categoriesTableBody"></tbody>
                    </table>
                </div>
            </div>
        </div>

        <!-- STOCK MANAGEMENT SECTION -->
        <div id="stockSection" class="content-section" style="display: none;">
            <div class="card">
                <div class="card-header bg-dark text-white">
                    <h5 class="mb-0 fs-6">Inventory Stock Levels</h5>
                </div>
                <div class="card-body p-0 table-responsive">
                    <table class="table table-hover align-middle mb-0">
                        <thead class="table-light">
                            <tr>
                                <th class="ps-3">Product ID</th>
                                <th>Product Name</th>
                                <th>Current Stock</th>
                                <th>Status</th>
                                <th class="pe-3 text-end">Quick Adjust</th>
                            </tr>
                        </thead>
                        <tbody id="stockTableBody"></tbody>
                    </table>
                </div>
            </div>
        </div>

        <!-- ORDERS SECTION -->
        <div id="ordersSection" class="content-section" style="display: none;">
            <div class="card">
                <div class="card-body p-0 table-responsive">
                    <table class="table table-hover align-middle mb-0">
                        <thead class="table-dark">
                            <tr>
                                <th class="ps-3">Order ID</th>
                                <th>Customer</th>
                                <th>Items</th>
                                <th>Total</th>
                                <th>Status</th>
                                <th>Date</th>
                                <th class="pe-3 text-end">Actions</th>
                            </tr>
                        </thead>
                        <tbody id="ordersTableBody"></tbody>
                    </table>
                </div>
            </div>
        </div>

        <!-- SUBSCRIBERS SECTION -->
        <div id="subscribersSection" class="content-section" style="display: none;">
            <div class="card mb-4">
                <div class="card-body d-flex flex-wrap justify-content-between align-items-center gap-2">
                    <span class="small text-muted">
                        Total Subscribers: <strong class="text-dark" id="subscriberCount">0</strong>
                    </span>
                    <button type="button" class="btn btn-outline-dark btn-sm" onclick="copyAllSubscriberEmails()">
                        <i class="bi bi-clipboard me-1"></i> Copy All Emails
                    </button>
                </div>
            </div>
            <div class="card">
                <div class="card-body p-0 table-responsive">
                    <table class="table table-hover align-middle mb-0">
                        <thead class="table-dark">
                            <tr>
                                <th class="ps-3">ID</th>
                                <th>Email</th>
                                <th>Subscribed At</th>
                                <th class="pe-3 text-end">Actions</th>
                            </tr>
                        </thead>
                        <tbody id="subscribersTableBody"></tbody>
                    </table>
                </div>
            </div>
        </div>

        <!-- CONTACT MESSAGES SECTION -->
        <div id="messagesSection" class="content-section" style="display: none;">
            <div class="card">
                <div class="card-body p-0 table-responsive">
                    <table class="table table-hover align-middle mb-0">
                        <thead class="table-dark">
                            <tr>
                                <th class="ps-3">ID</th>
                                <th>From</th>
                                <th>Message</th>
                                <th>Received</th>
                                <th class="pe-3 text-end">Actions</th>
                            </tr>
                        </thead>
                        <tbody id="messagesTableBody"></tbody>
                    </table>
                </div>
            </div>
        </div>
    </main>

    <!-- Order Details Modal -->
    <div class="modal fade" id="orderDetailModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-lg modal-dialog-centered">
            <div class="modal-content">
                <div class="modal-header border-bottom">
                    <h5 class="modal-title fw-semibold" id="orderDetailTitle">Order #0</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-4">
                    <div class="row g-3 mb-4">
                        <div class="col-md-6">
                            <div class="border p-3 h-100 bg-light">
                                <div class="text-uppercase text-muted small fw-semibold mb-2">Customer &amp; Delivery</div>
                                <div class="fw-semibold" id="odCustomerName">-</div>
                                <div class="small text-muted" id="odCustomerEmail">-</div>
                                <div class="small text-muted mb-2" id="odCustomerPhone">-</div>
                                <div class="small" id="odShippingAddress" style="white-space: pre-line;">-</div>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="border p-3 h-100">
                                <div class="text-uppercase text-muted small fw-semibold mb-2">Payment &amp; Status</div>
                                <div class="small mb-1"><span class="text-muted">Status:</span> <span class="badge bg-dark" id="odStatus">-</span></div>
                                <div class="small mb-1"><span class="text-muted">Paystack Ref:</span> <span class="fw-medium" id="odPaymentRef">-</span></div>
                                <div class="small mb-1"><span class="text-muted">Date:</span> <span id="odCreatedAt">-</span></div>
                                <div class="fs-5 fw-bold mt-2" id="odTotal">R 0.00</div>
                            </div>
                        </div>
                    </div>

                    <div class="text-uppercase text-muted small fw-semibold mb-2">Items Ordered</div>
                    <div class="table-responsive border">
                        <table class="table table-sm align-middle mb-0">
                            <thead class="table-light">
                                <tr>
                                    <th class="ps-3">Product</th>
                                    <th class="text-center">Qty</th>
                                    <th class="text-end">Unit Price</th>
                                    <th class="pe-3 text-end">Subtotal</th>
                                </tr>
                            </thead>
                            <tbody id="odItemsBody"></tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Product Modal (Add/Edit with Image Upload) -->
    <div class="modal fade" id="productModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <div class="modal-header border-bottom">
                    <h5 class="modal-title fw-semibold" id="productModalTitle">Add Product</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-4">
                    <form id="productForm" enctype="multipart/form-data">
                        <input type="hidden" id="modalProductId">

                        <div class="mb-3">
                            <label for="modalProductName" class="form-label small fw-semibold">Product Name</label>
                            <input type="text" class="form-control" id="modalProductName" required>
                        </div>

                        <div class="mb-3">
                            <label for="modalProductDesc" class="form-label small fw-semibold">Description</label>
                            <textarea class="form-control" id="modalProductDesc" rows="2"></textarea>
                        </div>

                        <div class="row g-3 mb-3">
                            <div class="col-sm-6">
                                <label for="modalProductPrice" class="form-label small fw-semibold">Price (R)</label>
                                <input type="number" step="0.01" min="0" class="form-control" id="modalProductPrice" required>
                            </div>
                            <div class="col-sm-6">
                                <label for="modalProductStock" class="form-label small fw-semibold">Stock Qty</label>
                                <input type="number" min="0" class="form-control" id="modalProductStock" required>
                            </div>
                        </div>

                        <div class="mb-3">
                            <label for="modalProductCategory" class="form-label small fw-semibold">Category</label>
                            <select class="form-select" id="modalProductCategory" required>
                                <option value="" disabled selected>Loading categories...</option>
                            </select>
                        </div>

                        <div class="mb-4">
                            <label for="modalProductImage" class="form-label small fw-semibold">Product Image (Optional)</label>
                            <div class="d-flex align-items-center gap-3">
                                <img id="modalImagePreview"
                                     src=""
                                     alt="Preview"
                                     class="modal-image-preview d-none">
                                <div class="flex-grow-1">
                                    <input type="file"
                                           class="form-control"
                                           id="modalProductImage"
                                           accept="image/jpeg,image/png,image/webp,image/gif">
                                    <small class="text-muted d-block mt-1">JPEG, PNG, WebP, or GIF (max 5 MB).</small>
                                </div>
                            </div>
                        </div>

                        <button type="submit" id="saveProductBtn" class="btn btn-carel w-100 py-2">
                            Save Product
                        </button>
                    </form>
                </div>
            </div>
        </div>
    </div>

    <!-- Category Modal (Add) -->
    <div class="modal fade" id="categoryModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <div class="modal-header border-bottom">
                    <h5 class="modal-title fw-semibold">Add Category</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-4">
                    <form id="categoryForm">
                        <div class="mb-3">
                            <label for="modalCategoryName" class="form-label small fw-semibold">Category Name</label>
                            <input type="text" class="form-control" id="modalCategoryName" required>
                        </div>
                        <div class="mb-4">
                            <label for="modalCategoryDesc" class="form-label small fw-semibold">Description</label>
                            <textarea class="form-control" id="modalCategoryDesc" rows="2"></textarea>
                        </div>
                        <button type="submit" class="btn btn-carel w-100 py-2">Save Category</button>
                    </form>
                </div>
            </div>
        </div>
    </div>

    <!-- Bootstrap JS Bundle -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"
            integrity="sha384-YvpcrYf0tY3lHB60NNkmXc5s9fDVZLESaAA55NDzOxhy9GkcIdslK1eN7N6jIeHz"
            crossorigin="anonymous"></script>

    <script>
        let productModal, categoryModal, orderDetailModal, menuOffcanvas;
        const productsCache = new Map();
        const categoriesCache = new Map();
        const ordersCache = new Map();
        let cachedSubscribers = [];

        document.addEventListener('DOMContentLoaded', () => {
            productModal = new bootstrap.Modal(document.getElementById('productModal'));
            categoryModal = new bootstrap.Modal(document.getElementById('categoryModal'));
            orderDetailModal = new bootstrap.Modal(document.getElementById('orderDetailModal'));
            menuOffcanvas = bootstrap.Offcanvas.getOrCreateInstance(document.getElementById('menuOffcanvas'));
            loadProducts();
        });

        function formatDate(raw) {
            if (!raw || raw === 'N/A') return 'N/A';
            const d = new Date(raw);
            return isNaN(d.getTime()) ? String(raw) : d.toLocaleString();
        }

        function escapeHtml(value) {
            return String(value ?? '')
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#39;');
        }

        async function fetchCategoryMap() {
            try {
                const res = await fetch('api/categories');
                if (res.status === 401) {
                    window.location.href = 'login.html';
                    return;
                }
                if (!res.ok) {
                    populateCategorySelect();
                    return;
                }
                const categories = await res.json();
                categoriesCache.clear();

                if (Array.isArray(categories)) {
                    categories.forEach(c => {
                        const cid = c.category_id ?? 0;
                        const cname = c.category_name ?? `Category #\${cid}`;
                        if (cid > 0) categoriesCache.set(cid, cname);
                    });
                }
                populateCategorySelect();
            } catch (err) {
                console.error('Error fetching categories:', err);
                populateCategorySelect();
            }
        }

        function populateCategorySelect(selectedId = '') {
            const select = document.getElementById('modalProductCategory');
            if (!select) return;

            select.innerHTML = '';

            if (categoriesCache.size === 0) {
                select.innerHTML = `<option value="" disabled selected>No categories available — add one first</option>`;
                return;
            }

            const defaultOpt = document.createElement('option');
            defaultOpt.value = '';
            defaultOpt.textContent = 'Select a category...';
            defaultOpt.disabled = true;
            defaultOpt.selected = true;
            select.appendChild(defaultOpt);

            categoriesCache.forEach((name, id) => {
                const opt = document.createElement('option');
                opt.value = String(id);
                opt.textContent = name;
                select.appendChild(opt);
            });

            if (selectedId && categoriesCache.has(Number(selectedId))) {
                select.value = String(selectedId);
            }
        }

        document.getElementById('modalProductImage').addEventListener('change', (e) => {
            const file = e.target.files[0];
            const preview = document.getElementById('modalImagePreview');
            if (file) {
                preview.src = URL.createObjectURL(file);
                preview.classList.remove('d-none');
            }
        });

        function switchTab(tabName, event) {
            if (event) event.preventDefault();
            document.querySelectorAll('.content-section').forEach(sec => sec.style.display = 'none');
            document.querySelectorAll('#menuOffcanvas .menu-nav-link').forEach(link => link.classList.remove('active'));

            if (tabName === 'products') {
                document.getElementById('productsSection').style.display = 'block';
                document.getElementById('navProducts').classList.add('active');
                document.getElementById('pageTitle').textContent = 'Product Management';
                loadProducts();
            } else if (tabName === 'categories') {
                document.getElementById('categoriesSection').style.display = 'block';
                document.getElementById('navCategories').classList.add('active');
                document.getElementById('pageTitle').textContent = 'Category Management';
                loadCategories();
            } else if (tabName === 'stock') {
                document.getElementById('stockSection').style.display = 'block';
                document.getElementById('navStock').classList.add('active');
                document.getElementById('pageTitle').textContent = 'Stock Level Management';
                loadStockLevels();
            } else if (tabName === 'orders') {
                document.getElementById('ordersSection').style.display = 'block';
                document.getElementById('navOrders').classList.add('active');
                document.getElementById('pageTitle').textContent = 'Customer Orders';
                loadOrders();
            } else if (tabName === 'subscribers') {
                document.getElementById('subscribersSection').style.display = 'block';
                document.getElementById('navSubscribers').classList.add('active');
                document.getElementById('pageTitle').textContent = 'Drop Subscribers';
                loadSubscribers();
            } else if (tabName === 'messages') {
                document.getElementById('messagesSection').style.display = 'block';
                document.getElementById('navMessages').classList.add('active');
                document.getElementById('pageTitle').textContent = 'Contact Messages';
                loadMessages();
            }

            if (menuOffcanvas) menuOffcanvas.hide();
        }

        // --- PRODUCTS LOGIC ---
        function renderProductRow(p) {
            const id = p.product_id ?? 0;
            const name = p.product_name ?? '';
            const desc = p.description ?? '';
            const price = p.price ?? 0;
            const stock = p.stock_quantity ?? 0;
            const catId = p.category_id ?? 0;
            const catName = catId > 0
                ? (categoriesCache.get(catId) ?? `Category #\${catId}`)
                : 'Uncategorized';
            const imageUrl = p.image_url ?? '';

            productsCache.set(id, p);

            const thumbHtml = imageUrl
                ? `<a href="product.jsp?id=\${id}" title="View product"><img src="\${escapeHtml(imageUrl)}" alt="\${escapeHtml(name)}" class="product-thumb"></a>`
                : `<a href="product.jsp?id=\${id}" title="View product" class="product-thumb-placeholder text-decoration-none"><i class="bi bi-image"></i></a>`;

            return `
                <tr>
                    <td class="ps-3">\${thumbHtml}</td>
                    <td>\${id}</td>
                    <td><a href="product.jsp?id=\${id}" class="product-link">\${escapeHtml(name)}</a></td>
                    <td class="text-muted">\${escapeHtml(desc)}</td>
                    <td>R \${Number(price).toFixed(2)}</td>
                    <td>\${stock}</td>
                    <td>\${escapeHtml(catName)}</td>
                    <td class="pe-3 text-end text-nowrap">
                        <a href="product.jsp?id=\${id}" class="btn btn-sm btn-outline-dark me-1" title="View Product"><i class="bi bi-eye"></i></a>
                        <button type="button" class="btn btn-sm btn-outline-dark me-1" onclick="openProductModal('edit', \${id})" title="Edit Product"><i class="bi bi-pencil"></i></button>
                        <button type="button" class="btn btn-sm btn-outline-danger" onclick="deleteProduct(\${id})" title="Delete Product"><i class="bi bi-trash"></i></button>
                    </td>
                </tr>
            `;
        }

        async function loadProducts() {
            const tbody = document.getElementById('productsTableBody');
            try {
                const [prodRes] = await Promise.all([
                    fetch('api/products?limit=100'),
                    fetchCategoryMap()
                ]);
                if (prodRes.status === 401) {
                    window.location.href = 'login.html';
                    return;
                }
                if (!prodRes.ok) {
                    tbody.innerHTML = `<tr><td colspan="8" class="text-center text-danger py-4">Failed to load products.</td></tr>`;
                    return;
                }
                const products = await prodRes.json();
                tbody.innerHTML = '';
                productsCache.clear();

                if (!Array.isArray(products) || products.length === 0) {
                    tbody.innerHTML = `<tr><td colspan="8" class="text-center text-muted py-4">No products found.</td></tr>`;
                    return;
                }

                products.forEach(p => { tbody.innerHTML += renderProductRow(p); });
            } catch (err) {
                console.error('Error loading products:', err);
                tbody.innerHTML = `<tr><td colspan="8" class="text-center text-danger py-4">Network error loading products.</td></tr>`;
            }
        }

        async function searchProductById() {
            const id = document.getElementById('searchProductId').value;
            if (!id) return loadProducts();

            try {
                const [res] = await Promise.all([
                    fetch(`api/products?id=\${encodeURIComponent(id)}`),
                    categoriesCache.size === 0 ? fetchCategoryMap() : Promise.resolve()
                ]);
                const tbody = document.getElementById('productsTableBody');
                tbody.innerHTML = '';

                if (res.ok) {
                    const p = await res.json();
                    tbody.innerHTML = renderProductRow(p);
                } else {
                    tbody.innerHTML = `<tr><td colspan="8" class="text-center text-danger py-4">Product not found.</td></tr>`;
                }
            } catch (err) {
                console.error('Search error:', err);
            }
        }

        async function openProductModal(mode, id = null) {
            document.getElementById('productForm').reset();
            const preview = document.getElementById('modalImagePreview');
            preview.src = '';
            preview.classList.add('d-none');

            await fetchCategoryMap();

            if (mode === 'edit' && id !== null && productsCache.has(id)) {
                const p = productsCache.get(id);
                document.getElementById('modalProductId').value = p.product_id ?? id;
                document.getElementById('modalProductName').value = p.product_name ?? '';
                document.getElementById('modalProductDesc').value = p.description ?? '';
                document.getElementById('modalProductPrice').value = p.price ?? 0;
                document.getElementById('modalProductStock').value = p.stock_quantity ?? 0;
                populateCategorySelect(p.category_id ?? '');
                document.getElementById('productModalTitle').textContent = 'Edit Product';

                if (p.image_url) {
                    preview.src = p.image_url;
                    preview.classList.remove('d-none');
                }
            } else {
                document.getElementById('modalProductId').value = '';
                populateCategorySelect('');
                document.getElementById('productModalTitle').textContent = 'Add Product';
            }

            productModal.show();
        }

        document.getElementById('productForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            const id = document.getElementById('modalProductId').value;
            const saveBtn = document.getElementById('saveProductBtn');

            const formData = new FormData();
            formData.append('product_name', document.getElementById('modalProductName').value.trim());
            formData.append('description', document.getElementById('modalProductDesc').value.trim());
            formData.append('price', document.getElementById('modalProductPrice').value);
            formData.append('stock_quantity', document.getElementById('modalProductStock').value);
            formData.append('category_id', document.getElementById('modalProductCategory').value);

            const fileInput = document.getElementById('modalProductImage');
            if (fileInput.files.length > 0) {
                formData.append('image', fileInput.files[0]);
            }

            const url = id ? `api/products?id=\${encodeURIComponent(id)}` : 'api/products';
            saveBtn.disabled = true;
            saveBtn.textContent = 'Saving...';

            try {
                const res = await fetch(url, { method: 'POST', body: formData });
                if (res.ok) {
                    productModal.hide();
                    loadProducts();
                    showFeedback(id ? 'Product updated successfully!' : 'Product created successfully!', 'success');
                } else {
                    const err = await res.json().catch(() => ({}));
                    showFeedback(err.error || 'Failed to save product.', 'danger');
                }
            } catch (err) {
                showFeedback('Network error while saving product.', 'danger');
            } finally {
                saveBtn.disabled = false;
                saveBtn.textContent = 'Save Product';
            }
        });

        async function deleteProduct(id) {
            if (!confirm('Are you sure you want to delete this product?')) return;
            try {
                const res = await fetch(`api/products?id=\${id}`, { method: 'DELETE' });
                if (res.ok) {
                    loadProducts();
                    showFeedback('Product deleted successfully.', 'success');
                } else {
                    showFeedback('Failed to delete product.', 'danger');
                }
            } catch (err) {
                showFeedback('Network error while deleting product.', 'danger');
            }
        }

        // --- CATEGORIES LOGIC ---
        async function loadCategories() {
            const tbody = document.getElementById('categoriesTableBody');
            try {
                const res = await fetch('api/categories');
                if (res.status === 401) { window.location.href = 'login.html'; return; }
                if (!res.ok) {
                    tbody.innerHTML = `<tr><td colspan="4" class="text-center text-danger py-4">Failed to load categories.</td></tr>`;
                    return;
                }
                const categories = await res.json();
                tbody.innerHTML = '';
                categoriesCache.clear();

                if (!Array.isArray(categories) || categories.length === 0) {
                    tbody.innerHTML = `<tr><td colspan="4" class="text-center text-muted py-4">No categories found.</td></tr>`;
                    populateCategorySelect();
                    return;
                }

                categories.forEach(c => {
                    const id = c.category_id ?? 0;
                    const name = c.category_name ?? '';
                    const desc = c.description ?? '';
                    if (id > 0) categoriesCache.set(id, name);

                    tbody.innerHTML += `
                        <tr>
                            <td class="ps-3">\${id}</td>
                            <td class="fw-medium">\${escapeHtml(name)}</td>
                            <td class="text-muted">\${escapeHtml(desc)}</td>
                            <td class="pe-3 text-end">
                                <button type="button" class="btn btn-sm btn-outline-danger" onclick="deleteCategory(\${id})"><i class="bi bi-trash"></i></button>
                            </td>
                        </tr>
                    `;
                });
                populateCategorySelect();
            } catch (err) {
                console.error('Error loading categories:', err);
            }
        }

        function openCategoryModal() {
            document.getElementById('categoryForm').reset();
            categoryModal.show();
        }

        document.getElementById('categoryForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            const payload = {
                category_name: document.getElementById('modalCategoryName').value.trim(),
                description: document.getElementById('modalCategoryDesc').value.trim()
            };
            try {
                const res = await fetch('api/categories', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });
                if (res.ok) {
                    categoryModal.hide();
                    loadCategories();
                    showFeedback('Category added successfully!', 'success');
                } else {
                    showFeedback('Failed to add category.', 'danger');
                }
            } catch (err) {
                showFeedback('Network error while adding category.', 'danger');
            }
        });

        async function deleteCategory(id) {
            if (!confirm('Delete this category?')) return;
            try {
                const res = await fetch(`api/categories?id=\${id}`, { method: 'DELETE' });
                if (res.ok) {
                    loadCategories();
                    showFeedback('Category deleted successfully.', 'success');
                } else {
                    showFeedback('Failed to delete category.', 'danger');
                }
            } catch (err) {
                console.error('Delete category error:', err);
            }
        }

        // --- STOCK MANAGEMENT LOGIC ---
        async function loadStockLevels() {
            const tbody = document.getElementById('stockTableBody');
            try {
                const res = await fetch('api/products?limit=100');
                if (res.status === 401) { window.location.href = 'login.html'; return; }
                if (!res.ok) {
                    tbody.innerHTML = `<tr><td colspan="5" class="text-center text-danger py-4">Failed to load stock levels.</td></tr>`;
                    return;
                }
                const products = await res.json();
                tbody.innerHTML = '';

                if (!Array.isArray(products) || products.length === 0) {
                    tbody.innerHTML = `<tr><td colspan="5" class="text-center text-muted py-4">No inventory items found.</td></tr>`;
                    return;
                }

                products.forEach(p => {
                    const id = p.product_id ?? 0;
                    const name = p.product_name ?? '';
                    const stock = p.stock_quantity ?? 0;
                    const badgeClass = stock <= 5 ? 'bg-danger' : stock <= 15 ? 'bg-warning text-dark' : 'bg-dark';
                    const statusText = stock <= 5 ? 'Low Stock' : stock <= 15 ? 'Moderate' : 'Adequate';

                    tbody.innerHTML += `
                        <tr>
                            <td class="ps-3">\${id}</td>
                            <td class="fw-medium">\${escapeHtml(name)}</td>
                            <td><span class="badge \${badgeClass} fs-6">\${stock}</span></td>
                            <td>\${statusText}</td>
                            <td class="pe-3 text-end text-nowrap">
                                <button type="button" class="btn btn-sm btn-outline-dark me-1" onclick="updateStock(\${id}, \${stock}, -1)">-</button>
                                <button type="button" class="btn btn-sm btn-outline-dark" onclick="updateStock(\${id}, \${stock}, 1)">+</button>
                            </td>
                        </tr>
                    `;
                });
            } catch (err) {
                console.error('Error loading stock levels:', err);
            }
        }

        async function updateStock(id, currentStock, delta) {
            const newStock = currentStock + delta;
            if (newStock < 0) return;
            try {
                const getRes = await fetch(`api/products?id=\${id}`);
                if (!getRes.ok) return;
                const p = await getRes.json();
                p.stock_quantity = newStock;

                const res = await fetch(`api/products?id=\${id}`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(p)
                });
                if (res.ok) {
                    loadStockLevels();
                    showFeedback('Stock level updated.', 'success');
                } else {
                    showFeedback('Failed to update stock quantity.', 'danger');
                }
            } catch (err) {
                console.error('Stock update error:', err);
            }
        }

        // --- ORDERS LOGIC (Customer contact + shipping + line items) ---
        async function ensureProductsCached() {
            if (productsCache.size > 0) return;
            try {
                const res = await fetch('api/products?limit=100');
                if (res.ok) {
                    const products = await res.json();
                    if (Array.isArray(products)) {
                        products.forEach(p => productsCache.set(p.product_id ?? 0, p));
                    }
                }
            } catch (ignored) {}
        }

        async function loadOrders() {
            const tbody = document.getElementById('ordersTableBody');
            try {
                const [res] = await Promise.all([
                    fetch('api/orders'),
                    ensureProductsCached()
                ]);
                if (res.status === 401) { window.location.href = 'login.html'; return; }
                if (!res.ok) {
                    tbody.innerHTML = `<tr><td colspan="7" class="text-center text-danger py-4">Failed to load orders.</td></tr>`;
                    return;
                }
                const orders = await res.json();
                tbody.innerHTML = '';
                ordersCache.clear();

                if (!Array.isArray(orders) || orders.length === 0) {
                    tbody.innerHTML = `<tr><td colspan="7" class="text-center text-muted py-4">No completed orders yet.</td></tr>`;
                    return;
                }

                orders.forEach(o => {
                    const orderId = o.order_id ?? 0;
                    const customerName = o.customer_name ?? 'Customer';
                    const customerEmail = o.customer_email ?? '';
                    const customerPhone = o.customer_phone ?? '';
                    const total = o.total_amount ?? 0;
                    const status = o.status ?? 'COMPLETED';
                    const createdAt = formatDate(o.created_at);
                    const items = Array.isArray(o.items) ? o.items : [];
                    const totalQty = items.reduce((sum, it) => sum + (it.quantity ?? 0), 0);

                    ordersCache.set(orderId, o);

                    const contactLine = [customerEmail, customerPhone].filter(Boolean).join(' • ');

                    tbody.innerHTML += `
                        <tr>
                            <td class="ps-3 fw-semibold">#\${orderId}</td>
                            <td>
                                <div class="fw-medium">\${escapeHtml(customerName)}</div>
                                <div class="small text-muted">\${escapeHtml(contactLine || 'No contact info')}</div>
                            </td>
                            <td>
                                <span class="badge bg-light text-dark border">\${totalQty} \${totalQty === 1 ? 'item' : 'items'}</span>
                            </td>
                            <td class="fw-semibold">R \${Number(total).toFixed(2)}</td>
                            <td><span class="badge bg-dark">\${escapeHtml(status)}</span></td>
                            <td class="small text-muted">\${escapeHtml(createdAt)}</td>
                            <td class="pe-3 text-end text-nowrap">
                                <button type="button" class="btn btn-sm btn-outline-dark me-1" onclick="openOrderDetail(\${orderId})" title="View Order Details">
                                    <i class="bi bi-eye"></i>
                                </button>
                                <button type="button" class="btn btn-sm btn-outline-danger" onclick="deleteOrder(\${orderId})" title="Delete Order">
                                    <i class="bi bi-trash"></i>
                                </button>
                            </td>
                        </tr>
                    `;
                });
            } catch (err) {
                console.error('Error loading orders:', err);
            }
        }

        function openOrderDetail(orderId) {
            const o = ordersCache.get(orderId);
            if (!o) return;

            document.getElementById('orderDetailTitle').textContent = `Order #\${o.order_id ?? orderId}`;
            document.getElementById('odCustomerName').textContent = o.customer_name ?? 'Customer';
            document.getElementById('odCustomerEmail').textContent = o.customer_email ?? 'No email';
            document.getElementById('odCustomerPhone').textContent = o.customer_phone ?? 'No phone';
            document.getElementById('odShippingAddress').textContent = o.shipping_address ?? 'No shipping address recorded.';
            document.getElementById('odStatus').textContent = o.status ?? 'COMPLETED';
            document.getElementById('odPaymentRef').textContent = o.payment_reference ?? 'N/A';
            document.getElementById('odCreatedAt').textContent = formatDate(o.created_at);
            document.getElementById('odTotal').textContent = `R \${Number(o.total_amount ?? 0).toFixed(2)}`;

            const itemsBody = document.getElementById('odItemsBody');
            itemsBody.innerHTML = '';

            const items = Array.isArray(o.items) ? o.items : [];
            if (items.length === 0) {
                itemsBody.innerHTML = `<tr><td colspan="4" class="text-center text-muted py-3">No item rows found.</td></tr>`;
            } else {
                items.forEach(it => {
                    const pid = it.product_id ?? 0;
                    const qty = it.quantity ?? 0;
                    const unit = Number(it.unit_price ?? 0);
                    const prod = productsCache.get(pid);
                    const prodName = prod ? (prod.product_name ?? `Product #\${pid}`) : `Product #\${pid}`;

                    itemsBody.innerHTML += `
                        <tr>
                            <td class="ps-3 fw-medium">\${escapeHtml(prodName)}</td>
                            <td class="text-center">\${qty}</td>
                            <td class="text-end">R \${unit.toFixed(2)}</td>
                            <td class="pe-3 text-end fw-semibold">R \${(unit * qty).toFixed(2)}</td>
                        </tr>
                    `;
                });
            }

            orderDetailModal.show();
        }

        async function deleteOrder(id) {
            if (!confirm('Are you sure you want to delete this order?')) return;
            try {
                const res = await fetch(`api/orders?id=\${id}`, { method: 'DELETE' });
                if (res.ok) {
                    loadOrders();
                    showFeedback('Order deleted successfully.', 'success');
                } else {
                    showFeedback('Failed to delete order.', 'danger');
                }
            } catch (err) {
                showFeedback('Network error while deleting order.', 'danger');
            }
        }

        // --- SUBSCRIBERS LOGIC ---
        async function loadSubscribers() {
            const tbody = document.getElementById('subscribersTableBody');
            try {
                const res = await fetch('api/subscribe');
                if (res.status === 401) { window.location.href = 'login.html'; return; }
                if (!res.ok) {
                    tbody.innerHTML = `<tr><td colspan="4" class="text-center text-danger py-4">Failed to load subscribers.</td></tr>`;
                    return;
                }
                const list = await res.json();
                cachedSubscribers = Array.isArray(list) ? list : [];
                document.getElementById('subscriberCount').textContent = String(cachedSubscribers.length);
                tbody.innerHTML = '';

                if (cachedSubscribers.length === 0) {
                    tbody.innerHTML = `<tr><td colspan="4" class="text-center text-muted py-4">No subscribers yet.</td></tr>`;
                    return;
                }

                cachedSubscribers.forEach(s => {
                    const id = s.subscriber_id ?? 0;
                    const email = s.email ?? '';
                    const createdAt = formatDate(s.created_at);

                    tbody.innerHTML += `
                        <tr>
                            <td class="ps-3">\${id}</td>
                            <td class="fw-medium">\${escapeHtml(email)}</td>
                            <td class="small text-muted">\${escapeHtml(createdAt)}</td>
                            <td class="pe-3 text-end">
                                <button type="button" class="btn btn-sm btn-outline-danger" onclick="deleteSubscriber(\${id})"><i class="bi bi-trash"></i></button>
                            </td>
                        </tr>
                    `;
                });
            } catch (err) {
                console.error('Error loading subscribers:', err);
            }
        }

        async function copyAllSubscriberEmails() {
            const emails = cachedSubscribers.map(s => s.email).filter(Boolean).join(', ');
            if (!emails) {
                showFeedback('No subscriber emails to copy.', 'warning');
                return;
            }
            try {
                await navigator.clipboard.writeText(emails);
                showFeedback('Copied all subscriber emails!', 'success');
            } catch {
                showFeedback('Could not access clipboard.', 'danger');
            }
        }

        async function deleteSubscriber(id) {
            if (!confirm('Remove this subscriber?')) return;
            try {
                const res = await fetch(`api/subscribe?id=\${id}`, { method: 'DELETE' });
                if (res.ok) {
                    loadSubscribers();
                    showFeedback('Subscriber removed.', 'success');
                }
            } catch (err) {
                console.error('Delete subscriber error:', err);
            }
        }

        // --- CONTACT MESSAGES LOGIC ---
        async function loadMessages() {
            const tbody = document.getElementById('messagesTableBody');
            try {
                const res = await fetch('api/contact');
                if (res.status === 401) { window.location.href = 'login.html'; return; }
                if (!res.ok) {
                    tbody.innerHTML = `<tr><td colspan="5" class="text-center text-danger py-4">Failed to load messages.</td></tr>`;
                    return;
                }
                const messages = await res.json();
                tbody.innerHTML = '';

                if (!Array.isArray(messages) || messages.length === 0) {
                    tbody.innerHTML = `<tr><td colspan="5" class="text-center text-muted py-4">No messages yet.</td></tr>`;
                    return;
                }

                messages.forEach(m => {
                    const id = m.message_id ?? 0;
                    const name = m.name ?? '';
                    const email = m.email ?? '';
                    const msg = m.message ?? '';
                    const createdAt = formatDate(m.created_at);

                    tbody.innerHTML += `
                        <tr>
                            <td class="ps-3">\${id}</td>
                            <td>
                                <div class="fw-medium">\${escapeHtml(name)}</div>
                                <a href="mailto:\${escapeHtml(email)}" class="small text-muted">\${escapeHtml(email)}</a>
                            </td>
                            <td class="msg-cell small">\${escapeHtml(msg)}</td>
                            <td class="small text-muted">\${escapeHtml(createdAt)}</td>
                            <td class="pe-3 text-end">
                                <button type="button" class="btn btn-sm btn-outline-danger" onclick="deleteMessage(\${id})"><i class="bi bi-trash"></i></button>
                            </td>
                        </tr>
                    `;
                });
            } catch (err) {
                console.error('Error loading messages:', err);
            }
        }

        async function deleteMessage(id) {
            if (!confirm('Delete this message?')) return;
            try {
                const res = await fetch(`api/contact?id=\${id}`, { method: 'DELETE' });
                if (res.ok) {
                    loadMessages();
                    showFeedback('Message deleted.', 'success');
                }
            } catch (err) {
                console.error('Delete message error:', err);
            }
        }

        function showFeedback(message, type = 'success') {
            const container = document.getElementById('alertContainer');
            if (!container) return;

            container.innerHTML = `
                <div class="alert alert-\${type} alert-dismissible fade show mb-0 py-2 px-3 shadow-sm" role="alert">
                    \${escapeHtml(message)}
                    <button type="button" class="btn-close py-2" data-bs-dismiss="alert" aria-label="Close"></button>
                </div>
            `;

            setTimeout(() => {
                const alertEl = container.querySelector('.alert');
                if (alertEl) {
                    const bsAlert = bootstrap.Alert.getInstance(alertEl) || new bootstrap.Alert(alertEl);
                    bsAlert.close();
                }
            }, 3500);
        }

        async function logout() {
            try {
                await fetch('api/auth/logout', { method: 'POST' });
            } finally {
                window.location.href = 'login.html';
            }
        }
    </script>
</body>
</html>