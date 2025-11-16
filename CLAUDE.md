# Claude AI Context - HAPI FHIR Server

**Repository:** ddx-hapifhir-clinics  
**Version:** 1.0.0  
**Date:** November 16, 2025  
**Owner:** Dudoxx UG

---

## 🎯 Repository Purpose

**HAPI FHIR JPA Server** - Healthcare Interoperability standard FHIR R4 REST API with multi-tenant partitioning and PostgreSQL storage.

### Technology Stack

| Technology | Version | Purpose |
|-----------|---------|---------|
| HAPI FHIR | 8.4.0 | FHIR R4 server |
| Java | 17+ | Programming language |
| Spring Boot | Latest | Application framework |
| Maven | 3.8+ | Build tool |
| PostgreSQL | 12+ | Database |
| MinIO | Latest | Binary storage (S3-compatible) |

### Ports & Communication

- **Port:** 8080 (default), configurable
- **Database:** PostgreSQL port 5432 (`ddx_hapifhir`)
- **Storage:** MinIO port 9000
- **Called by:** NestJS Backend API (Port 4100)
- **NEVER called by:** Browser or Next.js frontend

---

## ⚠️ CRITICAL: Internal Service Only

### Security Model

**This FHIR server is INTERNAL ONLY**

```
❌ Browser → HAPI FHIR (FORBIDDEN)
❌ Next.js → HAPI FHIR (FORBIDDEN)
✅ NestJS → HAPI FHIR (ALLOWED)
```

### Authentication

All requests require:
1. **Bearer Token:** `Authorization: Bearer ddx-api-token-2024`
2. **Clinic ID:** `X-Clinic-ID: ddx-hamburg-clinic`

**Interceptors:**
- `ApiTokenAuthInterceptor` - Validates Bearer token
- `ClinicPartitionInterceptor` - Enforces partition isolation

---

## 📁 Project Structure

```
ddx-hapifhir-clinics/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── ca/uhn/fhir/jpa/starter/
│   │   │       ├── common/
│   │   │       │   └── StarterJpaConfig.java
│   │   │       ├── interceptor/
│   │   │       │   ├── ApiTokenAuthInterceptor.java
│   │   │       │   └── ClinicPartitionInterceptor.java
│   │   │       └── AppProperties.java
│   │   │
│   │   └── resources/
│   │       ├── application.yaml           # FHIR server configuration
│   │       ├── init-partitions.sql        # Partition initialization
│   │       └── logback.xml                # Logging config
│   │
│   └── test/
│       └── java/
│           └── ca/uhn/fhir/jpa/starter/
│
├── pom.xml                                # Maven dependencies
├── README.md                              # Documentation
├── DUDOXX_CUSTOMIZATIONS.md               # Custom modifications
├── UPSTREAM_SYNC.md                       # Syncing with HAPI upstream
├── CLAUDE.md                              # This file
└── AGENTS.md                              # AI agent configuration
```

---

## 🔧 Configuration

### application.yaml (Key Settings)

```yaml
# Server Configuration
server:
  port: 8080
  servlet:
    context-path: /

# Database Configuration
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ddx_hapifhir
    username: dudoxx_user
    password: admin
    driverClassName: org.postgresql.Driver
  jpa:
    properties:
      hibernate:
        dialect: ca.uhn.fhir.jpa.model.dialect.HapiFhirPostgresDialect

# HAPI FHIR Configuration
hapi:
  fhir:
    # Version and Base URL
    fhir_version: R4
    server_address: http://localhost:8080/fhir
    
    # Authentication
    auth:
      enabled: true
      api_token: ddx-api-token-2024
    
    # Multi-Tenancy (Partitioning)
    partitioning:
      enabled: true
      allow_references_across_partitions: true  # For system operations
      partitioning_include_in_search_hashes: true
      default_partition_id: 0
    
    # CORS
    cors:
      enabled: true
      allowed_origin:
        - "*"  # Change in production
    
    # Features
    allow_external_references: true
    allow_cascading_deletes: true
    subscription:
      resthook_enabled: true
```

