# Architecture Diagram

ZavaPayGateway is a Java EE web application built on Apache Struts 1.3 that provides a payment gateway interface for ZavaBank. It authenticates users via an external SSO service, retrieves account data from SQL Server, and posts transactions to a downstream Ledger microservice.

## Application Architecture

```mermaid
flowchart TD
    subgraph Client["Client Layer"]
        Browser["Web Browser"]
    end

    subgraph Presentation["Presentation Layer - JSP"]
        LoginJSP["login.jsp"]
        PaymentJSP["payment.jsp"]
        HistoryJSP["payment-history.jsp"]
        HealthJSP["health.jsp"]
    end

    subgraph App["Application Layer - Apache Struts 1.3 / Java 8"]
        ActionServlet["ActionServlet (Struts Front Controller)"]
        subgraph Actions["Action Controllers"]
            LoginAction["LoginAction"]
            PaymentAction["PaymentAction"]
            HistoryAction["PaymentHistoryAction"]
            HealthAction["HealthAction"]
        end
        SsoService["SsoSessionService"]
        Config["PayGatewayConfig"]
        ConnFactory["PayGatewayConnectionFactory"]
    end

    subgraph Data["Data Layer"]
        JDBC["JDBC (mssql-jdbc 12.8.1)"]
        SQLServer[("SQL Server - ZavaBankDB")]
    end

    subgraph External["External Services"]
        AuthGW["Auth Gateway - WhoAmI.ashx (.NET)"]
        Ledger["Ledger Service - REST API"]
    end

    Browser -->|"HTTP *.do requests"| ActionServlet
    ActionServlet -->|"routes"| LoginAction
    ActionServlet -->|"routes"| PaymentAction
    ActionServlet -->|"routes"| HistoryAction
    ActionServlet -->|"routes"| HealthAction
    LoginAction -->|"resolves session"| SsoService
    PaymentAction -->|"resolves session"| SsoService
    HistoryAction -->|"resolves session"| SsoService
    SsoService -->|"token lookup"| ConnFactory
    PaymentAction -->|"account queries"| ConnFactory
    HistoryAction -->|"history queries"| ConnFactory
    ConnFactory -->|"SQL queries"| JDBC
    JDBC -->|"connects"| SQLServer
    SsoService -->|"SSO fallback"| AuthGW
    PaymentAction -->|"posts transactions"| Ledger
    LoginAction -->|"renders"| LoginJSP
    PaymentAction -->|"renders"| PaymentJSP
    HistoryAction -->|"renders"| HistoryJSP
    HealthAction -->|"renders"| HealthJSP
```

### Technology Stack Summary

| Layer | Technology | Version | Purpose |
|-------|-----------|---------|---------|
| Presentation | JSP (JavaServer Pages) | Servlet 3.1 | Server-rendered UI views |
| Web Framework | Apache Struts | 1.3.10 | MVC front-controller, routing, form binding |
| Web Framework | Apache Struts Taglib | 1.3.10 | JSP tag library for Struts forms |
| Runtime | Java SE | 1.8 | Application runtime language |
| Data Access | JDBC (mssql-jdbc) | 12.8.1.jre8 | Direct SQL Server connectivity |
| Build | Gradle | 8.9 | Build automation and dependency management |
| Packaging | WAR | — | Deployed as ROOT.war to servlet container |
| Server API | javax.servlet-api | 3.1.0 | Servlet container contract |

### Data Storage & External Services

The application uses **Microsoft SQL Server** (database `ZavaBankDB`, host `sqlserver:1433`) as its sole data store, accessed via raw JDBC through `PayGatewayConnectionFactory`. Tables queried include `SessionTokens`, `Users`, `Accounts`, and `PaymentHistory`. Two external HTTP services are consumed: the **Auth Gateway** (`zava-auth-gateway:8080/WhoAmI.ashx`), a .NET service used as a fallback SSO resolver when a `.ZAVAAUTH` cookie is present, and the **Ledger Service** (`zava-ledger:8080/api/transactions`), which receives XML-encoded payment transactions via HTTP POST.

### Key Architectural Decisions

