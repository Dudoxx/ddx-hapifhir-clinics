# Dudoxx Customizations

This document details all custom modifications made to the upstream HAPI FHIR JPA Server Starter.

## Overview

**Upstream Repository**: https://github.com/hapifhir/hapi-fhir-jpaserver-starter  
**Dudoxx Repository**: https://github.com/Dudoxx/ddx-hapifhir-clinics  
**Maintainer**: Dudoxx UG  
**Version**: 1.0.0  
**Date**: November 15, 2025

---

## Custom Files Added

### 1. Interceptors

#### ClinicPartitionInterceptor.java
**Path**: `src/main/java/ca/uhn/fhir/jpa/starter/interceptor/ClinicPartitionInterceptor.java`

**Purpose**: Multi-tenant partition routing based on clinic identification

**Features**:
- Intercepts all FHIR requests (READ, CREATE, UPDATE, DELETE)
- Extracts clinic ID from `X-Clinic-ID` header
- Maps clinic IDs to partition IDs
- Enforces strict data isolation between clinics
- Throws `AuthenticationException` for missing/invalid clinic IDs

**Clinic Mapping**:
```java
ddx-hamburg-clinic  → Partition 1
ddx-berlin-clinic   → Partition 2
ddx-munich-clinic   → Partition 3
ddx-frankfurt-clinic → Partition 4
ddx-cologne-clinic  → Partition 5
default             → Partition 0 (system)
```

#### ApiTokenAuthInterceptor.java
**Path**: `src/main/java/ca/uhn/fhir/jpa/starter/interceptor/ApiTokenAuthInterceptor.java`

**Purpose**: Bearer token authentication

**Features**:
- Validates `Authorization: Bearer <token>` headers
- Configurable via `application.yaml`
- Public endpoint whitelist (metadata, health, OAuth)
- Rejects unauthorized requests with HTTP 401

**Configuration**:
```yaml
hapi.fhir.auth.enabled: true
hapi.fhir.auth.api_token: ddx-api-token-2024
```

### 2. Database Initialization

#### init-partitions.sql
**Path**: `src/main/resources/init-partitions.sql`

**Purpose**: Initialize clinic partitions in PostgreSQL

**Contents**:
- Creates 6 partitions in `HFJ_PARTITION` table
- Idempotent with `ON CONFLICT DO NOTHING`
- Maps partition IDs to clinic names

---

## Modified Files

### 1. StarterJpaConfig.java
**Path**: `src/main/java/ca/uhn/fhir/jpa/starter/common/StarterJpaConfig.java`

**Changes**:
- Added dependency injection for `ApiTokenAuthInterceptor`
- Added dependency injection for `ClinicPartitionInterceptor`
- Registered both interceptors with FHIR server

**Code Added** (lines ~337-343):
```java
Optional<ApiTokenAuthInterceptor> apiTokenAuthInterceptor,
Optional<ClinicPartitionInterceptor> clinicPartitionInterceptor

// Register API Token Authentication Interceptor if present
apiTokenAuthInterceptor.ifPresent(fhirServer::registerInterceptor);

// Register Clinic Partition Interceptor for multi-tenancy if present
clinicPartitionInterceptor.ifPresent(fhirServer::registerInterceptor);
```

### 2. application.yaml
**Path**: `src/main/resources/application.yaml`

**Major Changes**:

#### A. Database Configuration
**Before**: H2 in-memory database
```yaml
datasource:
  url: jdbc:h2:mem:test_mem
  username: sa
  driver-class-name: org.h2.Driver
```

**After**: PostgreSQL
```yaml
datasource:
  url: jdbc:postgresql://localhost:5432/ddx_hapifhir
  username: dudoxx_user
  password: admin
  driver-class-name: org.postgresql.Driver
  hikari:
    maximum-pool-size: 10
jpa:
  properties:
    hibernate:
      dialect: ca.uhn.fhir.jpa.model.dialect.HapiFhirPostgresDialect
```

#### B. Authentication (New Section)
```yaml
hapi:
  fhir:
    auth:
      enabled: true
      api_token: ddx-api-token-2024
```

#### C. Multi-Tenancy/Partitioning (New Section)
```yaml
hapi:
  fhir:
    partitioning:
      enabled: true
      request_tenant_partitioning_mode: false
      allow_references_across_partitions: false
      partitioning_include_in_search_hashes: true
      conditional_create_duplicate_identifiers_enabled: true
      default_partition_id: 0
```

#### D. Feature Toggles
**Enabled**:
- Bulk export/import
- Cascading deletes
- External references
- Multiple delete
- REST Hook subscriptions
- Binary storage (inline below 4KB)
- Narrative generation

**Disabled**:
- Validation (can be enabled per environment)
- MDM (Master Data Management)
- Lucene/Elasticsearch search

#### E. Cleanup
- Removed ~150 lines of comments
- Streamlined configuration sections
- Removed unused MCP/CQL detailed settings

---

## Architecture Overview

### Request Flow

```
Client Request
    │
    ├─→ ApiTokenAuthInterceptor
    │   ├─ Validates Bearer token
    │   ├─ Checks public endpoint whitelist
    │   └─ Throws 401 if unauthorized
    │
    ├─→ ClinicPartitionInterceptor
    │   ├─ Extracts X-Clinic-ID header
    │   ├─ Maps clinic ID to partition ID
    │   ├─ Throws 401 if clinic ID missing/invalid
    │   └─ Sets RequestPartitionId
    │
    ├─→ HAPI FHIR Core
    │   └─ Processes request in assigned partition
    │
    └─→ PostgreSQL Database
        └─ Data stored/retrieved from partition
```

### Database Schema