---

## 🏗️ Multi-Tenancy Architecture

### Partition System

Each clinic has its own partition in PostgreSQL:

| Clinic ID | Partition | Name | Location |
|-----------|-----------|------|----------|
| `default` | 0 | System | Default partition |
| `ddx-hamburg-clinic` | 1 | Hamburg Clinic | Hamburg, Germany |
| `ddx-berlin-clinic` | 2 | Berlin Clinic | Berlin, Germany |
| `ddx-munich-clinic` | 3 | Munich Clinic | Munich, Germany |
| `ddx-frankfurt-clinic` | 4 | Frankfurt Clinic | Frankfurt, Germany |
| `ddx-cologne-clinic` | 5 | Cologne Clinic | Cologne, Germany |
| `ddx-shared-clinic` | 6 | Shared Resources | Cross-clinic |

### Partition Initialization

**File:** `src/main/resources/init-partitions.sql`

```sql
-- Run on first startup
INSERT INTO HFJ_PARTITION (PART_ID, PART_NAME, PART_DESC)
VALUES 
  (0, 'DEFAULT', 'Default System Partition'),
  (1, 'HAMBURG', 'Hamburg Clinic'),
  (2, 'BERLIN', 'Berlin Clinic'),
  (3, 'MUNICH', 'Munich Clinic'),
  (4, 'FRANKFURT', 'Frankfurt Clinic'),
  (5, 'COLOGNE', 'Cologne Clinic'),
  (6, 'SHARED', 'Shared Resources')
ON CONFLICT (PART_ID) DO NOTHING;
```

### ClinicPartitionInterceptor.java

```java
public class ClinicPartitionInterceptor {
    private static final Map<String, Integer> CLINIC_PARTITION_MAP = 
        Map.of(
            "ddx-hamburg-clinic", 1,
            "ddx-berlin-clinic", 2,
            "ddx-munich-clinic", 3,
            "ddx-frankfurt-clinic", 4,
            "ddx-cologne-clinic", 5,
            "ddx-shared-clinic", 6
        );
    
    @Hook(Pointcut.STORAGE_PARTITION_IDENTIFY_READ)
    public RequestPartitionId identifyRead(RequestDetails requestDetails) {
        String clinicId = requestDetails.getHeader("X-Clinic-ID");
        Integer partitionId = CLINIC_PARTITION_MAP.get(clinicId);
        return RequestPartitionId.fromPartitionId(partitionId);
    }
}
```

---

## 🚀 Development

### Building

```bash
# Clean and build
mvn clean package -DskipTests

# Build with tests
mvn clean package

# Quick rebuild
mvn package -DskipTests
```

### Running

```bash
# Run with Maven
mvn spring-boot:run

# Run JAR directly
java -jar target/ROOT.war

# With custom port
java -jar target/ROOT.war --server.port=8081
```

### Testing

```bash
# Test server health
curl http://localhost:8080/actuator/health

# Test FHIR metadata
curl http://localhost:8080/fhir/metadata

# Test with authentication
curl -H "Authorization: Bearer ddx-api-token-2024" \
     -H "X-Clinic-ID: ddx-hamburg-clinic" \
     http://localhost:8080/fhir/Patient
```

---

## 🎯 Common Tasks

### Task: Add New Partition

1. **Update init-partitions.sql:**
   ```sql
   INSERT INTO HFJ_PARTITION (PART_ID, PART_NAME, PART_DESC)
   VALUES (7, 'DUSSELDORF', 'Düsseldorf Clinic')
   ON CONFLICT DO NOTHING;
   ```

2. **Update ClinicPartitionInterceptor.java:**
   ```java
   CLINIC_PARTITION_MAP.put("ddx-dusseldorf-clinic", 7);
   ```

3. **Rebuild and restart:**
   ```bash
   mvn clean package -DskipTests
   ./stop-server.sh && ./start-server.sh
   ```

### Task: Modify Configuration

1. Edit `src/main/resources/application.yaml`
2. No rebuild needed (Spring Boot auto-reloads)
3. Restart server for changes to take effect

