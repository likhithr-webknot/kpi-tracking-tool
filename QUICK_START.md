# Quick Start Guide - KPI Tracking Tool

Get up and running in 5 minutes!

---

## ⚡ TL;DR (The Fastest Way)

```bash
# 1. Clone repo
git clone https://github.com/yourusername/kpi-tracking-tool.git
cd kpi-tracking-tool

# 2. Setup database
createdb webknot
psql -U postgres -d webknot -c "CREATE SCHEMA dev;"
psql -U postgres -d webknot -f sql/dev_schema.sql

# 3. Configure
cp .env.example .env
# Edit .env with your DB credentials

# 4. Build
mvn clean package -DskipTests

# 5. Run
java -jar target/kpi-0.0.1-SNAPSHOT.jar

# 6. Access
# API: http://localhost:8080
# Swagger: http://localhost:8080/swagger-ui.html
# Health: http://localhost:8080/actuator/health
```

---

## 🔧 System Requirements

✅ **Required**:
- Java 17+
- Maven 3.6+
- PostgreSQL 12+

✅ **Optional**:
- Redis 6+ (for caching)
- Docker (for containerization)

---

## 📥 Installation (Step by Step)

### 1️⃣ Prerequisites Check

```bash
java -version    # Should show Java 17+
mvn -version     # Should show Maven 3.6+
psql --version   # Should show PostgreSQL 12+
```

If any fail, install from [Prerequisites](./README.md#-prerequisites) section.

### 2️⃣ Clone Repository

```bash
git clone https://github.com/yourusername/kpi-tracking-tool.git
cd kpi-tracking-tool
```

### 3️⃣ Setup Database

```bash
# Start PostgreSQL (if not running)
# macOS: brew services start postgresql
# Linux: sudo systemctl start postgresql
# Windows: net start PostgreSQL-x64-XX

# Create database
createdb -U postgres webknot

# Create schema
psql -U postgres -d webknot -c "CREATE SCHEMA dev;"

# Load schema and seed data
psql -U postgres -d webknot -f sql/dev_schema.sql
psql -U postgres -d webknot -f sql/band_stream_directory_seed.sql
psql -U postgres -d webknot -f sql/designation_lookup_details_seed.sql
psql -U postgres -d webknot -f sql/certifications_webknot_values_seed.sql

# Verify database
psql -U postgres -d webknot -c "\d" 
```

### 4️⃣ Configure Environment

```bash
# Create .env file
cat > .env << EOF
DB_URL=jdbc:postgresql://localhost:5432/webknot
DB_USER=postgres
DB_PASSWORD=your_secure_password

REDIS_ENABLED=false
JWT_SECRET=your_super_secret_key_32_characters_or_more_here
JWT_EXPIRATION_MS=86400000
EOF

# Verify .env
cat .env
```

### 5️⃣ Build Application

```bash
# Clean and build
mvn clean package -DskipTests

# This takes 1-2 minutes
# You should see: BUILD SUCCESS
```

### 6️⃣ Run Application

```bash
java -jar target/kpi-0.0.1-SNAPSHOT.jar

# Wait for:
# Started KpiApplication in X seconds
# Application is running on http://localhost:8080
```

### 7️⃣ Verify It Works

```bash
# In a new terminal
curl http://localhost:8080/actuator/health
# Should return: {"status":"UP"}
```

---

## 🌐 Access Application

| Service | URL |
|---------|-----|
| **Application Home** | http://localhost:8080 |
| **Swagger API Docs** | http://localhost:8080/swagger-ui.html |
| **API JSON Spec** | http://localhost:8080/v3/api-docs |
| **Health Check** | http://localhost:8080/actuator/health |
| **Metrics** | http://localhost:8080/actuator/prometheus |

---

## 🔐 Test Login

### Create Test User (Optional)

```bash
# Using curl to create a test employee
curl -X POST http://localhost:8080/employees/create \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": "TEST001",
    "employeeName": "Test User",
    "email": "test@company.com",
    "empRole": "Employee",
    "band": "B2",
    "stream": "Engineering"
  }'
```

### Login

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@company.com",
    "password": "Password@123"
  }'

# Response includes accessToken
```

---

## 📊 First Steps After Installation

1. **Visit Swagger UI**: http://localhost:8080/swagger-ui.html
2. **Explore Endpoints**: Click on endpoints to see documentation
3. **Try Login**: Use /auth/login endpoint
4. **View Employees**: Use /employees/getall endpoint
5. **Check Health**: Visit /actuator/health

---

## 🐛 Common Issues & Quick Fixes

### ❌ "Database connection failed"

```bash
# Fix: Make sure PostgreSQL is running
# macOS:
brew services start postgresql

