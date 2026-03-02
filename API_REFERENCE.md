# KPI Tracking Tool - Complete API Reference

Comprehensive reference for all API endpoints in the KPI Tracking Tool application.

---

## 📑 Table of Contents

1. [Base Information](#base-information)
2. [Authentication Endpoints](#authentication-endpoints)
3. [Employee Endpoints](#employee-endpoints)
4. [Monthly Submission Endpoints](#monthly-submission-endpoints)
5. [KPI Definition Endpoints](#kpi-definition-endpoints)
6. [Notification Endpoints](#notification-endpoints)
7. [Admin Endpoints](#admin-endpoints)
8. [Support Endpoints](#support-endpoints)
9. [System Endpoints](#system-endpoints)

---

## Base Information

### Base URL
```
http://localhost:8080
```

### API Version
```
/v3/api-docs (OpenAPI 3.0.0)
```

### Content Type
```
application/json
```

### Authentication
```
Authorization: Bearer <JWT_TOKEN>
```

### Rate Limiting
- Default: No rate limiting (can be configured)
- Timeout: 30 seconds per request

---

## Authentication Endpoints

### 1. Login
Register a user and obtain JWT token.

```http
POST /auth/login
Content-Type: application/json

Request:
{
  "email": "user@company.com",
  "password": "Password@123"
}

Response: 200 OK
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@company.com","password":"Password@123"}'
```

**Errors:**
- `400 Bad Request`: Invalid email/password format
- `401 Unauthorized`: Invalid credentials
- `500 Server Error`: Server error

---

### 2. Logout
Invalidate current JWT token.

```http
POST /auth/logout
Authorization: Bearer <token>

Response: 200 OK
{
  "status": "success",
  "message": "Logged out successfully"
}
```

---

### 3. Forgot Password
Request password reset link via email.

```http
POST /auth/forgot-password
Content-Type: application/json

Request:
{
  "email": "user@company.com"
}

Response: 200 OK
{
  "status": "success",
  "message": "Password reset link sent to email"
}
```

**Errors:**
- `400 Bad Request`: Invalid email format
- `404 Not Found`: Email not found
- `500 Server Error`: Email service error

---

### 4. Reset Password
Reset password using reset token from email.

```http
POST /auth/reset-password
Content-Type: application/json

Request:
{
  "resetToken": "token_received_in_email",
  "newPassword": "NewPassword@123"
}

Response: 200 OK
{
  "status": "success",
  "message": "Password reset successfully"
}
```

**Errors:**
- `400 Bad Request`: Invalid token or weak password
- `401 Unauthorized`: Token expired
- `500 Server Error`: Server error

---

### 5. Refresh Token
Get new access token using refresh token.

```http
POST /auth/refresh
Authorization: Bearer <refresh_token>

Response: 200 OK
{
  "accessToken": "new_token_here",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

---

## Employee Endpoints

### 1. Get All Employees
List all employees with pagination support.

```http
GET /employees/getall?limit=10&cursor=null
Authorization: Bearer <token>

Response: 200 OK
{
  "items": [
    {
      "employeeId": "EMP001",
      "employeeName": "John Doe",
      "email": "john@company.com",
      "empRole": "Manager",
      "band": "B3",
      "stream": "Engineering",
      "manager": null,
      "createdAt": "2026-03-01T10:00:00"
    }
  ],
  "nextCursor": "EMP002",
  "total": 100,
  "managerCount": 10,
  "adminCount": 5,
  "employeeCount": 85,
  "bandCount": 5
}
```

**Query Parameters:**
- `limit`: Page size (1-100, default 10)
- `cursor`: Cursor ID for pagination

---

### 2. Get Employee by ID
Get details of a specific employee.

```http
GET /employees/{employeeId}
Authorization: Bearer <token>

Response: 200 OK
{
  "employeeId": "EMP001",
  "employeeName": "John Doe",
  "email": "john@company.com",
  "empRole": "Manager",
  "band": "B3",
  "stream": "Engineering",
  "designation": "Senior Engineer",
  "manager": null,
  "createdAt": "2026-03-01T10:00:00"
}
```

**Errors:**
- `404 Not Found`: Employee not found

---

### 3. Create Employee
Create a new employee record.

```http
POST /employees/create
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "employeeId": "EMP002",
  "employeeName": "Jane Smith",
  "email": "jane@company.com",
  "empRole": "Employee",
  "band": "B2",
  "stream": "QA",
  "designation": "QA Engineer"
}

Response: 201 Created
{
  "employeeId": "EMP002",
  "employeeName": "Jane Smith",
  "email": "jane@company.com",
  "empRole": "Employee",
  "band": "B2",
  "stream": "QA"
}
```

**Required Fields:**
- `employeeId`: Unique employee ID
- `employeeName`: Full name
- `email`: Valid email address
- `empRole`: Employee, Manager, or Admin

**Errors:**
- `400 Bad Request`: Validation error
- `409 Conflict`: Employee already exists
- `403 Forbidden`: Insufficient permissions

---

### 4. Update Employee
Update employee details.

```http
PUT /employees/update/{employeeId}
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "employeeName": "Jane Smith",
  "band": "B3",
  "designation": "Senior QA Engineer"
}

Response: 200 OK
{
  "employeeId": "EMP002",
  "employeeName": "Jane Smith",
  "band": "B3",
  "designation": "Senior QA Engineer"
}
```

---

### 5. Delete Employee
Delete an employee record.

```http
DELETE /employees/delete/{employeeId}
Authorization: Bearer <token>

Response: 200 OK
{
  "status": "success",
  "message": "Employee deleted successfully"
}
```

**Errors:**
- `404 Not Found`: Employee not found
- `403 Forbidden`: Insufficient permissions

---

## Monthly Submission Endpoints

### 1. Save Draft
Save submission as draft without submitting.

```http
POST /monthly-submissions/draft
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "month": "2026-03",
  "selfReviewText": "In this month, I successfully completed...",
  "kpiRatings": [
    {
      "kpiName": "Delivery",
      "rating": 4
    },
    {
      "kpiName": "Quality",
      "rating": 5
    }
  ],
  "certifications": [
    {
      "certificationName": "AWS Solutions Architect"
    }
  ],
  "recognitionsCount": 2
}

Response: 200 OK
{
  "id": 123,
  "month": "2026-03",
  "status": "DRAFT",
  "reviewStatus": "DRAFT",
  "submittedAt": null
}
```

---

### 2. Submit Submission
Submit a finalized submission for review.

```http
POST /monthly-submissions/submit
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "month": "2026-03",
  "selfReviewText": "Final submission text...",
  "kpiRatings": [...]
}

Response: 200 OK
{
  "id": 123,
  "status": "SUBMITTED",
  "reviewStatus": "SUBMITTED",
  "submittedAt": "2026-03-02T10:30:45"
}
```

**Errors:**
- `400 Bad Request`: Missing required fields
- `409 Conflict`: Submission already exists

---

### 3. Get My Submission
Get current user's submission for a month.

```http
GET /monthly-submissions/me?month=2026-03
Authorization: Bearer <token>

Response: 200 OK
{
  "id": 123,
  "month": "2026-03",
  "status": "SUBMITTED",
  "selfReviewText": "...",
  "reviewStatus": "NEEDS_REVIEW",
  "adminReview": {
    "action": "REJECT",
    "comments": "Please provide more specific examples...",
    "rejectedAt": "2026-03-02T14:30:45",
    "rejectedBy": "admin@company.com"
  }
}
```

---

### 4. Get Submission History
Get all past submissions for current user.

```http
GET /monthly-submissions/me/history
Authorization: Bearer <token>

Response: 200 OK
[
  {
    "id": 123,
    "month": "2026-03",
    "status": "SUBMITTED"
  },
  {
    "id": 122,
    "month": "2026-02",
    "status": "SUBMITTED"
  }
]
```

---

### 5. Manager: Get Team Submissions
Get all team member submissions.

```http
GET /monthly-submissions/manager/team?month=2026-03&status=SUBMITTED&limit=20&cursor=null
Authorization: Bearer <token>

Response: 200 OK
{
  "items": [
    {
      "id": 123,
      "employeeId": "EMP001",
      "month": "2026-03",
      "status": "SUBMITTED",
      "reviewStatus": "SUBMITTED"
    }
  ],
  "nextCursor": "EMP002",
  "total": 50,
  "submittedCount": 45,
  "reviewedCount": 30,
  "pendingManagerReviewCount": 15
}
```

**Query Parameters:**
- `month`: Filter by month (yyyy-MM)
- `status`: DRAFT, SUBMITTED
- `limit`: Page size
- `cursor`: Pagination cursor

---

### 6. Admin: Get All Submissions
Get all submissions system-wide.

```http
GET /monthly-submissions/admin/all?month=2026-03&status=SUBMITTED
Authorization: Bearer <token>

Response: 200 OK
[
  {
    "id": 123,
    "employeeId": "EMP001",
    "month": "2026-03",
    "status": "SUBMITTED",
    "reviewStatus": "NEEDS_REVIEW"
  }
]
```

---

### 7. Admin: Get Submission by ID
Get details of specific submission.

```http
GET /monthly-submissions/admin/{id}
Authorization: Bearer <token>

Response: 200 OK
{
  "id": 123,
  "employeeId": "EMP001",
  "month": "2026-03",
  "status": "SUBMITTED",
  "selfReviewText": "...",
  "kpiRatings": [...],
  "reviewStatus": "SUBMITTED"
}
```

---

### 8. Admin: Approve Submission
Approve a submission.

```http
POST /monthly-submissions/admin/review
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "id": 123,
  "employeeId": "EMP001",
  "adminReview": {
    "action": "APPROVE",
    "comments": "Excellent work this month"
  }
}

Response: 200 OK
{
  "id": 123,
  "reviewStatus": "ADMIN_APPROVED",
  "adminSubmittedAt": "2026-03-02T15:00:00"
}
```

---

### 9. Admin: Reject Submission (NEW)
Reject a submission with mandatory feedback comments.

```http
POST /monthly-submissions/admin/{id}/reject
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "rejectionComments": "Please provide more specific examples of achievements and measurable outcomes in your self-review section."
}

Response: 200 OK
{
  "id": 123,
  "status": "DRAFT",
  "reviewStatus": "NEEDS_REVIEW",
  "reopenedForResubmission": true,
  "adminReview": {
    "action": "REJECT",
    "comments": "Please provide more specific examples...",
    "rejectedAt": "2026-03-02T15:15:45",
    "rejectedBy": "admin@company.com"
  }
}
```

**Validation:**
- Comments must be at least 10 characters
- Comments must be 5000 characters or less

**Errors:**
- `400 Bad Request`: Comments too short
- `404 Not Found`: Submission not found

---

### 10. Admin: Delete Submission
Delete a submission.

```http
DELETE /monthly-submissions/admin/{id}
Authorization: Bearer <token>

Response: 200 OK
{
  "status": "success",
  "id": "123"
}
```

---

## KPI Definition Endpoints

### 1. Get All KPI Definitions
List all available KPI definitions.

```http
GET /kpi-definitions/getall
Authorization: Bearer <token>

Response: 200 OK
[
  {
    "id": 1,
    "kpiName": "Delivery",
    "description": "On-time delivery of assigned tasks",
    "createdAt": "2026-01-01T10:00:00"
  },
  {
    "id": 2,
    "kpiName": "Quality",
    "description": "Code quality and testing standards"
  }
]
```

---

### 2. Create KPI Definition
Create a new KPI definition (Admin only).

```http
POST /kpi-definitions/create
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "kpiName": "Innovation",
  "description": "Implementation of innovative solutions"
}

Response: 201 Created
{
  "id": 3,
  "kpiName": "Innovation",
  "description": "Implementation of innovative solutions"
}
```

---

### 3. Update KPI Definition
Update KPI details (Admin only).

```http
PUT /kpi-definitions/update/{id}
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "kpiName": "Innovation",
  "description": "Updated description"
}

Response: 200 OK
```

---

### 4. Delete KPI Definition
Delete a KPI definition (Admin only).

```http
DELETE /kpi-definitions/delete/{id}
Authorization: Bearer <token>

Response: 200 OK
```

---

## Notification Endpoints

### 1. Subscribe to Notifications (SSE)
Subscribe to real-time notifications using Server-Sent Events.

```http
GET /notifications/subscribe?types=EMPLOYEE_SUBMITTED,MANAGER_APPROVED
Authorization: Bearer <token>

Response: 200 OK (Event Stream)
data: {"type":"EMPLOYEE_SUBMITTED","message":"EMP001 submitted for 2026-03"}
data: {"type":"MANAGER_APPROVED","message":"Your submission was approved"}
```

**Notification Types:**
- `EMPLOYEE_SUBMITTED`: Employee submission received
- `MANAGER_APPROVED`: Manager approved submission
- `ADMIN_APPROVED`: Admin approved submission
- `NEEDS_REVIEW`: Submission needs review
- `FORGOT_PASSWORD_REQUESTED`: Password reset requested

---

### 2. Get Notifications
Get notification list with pagination.

```http
GET /notifications/list?limit=25&cursor=null&unreadOnly=false&types=EMPLOYEE_SUBMITTED
Authorization: Bearer <token>

Response: 200 OK
{
  "items": [
    {
      "id": 1,
      "type": "EMPLOYEE_SUBMITTED",
      "title": "New submission",
      "message": "EMP001 submitted their review",
      "read": false,
      "createdAt": "2026-03-02T10:30:45"
    }
  ],
  "nextCursor": "2",
  "unreadCount": 5
}
```

---

### 3. Mark Notification as Read
Mark a single notification as read.

```http
POST /notifications/{id}/read
Authorization: Bearer <token>

Response: 200 OK
{
  "id": 1,
  "read": true
}
```

---

### 4. Mark All Notifications as Read
Mark all notifications as read.

```http
POST /notifications/read-all
Authorization: Bearer <token>

Response: 200 OK
{
  "status": "success",
  "message": "All notifications marked as read"
}
```

---

## Admin Endpoints

### AI Agent Management

#### 1. List AI Agents
List all configured AI agents.

```http
GET /ai-agents/list?activeOnly=true&limit=10&cursor=null
Authorization: Bearer <token>

Response: 200 OK
{
  "items": [
    {
      "id": 1,
      "provider": "openai",
      "active": true,
      "createdAt": "2026-01-01T10:00:00"
    }
  ],
  "nextCursor": "2"
}
```

---

#### 2. Add AI Agent
Add a new AI agent.

```http
POST /ai-agents/add
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "provider": "openai",
  "apiKey": "sk-...",
  "active": true
}

