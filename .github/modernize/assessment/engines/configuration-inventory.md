# Configuration & Externalized Settings Inventory

ZavaPayGateway has a minimal configuration surface: a single `paygateway.properties` file bundled inside the WAR, with all properties overridable at runtime via environment variables. There are no profiles, no external config servers, and no secret stores.

## Configuration Sources

| Source | Type | Path / Location | Notes |
|--------|------|----------------|-------|
| `paygateway.properties` | Properties file (classpath) | `src/main/resources/paygateway.properties` | Loaded at class-initialization time via `PayGatewayConfig`; bundled into the WAR under `WEB-INF/classes/` |
| `paygateway.properties` (bin copy) | Properties file (classpath) | `bin/main/paygateway.properties` | Identical copy present in the Gradle incremental build output directory |
| `web.xml` | Java EE Deployment Descriptor | `src/main/webapp/WEB-INF/web.xml` | Configures the Struts `ActionServlet` and URL mapping for `*.do` pattern |
| `struts-config.xml` | Struts Framework Config | `src/main/webapp/WEB-INF/struts-config.xml` | Declares form beans, action mappings, and view forwards |
| Environment Variables | Runtime OS/Container env | Process environment | Every `paygateway.properties` key has a corresponding ENV override (see Properties Inventory). Environment variables take precedence over the properties file. |
| Dockerfile | Container Build Config | `Dockerfile` | Multi-stage build; no environment variable injection at build time |

No Spring Cloud Config, Azure App Configuration, AWS AppConfig, Consul KV, HashiCorp Vault, or Azure KeyVault integration is present.

## Build Profiles

| Profile | Activation | Purpose | Key Dependencies / Plugins |
|---------|-----------|---------|---------------------------|
| Default (no profile) | Always active | Single build configuration | `java`, `war` plugins; WAR packaged as `ROOT.war` |
| Docker build stage | Manual — `docker build` command | Compiles the WAR inside a Gradle 7.6 / JDK 8 container | Uses `gradle:7.6-jdk8` base image; runs `gradle war --no-daemon` |

There are no Maven/Gradle build profiles (no `build.gradle` `if (profile == …)` blocks, no separate source sets per environment). All dependencies are in a single configuration.

## Runtime Profiles

No runtime profile system is implemented. There are no `application-dev.properties`, `application-prod.properties`, or equivalent profile-specific configuration files. The single `paygateway.properties` file is the only configuration source; environment-specific values are injected at runtime via environment variables.

| Profile | Activation Method | Config Files | Key Overrides |
|---------|-----------------|-------------|--------------|
| Default (only profile) | Always active | `paygateway.properties` | All values can be overridden by environment variables at runtime |

## Properties Inventory

### ZavaPayGateway — `paygateway.properties`

| Property Key | Environment Variable | Default Value | Type | Purpose |
|-------------|---------------------|--------------|------|---------|
| `db.host` | `DB_HOST` | `sqlserver` | String (hostname) | SQL Server hostname |
| `db.port` | `DB_PORT` | `1433` | int | SQL Server port |
| `db.name` | `DB_NAME` | `ZavaBankDB` | String | Database name |
| `db.user` | `DB_USER` | `sa` | String | SQL Server login |
| `db.password` | `DB_PASSWORD` | `Zava123!` (plaintext) | String | SQL Server password — **sensitive** |
| `sso.cookie.name` | `SSO_COOKIE_NAME` | `.ZAVAAUTH` | String | Name of the FormsAuth cookie to forward to the Auth Gateway |
| `auth.gateway.whoami.url` | `AUTH_GATEWAY_WHOAMI_URL` | `http://zava-auth-gateway:8080/WhoAmI.ashx` | URL | Auth Gateway endpoint for SSO cookie resolution |
| `ledger.url` | `LEDGER_URL` | `http://zava-ledger:8080/api/transactions` | URL | Ledger Service endpoint for posting payment transactions |
| `ledger.settlement.account.id` | `LEDGER_SETTLEMENT_ACCOUNT_ID` | `1` | int | Default settlement/credit account ID used when no other account is configured |

**Resolution logic**: `PayGatewayConfig.read(envKey, propertyKey)` checks `System.getenv(envKey)` first; if the environment variable is non-null and non-blank it takes precedence. Otherwise, the value from `paygateway.properties` is used.

