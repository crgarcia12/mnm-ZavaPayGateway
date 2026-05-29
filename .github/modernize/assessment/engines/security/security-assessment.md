# Security Assessment Report

**Generated:** 2026-05-29T04:02:19.0000000Z

## Summary

| Metric | Count |
|--------|-------|
| Total Findings | 3 |
| CVE Vulnerabilities | 0 |
| CWE Vulnerabilities | 3 |
| Total Rules Assessed | 59 |
| Rules Passed | 56 |

### By Severity

| Severity | Count |
|----------|-------|
| mandatory | 0 |
| optional | 3 |
| potential | 0 |

## CVE Findings (Dependency Vulnerabilities)

No CVE findings met the configured severity threshold.

## CWE Findings (Code-Level Vulnerabilities)

### CWE-259: Use of Hard-coded Password
- **Category:** Credentials & Secrets
- **Severity:** optional
- **Story Points:** 5
- **Files:** src/main/resources/paygateway.properties

`paygateway.properties` defines `db.****** a plaintext hard-coded password used for outbound SQL Server authentication.

### CWE-798: Use of Hard-coded Credentials
- **Category:** Credentials & Secrets
- **Severity:** optional
- **Story Points:** 5
- **Files:** src/main/resources/paygateway.properties

Static credential material (`db.user=sa`, `db.****** is stored in repository configuration and read by `PayGatewayConfig`, creating hard-coded outbound database credentials.

### CWE-79: Improper Neutralization of Input During Web Page Generation ('Cross-site Scripting')
- **Category:** Injection Attacks
- **Severity:** optional
- **Story Points:** 8
- **Files:** src/main/webapp/WEB-INF/jsp/payment.jsp, src/main/webapp/WEB-INF/jsp/payment-history.jsp

JSP scriptlet expressions render request attribute values directly using `<%= ... %>` in payment and history headers (`username`, `statusMessage`, `statusError`) without output encoding, enabling reflected/stored XSS when upstream values are attacker-controlled.
