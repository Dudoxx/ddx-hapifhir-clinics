<p align="center">
  <img src="assets/branding/toucan_transparent.png" alt="Dudoxx Logo" width="120" />
</p>

<h1 align="center">Dudoxx HAPI FHIR Multi-Clinic Server</h1>

<p align="center">
  <strong>Port:</strong> 8080 | <strong>FHIR R4</strong> | <strong>Multi-Tenant</strong>
</p>

<p align="center">
  A production-ready FHIR R4 server with multi-tenant clinic isolation, authentication, and PostgreSQL backend.
</p>

---

## 🏢 About This Fork

This is a **Dudoxx-customized fork** of the official [HAPI FHIR JPA Server Starter](https://github.com/hapifhir/hapi-fhir-jpaserver-starter), enhanced with enterprise-grade features for multi-clinic healthcare environments.

### 🎯 Key Dudoxx Enhancements

- ✅ **Multi-Tenancy** - Complete data isolation between clinics using partition-based architecture
- ✅ **Authentication** - API token-based authentication with Bearer tokens
- ✅ **PostgreSQL Backend** - Production-ready database with optimized Hibernate dialect
- ✅ **Clinic Management** - Support for multiple clinics (Hamburg, Berlin, Munich, Frankfurt, Cologne)
- ✅ **TypeScript Client** - Full-featured REST client with type-safe operations
- ✅ **Security Interceptors** - Custom interceptors for authentication and partition routing

### 📚 Upstream Credits & Synchronization

This project builds upon the excellent work of the HAPI FHIR team:

- **Upstream Project**: [HAPI FHIR JPA Server Starter](https://github.com/hapifhir/hapi-fhir-jpaserver-starter)
- **Main HAPI FHIR Library**: [hapifhir/hapi-fhir](https://github.com/hapifhir/hapi-fhir)
- **Documentation**: [hapifhir.io](https://hapifhir.io/)
- **License**: Apache 2.0

**📝 Keeping Up-to-Date**: See [UPSTREAM_SYNC.md](./UPSTREAM_SYNC.md) for detailed instructions on syncing with the official HAPI FHIR repository to receive updates, security patches, and new features.

**Thank you to the HAPI FHIR team and contributors for maintaining this exceptional FHIR implementation!**

---

## 🚀 Quick Start

### Prerequisites

- **Java 17+** (JDK)
- **Maven 3.8+**
- **PostgreSQL 12+**
- **Node.js 18+** (for TypeScript client)

### 1. Database Setup

```bash
# Create PostgreSQL database
psql -U postgres -c "CREATE DATABASE ddx_hapifhir;"
psql -U postgres -c "CREATE USER dudoxx_user WITH PASSWORD 'admin';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE ddx_hapifhir TO dudoxx_user;"

# Initialize partitions
psql -U dudoxx_user -d ddx_hapifhir -f src/main/resources/init-partitions.sql
```

### 2. Start the Server

```bash
# Navigate to project root (dudoxx-hapifihr/)
cd ..

# Start server (from parent directory)
./start-server.sh
```

The server will be available at:
- **FHIR Base URL**: http://localhost:8080/fhir
- **Swagger UI**: http://localhost:8080/fhir/swagger-ui/
- **API Docs**: http://localhost:8080/fhir/api-docs

### 3. Test Multi-Tenancy

```bash
# Test multi-clinic isolation
./test-multi-tenancy.sh
```

---

## 📖 Documentation

### Core Documentation
- [Quick Start Guide](../QUICKSTART.md)
- [Multi-Tenancy Guide](../MULTI_TENANCY_GUIDE.md)
- [Authentication Setup](../AUTHENTICATION_STATUS.md)
- [Production Deployment](../PRODUCTION_SETUP.md)
- [Development Setup](../DEV_SETUP_GUIDE.md)
- **[Upstream Sync Guide](./UPSTREAM_SYNC.md)** - Keep your fork updated with official HAPI FHIR releases

### TypeScript Client
- [Client Documentation](../dudoxx-fhir-sdk-ts/README.md)
- [API Reference](../dudoxx-fhir-sdk-ts/REFERENCE_GUIDE.md)
- [Usage Examples](../dudoxx-fhir-sdk-ts/EXAMPLES_SUMMARY.md)

---

## 🏗️ Architecture

### Multi-Tenancy Design

```
┌─────────────────────────────────────────────────────────────┐
│                    HAPI FHIR Server                          │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  ApiTokenAuthInterceptor                             │  │
│  │  - Validates Bearer tokens                           │  │
│  │  - Public endpoint whitelist                         │  │
│  └──────────────────────────────────────────────────────┘  │
│                          │                                   │
│                          ▼                                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  ClinicPartitionInterceptor                          │  │
│  │  - Reads X-Clinic-ID header                          │  │
│  │  - Maps clinic ID → Partition ID                     │  │
│  │  - Enforces data isolation                           │  │
│  └──────────────────────────────────────────────────────┘  │
│                          │                                   │
│                          ▼                                   │
│  ┌─────────┬─────────┬─────────┬──────────┬──────────┐     │
│  │ Part 0  │ Part 1  │ Part 2  │ Part 3   │ Part 4   │     │
│  │ DEFAULT │ HAMBURG │ BERLIN  │ MUNICH   │ FRANKFURT│     │
│  └─────────┴─────────┴─────────┴──────────┴──────────┘     │
│                                                              │
│                          ▼                                   │
│                  PostgreSQL Database                         │
└─────────────────────────────────────────────────────────────┘
```

### Clinic Mapping

| Clinic ID | Partition ID | Partition Name | Location |
|-----------|--------------|----------------|----------|
| `ddx-hamburg-clinic` | 1 | HAMBURG | Hamburg, Germany |
| `ddx-berlin-clinic` | 2 | BERLIN | Berlin, Germany |
| `ddx-munich-clinic` | 3 | MUNICH | Munich, Germany |
| `ddx-frankfurt-clinic` | 4 | FRANKFURT | Frankfurt, Germany |
| `ddx-cologne-clinic` | 5 | COLOGNE | Cologne, Germany |
| `default` | 0 | DEFAULT | System partition |

---

## 🔧 Configuration

### Authentication

**File**: `src/main/resources/application.yaml`

```yaml
hapi:
  fhir:
    auth:
      enabled: true
      api_token: ddx-api-token-2024
```

### Multi-Tenancy

```yaml
hapi:
  fhir:
    partitioning:
      enabled: true
      allow_references_across_partitions: false
      partitioning_include_in_search_hashes: true
      default_partition_id: 0
```

### Database

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ddx_hapifhir
    username: dudoxx_user
    password: admin
    driver-class-name: org.postgresql.Driver
  jpa:
    properties:
      hibernate:
        dialect: ca.uhn.fhir.jpa.model.dialect.HapiFhirPostgresDialect
```

---

## 🔐 API Usage

### Authentication

All requests require authentication:

```bash
Authorization: Bearer ddx-api-token-2024
X-Clinic-ID: ddx-hamburg-clinic
```

### Example Requests

#### Create Patient in Hamburg Clinic

```bash
curl -X POST http://localhost:8080/fhir/Patient \
  -H "Authorization: Bearer ddx-api-token-2024" \
  -H "X-Clinic-ID: ddx-hamburg-clinic" \
  -H "Content-Type: application/fhir+json" \
  -d '{
    "resourceType": "Patient",
    "name": [{"family": "Schmidt", "given": ["Hans"]}],
    "gender": "male",
    "birthDate": "1980-05-15"
  }'
```

#### Search Patients (Hamburg Only)

```bash
curl -H "Authorization: Bearer ddx-api-token-2024" \
     -H "X-Clinic-ID: ddx-hamburg-clinic" \
     http://localhost:8080/fhir/Patient
```

---

## 🛠️ Development

### Build from Source

```bash
cd dudoxx-fhir-server
mvn clean package -DskipTests
```

### Run Tests

```bash
mvn test
```

### Run with Maven

```bash
mvn spring-boot:run
```

### Docker Build

```bash
./build-docker-image.sh
docker run -p 8080:8080 hapi-fhir/hapi-fhir-jpaserver-starter:latest
```

---

## 📦 What's Included

### Custom Components

#### 1. **ClinicPartitionInterceptor.java**
- **Path**: `src/main/java/ca/uhn/fhir/jpa/starter/interceptor/`
- **Purpose**: Multi-tenant partition routing
- **Features**:
  - X-Clinic-ID header parsing
  - Subdomain extraction (future)
  - URL path parsing (future)
  - Dynamic clinic registration

#### 2. **ApiTokenAuthInterceptor.java**
- **Path**: `src/main/java/ca/uhn/fhir/jpa/starter/interceptor/`
- **Purpose**: Bearer token authentication
- **Features**:
  - Token validation
  - Public endpoint whitelist
  - Configurable via `application.yaml`

#### 3. **init-partitions.sql**
- **Path**: `src/main/resources/`
- **Purpose**: Database partition initialization
- **Features**:
  - Creates 6 clinic partitions
  - Idempotent (ON CONFLICT DO NOTHING)

---

## 🚀 Deployment

### Production Considerations

1. **Security**
   - Use strong API tokens (externalize from config)
   - Implement OAuth 2.0 / OIDC for production
   - Enable SSL/TLS
   - Configure firewall rules

2. **Database**
   - Use managed PostgreSQL (AWS RDS, Azure PostgreSQL)
   - Configure backups and replication
   - Optimize connection pooling
   - Monitor partition sizes

3. **Monitoring**
   - Configure application metrics (Spring Actuator)
   - Set up alerting (partition growth, errors)
   - Enable audit logging per partition
   - Track API usage per clinic

4. **Scaling**
   - Use horizontal scaling (multiple server instances)
   - Configure shared cache (Redis)
   - Implement load balancing
   - Consider message broker for subscriptions

See [Production Setup Guide](../PRODUCTION_SETUP.md) for detailed instructions.

---

## 🤝 Contributing

This is a Dudoxx-internal project. For contributions:

1. Fork this repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project inherits the **Apache License 2.0** from the upstream HAPI FHIR project.

See [LICENSE](LICENSE) for details.

---

## 🔗 Links

### Dudoxx Resources
- **Organization**: [Dudoxx UG](https://dudoxx.com)
- **TypeScript Client**: [dudoxx-fhir-sdk-ts](../dudoxx-fhir-sdk-ts/)
- **Documentation**: [../docs/](../docs/)

### Upstream HAPI FHIR
- **GitHub**: https://github.com/hapifhir/hapi-fhir-jpaserver-starter
- **Main Library**: https://github.com/hapifhir/hapi-fhir
- **Documentation**: https://hapifhir.io/
- **Community**: https://chat.fhir.org/

### FHIR Specification
- **FHIR R4**: http://hl7.org/fhir/R4/
- **HL7 FHIR**: https://www.hl7.org/fhir/

---

## 📞 Support

For Dudoxx-specific issues:
- Create an issue in this repository
- Contact: support@dudoxx.com

For upstream HAPI FHIR questions:
- HAPI FHIR Community: https://chat.fhir.org/
- GitHub Issues: https://github.com/hapifhir/hapi-fhir/issues
- Getting Help: https://github.com/hapifhir/hapi-fhir/wiki/Getting-Help

---

## 📝 Changelog

### Dudoxx Customizations (v1.0.0)

**Added:**
- Multi-tenant partition system (6 clinics)
- API token authentication
- Custom interceptors (Auth + Partition routing)
- PostgreSQL configuration
- TypeScript REST client
- Comprehensive documentation
- Test scripts for multi-tenancy

**Modified:**
- `StarterJpaConfig.java` - Added interceptor registration
- `application.yaml` - PostgreSQL, auth, and partitioning config

**Database:**
- PostgreSQL with HapiFhirPostgresDialect
- 6 clinic partitions initialized

---

**Built with ❤️ by Dudoxx UG**  
**Powered by HAPI FHIR**
