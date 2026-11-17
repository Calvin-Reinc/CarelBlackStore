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
    <title>Carel Black - Product Details</title>

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

        .card,
        .modal-content,
        .form-control,
        .form-select,
        .btn,
        .alert,
        .badge {
            border-radius: 0 !important;
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

        .product-image-frame {
            width: 100%;
            aspect-ratio: 1 / 1;
            max-height: 520px;
            border: 1px solid #e5e7eb;
            background-color: #f8f9fa;
            display: flex;
            align-items: center;
            justify-content: center;
            overflow: hidden;
        }

        .product-image-frame img {
            width: 100%;
            height: 100%;
            object-fit: cover;
            display: block;
        }

        .meta-box {
            border: 1px solid #e5e7eb;
            background-color: #ffffff;
            padding: 0.9rem 1rem;
        }

        .meta-label {
            font-size: 0.72rem;
            text-transform: uppercase;
            letter-spacing: 0.05em;
            color: #6c757d;
            display: block;
            margin-bottom: 0.25rem;
        }

        .modal-image-preview {
            width: 88px;
            height: 88px;
            object-fit: cover;
            border: 1px solid #dee2e6;
            background-color: #f8f9fa;
        }
    </style>
</head>
<body class="bg-white">

    <!-- Top Header: Left Back | Center Logo | Right Account -->
    <header class="top-header px-3 px-md-4">
        <div class="d-flex justify-content-start">
            <a href="dashboard.jsp"
               class="btn btn-outline-dark btn-sm d-inline-flex align-items-center gap-2 px-3"
               style="height: 40px;">
                <i class="bi bi-arrow-left"></i>
                <span class="d-none d-sm-inline">Dashboard</span>
            </a>
        </div>

        <div class="d-flex justify-content-center">
            <a href="index.html" class="d-inline-flex align-items-center text-decoration-none" title="Carel Black Home">
                <img src="assets/img/logo.png" alt="Carel Black Logo" class="header-logo">
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

    <!-- Account / Profile Settings & Logout Window -->
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

    <!-- Main Content -->
    <main class="container-xl px-3 px-md-4 py-4 py-md-5">
        <div id="alertContainer" class="mb-3"></div>

        <!-- Loading State -->
        <div id="loadingState" class="text-center py-5">
            <div class="spinner-border text-dark" role="status">
                <span class="visually-hidden">Loading product...</span>
            </div>
            <p class="text-muted small mt-3 mb-0">Loading product details...</p>
        </div>

        <!-- Error / Not Found State -->
        <div id="errorState" class="card p-5 text-center d-none">
            <i class="bi bi-box-seam fs-1 text-muted mb-3"></i>
            <h2 class="h4 fw-semibold">Product Not Found</h2>
            <p class="text-muted mb-4" id="errorStateMessage">The requested product could not be loaded.</p>
            <div>
                <a href="dashboard.jsp" class="btn btn-carel px-4 py-2">Back to Dashboard</a>
            </div>
        </div>

        <!-- Product Detail View -->
        <div id="productContent" class="row g-4 g-lg-5 align-items-start d-none">
            <!-- Left: Product Image -->
            <div class="col-12 col-lg-6">
                <div class="product-image-frame">
                    <img id="productImage" src="" alt="Product Image" class="d-none">
                    <div id="productImagePlaceholder" class="text-center text-muted p-4">
                        <i class="bi bi-image display-4 d-block mb-2"></i>
                        <span class="small">No image uploaded</span>
                    </div>
                </div>
            </div>

            <!-- Right: Product Info & Actions -->
            <div class="col-12 col-lg-6">
                <div class="d-flex flex-wrap align-items-center gap-2 mb-2">
                    <span class="badge bg-dark" id="productCategoryBadge">Category</span>
                    <span class="text-muted small" id="productIdBadge">ID #0</span>
                </div>

                <h1 class="display-6 fw-semibold mb-2" id="productName">Product Name</h1>

                <div class="d-flex flex-wrap align-items-center gap-3 mb-4 pb-3 border-bottom">
                    <span class="fs-3 fw-bold" id="productPrice">R 0.00</span>
                    <span class="badge" id="productStockBadge">0 in stock</span>
                </div>

                <div class="mb-4">
                    <span class="meta-label">Description</span>
                    <p class="text-secondary mb-0" id="productDescription" style="white-space: pre-line;">No description provided.</p>
                </div>

                <div class="row g-3 mb-4">
                    <div class="col-6">
                        <div class="meta-box">
                            <span class="meta-label">Stock Quantity</span>
                            <span class="fw-semibold fs-5" id="metaStock">0</span>
                        </div>
                    </div>
                    <div class="col-6">
                        <div class="meta-box">
                            <span class="meta-label">Category</span>
                            <span class="fw-semibold fs-5" id="metaCategoryName">Uncategorized</span>
                        </div>
                    </div>
                    <div class="col-12">
                        <div class="meta-box">
                            <span class="meta-label">Created At</span>
                            <span class="fw-medium" id="metaCreatedAt">N/A</span>
                        </div>
                    </div>
                </div>

                <div class="d-flex flex-wrap gap-2">
                    <button type="button" class="btn btn-carel px-4 py-2 flex-grow-1 flex-sm-grow-0" onclick="openEditModal()">
                        <i class="bi bi-pencil me-2"></i> Edit Product
                    </button>
                    <button type="button" class="btn btn-outline-danger px-4 py-2 flex-grow-1 flex-sm-grow-0" onclick="deleteCurrentProduct()">
                        <i class="bi bi-trash me-2"></i> Delete Product
                    </button>
                </div>
            </div>
        </div>
    </main>

    <!-- Edit Product Modal -->
    <div class="modal fade" id="editProductModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <div class="modal-header border-bottom">
                    <h5 class="modal-title fw-semibold">Edit Product</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-4">
                    <form id="editProductForm" enctype="multipart/form-data">
                        <div class="mb-3">
                            <label for="editProductName" class="form-label small fw-semibold">Product Name</label>
                            <input type="text" class="form-control" id="editProductName" required>
                        </div>

                        <div class="mb-3">
                            <label for="editProductDesc" class="form-label small fw-semibold">Description</label>
                            <textarea class="form-control" id="editProductDesc" rows="3"></textarea>
                        </div>

                        <div class="row g-3 mb-3">
                            <div class="col-sm-6">
                                <label for="editProductPrice" class="form-label small fw-semibold">Price (R)</label>
                                <input type="number" step="0.01" min="0" class="form-control" id="editProductPrice" required>
                            </div>
                            <div class="col-sm-6">
                                <label for="editProductStock" class="form-label small fw-semibold">Stock Qty</label>
                                <input type="number" min="0" class="form-control" id="editProductStock" required>
                            </div>
                        </div>

                        <div class="mb-3">
                            <label for="editProductCategory" class="form-label small fw-semibold">Category</label>
                            <select class="form-select" id="editProductCategory" required>
                                <option value="" disabled selected>Loading categories...</option>
                            </select>
                        </div>

                        <div class="mb-4">
                            <label for="editProductImage" class="form-label small fw-semibold">Replace Image (Optional)</label>
                            <div class="d-flex align-items-center gap-3">
                                <img id="editImagePreview" src="" alt="Preview" class="modal-image-preview d-none">
                                <div class="flex-grow-1">
                                    <input type="file"
                                           class="form-control"
                                           id="editProductImage"
                                           accept="image/jpeg,image/png,image/webp,image/gif">
                                    <small class="text-muted d-block mt-1">Leave empty to keep current image.</small>
                                </div>
                            </div>
                        </div>

                        <button type="submit" id="saveEditBtn" class="btn btn-carel w-100 py-2">
                            Save Changes
                        </button>
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
        let currentProduct = null;
        let editModal = null;
        const categoryNames = new Map();

        document.addEventListener('DOMContentLoaded', () => {
            editModal = new bootstrap.Modal(document.getElementById('editProductModal'));
            loadProductPage();
        });

        document.getElementById('editProductImage').addEventListener('change', (e) => {
            const file = e.target.files[0];
            const preview = document.getElementById('editImagePreview');
            if (file) {
                preview.src = URL.createObjectURL(file);
                preview.classList.remove('d-none');
            }
        });

        async function loadProductPage() {
            const params = new URLSearchParams(window.location.search);
            const id = params.get('id');

            if (!id || Number(id) <= 0) {
                showErrorState('No valid product ID was provided.');
                return;
            }

            try {
                const [prodRes, catRes] = await Promise.all([
                    fetch(`api/products?id=\${encodeURIComponent(id)}`),
                    fetch('api/categories').catch(() => null)
                ]);

                if (prodRes.status === 401) {
                    window.location.href = 'login.html';
                    return;
                }

                categoryNames.clear();
                if (catRes && catRes.ok) {
                    const cats = await catRes.json();
                    if (Array.isArray(cats)) {
                        cats.forEach(c => {
                            const cid = c.category_id ?? 0;
                            if (cid > 0) {
                                categoryNames.set(cid, c.category_name ?? `Category #\${cid}`);
                            }
                        });
                    }
                }

                if (!prodRes.ok) {
                    showErrorState('Product not found or could not be retrieved.');
                    return;
                }

                const p = await prodRes.json();
                renderProduct(p);
            } catch (err) {
                console.error('Error loading product:', err);
                showErrorState('Unable to connect to the server.');
            }
        }

        function populateEditCategorySelect(selectedId = '') {
            const select = document.getElementById('editProductCategory');
            if (!select) return;

            select.innerHTML = '';

            if (categoryNames.size === 0) {
                select.innerHTML = `<option value="" disabled selected>No categories available</option>`;
                return;
            }

            const defaultOpt = document.createElement('option');
            defaultOpt.value = '';
            defaultOpt.textContent = 'Select a category...';
            defaultOpt.disabled = true;
            defaultOpt.selected = true;
            select.appendChild(defaultOpt);

            categoryNames.forEach((name, id) => {
                const opt = document.createElement('option');
                opt.value = String(id);
                opt.textContent = name;
                select.appendChild(opt);
            });

            if (selectedId && categoryNames.has(Number(selectedId))) {
                select.value = String(selectedId);
            }
        }

        function renderProduct(p) {
            currentProduct = p;

            const id = p.product_id ?? 0;
            const name = p.product_name ?? 'Untitled Product';
            const desc = (p.description && p.description.trim()) ? p.description : 'No description provided.';
            const price = p.price ?? 0;
            const stock = p.stock_quantity ?? 0;
            const catId = p.category_id ?? 0;
            const catName = catId > 0
                ? (categoryNames.get(catId) || `Category #\${catId}`)
                : 'Uncategorized';
            const createdAt = p.created_at ?? 'N/A';
            const imageUrl = p.image_url ?? '';

            document.title = `Carel Black - \${name}`;
            document.getElementById('productIdBadge').textContent = `Product #\${id}`;
            document.getElementById('productCategoryBadge').textContent = catName;
            document.getElementById('productName').textContent = name;
            document.getElementById('productPrice').textContent = `R \${Number(price).toFixed(2)}`;
            document.getElementById('productDescription').textContent = desc;
            document.getElementById('metaStock').textContent = stock;
            document.getElementById('metaCategoryName').textContent = catName;
            document.getElementById('metaCreatedAt').textContent = formatTimestamp(createdAt);

            const stockBadge = document.getElementById('productStockBadge');
            if (stock <= 0) {
                stockBadge.className = 'badge bg-danger';
                stockBadge.textContent = 'Out of Stock';
            } else if (stock <= 5) {
                stockBadge.className = 'badge bg-danger';
                stockBadge.textContent = `Low Stock (\${stock})`;
            } else if (stock <= 15) {
                stockBadge.className = 'badge bg-warning text-dark';
                stockBadge.textContent = `Moderate Stock (\${stock})`;
            } else {
                stockBadge.className = 'badge bg-dark';
                stockBadge.textContent = `In Stock (\${stock})`;
            }

            const imgEl = document.getElementById('productImage');
            const placeholderEl = document.getElementById('productImagePlaceholder');
            if (imageUrl) {
                imgEl.src = imageUrl;
                imgEl.alt = name;
                imgEl.classList.remove('d-none');
                placeholderEl.classList.add('d-none');
            } else {
                imgEl.src = '';
                imgEl.classList.add('d-none');
                placeholderEl.classList.remove('d-none');
            }

            document.getElementById('loadingState').classList.add('d-none');
            document.getElementById('errorState').classList.add('d-none');
            document.getElementById('productContent').classList.remove('d-none');
        }

        function formatTimestamp(raw) {
            if (!raw || raw === 'N/A') return 'N/A';
            const parsed = new Date(raw);
            return isNaN(parsed.getTime()) ? String(raw) : parsed.toLocaleString();
        }

        function showErrorState(msg) {
            document.getElementById('loadingState').classList.add('d-none');
            document.getElementById('productContent').classList.add('d-none');
            document.getElementById('errorStateMessage').textContent = msg;
            document.getElementById('errorState').classList.remove('d-none');
        }

        function openEditModal() {
            if (!currentProduct) return;

            document.getElementById('editProductForm').reset();
            document.getElementById('editProductName').value = currentProduct.product_name ?? '';
            document.getElementById('editProductDesc').value = currentProduct.description ?? '';
            document.getElementById('editProductPrice').value = currentProduct.price ?? 0;
            document.getElementById('editProductStock').value = currentProduct.stock_quantity ?? 0;
            populateEditCategorySelect(currentProduct.category_id ?? '');

            const preview = document.getElementById('editImagePreview');
            if (currentProduct.image_url) {
                preview.src = currentProduct.image_url;
                preview.classList.remove('d-none');
            } else {
                preview.src = '';
                preview.classList.add('d-none');
            }

            editModal.show();
        }

        document.getElementById('editProductForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            if (!currentProduct) return;

            const id = currentProduct.product_id ?? 0;
            const saveBtn = document.getElementById('saveEditBtn');

            const formData = new FormData();
            formData.append('product_name', document.getElementById('editProductName').value.trim());
            formData.append('description', document.getElementById('editProductDesc').value.trim());
            formData.append('price', document.getElementById('editProductPrice').value);
            formData.append('stock_quantity', document.getElementById('editProductStock').value);
            formData.append('category_id', document.getElementById('editProductCategory').value);

            const fileInput = document.getElementById('editProductImage');
            if (fileInput.files.length > 0) {
                formData.append('image', fileInput.files[0]);
            }

            saveBtn.disabled = true;
            saveBtn.textContent = 'Saving...';

            try {
                const res = await fetch(`api/products?id=\${encodeURIComponent(id)}`, {
                    method: 'POST',
                    body: formData
                });

                if (res.ok) {
                    editModal.hide();
                    await loadProductPage();
                    showFeedback('Product updated successfully!', 'success');
                } else {
                    const err = await res.json().catch(() => ({}));
                    showFeedback(err.error || 'Failed to update product.', 'danger');
                }
            } catch (err) {
                console.error('Update error:', err);
                showFeedback('Network error while updating product.', 'danger');
            } finally {
                saveBtn.disabled = false;
                saveBtn.textContent = 'Save Changes';
            }
        });

        async function deleteCurrentProduct() {
            if (!currentProduct) return;
            const id = currentProduct.product_id ?? 0;
            if (!confirm('Are you sure you want to delete this product?')) return;

            try {
                const res = await fetch(`api/products?id=\${encodeURIComponent(id)}`, {
                    method: 'DELETE'
                });

                if (res.ok) {
                    window.location.href = 'dashboard.jsp';
                } else {
                    showFeedback('Failed to delete product.', 'danger');
                }
            } catch (err) {
                console.error('Delete error:', err);
                showFeedback('Network error while deleting product.', 'danger');
            }
        }

        function showFeedback(message, type = 'success') {
            const container = document.getElementById('alertContainer');
            if (!container) return;

            container.innerHTML = `
                <div class="alert alert-\${type} alert-dismissible fade show py-2 px-3 shadow-sm" role="alert">
                    <span class="alert-msg"></span>
                    <button type="button" class="btn-close py-2" data-bs-dismiss="alert" aria-label="Close"></button>
                </div>
            `;
            container.querySelector('.alert-msg').textContent = message;

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