```
PostgreSQL: ddx_hapifhir
│
├─ HFJ_PARTITION (Partition definitions)
│  ├─ PART_ID=0, PART_NAME=DEFAULT
│  ├─ PART_ID=1, PART_NAME=HAMBURG
│  ├─ PART_ID=2, PART_NAME=BERLIN
│  ├─ PART_ID=3, PART_NAME=MUNICH
│  ├─ PART_ID=4, PART_NAME=FRANKFURT
│  └─ PART_ID=5, PART_NAME=COLOGNE
│
├─ HFJ_RESOURCE (All FHIR resources)
│  └─ PARTITION_ID → references HFJ_PARTITION
│
├─ HFJ_RES_VER (Resource versions)
│  └─ PARTITION_ID → references HFJ_PARTITION
│
└─ ... (all other HAPI FHIR tables with PARTITION_ID)
```

---

## Configuration Requirements

### Environment Variables

**Development**:
```bash
export FHIR_BASE_URL=http://localhost:8080/fhir
export FHIR_API_TOKEN=ddx-api-token-2024
```

**Production**:
```bash
export FHIR_BASE_URL=https://fhir.dudoxx.com/fhir
export FHIR_API_TOKEN=<strong-token-from-secrets-manager>
export DB_URL=jdbc:postgresql://<db-host>:5432/ddx_hapifhir
export DB_USERNAME=<db-user>
export DB_PASSWORD=<db-password-from-secrets-manager>
```

### Database Setup

```bash
# Create database
createdb -U postgres ddx_hapifhir

# Create user
psql -U postgres -c "CREATE USER dudoxx_user WITH PASSWORD 'admin';"

# Grant privileges
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE ddx_hapifhir TO dudoxx_user;"

# Initialize partitions
psql -U dudoxx_user -d ddx_hapifhir -f src/main/resources/init-partitions.sql
```

---

## Security Considerations

### Current Implementation
- Simple API token authentication
- Token stored in `application.yaml`
- Single token for all clinics

### Production Recommendations
1. **OAuth 2.0 / OIDC**
   - Integrate with Keycloak, Auth0, or Azure AD
   - Token-to-clinic binding
   - Role-based access control (RBAC)

2. **Token Management**
   - Externalize tokens to secrets manager (AWS Secrets Manager, Azure Key Vault)
   - Implement token rotation
   - Add token expiration

3. **Audit Logging**
   - Log all access per partition
   - Track authentication attempts
   - Monitor for suspicious activity

4. **Network Security**
   - Enable SSL/TLS
   - Configure firewall rules
   - Use VPC/private subnets for database

---

## Testing

### Multi-Tenancy Tests

**Script**: `test-multi-tenancy.sh` (parent directory)

**Validates**:
- Data isolation between clinics
- Authentication with API tokens
- X-Clinic-ID header routing
- Cross-partition access prevention

**Usage**:
```bash
cd /path/to/dudoxx-hapifihr
./test-multi-tenancy.sh
```

### Manual Testing

```bash
# Create patient in Hamburg
curl -X POST http://localhost:8080/fhir/Patient \
  -H "Authorization: Bearer ddx-api-token-2024" \
  -H "X-Clinic-ID: ddx-hamburg-clinic" \
  -H "Content-Type: application/fhir+json" \
  -d '{"resourceType":"Patient","name":[{"family":"Test"}]}'

# Search Hamburg (should only see Hamburg patients)
curl -H "Authorization: Bearer ddx-api-token-2024" \
     -H "X-Clinic-ID: ddx-hamburg-clinic" \
     http://localhost:8080/fhir/Patient

# Search Berlin (should NOT see Hamburg patients)
curl -H "Authorization: Bearer ddx-api-token-2024" \
     -H "X-Clinic-ID: ddx-berlin-clinic" \
     http://localhost:8080/fhir/Patient
```

---

## Maintenance & Upgrades

### Syncing with Upstream

```bash
# Add upstream remote (if not already added)
git remote add upstream https://github.com/hapifhir/hapi-fhir-jpaserver-starter.git

# Fetch upstream changes
git fetch upstream

# Review changes
git log HEAD..upstream/master --oneline

# Merge upstream changes (carefully resolve conflicts)
git merge upstream/master

# Test thoroughly after merge
mvn clean test
./test-multi-tenancy.sh
```

### Conflict Resolution Areas

When merging upstream changes, pay special attention to:
1. `StarterJpaConfig.java` - Interceptor registration
2. `application.yaml` - Configuration structure
3. `pom.xml` - Dependency versions

### Version Tracking

| Component | Version | Last Updated |
|-----------|---------|--------------|
| HAPI FHIR | 8.4.0 | 2025-11-15 |
| Spring Boot | 3.x | Managed by HAPI |
| PostgreSQL Dialect | HapiFhirPostgresDialect | - |
| Dudoxx Customizations | 1.0.0 | 2025-11-15 |

---

## Support & Documentation

### Dudoxx Documentation
- Main README: [README.md](README.md)
- Quick Start: [../QUICKSTART.md](../QUICKSTART.md)
- Multi-Tenancy Guide: [../MULTI_TENANCY_GUIDE.md](../MULTI_TENANCY_GUIDE.md)
- Production Setup: [../PRODUCTION_SETUP.md](../PRODUCTION_SETUP.md)

### Upstream Documentation
- HAPI FHIR Docs: https://hapifhir.io/
- GitHub: https://github.com/hapifhir/hapi-fhir-jpaserver-starter
- Community: https://chat.fhir.org/

---

## License

This project inherits the **Apache License 2.0** from the upstream HAPI FHIR project.

All Dudoxx customizations are also released under Apache License 2.0.

---

**Maintained by**: Dudoxx UG  
**Last Updated**: November 15, 2025  
**Version**: 1.0.0
