<%-- 
    Document   : products
    Created on : Nov 14, 2026, 9:13:56 PM
    Author     : squid
--%>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CarelStock - Product Management</title>
    <!-- Bootstrap 5 CSS CDN -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css">
</head>
<body class="bg-light">

    <!-- Navigation Bar -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark shadow-sm">
        <div class="container-fluid">
            <a class="navbar-brand fw-bold" href="dashboard.jsp"><i class="bi bi-box-seam me-2"></i>CarelStock Admin</a>
            <div class="d-flex align-items-center">
                <a href="dashboard.jsp" class="btn btn-outline-light btn-sm me-2"><i class="bi bi-speedometer2 me-1"></i>Dashboard</a>
                <button class="btn btn-outline-light btn-sm" id="logoutBtn"><i class="bi bi-box-arrow-right me-1"></i>Logout</button>
            </div>
        </div>
    </nav>

    <!-- Main Container -->
    <div class="container my-4">
        <div class="row mb-4 align-items-center">
            <div class="col-md-6">
                <h2>Product Management</h2>
                <p class="text-muted">View, add, and manage stock inventory items.</p>
            </div>
            <div class="col-md-6 text-md-end">
                <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#addProductModal">
                    <i class="bi bi-plus-lg me-1"></i>Add New Product
                </button>
            </div>
        </div>

        <!-- Feedback Alert Banner -->
        <div id="actionAlert" class="alert d-none" role="alert"></div>

        <!-- Products Table Card -->
        <div class="card border-0 shadow-sm">
            <div class="card-body">
                <div class="table-responsive">
                    <table class="table table-hover align-middle">
                        <thead class="table-dark">
                            <tr>
                                <th>ID</th>
                                <th>Name</th>
                                <th>Category</th>
                                <th>Price</th>
                                <th>Stock</th>
                                <th class="text-end">Actions</th>
                            </tr>
                        </thead>
                        <tbody id="productTableBody">
                            <tr>
                                <td colspan="6" class="text-center text-muted py-4">Loading inventory...</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>

    <!-- Add Product Modal -->
    <div class="modal fade" id="addProductModal" tabindex="-1" aria-labelledby="addProductModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <form id="addProductForm">
                    <div class="modal-header">
                        <h5 class="modal-title" id="addProductModalLabel">Add New Product</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <div class="mb-3">
                            <label for="productName" class="form-label">Product Name</label>
                            <input type="text" class="form-control" id="productName" required>
                        </div>
                        <div class="mb-3">
                            <label for="productCategoryId" class="form-label">Category ID</label>
                            <input type="number" class="form-control" id="productCategoryId" required>
                        </div>
                        <div class="mb-3">
                            <label for="productPrice" class="form-label">Price ($)</label>
                            <input type="number" step="0.01" class="form-control" id="productPrice" required>
                        </div>
                        <div class="mb-3">
                            <label for="productStock" class="form-label">Stock Quantity</label>
                            <input type="number" class="form-control" id="productStock" required>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                        <button type="submit" class="btn btn-primary">Save Product</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Bootstrap JS Bundle -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>

    <!-- Client-Side JavaScript -->
    <script>
        const contextPath = '${pageContext.request.contextPath}';

        // Load Products on page load
        async function loadProducts() {
        const spinner = document.getElementById('loadingSpinner'); // Adjust to your spinner ID
        const tbody = document.getElementById('productTableBody'); // Adjust to your table body ID

        try {
            if (spinner) spinner.style.display = 'block';

            const response = await fetch(contextPath + '/api/products', {
                method: 'GET',
                credentials: 'include'
            });

            if (!response.ok) {
                throw new Error(`Server returned status ${response.status}`);
            }

            const products = await response.json();

            // Clear and render products table
            tbody.innerHTML = '';
            products.forEach(p => {
                // your table row rendering code here
            });

        } catch (error) {
            console.error('Failed to load products:', error);
            tbody.innerHTML = `<tr><td colspan="6" class="text-center text-danger">Failed to load inventory. Check console.</td></tr>`;
        } finally {
            // ALWAYS hides the spinner whether it succeeds or fails
            if (spinner) spinner.style.display = 'none';
        }
    }

        // Handle Add Product Form Submission
        document.getElementById('productForm').addEventListener('submit', async function(e) {
        e.preventDefault(); // Stop default HTML form reload

        // Gather input values (make sure IDs match your input fields)
        const productData = {
            product_name: document.getElementById('productName').value,
            description: document.getElementById('description').value,
            price: parseFloat(document.getElementById('price').value),
            stock_quantity: parseInt(document.getElementById('stockQuantity').value),
            category_id: parseInt(document.getElementById('categoryId').value)
        };

        try {
            const response = await fetch(contextPath + '/api/products', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'include',
                body: JSON.stringify(productData)
            });

            if (response.ok) {
                // Success: close modal, reset form, or reload table
                location.reload(); 
            } else {
                const errData = await response.json();
                alert('Error saving product: ' + (errData.error || 'Unknown error'));
            }
        } catch (error) {
            console.error('Network error:', error);
        }
    });

        // Delete Product Function
        async function deleteProduct(id) {
            if (!confirm('Are you sure you want to delete this product?')) return;

            try {
                const response = await fetch(contextPath + '/api/products?product_id=' + id, {
                    method: 'DELETE',
                    credentials: 'include'
                });

                if (response.ok) {
                    loadProducts();
                    showAlert('Product deleted successfully.', 'success');
                } else {
                    showAlert('Failed to delete product.', 'danger');
                }
            } catch (error) {
                showAlert('Network error while deleting product.', 'danger');
            }
        }

        // Helper for Alert Banners
        function showAlert(message, type) {
            const alert = document.getElementById('actionAlert');
            alert.className = 'alert alert-' + type;
            alert.textContent = message;
            alert.classList.remove('d-none');
            setTimeout(() => alert.classList.add('d-none'), 4000);
        }

        // Handle Logout
        document.getElementById('logoutBtn').addEventListener('click', async () => {
            await fetch(contextPath + '/api/auth/logout', { method: 'POST', credentials: 'include' });
            window.location.href = 'login.jsp';
        });

        window.addEventListener('DOMContentLoaded', loadProducts);
    </script>
</body>
</html>