# Linux:
sudo systemctl start postgresql

# Verify:
pg_isready -h localhost -p 5432
# Should show: accepting connections
```

### ❌ "Port 8080 already in use"

```bash
# Fix: Kill process using port or use different port
lsof -i :8080          # Find process
kill -9 <PID>          # Kill it

# Or use different port:
java -Dserver.port=8081 -jar target/kpi-0.0.1-SNAPSHOT.jar
```

### ❌ "Build failed"

```bash
# Fix: Clean and rebuild
mvn clean package -DskipTests

# If still fails, check Java version:
java -version    # Must be 17+
```

### ❌ "404 Not Found on Swagger UI"

```bash
# Fix: Check if application is really running
curl http://localhost:8080/actuator/health

# If it works, Swagger might be loading. Wait a moment and refresh.
```

---

## 📁 Project Structure

```
kpi-tracking-tool/
├── README.md                 # Main documentation
├── QUICK_START.md           # This file
├── pom.xml                  # Maven configuration
├── src/
│   ├── main/
│   │   ├── java/com/webknot/kpi/
│   │   │   ├── controller/  # API endpoints
│   │   │   ├── service/     # Business logic
│   │   │   ├── repository/  # Database access
│   │   │   ├── models/      # Entity classes
│   │   │   └── config/      # Application config
│   │   └── resources/
│   │       └── application.properties
│   └── test/                # Unit tests
├── sql/                     # Database schemas & seed data
├── target/                  # Build output
└── .env                     # Environment config (create this)
```

---

## 🚀 Common Commands

### Building

```bash
# Full build with tests
mvn clean package

# Build without tests (faster)
mvn clean package -DskipTests

# Run tests only
mvn test

# Check dependencies
mvn dependency:tree
```

### Running

```bash
# Using Maven
mvn spring-boot:run

# Using JAR
java -jar target/kpi-0.0.1-SNAPSHOT.jar

# With custom port
java -Dserver.port=8081 -jar target/kpi-0.0.1-SNAPSHOT.jar
```

### Database

```bash
# Connect to database
psql -U postgres -d webknot

# Backup database
pg_dump -U postgres -d webknot > backup.sql

# Restore database
psql -U postgres -d webknot < backup.sql

# Drop database
dropdb -U postgres webknot
```

---

## 📚 Next Steps

After successful installation:

1. **Read Full README**: Open [README.md](./README.md)
2. **Explore API**: Check all available endpoints in [API Documentation](./README.md#-api-documentation)
3. **Configure Redis**: Optional, but recommended for production
4. **Deploy**: See deployment section in README
5. **Monitor**: Setup monitoring and alerts

---

## 🆘 Need Help?

1. **Check Logs**: Look in terminal where application runs
2. **Read README**: Detailed docs in [README.md](./README.md)
3. **Troubleshoot**: See [Troubleshooting](./README.md#-troubleshooting) section
4. **Check Health**: Visit `/actuator/health` endpoint
5. **Ask Team**: Contact development team

---

## ✅ Installation Verification Checklist

- [ ] Java 17+ installed
- [ ] Maven 3.6+ installed
- [ ] PostgreSQL 12+ installed
- [ ] Repository cloned
- [ ] Database created
- [ ] Schema loaded
- [ ] .env file created and configured
- [ ] Application built successfully
- [ ] Application running (no errors)
- [ ] Health endpoint returns UP
- [ ] Swagger UI loads
- [ ] Can login with test user

---

## 📝 Environment File (.env)

**Minimum required:**

```bash
DB_URL=jdbc:postgresql://localhost:5432/webknot
DB_USER=postgres
DB_PASSWORD=your_password
JWT_SECRET=your_secret_key_here
```

**All options:** See [Environment Variables](./README.md#-environment-variables) in README

---

## 💡 Pro Tips

✨ **Tip 1**: Use Swagger UI for API testing - no curl needed!

✨ **Tip 2**: Check logs when something fails:
```bash
tail -f target/log/application.log
```

✨ **Tip 3**: Database schema is in `src/main/resources/sql/` - useful for reference

✨ **Tip 4**: Use `-DskipTests` flag for faster builds during development:
```bash
mvn clean package -DskipTests
```

✨ **Tip 5**: Set `logging.level.com.webknot.kpi=DEBUG` in application.properties for detailed logs

---

## 🎓 Learning Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [JWT Authentication](https://jwt.io/introduction)
- [REST API Best Practices](https://restfulapi.net/)

---

**Version**: 1.0  
**Last Updated**: March 2, 2026  
**Status**: Ready to Use

**Next**: Read the full [README.md](./README.md) for comprehensive documentation!
