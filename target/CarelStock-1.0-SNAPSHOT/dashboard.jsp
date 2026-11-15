<%@ page contentType="text/html;charset=UTF-8" %>
<%
    if (session == null || session.getAttribute("adminUser") == null) {
        response.sendRedirect("login.html");
        return;
    }
%>
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CarelStock - Admin Dashboard</title>
    <!-- Bootstrap 5 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- Bootstrap Icons -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
</head>
<body class="bg-light">

    <div class="container-fluid">
        <div class="row">
            <!-- Sidebar -->
            <nav id="sidebar" class="col-md-3 col-lg-2 d-md-block bg-dark text-white sidebar collapse min-vh-100 p-3">
                <div class="position-sticky pt-3">
                    <h4 class="text-center fw-bold mb-4">CarelStock</h4>
                    <ul class="nav flex-column gap-2">
                        <li class="nav-item">
                            <a class="nav-link text-white active rounded py-2 px-3" href="#" onclick="switchTab('products', event)" id="navProducts">
                                <i class="bi bi-box-seam me-2"></i> Products
                            </a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link text-white rounded py-2 px-3" href="#" onclick="switchTab('categories', event)" id="navCategories">
                                <i class="bi bi-tags me-2"></i> Categories
                            </a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link text-white rounded py-2 px-3" href="#" onclick="switchTab('stock', event)" id="navStock">
                                <i class="bi bi-clipboard-data me-2"></i> Stock Management
                            </a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link text-white rounded py-2 px-3" href="#" onclick="switchTab('orders', event)" id="navOrders">
                                <i class="bi bi-receipt me-2"></i> Orders
                            </a>
                        </li>
                        <li class="nav-item mt-5">
                            <a class="nav-link text-danger rounded py-2 px-3" href="#" onclick="logout()">
                                <i class="bi bi-box-arrow-right me-2"></i> Logout
                            </a>
                        </li>
                    </ul>
                </div>
            </nav>

            <!-- Main Content Area -->
            <main class="col-md-9 ms-sm-auto col-lg-10 px-md-4 py-4">
                <div class="d-flex justify-content-between flex-wrap flex-md-nowrap align-items-center pt-3 pb-2 mb-3 border-bottom">
                    <h1 class="h2" id="pageTitle">Product Management</h1>
                    <div id="alertContainer"></div>
                </div>

                <!-- PRODUCTS SECTION -->
                <div id="productsSection" class="content-section">
                    <div class="card shadow-sm mb-4">
                        <div class="card-body">
                            <div class="row g-3 align-items-center">
                                <div class="col-auto">
                                    <input type="number" id="searchProductId" class="form-control" placeholder="Search by Product ID...">
                                </div>
                                <div class="col-auto">
                                    <button class="btn btn-outline-primary" onclick="searchProductById()">Search</button>
                                    <button class="btn btn-outline-secondary" onclick="loadProducts()">Reset</button>
                                </div>
                                <div class="col-auto ms-auto">
                                    <button class="btn btn-primary" onclick="openProductModal('add')"><i class="bi bi-plus-lg me-1"></i> Add Product</button>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="card shadow-sm">
                        <div class="card-body table-responsive">
                            <table class="table table-hover align-middle">
                                <thead class="table-dark">
                                    <tr>
                                        <th>ID</th>
                                        <th>Name</th>
                                        <th>Description</th>
                                        <th>Price</th>
                                        <th>Stock</th>
                                        <th>Category ID</th>
                                        <th>Actions</th>
                                    </tr>
                                </thead>
                                <tbody id="productsTableBody">
                                    <!-- Populated dynamically -->
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>

                <!-- CATEGORIES SECTION -->
                <div id="categoriesSection" class="content-section" style="display: none;">
                    <div class="card shadow-sm mb-4">
                        <div class="card-body text-end">
                            <button class="btn btn-primary" onclick="openCategoryModal()"><i class="bi bi-plus-lg me-1"></i> Add Category</button>
                        </div>
                    </div>
                    <div class="card shadow-sm">
                        <div class="card-body table-responsive">
                            <table class="table table-hover align-middle">
                                <thead class="table-dark">
                                    <tr>
                                        <th>ID</th>
                                        <th>Category Name</th>
                                        <th>Description</th>
                                        <th>Actions</th>
                                    </tr>
                                </thead>
                                <tbody id="categoriesTableBody">
                                    <!-- Populated dynamically -->
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>

                <!-- STOCK MANAGEMENT SECTION -->
                <div id="stockSection" class="content-section" style="display: none;">
                    <div class="card shadow-sm">
                        <div class="card-header bg-dark text-white">
                            <h5 class="mb-0">Inventory Stock Levels</h5>
                        </div>
                        <div class="card-body table-responsive">
                            <table class="table table-hover align-middle">
                                <thead class="table-secondary">
                                    <tr>
                                        <th>Product ID</th>
                                        <th>Product Name</th>
                                        <th>Current Stock</th>
                                        <th>Status</th>
                                        <th>Quick Adjust</th>
                                    </tr>
                                </thead>
                                <tbody id="stockTableBody">
                                    <!-- Populated dynamically -->
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>

                <!-- ORDERS SECTION -->
                <div id="ordersSection" class="content-section" style="display: none;">
                    <div class="card shadow-sm">
                        <div class="card-body table-responsive">
                            <table class="table table-hover align-middle">
                                <thead class="table-dark">
                                    <tr>
                                        <th>Order ID</th>
                                        <th>User ID</th>
                                        <th>Total Amount</th>
                                        <th>Status</th>
                                        <th>Created At</th>
                                        <th>Actions</th>
                                    </tr>
                                </thead>
                                <tbody id="ordersTableBody">
                                    <!-- Populated dynamically -->
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </main>
        </div>
    </div>

    <!-- Product Modal (Add/Edit) -->
    <div class="modal fade" id="productModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="productModalTitle">Add Product</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <form id="productForm">
                        <input type="hidden" id="modalProductId">
                        <div class="mb-3">
                            <label class="form-label">Product Name</label>
                            <input type="text" class="form-control" id="modalProductName" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Description</label>
                            <textarea class="form-control" id="modalProductDesc" rows="2"></textarea>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Price ($)</label>
                            <input type="number" step="0.01" class="form-control" id="modalProductPrice" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Stock Quantity</label>
                            <input type="number" class="form-control" id="modalProductStock" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Category ID</label>
                            <input type="number" class="form-control" id="modalProductCategory" required>
                        </div>
                        <button type="submit" class="btn btn-success w-100">Save Product</button>
                    </form>
                </div>
            </div>
        </div>
    </div>

    <!-- Category Modal (Add) -->
    <div class="modal fade" id="categoryModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">Add Category</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <form id="categoryForm">
                        <div class="mb-3">
                            <label class="form-label">Category Name</label>
                            <input type="text" class="form-control" id="modalCategoryName" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Description</label>
                            <textarea class="form-control" id="modalCategoryDesc" rows="2"></textarea>
                        </div>
                        <button type="submit" class="btn btn-success w-100">Save Category</button>
                    </form>
                </div>
            </div>
        </div>
    </div>

    <!-- Bootstrap JS Bundle -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>

    <script>
        let productModal, categoryModal;

        document.addEventListener('DOMContentLoaded', () => {
            productModal = new bootstrap.Modal(document.getElementById('productModal'));
            categoryModal = new bootstrap.Modal(document.getElementById('categoryModal'));
            loadProducts();
        });

        // Tab Navigation
        function switchTab(tabName, event) {
            if (event) event.preventDefault();
            document.querySelectorAll('.content-section').forEach(sec => sec.style.display = 'none');
            document.querySelectorAll('#sidebar .nav-link').forEach(link => link.classList.remove('active'));

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
            }
        }

        // --- PRODUCTS LOGIC ---
        async function loadProducts() {
            try {
                const res = await fetch('api/products');
                const products = await res.json();
                const tbody = document.getElementById('productsTableBody');
                tbody.innerHTML = '';

                products.forEach(p => {
                    const id = p.product_id || p.productId;
                    const name = p.product_name || p.productName;
                    const desc = p.description || '';
                    const price = p.price;
                    const stock = p.stock_quantity || p.stockQuantity;
                    const catId = p.category_id || p.categoryId;

                    tbody.innerHTML += `
                        <tr>
                            <td>\${id}</td>
                            <td>\${name}</td>
                            <td>\${desc}</td>
                            <td>$\${Number(price).toFixed(2)}</td>
                            <td>\${stock}</td>
                            <td>\${catId}</td>
                            <td>
                                <button class="btn btn-sm btn-warning me-1" onclick="openProductModal('edit', \${id}, '\${name}', '\${desc}', \${price}, \${stock}, \${catId})"><i class="bi bi-pencil"></i></button>
                                <button class="btn btn-sm btn-danger" onclick="deleteProduct(\${id})"><i class="bi bi-trash"></i></button>
                            </td>
                        </tr>
                    `;
                });
            } catch (err) {
                console.error('Error loading products:', err);
            }
        }

        async function searchProductById() {
            const id = document.getElementById('searchProductId').value;
            if (!id) return loadProducts();

            try {
                const res = await fetch(`api/products?id=\${id}`);
                const tbody = document.getElementById('productsTableBody');
                tbody.innerHTML = '';

                if (res.ok) {
                    const p = await res.json();
                    const pid = p.product_id || p.productId;
                    const name = p.product_name || p.productName;
                    const desc = p.description || '';
                    const price = p.price;
                    const stock = p.stock_quantity || p.stockQuantity;
                    const catId = p.category_id || p.categoryId;

                    tbody.innerHTML = `
                        <tr>
                            <td>\${pid}</td>
                            <td>\${name}</td>
                            <td>\${desc}</td>
                            <td>$\${Number(price).toFixed(2)}</td>
                            <td>\${stock}</td>
                            <td>\${catId}</td>
                            <td>
                                <button class="btn btn-sm btn-warning me-1" onclick="openProductModal('edit', \${pid}, '\${name}', '\${desc}', \${price}, \${stock}, \${catId})"><i class="bi bi-pencil"></i></button>
                                <button class="btn btn-sm btn-danger" onclick="deleteProduct(\${pid})"><i class="bi bi-trash"></i></button>
                            </td>
                        </tr>
                    `;
                } else {
                    tbody.innerHTML = `<tr><td colspan="7" class="text-center text-danger">Product not found</td></tr>`;
                }
            } catch (err) {
                console.error('Search error:', err);
            }
        }

        function openProductModal(mode, id='', name='', desc='', price='', stock='', catId='') {
            document.getElementById('modalProductId').value = id;
            document.getElementById('modalProductName').value = name;
            document.getElementById('modalProductDesc').value = desc;
            document.getElementById('modalProductPrice').value = price;
            document.getElementById('modalProductStock').value = stock;
            document.getElementById('modalProductCategory').value = catId;
            document.getElementById('productModalTitle').textContent = mode === 'add' ? 'Add Product' : 'Edit Product';
            productModal.show();
        }

        document.getElementById('productForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            const id = document.getElementById('modalProductId').value;
            
            // Payload using backend-expected snake_case keys
            const payload = {
                product_name: document.getElementById('modalProductName').value,
                description: document.getElementById('modalProductDesc').value,
                price: parseFloat(document.getElementById('modalProductPrice').value),
                stock_quantity: parseInt(document.getElementById('modalProductStock').value),
                category_id: parseInt(document.getElementById('modalProductCategory').value)
            };

            const method = id ? 'PUT' : 'POST';
            const url = id ? `api/products?id=\${id}` : 'api/products';

            try {
                const res = await fetch(url, {
                    method: method,
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });

                if (res.ok) {
                    productModal.hide();
                    loadProducts();
                } else {
                    alert('Failed to save product.');
                }
            } catch (err) {
                console.error('Save product error:', err);
            }
        });

        async function deleteProduct(id) {
            if (!confirm('Are you sure you want to delete this product?')) return;
            try {
                const res = await fetch(`api/products?id=\${id}`, { method: 'DELETE' });
                if (res.ok) loadProducts();
                else alert('Failed to delete product.');
            } catch (err) {
                console.error('Delete error:', err);
            }
        }

        // --- CATEGORIES LOGIC ---
        async function loadCategories() {
            try {
                const res = await fetch('api/categories');
                const categories = await res.json();
                const tbody = document.getElementById('categoriesTableBody');
                tbody.innerHTML = '';

                categories.forEach(c => {
                    const id = c.category_id || c.categoryId;
                    const name = c.category_name || c.categoryName;
                    const desc = c.description || '';

                    tbody.innerHTML += `
                        <tr>
                            <td>\${id}</td>
                            <td>\${name}</td>
                            <td>\${desc}</td>
                            <td>
                                <button class="btn btn-sm btn-danger" onclick="deleteCategory(\${id})"><i class="bi bi-trash"></i></button>
                            </td>
                        </tr>
                    `;
                });
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
            
            // Payload using backend-expected snake_case category_name key
            const payload = {
                category_name: document.getElementById('modalCategoryName').value,
                description: document.getElementById('modalCategoryDesc').value
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
                } else {
                    alert('Failed to add category.');
                }
            } catch (err) {
                console.error('Add category error:', err);
            }
        });

        async function deleteCategory(id) {
            if (!confirm('Delete this category?')) return;
            try {
                const res = await fetch(`api/categories?id=\${id}`, { method: 'DELETE' });
                if (res.ok) loadCategories();
                else alert('Failed to delete category.');
            } catch (err) {
                console.error('Delete category error:', err);
            }
        }

        // --- STOCK MANAGEMENT LOGIC ---
        async function loadStockLevels() {
            try {
                const res = await fetch('api/products');
                const products = await res.json();
                const tbody = document.getElementById('stockTableBody');
                tbody.innerHTML = '';

                products.forEach(p => {
                    const id = p.product_id || p.productId;
                    const name = p.product_name || p.productName;
                    const stock = p.stock_quantity || p.stockQuantity;
                    const badgeClass = stock <= 5 ? 'bg-danger' : stock <= 15 ? 'bg-warning text-dark' : 'bg-success';
                    const statusText = stock <= 5 ? 'Low Stock' : 'Adequate';

                    tbody.innerHTML += `
                        <tr>
                            <td>\${id}</td>
                            <td>\${name}</td>
                            <td><span class="badge \${badgeClass} fs-6">\${stock}</span></td>
                            <td>\${statusText}</td>
                            <td>
                                <button class="btn btn-sm btn-outline-secondary" onclick="updateStock(\${id}, \${stock}, -1)">-</button>
                                <button class="btn btn-sm btn-outline-primary" onclick="updateStock(\${id}, \${stock}, 1)">+</button>
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

                // Corrected to snake_case stock_quantity to match backend model/DTO
                p.stock_quantity = newStock;

                const res = await fetch(`api/products?id=\${id}`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(p)
                });

                if (res.ok) {
                    loadStockLevels();
                } else {
                    alert('Failed to update stock quantity.');
                }
            } catch (err) {
                console.error('Stock update error:', err);
            }
        }

        // --- ORDERS LOGIC ---
        async function loadOrders() {
            try {
                const res = await fetch('api/orders');
                const orders = await res.json();
                const tbody = document.getElementById('ordersTableBody');
                tbody.innerHTML = '';

                orders.forEach(o => {
                    const orderId = o.order_id || o.orderId;
                    const userId = o.user_id || o.userId;
                    const total = o.total_amount || o.totalAmount;
                    const status = o.status;
                    const createdAt = o.created_at || o.createdAt || 'N/A';

                    tbody.innerHTML += `
                        <tr>
                            <td>\${orderId}</td>
                            <td>\${userId}</td>
                            <td>$\${Number(total).toFixed(2)}</td>
                            <td><span class="badge bg-info text-dark">\${status}</span></td>
                            <td>\${createdAt}</td>
                            <td>
                                <button class="btn btn-sm btn-danger" onclick="deleteOrder(\${orderId})"><i class="bi bi-trash"></i></button>
                            </td>
                        </tr>
                    `;
                });
            } catch (err) {
                console.error('Error loading orders:', err);
            }
        }

        async function deleteOrder(id) {
            if (!confirm('Are you sure you want to delete this order?')) return;
            try {
                const res = await fetch(`api/orders?id=\${id}`, { method: 'DELETE' });
                if (res.ok) loadOrders();
                else alert('Failed to delete order.');
            } catch (err) {
                console.error('Order delete error:', err);
            }
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