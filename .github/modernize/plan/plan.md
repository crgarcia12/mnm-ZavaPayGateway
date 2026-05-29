# Modernization Plan: ZavaPayGateway — Modernize and Deploy to Azure

**Project**: ZavaPayGateway

---

## Technical Framework

- **Language**: Java 8
- **Framework**: Apache Struts 1.3.10
- **Build Tool**: Gradle 7.6
- **Database**: Microsoft SQL Server (mssql-jdbc 12.8.1)
- **Web Container**: Tomcat 9 (containerized via Docker)
- **Key Dependencies**: Apache Struts 1.3.10, Microsoft SQL Server JDBC Driver 12.8.1, javax.servlet-api 3.1.0

---

## Overview

> This migration modernizes the ZavaPayGateway payment processing application for Azure deployment. The application currently runs on Java 8 with Apache Struts 1.3.10, connects to SQL Server with hardcoded plaintext credentials, and is packaged as a WAR deployed on Tomcat 9.
>
> The new architecture will:
>
> - Upgrade the Java runtime to version 21 (LTS) to enable modern cloud-native patterns and tooling
> - Replace hardcoded plaintext credentials with Azure Key Vault for centralized, secure secret management
> - Replace username/password SQL Server authentication with Azure Managed Identity for passwordless, credential-free database access
> - Remediate all known CVE vulnerabilities in project dependencies prior to deployment
> - Deploy the containerized application to Azure Container Apps for scalable, managed cloud hosting
>
> The migration follows an incremental approach: runtime upgrade first, then cloud service integrations, then security hardening, and finally deployment to Azure.

---

## Migration Impact Summary

| Application       | Original Service                  | New Azure Service          | Authentication     | Comments                                                              |
|-------------------|-----------------------------------|----------------------------|--------------------|-----------------------------------------------------------------------|
| ZavaPayGateway    | Java 8 runtime                    | Java 21 runtime            | N/A                | Upgrade JDK to latest LTS for cloud-native compatibility              |
| ZavaPayGateway    | Hardcoded credentials in .properties | Azure Key Vault         | Managed Identity   | Migrate plaintext db.password and other secrets to Azure Key Vault    |
| ZavaPayGateway    | SQL Server (username/password)    | Azure SQL Database         | Managed Identity   | Replace password-based JDBC auth with passwordless Managed Identity   |
| ZavaPayGateway    | Local deployment / Tomcat         | Azure Container Apps       | Managed Identity   | Deploy containerized app to Azure Container Apps using existing Docker |

---

## Open Questions & Questionnaire

- [x] Q: Should the plan include environment/infrastructure provisioning? → A: No — no infrastructure provisioning; focus on code migration and deployment only
- [x] Q: Should the plan include integration testing to verify migrated services? → A: No — skip integration testing (no environment available)
- [x] Q: Should the plan include a security scan and CVE remediation task? → A: Yes — include security/CVE remediation (default)
- [x] Q: Which Azure deployment target should the plan use? → A: Azure Container Apps (default)
- [x] Q: What Java version should the upgrade target? → A: Java 21 (latest LTS). Note: if Java 25 (latest stable) is desired, the plan requires Spring Boot 4.x migration as well — please confirm the desired target version.