Response: 201 Created
{
  "id": 1,
  "provider": "openai",
  "active": true
}
```

---

#### 3. Update AI Agent
Update AI agent settings.

```http
POST /ai-agents/update/{id}
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "provider": "openai",
  "apiKey": "new_key_here",
  "active": true
}

Response: 200 OK
```

---

#### 4. Delete AI Agent
Delete an AI agent.

```http
DELETE /ai-agents/{id}
Authorization: Bearer <token>

Response: 200 OK
```

---

#### 5. Get AI Enhancement
Enhance review text using AI.

```http
POST /ai-agents/enhance
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "text": "This month I worked on feature X and completed it on time.",
  "mode": "self_review"
}

Response: 200 OK
{
  "text": "Enhanced review text with better wording and structure.",
  "provider": "openai",
  "model": "gpt-4o-mini"
}
```

**Modes:**
- `self_review`: Enhance self-review comments
- `manager_review`: Enhance manager review comments

---

### Cache Management

#### 1. Clear Cache
Clear application cache (Redis if enabled).

```http
POST /admin/cache/clear
Authorization: Bearer <token>

Response: 200 OK
{
  "status": "success",
  "message": "Cache cleared"
}
```

---

#### 2. Get Cache Statistics
Get cache performance metrics.

```http
GET /admin/cache/stats
Authorization: Bearer <token>

