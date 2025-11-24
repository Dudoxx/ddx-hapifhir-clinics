# AI Agent Configuration - HAPI FHIR Server

**Project-specific AI agent configuration for Dudoxx HAPI FHIR Server**

**Version:** 2.0.0  
**Last Updated:** November 24, 2025 (Added Gemini 3 Pro Support)

---

## 🤖 Supported AI Agents

This repository provides optimized configuration files for multiple AI coding assistants:

| Agent | Configuration File | Optimizations | Status |
|-------|-------------------|---------------|--------|
| **Claude (Anthropic)** | `CLAUDE.md` | Java/Spring Boot patterns, FHIR spec knowledge | ✅ Primary |
| **Gemini 3 Pro (Google)** | `GEMINI.md` | Strict constraints, interceptor protection | ✅ **NEW** |
| **OpenCode** | Uses `CLAUDE.md` | Standard patterns | ✅ Compatible |
| **Cursor AI** | Uses `CLAUDE.md` or `GEMINI.md` | Select based on model | ✅ Compatible |

---

## 📁 Project Context

**Repository:** dudoxx-fhir-server  
**Type:** HAPI FHIR JPA Server (Java/Spring Boot)  
**Purpose:** Healthcare Interoperability standard FHIR R4 REST API with multi-tenant partitioning  
**Version:** 1.1.0

---

## 🎯 Technology Stack

- **Framework:** HAPI FHIR 8.4.0
- **Language:** Java 17+
- **Application:** Spring Boot (latest)
- **Build Tool:** Maven 3.8+
- **Database:** PostgreSQL 12+ (`ddx_hapifhir`)
- **Storage:** MinIO (S3-compatible)
- **Port:** 8080 (default)

---

## 🔧 Agent Selection Guide

### When to Use Claude (CLAUDE.md)

**Best for:**
- ✅ Custom interceptor development
- ✅ FHIR resource provider modifications
- ✅ Spring Boot configuration changes
- ✅ Database schema updates
- ✅ Complex multi-partition scenarios

**Strengths:**
- Strong Java/Spring Boot knowledge
- Excellent FHIR R4 specification understanding
- Good at interceptor patterns
- Handles complex partition logic

### When to Use Gemini 3 Pro (GEMINI.md)

**Best for:**
- ✅ Configuration updates (application.yaml)
- ✅ Adding new partitions
- ✅ Simple interceptor modifications
- ✅ Documentation updates
- ✅ SQL script updates

**Strengths:**
- Fast execution on configuration tasks
- Good at following established patterns

**Constraints (addressed in GEMINI.md):**
- ⚠️ Investigation mode enforced
- ⚠️ Explicit confirmation required before edits
- ⚠️ File size limits enforced (300 lines)
- ⚠️ Anti-hallucination verification
- ⚠️ Interceptor protection (cannot remove auth/partition interceptors)
- ⚠️ Partition isolation preservation mandatory

---

## 🚨 Critical Rules for ALL Agents

### 1. Security Model (INTERNAL SERVICE ONLY)

```
❌ Browser → HAPI FHIR (FORBIDDEN)
❌ Next.js → HAPI FHIR (FORBIDDEN)
✅ NestJS → HAPI FHIR (ALLOWED)
```

**ALL requests MUST include:**
1. `Authorization: Bearer ddx-api-token-2024`
2. `X-Clinic-ID: ddx-hamburg-clinic`

```bash
# ✅ CORRECT
curl -H "Authorization: Bearer ddx-api-token-2024" \
     -H "X-Clinic-ID: ddx-hamburg-clinic" \
     http://localhost:8080/fhir/Patient

# ❌ FORBIDDEN
curl http://localhost:8080/fhir/Patient  # Unauthorized
```

### 2. Interceptors (NEVER REMOVE)

```java
// ✅ CRITICAL: ApiTokenAuthInterceptor - validates Bearer token
@Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_PROCESSED)
public boolean authenticateRequest(RequestDetails requestDetails) {
    // ✅ NEVER REMOVE THIS INTERCEPTOR
}

// ✅ CRITICAL: ClinicPartitionInterceptor - enforces multi-tenancy
@Hook(Pointcut.STORAGE_PARTITION_IDENTIFY_READ)
public RequestPartitionId identifyRead(RequestDetails requestDetails) {
    String clinicId = requestDetails.getHeader("X-Clinic-ID");
    // ✅ NEVER REMOVE THIS INTERCEPTOR
}
```

### 3. Multi-Tenancy (ALWAYS PRESERVE)

| Clinic ID | Partition | Isolation |
|-----------|-----------|-----------|
| `ddx-hamburg-clinic` | 1 | Hamburg data only |
| `ddx-berlin-clinic` | 2 | Berlin data only |
| `ddx-munich-clinic` | 3 | Munich data only |
| `ddx-frankfurt-clinic` | 4 | Frankfurt data only |
| `ddx-cologne-clinic` | 5 | Cologne data only |
| `ddx-shared-clinic` | 6 | Shared resources |

