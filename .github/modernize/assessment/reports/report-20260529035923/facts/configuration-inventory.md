# Configuration & Externalized Settings Inventory

Configuration is provided through Gradle/build descriptors, a properties file, and environment variable overrides with no profile-specific configuration files detected.

## Configuration Sources

| Source | Type | Path/Location | Notes |
|---|---|---|---|
| build.gradle | Build config | /tmp/workspace/crgarcia12/mnm-ZavaPayGateway/build.gradle | Declares plugins and dependencies |
| settings.gradle | Build config | /tmp/workspace/crgarcia12/mnm-ZavaPayGateway/settings.gradle | Root project naming |
| paygateway.properties | Runtime properties | /tmp/workspace/crgarcia12/mnm-ZavaPayGateway/src/main/resources/paygateway.properties | Default DB and integration endpoints |
| web.xml | Servlet config | /tmp/workspace/crgarcia12/mnm-ZavaPayGateway/src/main/webapp/WEB-INF/web.xml | Struts servlet registration |
| struts-config.xml | Action mapping config | /tmp/workspace/crgarcia12/mnm-ZavaPayGateway/src/main/webapp/WEB-INF/struts-config.xml | Route/action and form bindings |
| Environment variables | Externalized settings | Process environment | Overrides configured properties when present |

## Build Profiles

| Profile | Activation | Purpose | Key Dependencies/Plugins |
|---|---|---|---|
| default | automatic | Builds Java WAR artifact | `java`, `war` plugins |

## Runtime Profiles

| Profile | Activation Method | Config Files | Key Overrides |
|---|---|---|---|
| default | automatic | `paygateway.properties` | DB/auth/ledger defaults |
| env override | environment variable presence | environment + `paygateway.properties` fallback | DB credentials, host, URLs, settlement account ID |

## Properties Inventory

| Property Key | Default | Profiles | Source |
|---|---|---|---|
| DB_HOST / db.host | `sqlserver` | env override/default | env or paygateway.properties |
| DB_PORT / db.port | `1433` | env override/default | env or paygateway.properties |
| DB_NAME / db.name | `ZavaBankDB` | env override/default | env or paygateway.properties |
| DB_USER / db.user | `sa` | env override/default | env or paygateway.properties |
| DB_PASSWORD / db.password | `[MASKED]` | env override/default | env or paygateway.properties |
| SSO_COOKIE_NAME / sso.cookie.name | `.ZAVAAUTH` | env override/default | env or paygateway.properties |
| AUTH_GATEWAY_WHOAMI_URL / auth.gateway.whoami.url | `http://zava-auth-gateway:8080/WhoAmI.ashx` | env override/default | env or paygateway.properties |
| LEDGER_URL / ledger.url | `http://zava-ledger:8080/api/transactions` | env override/default | env or paygateway.properties |
| LEDGER_SETTLEMENT_ACCOUNT_ID / ledger.settlement.account.id | `1` | env override/default | env or paygateway.properties |

## Startup Parameters & Resource Requirements

| Service | JVM/Runtime Options | Memory | Instance Count |
|---|---|---|---|
| mnm-ZavaPayGateway | none explicitly configured in repo | not specified | not specified |

## Startup Dependency Chain

1. Application starts `ActionServlet` and loads Struts mappings.
2. Runtime availability depends on SQL Server reachability.
3. Login token fallback depends on auth gateway availability.
4. Payment submission depends on ledger API availability.

## Secrets & Sensitive Configuration

| Secret Reference | Type | Storage (masked) |
|---|---|---|
| DB_PASSWORD / db.password | Database credential | Environment variable or properties file `[MASKED]` |

### Secrets Provisioning Workflow

Secrets can be supplied by environment variables at runtime and otherwise fall back to file-based defaults in `paygateway.properties`. No managed secret store integration or identity-based retrieval flow was detected.

## Feature Flags

No feature flag framework or conditional feature toggles were detected.

## Framework & Runtime Versions

| Component | Version | Source |
|---|---|---|
| Java target compatibility | 1.8 | build.gradle |
| Apache Struts | 1.3.10 | build.gradle |
| Servlet API | 3.1.0 | build.gradle |
| SQL Server JDBC Driver | 12.8.1.jre8 | build.gradle |
| Gradle plugins | java, war | build.gradle |