Response: 200 OK
{
  "hitCount": 1000,
  "missCount": 150,
  "hitRate": 86.96,
  "size": 256
}
```

---

## Support Endpoints

### 1. Submission Window
Get current submission window details.

```http
GET /submission-windows/current
Authorization: Bearer <token>

Response: 200 OK
{
  "id": 1,
  "windowName": "March 2026",
  "startDate": "2026-03-01",
  "endDate": "2026-03-31",
  "isActive": true
}
```

---

### 2. Band Directory
Get all bands/levels.

```http
GET /band-directory/all
Authorization: Bearer <token>

Response: 200 OK
[
  {
    "band": "B1",
    "description": "Junior"
  },
  {
    "band": "B2",
    "description": "Mid-level"
  }
]
```

---

### 3. Stream Directory
Get all streams/departments.

```http
GET /stream-directory/all
Authorization: Bearer <token>

Response: 200 OK
[
  {
    "stream": "Engineering",
    "description": "Software Development"
  },
  {
    "stream": "QA",
    "description": "Quality Assurance"
  }
]
```

---

### 4. Certifications
Get all certifications.

```http
GET /certifications/all
Authorization: Bearer <token>

Response: 200 OK
[
  {
    "id": 1,
    "certificationName": "AWS Solutions Architect"
  }
]
```

---

### 5. Webknot Values
Get company values.

```http
GET /webknot-values/all
Authorization: Bearer <token>

