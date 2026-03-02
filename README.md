# KPI Tracking Tool

A comprehensive Java-based Spring Boot application for managing Key Performance Indicators (KPIs), employee submissions, performance reviews, and organizational metrics.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
  - [Authentication](#authentication-endpoints)
  - [Employees](#employee-endpoints)
  - [Monthly Submissions](#monthly-submission-endpoints)
  - [KPI Definitions](#kpi-definition-endpoints)
  - [Notifications](#notification-endpoints)
  - [Admin Operations](#admin-endpoints)
  - [Other Endpoints](#other-endpoints)
- [Database Setup](#database-setup)
- [Environment Variables](#environment-variables)
- [Monitoring & Health](#monitoring--health)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

---

## 📖 Overview

The KPI Tracking Tool is a comprehensive performance management system that enables:

- **Employee Management**: Create and manage employee profiles with roles and designations
- **KPI Submission**: Employees can submit monthly KPIs, self-reviews, and certifications
- **Performance Review**: Managers and admins can review, approve, or reject submissions
- **Real-time Notifications**: SSE-based notifications for submission status updates
- **Admin Dashboard**: Comprehensive admin tools for cache management and system monitoring
- **AI Enhancement**: AI-powered review comment enhancement using OpenAI
- **Audit Trail**: Complete tracking of all submissions and rejections
- **Multi-role Support**: Employee, Manager, and Admin roles with different permissions

---

## 🛠 Tech Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| **Language** | Java | 17 |
| **Framework** | Spring Boot | 4.0.2 |
| **ORM** | Hibernate JPA | Latest |
| **Database** | PostgreSQL | 12+ |
| **Cache** | Redis | Optional |
| **Authentication** | JWT (JJWT) | 0.12.6 |
| **API Documentation** | Springdoc OpenAPI | 2.8.6 |
| **Monitoring** | Micrometer Prometheus | Latest |
| **Session Management** | Spring Session Redis | Latest |
| **Build Tool** | Maven | 3.6+ |

---

## 📋 Prerequisites

Before you begin, ensure you have the following installed:

### Required Software
- **Java 17 JDK** - [Download](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
- **Maven 3.6 or higher** - [Download](https://maven.apache.org/download.cgi)
- **PostgreSQL 12 or higher** - [Download](https://www.postgresql.org/download/)
- **Git** - [Download](https://git-scm.com/)

### Optional Software
- **Redis 6.0+** - For caching and session management
- **Docker & Docker Compose** - For containerized deployment

### Verify Installation
```bash
# Check Java version
java -version

# Check Maven version
mvn -version

# Check PostgreSQL
psql --version
```

---

## 📦 Installation

### Step 1: Clone the Repository

```bash
git clone https://github.com/yourusername/kpi-tracking-tool.git
cd kpi-tracking-tool
```

### Step 2: Create PostgreSQL Database

```bash
# Connect to PostgreSQL
psql -U postgres

# Create database
CREATE DATABASE webknot;

# Create schema
CREATE SCHEMA dev;

# Exit
\q
```

### Step 3: Load Database Schema

```bash
# Apply schema from SQL files
psql -U postgres -d webknot -f sql/dev_schema.sql
psql -U postgres -d webknot -f sql/band_stream_directory_seed.sql
psql -U postgres -d webknot -f sql/designation_lookup_details_seed.sql
psql -U postgres -d webknot -f sql/certifications_webknot_values_seed.sql
```

### Step 4: Configure Environment

```bash
# Copy example environment file
cp .env.example .env

# Edit .env with your settings
nano .env
```

See [Environment Variables](#environment-variables) section for details.

### Step 5: Build the Application

```bash
# Clean and build
mvn clean package

# Build without running tests
mvn clean package -DskipTests
```

### Step 6: Verify Build

```bash
# Check if JAR was created
ls -la target/kpi-0.0.1-SNAPSHOT.jar
```

---

## ⚙️ Configuration

### application.properties

Key configuration options in `src/main/resources/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/webknot
spring.datasource.username=likhithrajup
spring.datasource.password=your_password
spring.jpa.properties.hibernate.default_schema=dev

# Redis (optional)
spring.redis.enabled=false
spring.redis.host=localhost
spring.redis.port=6379

# JWT
jwt.secret=your_secret_key_here
jwt.expiration-ms=86400000

# Server
server.port=8080
server.servlet.context-path=/

# HikariCP Connection Pool
spring.datasource.hikari.maximum-pool-size=50
spring.datasource.hikari.minimum-idle=10
spring.datasource.hikari.connection-timeout=30000

# Logging
logging.level.root=INFO
logging.level.com.webknot.kpi=DEBUG
```

### .env File

Create a `.env` file in the root directory:

```bash
# Database Configuration
DB_URL=jdbc:postgresql://localhost:5432/webknot
DB_USER=likhithrajup
DB_PASSWORD=your_secure_password

# Redis Configuration
REDIS_ENABLED=false
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT Configuration
JWT_SECRET=your_super_secret_key_32_characters_or_more
JWT_EXPIRATION_MS=86400000

# Authentication Tokens
AUTH_RESET_TOKEN_EXPIRATION_MS=900000

# Cookie Settings
AUTH_COOKIE_NAME=access_token
AUTH_COOKIE_SECURE=false
AUTH_COOKIE_SAME_SITE=Lax
AUTH_COOKIE_MAX_AGE_SECONDS=86400

# AI Service
AI_ENHANCE_MODEL=gpt-4o-mini

# Logging
LOG_LEVEL=INFO
```

---

## 🚀 Running the Application

### Option 1: Using Maven

```bash
# Run the application
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

### Option 2: Using JAR File

```bash
# Run the built JAR
java -jar target/kpi-0.0.1-SNAPSHOT.jar

# Run with environment variables
java -Dspring.datasource.url=jdbc:postgresql://localhost:5432/webknot \
     -Dspring.datasource.username=likhithrajup \
     -Dspring.datasource.password=yourpassword \
     -jar target/kpi-0.0.1-SNAPSHOT.jar
```

### Option 3: Using Docker

```bash
# Build Docker image
docker build -t kpi-tracking-tool .

# Run container
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://postgres:5432/webknot \
  -e DB_USER=postgres \
  -e DB_PASSWORD=yourpassword \
  kpi-tracking-tool
```

### Verify Application Started

```bash
# Check if application is running
curl http://localhost:8080/actuator/health

# Expected response
{"status":"UP"}
```

Access the application:
- **Application**: http://localhost:8080
- **API Documentation (Swagger)**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/prometheus

---

## 📚 API Documentation

### Base URL
```
http://localhost:8080
```

### Response Format
All endpoints return JSON responses with the following structure:

**Success (200 OK)**:
```json
{
  "id": 1,
  "data": "...",
  "status": "success"
}
```

**Error (4xx, 5xx)**:
```json
{
  "error": "Error message",
  "status": "error",
  "timestamp": "2026-03-02T10:30:45"
}
```

---

## 🔐 Authentication Endpoints

### Login
```http
POST /auth/login
Content-Type: application/json

{
  "email": "employee@company.com",
  "password": "Password@123"
}

Response: 200 OK
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

**cURL Example:**
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password"}'
```

### Forgot Password
```http
POST /auth/forgot-password
Content-Type: application/json

{
  "email": "employee@company.com"
}

Response: 200 OK
{
  "status": "success",
  "message": "Reset link sent to email"
}
```

### Reset Password
```http
POST /auth/reset-password
Content-Type: application/json

{
  "resetToken": "token_from_email",
  "newPassword": "NewPassword@123"
}

Response: 200 OK
{
  "status": "success",
  "message": "Password reset successfully"
}
```

### Logout
```http
POST /auth/logout
Authorization: Bearer {token}

Response: 200 OK
{
  "status": "success",
  "message": "Logged out successfully"
}
```

---

## 👥 Employee Endpoints

### Get All Employees
```http
GET /employees/getall?limit=10&cursor=null
Authorization: Bearer {token}

Response: 200 OK
[
  {
    "employeeId": "EMP001",
    "employeeName": "John Doe",
    "email": "john@company.com",
    "empRole": "Manager",
    "band": "B3",
    "stream": "Engineering"
  }
]
```

### Get Employee by ID
```http
GET /employees/{employeeId}
Authorization: Bearer {token}

Response: 200 OK
{
  "employeeId": "EMP001",
  "employeeName": "John Doe",
  "email": "john@company.com",
  "empRole": "Manager"
}
```

### Create Employee
```http
POST /employees/create
Authorization: Bearer {token}
Content-Type: application/json

{
  "employeeId": "EMP002",
  "employeeName": "Jane Smith",
  "email": "jane@company.com",
  "empRole": "Employee",
  "band": "B2",
  "stream": "QA"
}

Response: 201 Created
```

### Update Employee
```http
PUT /employees/update/{employeeId}
Authorization: Bearer {token}
Content-Type: application/json

{
  "employeeName": "Jane Smith Updated",
  "band": "B3"
}

Response: 200 OK
```

### Delete Employee
```http
DELETE /employees/delete/{employeeId}
Authorization: Bearer {token}

Response: 200 OK
{
  "status": "success",
  "message": "Employee deleted"
}
```

---

## 📤 Monthly Submission Endpoints

### Save Draft
```http
POST /monthly-submissions/draft
Authorization: Bearer {token}
Content-Type: application/json

{
  "month": "2026-03",
  "selfReviewText": "This month I...",
  "kpiRatings": [
    {"kpiName": "Delivery", "rating": 4}
  ],
  "recognitionsCount": 2
}

Response: 200 OK
```

### Submit Submission
```http
POST /monthly-submissions/submit
Authorization: Bearer {token}
Content-Type: application/json

{
  "month": "2026-03",
  "selfReviewText": "Final submission..."
}

Response: 200 OK
```

### Get My Submission
```http
GET /monthly-submissions/me?month=2026-03
Authorization: Bearer {token}

Response: 200 OK
{
  "id": 123,
  "status": "SUBMITTED",
  "month": "2026-03"
}
```

### Get Submission History
```http
GET /monthly-submissions/me/history
Authorization: Bearer {token}

Response: 200 OK
[...]
```

### Manager: Get Team Submissions
```http
GET /monthly-submissions/manager/team?month=2026-03&status=SUBMITTED
Authorization: Bearer {token}

Response: 200 OK
[...]
```

### Admin: Get All Submissions
```http
GET /monthly-submissions/admin/all?month=2026-03&status=SUBMITTED
Authorization: Bearer {token}

Response: 200 OK
[...]
```

### Admin: Get Submission by ID
```http
GET /monthly-submissions/admin/{id}
Authorization: Bearer {token}

Response: 200 OK
{
  "id": 123,
  "status": "SUBMITTED",
  "month": "2026-03"
}
```

### Admin: Review Submission
```http
POST /monthly-submissions/admin/review
Authorization: Bearer {token}
Content-Type: application/json

{
  "id": 123,
  "employeeId": "EMP001",
  "adminReview": {
    "action": "APPROVE",
    "comments": "Good work"
  }
}

Response: 200 OK
```

### Admin: Reject Submission (NEW)
```http
POST /monthly-submissions/admin/{id}/reject
Authorization: Bearer {token}
Content-Type: application/json

{
  "rejectionComments": "Please add more specific examples to your self-review."
}

Response: 200 OK
{
  "status": "DRAFT",
  "reviewStatus": "NEEDS_REVIEW",
  "adminReview": {
    "action": "REJECT",
    "comments": "..."
  }
}
```

### Admin: Delete Submission
```http
DELETE /monthly-submissions/admin/{id}
Authorization: Bearer {token}

Response: 200 OK
```

---

## 📋 KPI Definition Endpoints

### Get All KPIs
```http
GET /kpi-definitions/getall
Authorization: Bearer {token}

Response: 200 OK
[
  {
    "id": 1,
    "kpiName": "Delivery",
    "description": "On-time delivery of tasks",
    "rating": 5
  }
]
```

### Create KPI
```http
POST /kpi-definitions/create
Authorization: Bearer {token}
Content-Type: application/json

{
  "kpiName": "Quality",
  "description": "Code quality metrics"
}

Response: 201 Created
```

### Update KPI
```http
PUT /kpi-definitions/update/{id}
Authorization: Bearer {token}

Response: 200 OK
```

### Delete KPI
```http
DELETE /kpi-definitions/delete/{id}
Authorization: Bearer {token}

Response: 200 OK
```

---

## 🔔 Notification Endpoints

### Subscribe to Notifications (SSE)
```http
GET /notifications/subscribe?types=EMPLOYEE_SUBMITTED,MANAGER_APPROVED
Authorization: Bearer {token}

Response: 200 OK (Event Stream)
data: {"type":"EMPLOYEE_SUBMITTED","message":"..."}
```

### Get Notifications
```http
GET /notifications/list?limit=10&cursor=null&unreadOnly=false
Authorization: Bearer {token}

Response: 200 OK
[
  {
    "id": 1,
    "type": "EMPLOYEE_SUBMITTED",
    "title": "New submission",
    "message": "Employee submitted...",
    "read": false
  }
]
```

### Mark as Read
```http
POST /notifications/{id}/read
Authorization: Bearer {token}

Response: 200 OK
```

### Mark All as Read
```http
POST /notifications/read-all
Authorization: Bearer {token}

Response: 200 OK
```

---

## ⚙️ Admin Endpoints

### AI Agent Management

**List AI Agents:**
```http
GET /ai-agents/list?activeOnly=true&limit=10
Authorization: Bearer {token}

Response: 200 OK
```

**Add AI Agent:**
```http
POST /ai-agents/add
Authorization: Bearer {token}
Content-Type: application/json

{
  "provider": "openai",
  "apiKey": "sk-...",
  "active": true
}

Response: 201 Created
```

**Update AI Agent:**
```http
POST /ai-agents/update/{id}
Authorization: Bearer {token}

Response: 200 OK
```

**Delete AI Agent:**
```http
DELETE /ai-agents/{id}
Authorization: Bearer {token}

Response: 200 OK
```

### AI Enhancement
```http
POST /ai-agents/enhance
Authorization: Bearer {token}
Content-Type: application/json

{
  "text": "Review text to enhance",
  "mode": "self_review"
}

Response: 200 OK
{
  "text": "Enhanced review text",
  "provider": "openai",
  "model": "gpt-4o-mini"
}
```

### Cache Management

**Clear Cache:**
```http
POST /admin/cache/clear
Authorization: Bearer {token}

Response: 200 OK
```

**Get Cache Stats:**
```http
GET /admin/cache/stats
Authorization: Bearer {token}

Response: 200 OK
```

---

## 📊 Other Endpoints

### Submission Window
```http
GET /submission-windows/current
Authorization: Bearer {token}

GET /submission-windows/all
Authorization: Bearer {token}

POST /submission-windows/create
Authorization: Bearer {token}
```

### Band Directory
```http
GET /band-directory/all
Authorization: Bearer {token}
```

### Stream Directory
```http
GET /stream-directory/all
Authorization: Bearer {token}
```

### Certifications
```http
GET /certifications/all
Authorization: Bearer {token}

POST /certifications/create
Authorization: Bearer {token}
```

### Webknot Values
```http
GET /webknot-values/all
Authorization: Bearer {token}
```

---

## 🗄️ Database Setup

### Supported Databases
- **PostgreSQL 12+** (Recommended)

### Schema Files
Located in `sql/` directory:

1. **dev_schema.sql** - Core schema creation
2. **band_stream_directory_seed.sql** - Band and stream data
3. **designation_lookup_details_seed.sql** - Designations
4. **certifications_webknot_values_seed.sql** - Certifications

### Initialize Database

```bash
# 1. Create database
createdb -U postgres webknot

# 2. Create schema
psql -U postgres -d webknot -c "CREATE SCHEMA dev;"

# 3. Load schema
psql -U postgres -d webknot -f sql/dev_schema.sql

# 4. Load seed data
psql -U postgres -d webknot -f sql/band_stream_directory_seed.sql
psql -U postgres -d webknot -f sql/designation_lookup_details_seed.sql
psql -U postgres -d webknot -f sql/certifications_webknot_values_seed.sql
```

### Database Cleanup

```bash
# Drop schema (WARNING: deletes all data)
psql -U postgres -d webknot -c "DROP SCHEMA dev CASCADE;"

# Drop database (WARNING: deletes everything)
dropdb -U postgres webknot
```

---

## 🌍 Environment Variables

### Required Variables

```bash
# Database
DB_URL=jdbc:postgresql://localhost:5432/webknot
DB_USER=likhithrajup
DB_PASSWORD=secure_password

# JWT
JWT_SECRET=your_32_character_or_longer_secret_key
JWT_EXPIRATION_MS=86400000
```

### Optional Variables

```bash
# Redis
REDIS_ENABLED=false
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DB=0
REDIS_TIMEOUT=2000

# Redis Connection Pool
REDIS_POOL_MAX_ACTIVE=20
REDIS_POOL_MAX_IDLE=10
REDIS_POOL_MIN_IDLE=5
REDIS_POOL_MAX_WAIT=-1ms

# Authentication
AUTH_RESET_TOKEN_EXPIRATION_MS=900000
AUTH_COOKIE_NAME=access_token
AUTH_COOKIE_SECURE=false
AUTH_COOKIE_SAME_SITE=Lax
AUTH_COOKIE_MAX_AGE_SECONDS=86400

# AI Service
AI_ENHANCE_MODEL=gpt-4o-mini
```

### Loading from .env

The application automatically loads from `.env` file via:
```properties
spring.config.import=optional:file:.env[.properties]
```

---

## 📊 Monitoring & Health

### Health Check Endpoint
```bash
curl http://localhost:8080/actuator/health
```

### Application Metrics (Prometheus)
```bash
curl http://localhost:8080/actuator/prometheus
```

### Available Metrics
- JVM memory, garbage collection
- HTTP requests (count, timing)
- Database connections (HikariCP)
- Custom application metrics

### Health Indicators
- Database connectivity
- Redis connectivity (if enabled)
- Disk space
- Custom health checks

### Actuator Endpoints
```
/actuator/health          - Application health
/actuator/info            - Application info
/actuator/prometheus      - Prometheus metrics
/actuator/metrics         - All metrics
/actuator/env             - Environment properties
```

---

## 🐛 Troubleshooting

### Issue: Database Connection Failed

**Error**: `PSQLException: Connection to localhost:5432 refused`

**Solution**:
```bash
# 1. Check if PostgreSQL is running
pg_isready -h localhost -p 5432

# 2. Verify credentials in .env
cat .env | grep DB_

# 3. Verify database exists
psql -U postgres -l | grep webknot

# 4. Create database if missing
createdb -U postgres webknot
```

### Issue: Port 8080 Already in Use

**Error**: `Address already in use`

**Solution**:
```bash
# Find process using port 8080
lsof -i :8080

# Kill the process (replace PID)
kill -9 <PID>

# Or use different port
java -Dserver.port=8081 -jar target/kpi-0.0.1-SNAPSHOT.jar
```

### Issue: Connection Leak Detection

**Error**: `Connection leak detected for org.postgresql.jdbc.PgConnection`

**Solution**: This is normal with HikariCP. Check that:
- All transactions complete within timeout (default 30s)
- No long-running external API calls inside transactions
- Database queries are optimized

See `CONNECTION_LEAK_ANALYSIS.md` for details.

### Issue: Swagger UI Not Loading

**Error**: `http://localhost:8080/swagger-ui.html` returns 404

**Solution**:
```bash
# Verify dependency is included
mvn dependency:tree | grep springdoc

# Check if endpoint is exposed
curl http://localhost:8080/v3/api-docs
```

### Issue: Redis Connection Failed

**Error**: `Error connecting to Redis at localhost:6379`

**Solution**: 
```bash
# Option 1: Disable Redis
echo "REDIS_ENABLED=false" >> .env

# Option 2: Start Redis
redis-server

# Option 3: Verify Redis
redis-cli ping
```

### Issue: Authentication Token Expired

**Error**: `401 Unauthorized: Invalid or expired token`

**Solution**:
```bash
# Get new token by logging in again
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password"}'
```

### Viewing Logs

```bash
# Check application logs
tail -f target/log/application.log

# View logs during execution
java -jar target/kpi-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
tail -f app.log

# Enable debug logging
-Dlogging.level.com.webknot.kpi=DEBUG
```

---

## 🤝 Contributing

### Code Standards
- Java 17 compatible
- Follow Spring Boot conventions
- Add JavaDoc for public methods
- Write unit tests for new features

### Pull Request Process
1. Create feature branch: `git checkout -b feature/feature-name`
2. Make changes and commit: `git commit -am 'Add feature'`
3. Push to branch: `git push origin feature/feature-name`
4. Open Pull Request with description

### Testing
```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=TestClassName

# Run with coverage
mvn test jacoco:report
```

---

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## 📞 Support & Documentation

### Additional Documentation
- [Connection Leak Analysis](./CONNECTION_LEAK_ANALYSIS.md)
- [Admin Rejection Feature](./ADMIN_REJECTION_README.md)
- [API Implementation Summary](./ADMIN_REJECTION_IMPL_SUMMARY.md)

### Getting Help
1. Check the troubleshooting section above
2. Review logs in `target/log/` directory
3. Check GitHub issues
4. Contact the development team

---

## ✅ Quick Checklist

Before deploying to production:

- [ ] Database is properly initialized
- [ ] All environment variables are set
- [ ] Application builds without errors (`mvn clean package`)
- [ ] Tests pass (`mvn test`)
- [ ] Health check returns UP (`/actuator/health`)
- [ ] Can login successfully
- [ ] Swagger UI is accessible
- [ ] Redis is configured (if needed)
- [ ] SSL/TLS is enabled (for production)
- [ ] Backups are configured

---

## 🎯 What's Next?

1. **Explore API**: Visit Swagger UI at `http://localhost:8080/swagger-ui.html`
2. **Create Test Users**: Use Employee endpoints to create test accounts
3. **Submit Data**: Test submission workflow
4. **Review Features**: Test manager and admin features
5. **Monitor**: Check health and metrics endpoints

---

**Last Updated**: March 2, 2026  
**Version**: 0.0.1-SNAPSHOT  
**Status**: Active Development

---

For questions or issues, please open a GitHub issue or contact the development team.