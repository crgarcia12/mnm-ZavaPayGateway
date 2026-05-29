# Architecture Diagram

This application is a Java web gateway that handles login, payment submission, and payment history views using Struts actions and JSP pages.

## Application Architecture

```mermaid
flowchart TD
    subgraph Client["Client Layer"]
        Browser["Web Browser"]
    end
    subgraph App["Application Layer - Java Struts"]
        Struts["Struts ActionServlet"]
        Actions["Login Payment History Actions"]
        Views["JSP Views"]
        SsoSvc["SsoSessionService"]
    end
    subgraph Data["Data Layer"]
        Jdbc["JDBC Access"]
        Sql[("SQL Server")]
    end
    subgraph External["External Services"]
        Auth["Auth Gateway WhoAmI"]
        Ledger["Ledger Transactions API"]
    end

    Browser -->|"HTTP requests"| Struts
    Struts -->|"dispatch"| Actions
    Actions -->|"render"| Views
    Actions -->|"session resolution"| SsoSvc
    Actions -->|"queries"| Jdbc
    Jdbc -->|"SQL"| Sql
    SsoSvc -->|"cookie lookup"| Auth
    Actions -->|"payment post"| Ledger
```

### Technology Stack Summary

| Layer | Technology | Version | Purpose |
|---|---|---|---|
| Presentation | JSP + Struts ActionServlet | Struts 1.3.10 | Request handling and page rendering |
| Business | Java Action classes | Java 8 target | Login, payment posting, history retrieval |
| Data Access | JDBC + SQL Server Driver | mssql-jdbc 12.8.1.jre8 | Read accounts/history and validate sessions |
| External Integration | HTTP services | N/A | Auth token resolution and ledger posting |

### Data Storage & External Services

The application reads and writes operational payment data from SQL Server and integrates with two HTTP services: an authentication gateway for resolving session tokens and a ledger API for posting transaction entries.

### Key Architectural Decisions

- Uses classic Struts action mapping (`*.do`) with server-rendered JSP pages.
- Uses direct JDBC queries instead of repository abstractions.
- Delegates token resolution to a remote auth gateway when SSO cookie is present.

## Component Relationships

```mermaid
flowchart LR
    subgraph Presentation
        LoginAction["LoginAction"]
        PaymentAction["PaymentAction"]
        PaymentHistoryAction["PaymentHistoryAction"]
        HealthAction["HealthAction"]
    end
    subgraph Business["Business Logic"]
        SsoService["SsoSessionService"]
        Config["PayGatewayConfig"]
    end
    subgraph DataAccess["Data Access"]
        ConnFactory["PayGatewayConnectionFactory"]
        SqlServer["SQL Server"]
    end
    subgraph Infra["Infrastructure"]
        StrutsServlet["ActionServlet"]
        AuthGateway["Auth Gateway"]
        LedgerApi["Ledger API"]
    end

    StrutsServlet -->|"routes"| LoginAction
    StrutsServlet -->|"routes"| PaymentAction
    StrutsServlet -->|"routes"| PaymentHistoryAction
    StrutsServlet -->|"routes"| HealthAction
    LoginAction -->|"resolve user"| SsoService
    PaymentAction -->|"ensure user"| SsoService
    PaymentHistoryAction -->|"ensure user"| SsoService
    SsoService -->|"open DB connection"| ConnFactory
    PaymentAction -->|"load accounts"| ConnFactory
    PaymentHistoryAction -->|"read history"| ConnFactory
    ConnFactory -->|"JDBC"| SqlServer
    SsoService -->|"WhoAmI call"| AuthGateway
    PaymentAction -->|"POST transaction"| LedgerApi
    LoginAction -->|"read endpoints"| Config
    PaymentAction -->|"read settings"| Config
    SsoService -->|"read settings"| Config
```

### Component Inventory

| Component | Layer | Type | Responsibility |
|---|---|---|---|
| ActionServlet | Infrastructure | Servlet | Dispatches `*.do` routes to Struts actions |
| LoginAction | Presentation | Struts Action | Handles token-based login and session initialization |
| PaymentAction | Presentation | Struts Action | Validates payment input and posts ledger transactions |
| PaymentHistoryAction | Presentation | Struts Action | Loads and renders recent payment history |
| SsoSessionService | Business Logic | Service | Resolves session user from token/cookie + DB |
| PayGatewayConnectionFactory | Data Access | Factory | Creates SQL Server JDBC connections |
| PayGatewayConfig | Business Logic | Config utility | Resolves environment/property-based settings |
