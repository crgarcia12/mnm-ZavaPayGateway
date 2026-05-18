package com.zavabank.paygateway;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class PayGatewayConnectionFactory {
    static {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException exception) {
            throw new RuntimeException("SQL Server JDBC driver not found", exception);
        }
    }

    private PayGatewayConnectionFactory() {
    }

    public static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(
            PayGatewayConfig.getDbUrl(),
            PayGatewayConfig.getDbUser(),
            PayGatewayConfig.getDbPassword()
        );
    }
}