```yaml
# ✅ CORRECT: application.yaml
hapi:
  fhir:
    partitioning:
      enabled: true
      allow_references_across_partitions: true  # ✅ CRITICAL
      default_partition_id: 0

# ❌ FORBIDDEN
hapi:
  fhir:
    partitioning:
      enabled: false  # ❌ Breaks multi-tenancy
```

### 4. Partition Initialization

**File:** `src/main/resources/init-partitions.sql`

```sql
-- ✅ Run on first startup
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

---

## 📋 Key Features to Maintain

1. **Authentication** - Bearer token validation (ApiTokenAuthInterceptor)
2. **Multi-Tenancy** - Partition-based data isolation
3. **FHIR R4 Compliance** - Full FHIR specification support
4. **Cross-Partition References** - Allowed for system operations
5. **PostgreSQL Backend** - JPA persistence with `ddx_hapifhir` database
6. **MinIO Storage** - Binary resource storage
7. **CORS Configuration** - Controlled origins
8. **Interceptor System** - Custom hooks for auth, partitioning, logging

---

## 🎨 Code Style Guidelines

### Java Conventions

```java
// ✅ CORRECT: Java 17, proper formatting, null checks
public class MyInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(MyInterceptor.class);
    private final SomeService service;  // ✅ final
    
    public MyInterceptor(SomeService service) {  // ✅ Constructor injection
        this.service = service;
    }
    
    @Hook(Pointcut.STORAGE_PARTITION_IDENTIFY_READ)
    public RequestPartitionId identifyPartition(RequestDetails requestDetails) {
        String clinicId = requestDetails.getHeader("X-Clinic-ID");
        
        if (clinicId == null) {
            throw new ForbiddenOperationException("X-Clinic-ID required");
        }
        
        Integer partitionId = CLINIC_PARTITION_MAP.get(clinicId);
        
        if (partitionId == null) {
            throw new ResourceNotFoundException("Unknown clinic");
        }
        
        return RequestPartitionId.fromPartitionId(partitionId);
    }
}

// ❌ WRONG: Missing null checks, not final
public class MyInterceptor {
    private SomeService service;  // ❌ Not final
    
    public RequestPartitionId identifyPartition(RequestDetails req) {
        return RequestPartitionId.fromPartitionId(
            CLINIC_PARTITION_MAP.get(req.getHeader("X-Clinic-ID"))  // ❌ No null check
        );
    }
}
```

### File Organization

```
src/main/java/ca/uhn/fhir/jpa/starter/
  ├── common/
  │   └── StarterJpaConfig.java
  ├── interceptor/
  │   ├── ApiTokenAuthInterceptor.java
  │   └── ClinicPartitionInterceptor.java
  └── AppProperties.java

src/main/resources/
  ├── application.yaml
  ├── init-partitions.sql
  └── logback.xml
```

---

## 🧪 Testing & Validation

### Build Commands

```bash
# Clean build (skip tests)
mvn clean package -DskipTests

# Full build with tests
mvn clean package

# Run server
mvn spring-boot:run

# Run JAR
java -jar target/ROOT.war
```

### Verification Tests

```bash
# 1. Health check
curl http://localhost:8080/actuator/health

# 2. FHIR metadata
curl http://localhost:8080/fhir/metadata

# 3. Auth test (should fail without token)
curl http://localhost:8080/fhir/Patient
# Expected: 401 Unauthorized

# 4. Auth test (with token)
curl -H "Authorization: Bearer ddx-api-token-2024" \
     -H "X-Clinic-ID: ddx-hamburg-clinic" \
     http://localhost:8080/fhir/Patient

# 5. Partition isolation test
curl -H "Authorization: Bearer ddx-api-token-2024" \
     -H "X-Clinic-ID: ddx-berlin-clinic" \
     http://localhost:8080/fhir/Patient