- **Apache Struts 1.3 MVC pattern**: All HTTP requests matching `*.do` are handled by the Struts `ActionServlet`, which dispatches to concrete `Action` subclasses, providing a classic front-controller structure.
- **Direct JDBC with no ORM**: Data access uses raw `PreparedStatement` calls rather than JPA or any ORM, keeping the data layer thin but tightly coupled to SQL Server.
- **External SSO integration**: Session resolution is delegated to `SsoSessionService`, which first checks for an `X-Session-Token` header or local `SessionTokens` table, then falls back to calling the `.NET` Auth Gateway's `WhoAmI.ashx` endpoint via HTTP cookie forwarding.

## Component Relationships

```mermaid
flowchart LR
    subgraph Presentation["Presentation (JSP Views)"]
        LoginJSP["login.jsp"]
        PaymentJSP["payment.jsp"]
        HistoryJSP["payment-history.jsp"]
        HealthJSP["health.jsp"]
    end

    subgraph WebLayer["Web Layer (Struts Actions)"]
        ActionServlet["ActionServlet"]
        LoginAction["LoginAction"]
        PaymentAction["PaymentAction"]
        HistoryAction["PaymentHistoryAction"]
        HealthAction["HealthAction"]
    end

    subgraph Forms["Form Beans"]
        LoginForm["LoginForm"]
        PaymentForm["PaymentForm"]
    end

    subgraph Domain["Domain / Model"]
        SessionUser["SessionUser"]
        AccountOption["AccountOption"]
        HistoryRecord["PaymentHistoryRecord"]
    end

    subgraph Infra["Infrastructure"]
        SsoService["SsoSessionService"]
        ConnFactory["PayGatewayConnectionFactory"]
        Config["PayGatewayConfig"]
    end

    ActionServlet -->|"dispatches"| LoginAction
    ActionServlet -->|"dispatches"| PaymentAction
    ActionServlet -->|"dispatches"| HistoryAction
    ActionServlet -->|"dispatches"| HealthAction
    LoginAction -->|"binds"| LoginForm
    PaymentAction -->|"binds"| PaymentForm
    LoginAction -->|"uses"| SsoService
    PaymentAction -->|"uses"| SsoService
    HistoryAction -->|"uses"| SsoService
    SsoService -->|"opens"| ConnFactory
    SsoService -->|"returns"| SessionUser
    PaymentAction -->|"opens"| ConnFactory
    PaymentAction -->|"populates"| AccountOption
    HistoryAction -->|"opens"| ConnFactory
    HistoryAction -->|"populates"| HistoryRecord
    ConnFactory -->|"reads"| Config
    SsoService -->|"reads"| Config
    PaymentAction -->|"reads"| Config
    LoginAction -->|"forwards"| LoginJSP
    PaymentAction -->|"forwards"| PaymentJSP
    HistoryAction -->|"forwards"| HistoryJSP
    HealthAction -->|"forwards"| HealthJSP
```

### Component Inventory

| Component | Layer | Type | Responsibility |
|-----------|-------|------|---------------|
| ActionServlet | Web Layer | Struts Front Controller | Routes all `*.do` requests to the correct Action |
| LoginAction | Web Layer | Struts Action | Handles user login; validates session and redirects |
| PaymentAction | Web Layer | Struts Action | Displays payment form, loads accounts, submits transactions to ledger |
| PaymentHistoryAction | Web Layer | Struts Action | Retrieves and displays payment history for the current user |
| HealthAction | Web Layer | Struts Action | Provides a health-check endpoint |
| LoginForm | Form Beans | Struts ActionForm | Carries username/password fields from login form |
| PaymentForm | Form Beans | Struts ActionForm | Carries payment fields (from/to account, amount, note) |
| SsoSessionService | Infrastructure | Service | Resolves session tokens via DB lookup or Auth Gateway HTTP fallback |
| PayGatewayConnectionFactory | Infrastructure | Factory | Opens JDBC connections to SQL Server using config properties |
| PayGatewayConfig | Infrastructure | Configuration | Loads `paygateway.properties` and exposes typed accessors |
| SessionUser | Domain | Model / DTO | Carries authenticated user identity (userId, username) |
| AccountOption | Domain | Model / DTO | Represents a selectable bank account (id, number, type, balance) |
| PaymentHistoryRecord | Domain | Model / DTO | Represents a single payment history entry |
| login.jsp | Presentation | JSP View | Renders the login form |
| payment.jsp | Presentation | JSP View | Renders the payment submission form with account dropdowns |
| payment-history.jsp | Presentation | JSP View | Renders the tabular payment history |
| health.jsp | Presentation | JSP View | Renders the application health status |
