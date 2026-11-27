# CarelStock

![CI](https://github.com/Calvin-Reinc/CarelStock/actions/workflows/ci.yml/badge.svg)

CarelStock is a full-stack inventory management system and online storefront for flying Jvck, an independent Pretoria streetwear and hand-painted canvas art brand. The application combines a public product catalogue and checkout with an administrator portal for managing products, categories, and orders.

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

## Configuration

Configure the application with environment variables. Do not commit credentials or add real secrets to this README.

| Variable | Required | Purpose |
| --- | --- | --- |
| `DB_URL` | Alternative | JDBC URL; use together with `DB_USER` and `DB_PASSWORD`. |
| `DB_USER` | Alternative | PostgreSQL username for `DB_URL`. |
| `DB_PASSWORD` | Alternative | PostgreSQL password for `DB_URL`. |
| `DATABASE_URL` | Alternative | PostgreSQL connection URL in `postgres://` or `postgresql://` form. |
| `PAYSTACK_SECRET_KEY` | For payments | Paystack test or live secret key. |
| `PAYSTACK_PUBLIC_BASE_URL` | Optional | Public application base URL used for the payment return URL. If omitted, the request URL is used. |
| `CLOUDINARY_URL` | For image uploads | Cloudinary URL; alternatively set all three `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, and `CLOUDINARY_API_SECRET`. |
| `CLOUDINARY_CLOUD_NAME` | Alternative | Cloudinary cloud name. |
| `CLOUDINARY_API_KEY` | Alternative | Cloudinary API key. |
| `CLOUDINARY_API_SECRET` | Alternative | Cloudinary API secret. |
| `CORS_ALLOWED_ORIGIN` | Optional | The single allowed origin for cross-origin API requests. CORS is disabled when unset. |
| `PORT` | Optional | HTTP port used by the Docker image; defaults to `8080`. |

Database configuration is resolved in this order: `DB_URL` plus `DB_USER` and `DB_PASSWORD`, then `DATABASE_URL`, then a local `db.properties` classpath resource. Paystack and Cloudinary also support local properties resources as fallbacks; use environment variables for deployments.

For local Docker use, create a private `.env` file in the project root and populate only the variables needed for your setup. Keep it out of version control.

Paystack must be configured to send `charge.success` events to:

```text
https://<your-public-host>/api/checkout/paystack/webhook
```

## Build and test

Run the unit tests:

```bash
mvn test
```

Package the application as a WAR:

```bash
mvn clean package
```

The artifact is created at:

```text
target/CarelStock-1.0-SNAPSHOT.war
```

## Run with Docker

Build the image:

```bash
docker build -t carelstock .
```

Run it locally after creating the `.env` file described above:

```bash
docker run --rm --name carelstock --env-file .env -p 8080:8080 carelstock
```

Open [http://localhost:8080](http://localhost:8080). The Docker image builds the WAR with Maven and runs it on Tomcat 10.1 with Java 21. It listens on the platform-provided `PORT` when available, or port `8080` otherwise.

Alternatively, deploy the WAR to an Apache Tomcat 10.1 installation configured for Java 21. The application is packaged as `CarelStock-1.0-SNAPSHOT.war`; its context path depends on the WAR filename.

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