## Startup Parameters & Resource Requirements

| Service | JVM / Runtime Options | Memory | CPU | Instance Count | Notes |
|---------|----------------------|--------|-----|---------------|-------|
| ZavaPayGateway (WAR in Tomcat) | None specified in Dockerfile or any startup script | Not configured | Not configured | 1 | The Dockerfile uses `CMD ["catalina.sh", "run"]` with Tomcat defaults; no `-Xms`/`-Xmx` flags, no `-D` system properties |
| Docker build stage | None | Not configured | Not configured | 1 | `gradle:7.6-jdk8` image; `--no-daemon` flag for Gradle |

No Kubernetes manifests, Helm charts, or Docker Compose files are present in the repository. Resource limits and scaling must be configured externally by the deployment platform.

## Startup Dependency Chain

The application has no built-in startup dependency management. The following external services must be available before ZavaPayGateway can serve requests:

1. **SQL Server (`sqlserver:1433`)** — required at first request. There is no connection pool warm-up; the first database query will fail if SQL Server is unreachable. No retry or health-check wait mechanism is configured.
2. **Auth Gateway (`zava-auth-gateway:8080`)** — required only when a user presents a `.ZAVAAUTH` cookie. If unavailable, login will fail for SSO-cookie-based users but the application will not crash.
3. **Ledger Service (`zava-ledger:8080`)** — required only when a payment is submitted. If unavailable, the payment POST returns an error to the user but the application remains running.

No `dockerize`, `wait-for-tcp`, Kubernetes readiness probes, or Docker Compose `depends_on` mechanisms are configured.

## Secrets & Sensitive Configuration

| Secret Reference | Type | File / Source | Masked Value |
|-----------------|------|--------------|-------------|
| `db.password` / `DB_PASSWORD` | Database password | `src/main/resources/paygateway.properties` | [MASKED — hard-coded plaintext in committed file] |
| `db.user` / `DB_USER` | Database username | `src/main/resources/paygateway.properties` | [MASKED] |

No encryption is applied to any sensitive values. The database password is stored as plain text in a committed properties file and is visible in version control history.

### Secrets Provisioning Workflow

There is no secrets management workflow. All sensitive configuration is hard-coded in `paygateway.properties` which is committed to source control and bundled into the WAR artifact at build time. The only override mechanism is setting the corresponding environment variable (`DB_PASSWORD`, `DB_USER`) at container/JVM startup, which would shadow the built-in values. No Key Vault, Vault, Secrets Manager, Sealed Secrets, Jasypt encryption, or any other secrets management tool is integrated.

**Recommended remediation**: Remove `db.password` (and `db.user`) from `paygateway.properties`, require them to be injected exclusively via environment variables or a secret store, and rotate the existing credentials immediately since they are in version control history.

## Feature Flags

No feature flag framework is present. There are no `@ConditionalOnProperty`, `@ConditionalOnExpression`, LaunchDarkly, Unleash, or any other feature toggle mechanism. All features are statically enabled.

| Flag Name | Default | Controlled By |
|-----------|---------|--------------|
| N/A | N/A | No feature flags detected |

## Framework & Runtime Versions

| Component | Version | Source |
|-----------|---------|--------|
| Java (source / target) | 1.8 (Java 8) | `build.gradle` `sourceCompatibility` / `targetCompatibility` |
| Gradle (build tool) | 7.6 (Docker build stage), 8.9 (local wrapper) | `Dockerfile` base image; `.gradle/8.9/` directory |
| Apache Struts Core | 1.3.10 | `build.gradle` `dependencies` |
| Apache Struts Taglib | 1.3.10 | `build.gradle` `dependencies` |
| Microsoft JDBC Driver for SQL Server | 12.8.1.jre8 | `build.gradle` `dependencies` |
| javax.servlet-api | 3.1.0 | `build.gradle` `dependencies` (compileOnly) |
| Servlet container (runtime) | Apache Tomcat 9 (JDK 8 variant) | `Dockerfile` `FROM tomcat:9-jdk8` |
| Docker build base image | `gradle:7.6-jdk8` | `Dockerfile` first stage |
| Docker runtime base image | `tomcat:9-jdk8` | `Dockerfile` second stage |