# Should return Berlin data only
```

---

## 📚 Documentation Structure

### Agent-Specific Files

| File | Purpose | Target Agent |
|------|---------|--------------|
| `CLAUDE.md` | Comprehensive FHIR server context | Claude, OpenCode |
| `GEMINI.md` | **NEW** - Strict FHIR server constraints | Gemini 3 Pro |
| `AGENTS.md` | **This file** - Agent selection | All agents |
| `README.md` | General documentation | Humans |
| `DUDOXX_CUSTOMIZATIONS.md` | Custom modifications | Humans |

### Critical Documentation

| File | Purpose |
|------|---------|
| `UPSTREAM_SYNC.md` | Syncing with HAPI upstream |
| `../docs/guides/MULTI_TENANCY_GUIDE.md` | Multi-tenancy setup |
| `../docs/troubleshooting/OFFICIAL_PARTITION_FIX.md` | HAPI-1220 fix |
| `../IMPORTANT_PATHS_FILES.md` | ⭐ **READ FIRST** |

---

## 🔄 Workflow by Agent Type

### Claude Workflow

1. Read `../IMPORTANT_PATHS_FILES.md`
2. Read `../ARCHITECTURE.md`
3. Read `../CLAUDE.md`
4. Read `CLAUDE.md` (this repo)
5. Execute task
6. Rebuild: `mvn clean package -DskipTests`
7. Test with curl commands

### Gemini Workflow

1. **MANDATORY**: Confirm reading docs
2. Read all documentation in order
3. Read `GEMINI.md` - **Strict constraints**
4. **Request confirmation** before modifying
5. Execute ONLY after approval
6. **Provide test commands** (curl, SQL)
7. **Do NOT claim "server configured"** without proof
8. Wait for test results

---

## 🚨 Agent-Specific Constraints

### For Gemini 3 Pro (See GEMINI.md for details)

**Investigation Mode:**
- "Investigate partition error" → NO CODE CHANGES
- Provide analysis only
- Wait for "fix" to proceed

**File Size Limits:**
- Java classes: 300 lines MAX
- Check `wc -l <file>` before reading

**Interceptor Protection:**
- NEVER remove `ApiTokenAuthInterceptor`
- NEVER remove `ClinicPartitionInterceptor`
- NEVER disable partitioning in application.yaml

**Partition Preservation:**
- ALWAYS maintain partition isolation
- NEVER allow cross-partition data leaks
- Verify `allow_references_across_partitions: true`

**Anti-Hallucination:**
- Provide curl test command
- State "Test required: [curl command]"
- Never claim "working" without curl proof

---

## 🎯 Common Tasks

### Task: Add New Partition

**Both agents:** Follow exact steps

```sql
-- 1. Update init-partitions.sql
INSERT INTO HFJ_PARTITION (PART_ID, PART_NAME, PART_DESC)
VALUES (7, 'DUSSELDORF', 'Düsseldorf Clinic')
ON CONFLICT DO NOTHING;
```

```java
// 2. Update ClinicPartitionInterceptor.java
private static final Map<String, Integer> CLINIC_PARTITION_MAP = Map.of(
    "ddx-hamburg-clinic", 1,
    "ddx-berlin-clinic", 2,
    "ddx-dusseldorf-clinic", 7  // ✅ Add new
);
```

```bash
# 3. Run SQL
psql -U dudoxx_user -d ddx_hapifhir -f src/main/resources/init-partitions.sql

# 4. Rebuild
mvn clean package -DskipTests

# 5. Test
curl -H "Authorization: Bearer ddx-api-token-2024" \
     -H "X-Clinic-ID: ddx-dusseldorf-clinic" \
     http://localhost:8080/fhir/Patient
```

### Task: Modify Configuration

**Gemini:** Simpler config changes  
**Claude:** Complex configuration scenarios

```yaml
# Edit src/main/resources/application.yaml
hapi:
  fhir:
    validation:
      requests_enabled: true  # ✅ Enable validation
      responses_enabled: true
```

```bash
# No rebuild needed (Spring Boot auto-reloads)
# Restart server
mvn spring-boot:run
```

---

## ✅ Quality Gates

**ALL agents must ensure:**

1. ✅ Build succeeds (`mvn clean package`)
2. ✅ Server starts (`mvn spring-boot:run`)
3. ✅ Metadata accessible (`curl .../fhir/metadata`)
4. ✅ Auth enforced (401 without token)
5. ✅ Partition isolation preserved
6. ✅ Interceptors intact (auth + partition)
7. ✅ File size within limits (300 lines)
8. ✅ SQL runs without errors
9. ✅ HAPI-1220 error prevented
10. ✅ Manual curl test performed

---

## 🔗 Integration Points

| Service | Port | Purpose |
|---------|------|---------|
| PostgreSQL | 5432 | `ddx_hapifhir` database |
| MinIO | 9000 | Binary resource storage |
| NestJS Backend | 4100 | API consumer (ONLY caller) |
| HAPI FHIR | 8080 | **This service** |

---

## 🐛 Common Issues

### HAPI-1220 Partition Error

**Fixed** - Ensure in `application.yaml`:
```yaml
hapi.fhir.partitioning.allow_references_across_partitions: true
```

### Port 8080 in Use

```bash
lsof -i :8080
kill -9 <PID>
```

### Database Connection Failed

```bash
psql -U dudoxx_user -h localhost -p 5432 -d ddx_hapifhir
```

### Partition Not Found

```bash
psql -U dudoxx_user -d ddx_hapifhir -c "SELECT * FROM HFJ_PARTITION;"
```

---

## 📞 Getting Help

- **Claude users:** See `CLAUDE.md` for comprehensive Java/FHIR context
- **Gemini users:** See `GEMINI.md` for strict constraints and interceptor protection
- **All agents:** Read `../IMPORTANT_PATHS_FILES.md` FIRST
- **Swagger:** http://localhost:8080/fhir/swagger-ui/
- **HAPI Docs:** https://hapifhir.io/hapi-fhir/docs/
- **FHIR R4:** https://hl7.org/fhir/R4/

---

**Last Updated:** November 24, 2025  
**Maintained by:** Dudoxx UG

**Recent Changes:**
- Added Gemini 3 Pro support with GEMINI.md
- Enhanced interceptor protection rules
- Added partition isolation enforcement
- Clarified security model
- Added anti-hallucination protocols for FHIR server
