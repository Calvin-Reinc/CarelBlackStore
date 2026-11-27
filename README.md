# CarelStock — Inventory & E-Commerce Platform (`flying Jvck`)

![CarelStock CI](https://github.com/Calvin-Reinc/CarelStock/actions/workflows/ci.yml/badge.svg)
![Java 21](https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=openjdk&logoColor=white)
![Jakarta EE 10](https://img.shields.io/badge/Jakarta%20EE-10-007396?style=flat)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Neon-4169E1?style=flat&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Tomcat%2010.1-2496ED?style=flat&logo=docker&logoColor=white)
![Paystack](https://img.shields.io/badge/Payments-Paystack%20ZAR-00C3F7?style=flat)
![Cloudinary](https://img.shields.io/badge/Media-Cloudinary-3448C5?style=flat&logo=cloudinary&logoColor=white)
![JUnit 5](https://img.shields.io/badge/Tested-JUnit%205%20%2B%20Mockito-25A162?style=flat)

CarelStock is a full-stack inventory management system and online storefront for flying Jvck, an independent Pretoria streetwear and hand-painted canvas art brand. The application combines a public product catalogue and checkout with an administrator portal for managing products, categories, and orders.


* **Live Storefront:** `https://carelblackstore.onrender.com/` *(Hosted on Render free tier — first request after sleep may take ~30s to wake)*
* **Admin Login:** `https://carelblackstore.onrender.com/login.html`

## Features

- **Storefront:** browse and filter products, view product details, and manage a shopping cart.
- **Inventory administration:** authenticated administrators can manage products, categories, stock, and orders.
- **Image management:** product images are uploaded to Cloudinary.
- **Payments:** Paystack checkout in ZAR, with server-side price and stock validation, transaction verification, and signed webhook handling.
- **Community forms:** contact messages and newsletter subscriptions are stored in PostgreSQL.
- **Security controls:** session-based administrator access, method-scoped public API routes, JSON request validation, security response headers, and optional explicitly configured CORS.

## Technology

- Java 21
- Jakarta EE 10 (Servlets, JSP, Filters, and JAX-RS API)
- PostgreSQL with raw JDBC and HikariCP
- Jackson, BCrypt, Cloudinary Java SDK, and Paystack API
- HTML, CSS, JavaScript, and Bootstrap 5
- Maven, JUnit 5, Mockito, Docker, and Apache Tomcat 10.1

## Architecture

The backend follows a layered structure:

```text
Servlet controllers -> Services -> DAOs -> PostgreSQL
```

Servlets expose JSON APIs, and the storefront and admin pages are served from the same web application. Database access uses JDBC with a HikariCP connection pool. Paystack checkout creates a pending checkout using product prices and stock read from the database; after payment is verified, the order is finalized and stock is deducted transactionally.

## Requirements

- Java 21
- Maven 3.9 or later
- A PostgreSQL database with the schema expected by the application
- Paystack credentials for checkout
- Cloudinary credentials for product image uploads
- Docker, if using the container workflow below

> **Database schema:** This repository does not currently include SQL migrations or a schema creation script. Provision the tables and constraints expected by the DAOs before starting the application. The application uses `users`, `categories`, `products`, `orders`, `order_items`, `pending_checkouts`, `subscribers`, and `contact_messages`.


## How to Run Locally

### Prerequisites
* Java 21 (JDK 21)
* Apache Maven 3.9+
* PostgreSQL 16+ (local or Neon)
* Apache Tomcat 10.1 (or Docker)

### 1. Clone the Repository

git clone https://github.com/Calvin-Reinc/CarelStock.git
cd CarelStock

### 2. Configure Database & Local Credentials
Create your PostgreSQL schema (users, categories, products, orders, order_items, pending_checkouts, subscribers, contact_messages) and seed an initial ADMIN user.

follow this Entity Relationship Diagram:
## Database Schema (ER Diagram)

## Database Schema (ER Diagram)

```mermaid
erDiagram
    categories ||--o{ products : "categorizes (SET NULL)"
    orders ||--|{ order_items : "contains (CASCADE)"
    products ||--o{ order_items : "referenced in (RESTRICT)"
    orders |o--o| pending_checkouts : "finalized from (SET NULL)"

    users {
        int user_id PK
        varchar username UK
        varchar password_hash
        varchar role
        timestamp created_at
    }

    categories {
        int category_id PK
        varchar category_name
        text description
    }

    products {
        int product_id PK
        varchar product_name
        text description
        numeric price
        int stock_quantity
        int category_id FK
        text image_url
        varchar image_public_id
        timestamp created_at
    }

    orders {
        int order_id PK
        varchar customer_name
        varchar customer_email
        varchar customer_phone
        text shipping_address
        varchar payment_reference UK
        numeric total_amount
        varchar status
        timestamp created_at
    }

    order_items {
        int order_item_id PK
        int order_id FK
        int product_id FK
        int quantity
        numeric unit_price
    }

    pending_checkouts {
        int checkout_id PK
        varchar merchant_reference UK
        varchar ozow_payment_id
        varchar customer_name
        varchar customer_email
        varchar customer_phone
        text shipping_address
        text items_json
        numeric total_amount
        varchar status
        int order_id FK
        timestamp created_at
        timestamp updated_at
    }

    subscribers {
        int subscriber_id PK
        varchar email UK
        timestamp created_at
    }

    contact_messages {
        int message_id PK
        varchar name
        varchar email
        text message
        timestamp created_at
    }


### Then create three gitignored files inside src/main/resources/:

 ## src/main/resources/db.properties

Properties
db.url=jdbc:postgresql://localhost:5432/stockflow
db.user=postgres
db.password=YOUR_DB_PASSWORD

## src/main/resources/cloudinary.properties

Properties
cloudinary.cloud_name=YOUR_CLOUD_NAME
cloudinary.api_key=YOUR_API_KEY
cloudinary.api_secret=YOUR_API_SECRET

## src/main/resources/paystack.properties

Properties
paystack.secret_key=sk_test_YOUR_PAYSTACK_SECRET_KEY

### 3. Run unit tests and build
mvn clean test
mvn clean package -DskipTests

### 4. Run Locally (Tomcat 10.1 or Docker)
* Tomcat => Copy target/CarelStock-1.0-SNAPSHOT.war into Tomcat's webapps/CarelStock.war, run catalina.sh run, and open http://localhost:8080/CarelStock/
* Docker => docker build -t carelstock .
docker run --rm -p 8080:8080 \
  -e DATABASE_URL="postgresql://USER:PASSWORD@HOST:5432/neondb?sslmode=require" \
  -e CLOUDINARY_CLOUD_NAME="your_cloud_name" \
  -e CLOUDINARY_API_KEY="your_api_key" \
  -e CLOUDINARY_API_SECRET="your_api_secret" \
  -e PAYSTACK_SECRET_KEY="sk_test_..." \
  carelstock

## API overview

| Endpoint | Methods | Access |
| --- | --- | --- |
| `/api/products` | GET | Public |
| `/api/products` | POST, PUT, DELETE | Administrator |
| `/api/categories` | GET | Public |
| `/api/categories` | POST, PUT, DELETE | Administrator |
| `/api/checkout/paystack` | POST | Public |
| `/api/checkout/paystack/status` | GET | Public |
| `/api/checkout/paystack/webhook` | POST | Paystack webhook signature required |
| `/api/auth/login` | POST | Public |
| `/api/auth/logout` | POST | Public |
| `/api/auth/register` | POST | Administrator |
| `/api/orders` | GET, POST, DELETE | Administrator |
| `/api/dashboard` | GET | Administrator |
| `/api/subscribe` | POST | Public |
| `/api/contact` | POST | Public |

API request and response fields use `snake_case`. Product image uploads use `multipart/form-data`; other API writes use JSON unless noted by the endpoint.

## Deployment notes

- Ensure the production database has the required schema before deploying.
- Configure Paystack with live credentials and the public webhook URL for production.
- Review `AdminInitializer` before production use: it provisions a built-in administrator account when one is absent. Replace this bootstrap behavior with a secure deployment-specific process, and rotate any credentials if the application has already been deployed.
- Configure HTTPS at the hosting platform or reverse proxy. The application session cookie's `secure` setting is currently disabled in `WEB-INF/web.xml` and should be reviewed for HTTPS deployments.
- The Docker build skips Maven tests; run `mvn test` separately in CI or before building a release.

## Project layout

```text
src/main/java/com/carelstock/
  config/       Application startup configuration
  controller/   Servlet API endpoints
  dao/          JDBC data access
  database/     Connection pool and database configuration
  filter/       API security filter
  model/        Domain models
  service/      Business logic and external integrations
src/main/webapp/
  WEB-INF/      Web application configuration
  *.html        Storefront and customer-facing pages
  *.jsp         Admin and server-rendered pages
src/test/java/  Unit tests
Dockerfile      Multi-stage WAR build and Tomcat runtime
pom.xml         Maven dependencies and build configuration
```


A full-stack inventory management system and e-commerce storefront built from scratch with **Java 21, Jakarta EE 10, PostgreSQL, Paystack, Cloudinary, and Docker** for **flying Jvck**, an independent Pretoria streetwear and hand-painted canvas art brand.


---

## Screenshots

| Storefront Home (`flying Jvck`) | Lookbook Gallery |
| :---: | :---: |
| ![Storefront Home](docs/screenshots/01-storefront-home.png) | ![Lookbook](docs/screenshots/02-lookbook.png) |

| Shop & Cart Drawer | Admin Dashboard |
| :---: | :---: |
| ![Shop and Cart](docs/screenshots/03-shop-cart.png) | ![Admin Dashboard](docs/screenshots/04-admin-products.png) |

---

## Key Features

### Public Storefront (`flying Jvck`)
* **Brand Home (`index.html`):** Supreme-inspired editorial landing page with custom `@font-face` typography, art-through-letters hover reveal, live Pretoria (`PTA // SAST`) clock, **Drop Subscribe** modal, and **Contact** inquiry modal.
* **Lookbook (`lookbook.html`):** Defaults to the latest category and presents a large focused piece alongside a responsive thumbnail grid, keyboard/button paging, and a bottom category dropup selector.
* **Previews (`previews.html`):** Grouped category preview grid with story/notes view, swipe/arrow navigation between items, and a click-to-zoom lightbox window (`1×` / `2×` close-up).
* **Shop & Guest Checkout (`store.html`, `item.html`, `payment-result.html`):** Category sidebar (dropdown on mobile), persistent `localStorage` cart with real-time stock limits, and zero-login guest checkout powered by **Paystack (`ZAR`)**.
* **Custom Error Page (`error.jsp`):** Minimalist `404` / `500` handler mapped in `web.xml` displaying the brand logo, title, and Pretoria clock.

### Admin Portal (`dashboard.jsp`, `product.jsp`, `register.jsp`)
* **Inventory & Cloudinary Media:** Full product CRUD with `@MultipartConfig` image uploads (`POST` create & update), live previews, category dropdowns, and quick stock level adjustments.
* **Order Fulfillment:** Displays completed Paystack orders with full customer contact details (`name`, `email`, `phone`, `shipping_address`), Paystack reference, and itemized line items.
* **Community Inbox:** Manage drop-alert subscribers (with one-click email export) and customer contact messages.
* **Admin Account Provisioning (`register.jsp`):** Session-guarded registration so existing admins can onboard co-admins securely.

---

## Architecture & Engineering Highlights

* **Zero-Framework Layered Java Backend (`Controller → Service → DAO → Model`):** Built directly on Jakarta Servlets, WebFilters, JSP, and JDBC to maintain complete control over HTTP lifecycles, SQL queries, and database transactions.
* **Payment-First Atomic Checkout (Paystack `ZAR`):**
  * The frontend only submits `{ product_id, quantity }`—never prices.
  * `PaystackService` validates stock and computes authoritative totals from PostgreSQL, stores a `PENDING` record in `pending_checkouts`, and initializes Paystack in cents.
  * Orders and stock deductions are **only** written to `orders` / `order_items` after server-side Paystack verification (`GET /transaction/verify/{reference}`) or a verified `x-paystack-signature` (`HMAC-SHA512`) webhook.
  * An atomic `UPDATE ... WHERE status = 'PENDING'` lock + unique `payment_reference` constraint guarantees duplicate webhooks/redirects never create duplicate orders.
* **Defense-in-Depth Security (`SecurityFilter`):**
  * Uses normalized `getServletPath() + getPathInfo()` instead of raw `getRequestURI()` to prevent path-traversal bypasses (`/api/products/../orders`).
  * Applies `X-Content-Type-Options`, `X-Frame-Options`, and `Cache-Control: no-store` headers.
  * Enforces strict `application/json` checks (`415`) on mutations (except explicit `multipart/form-data` image routes) and separates `PUBLIC_GET_PATHS` from `PUBLIC_POST_PATHS` so guest subscribe/contact forms stay public while subscriber emails remain `ADMIN`-only (`401` / `403`).
* **ACID Transactions & Cloud Media Consistency:**
  * `OrderDAO` executes order insertion, line-item batching, and stock deduction inside an explicit JDBC transaction (`setAutoCommit(false)` / `rollback()`).
  * `ProductService` validates fields before uploading to Cloudinary, rolls back uploaded assets if the DB write fails, and deletes stale Cloudinary images on replacement or deletion.
* **Pooled Cloud Persistence & Portability:**
  * `DatabaseConnection` uses **HikariCP** with `DATABASE_URL` (`postgres://` → `jdbc:postgresql://...?sslmode=require`) environment variable parsing in production and gitignored `*.properties` files locally, plus `DatabaseCleanupListener` for clean Tomcat shutdown.

---

## REST API Summary

| Endpoint | Methods | Access | Purpose |
| :--- | :--- | :--- | :--- |
| `/api/auth/login`, `/api/auth/logout` | `POST` | Public | Session login (`BCrypt`) & logout |
| `/api/auth/register` | `POST` | Admin | Register additional admin user |
| `/api/products` | `GET` | Public | List/filter/sort/paginate or get single product |
| `/api/products` | `POST`, `PUT`, `DELETE` | Admin | Create/update (JSON or `multipart/form-data`) & delete |
| `/api/categories` | `GET` | Public | List categories |
| `/api/categories` | `POST`, `DELETE` | Admin | Create & delete categories |
| `/api/orders` | `GET`, `DELETE` | Admin | View completed orders with line items & delete |
| `/api/checkout/paystack` | `POST` | Public | Validate cart & initialize Paystack payment |
| `/api/checkout/paystack/status` | `GET` | Public | Verify Paystack transaction & finalize order |
| `/api/checkout/paystack/webhook` | `POST` | Paystack | `HMAC-SHA512` verified `charge.success` webhook |
| `/api/subscribe`, `/api/contact` | `POST` | Public | Save drop subscribers & contact messages |
| `/api/subscribe`, `/api/contact` | `GET`, `DELETE` | Admin | Read & manage subscribers/messages |

---