Response: 200 OK
[
  {
    "id": 1,
    "valueName": "Integrity"
  }
]
```

---

## System Endpoints

### 1. Health Check
Application health status.

```http
GET /actuator/health
Authorization: Optional

Response: 200 OK
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    },
    "redis": {
      "status": "UP"
    }
  }
}
```

---

### 2. Application Metrics
Prometheus metrics endpoint.

```http
GET /actuator/prometheus
Authorization: Optional

Response: 200 OK
# Prometheus format metrics
jvm_memory_used_bytes{...} 1000000
http_requests_total{...} 5000
```

---

### 3. API Documentation
Interactive Swagger UI.

```
GET /swagger-ui.html
```

---

### 4. OpenAPI Specification
Machine-readable API spec.

```http
GET /v3/api-docs

Response: 200 OK
{
  "openapi": "3.0.0",
  "info": {...},
  "paths": {...}
}
```

---

## Error Handling

### Standard Error Response

```json
{
  "status": "error",
  "message": "Error description",
  "code": "ERROR_CODE",
  "timestamp": "2026-03-02T10:30:45"
}
```

### HTTP Status Codes

| Code | Meaning |
|------|---------|
| 200 | OK - Request successful |
| 201 | Created - Resource created |
| 204 | No Content - Request processed, no response |
| 400 | Bad Request - Invalid input |
| 401 | Unauthorized - Authentication required |
| 403 | Forbidden - Access denied |
| 404 | Not Found - Resource not found |
| 409 | Conflict - Resource conflict |
| 500 | Server Error - Internal server error |
| 503 | Service Unavailable - Service down |

---

## Rate Limiting & Quotas

- **Requests per minute**: No limit (configurable)
- **Concurrent connections**: 50 (HikariCP default)
- **Request timeout**: 30 seconds
- **File upload limit**: 10MB

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | Mar 2, 2026 | Initial API documentation |
| 0.9 | Feb 28, 2026 | Beta documentation |

---

**Last Updated**: March 2, 2026  
**API Version**: 1.0  
**Status**: Production Ready

---

For more information, visit the [README.md](./README.md) file.