### Task: Add Custom Interceptor

1. Create Java class in `src/main/java/.../interceptor/`
2. Implement appropriate `@Hook` methods
3. Register in `StarterJpaConfig.java`
4. Rebuild and restart

### Task: Enable/Disable Feature

Edit `application.yaml`:

```yaml
hapi:
  fhir:
    # Example: Enable validation
    validation:
      requests_enabled: true
      responses_enabled: true
    
    # Example: Enable full-text search
    lasn:
      enabled: true
```

---

## 🐛 Troubleshooting

### Issue: HAPI-1220 Partition Error

**Error:** "This server is not configured to support search against all partitions"

**Solution:** Already fixed. Check `application.yaml`:
```yaml
hapi:
  fhir:
    partitioning:
      allow_references_across_partitions: true
```

See: `docs/troubleshooting/OFFICIAL_PARTITION_FIX.md`

### Issue: Port 8080 Already in Use

```bash
# Find process
lsof -i :8080

# Kill process
kill -9 <PID>

# Or use different port
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

### Issue: Database Connection Failed

```bash
# Test PostgreSQL
psql -U dudoxx_user -h localhost -p 5432 -d ddx_hapifhir

# Check credentials in application.yaml
# Verify PostgreSQL is running
pg_isready -h localhost -p 5432
```

### Issue: Partition Not Found

```bash
# Check partitions exist
psql -U dudoxx_user -d ddx_hapifhir -c "SELECT * FROM HFJ_PARTITION;"

# Re-run initialization
psql -U dudoxx_user -d ddx_hapifhir -f src/main/resources/init-partitions.sql
```

---

## 📊 Database Schema

### Key Tables

- `HFJ_RESOURCE` - FHIR resources
- `HFJ_RES_VER` - Resource versions
- `HFJ_PARTITION` - Partition definitions
- `HFJ_RES_LINK` - Resource references
- `HFJ_SPIDX_*` - Search parameter indexes

### Viewing Tables

```bash
psql -U dudoxx_user -d ddx_hapifhir -c "\dt"
```

### Table Statistics

```bash
psql -U dudoxx_user -d ddx_hapifhir -c "
SELECT 
  schemaname, 
  tablename, 
  n_tup_ins as inserts,
  n_tup_upd as updates,
  n_tup_del as deletes
FROM pg_stat_user_tables
ORDER BY n_tup_ins DESC
LIMIT 10;
"
```

---

## 🔐 Security Considerations

### Production Checklist

- [ ] Change `hapi.fhir.auth.api_token` from default
- [ ] Restrict CORS origins in `application.yaml`
- [ ] Set proper `server_address` (production URL)
- [ ] Enable PostgreSQL SSL/TLS
- [ ] Use strong database password
- [ ] Enable request/response validation
- [ ] Configure rate limiting
- [ ] Set up monitoring and logging

### CORS Configuration (Production)

```yaml
hapi:
  fhir:
    cors:
      allowed_origin:
        - "https://api.dudoxx.com"  # NestJS backend only
      allowed_methods:
        - GET
        - POST
        - PUT
        - DELETE
        - PATCH
```

---

## 📚 Related Documentation

### Internal
- `README.md` - General documentation
- `DUDOXX_CUSTOMIZATIONS.md` - Custom modifications
- `UPSTREAM_SYNC.md` - Syncing with HAPI upstream
- `../ARCHITECTURE.md` - Global architecture
- `../docs/guides/MULTI_TENANCY_GUIDE.md` - Multi-tenancy setup

### External
- [HAPI FHIR Docs](https://hapifhir.io/hapi-fhir/docs/)
- [FHIR R4 Spec](https://hl7.org/fhir/R4/)
- [PostgreSQL Docs](https://www.postgresql.org/docs/)

### API Documentation
- Swagger UI: http://localhost:8080/fhir/swagger-ui/
- OpenAPI JSON: http://localhost:8080/fhir/api-docs

---

**Maintained by:** Dudoxx UG  
**Last Updated:** November 16, 2025
