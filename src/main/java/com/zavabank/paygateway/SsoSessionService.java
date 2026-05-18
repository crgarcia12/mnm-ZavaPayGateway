package com.zavabank.paygateway;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

public class SsoSessionService {
    public static final String SESSION_USER = "zavaPayGateway.user";

    public SessionUser resolveSessionUser(HttpServletRequest request, String explicitToken) {
        String token = normalize(explicitToken);
        if (token.isEmpty()) {
            token = extractToken(request);
        }
        if (token.isEmpty()) {
            return null;
        }

        try (Connection connection = PayGatewayConnectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT TOP 1 st.UserID, u.Username " +
                     "FROM SessionTokens st INNER JOIN Users u ON st.UserID = u.UserID " +
                     "WHERE st.Token = ? AND st.IsActive = 1 AND st.ExpiresAt > GETDATE() AND u.IsActive = 1 " +
                     "ORDER BY st.ExpiresAt DESC")) {
            statement.setString(1, token);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new SessionUser(resultSet.getInt(1), resultSet.getString(2));
                }
            }
        } catch (SQLException ignored) {
        }
        return null;
    }

    private String extractToken(HttpServletRequest request) {
        String fromParam = normalize(request.getParameter("sessionToken"));
        if (!fromParam.isEmpty()) {
            return fromParam;
        }

        String fromHeader = normalize(request.getHeader("X-Session-Token"));
        if (!fromHeader.isEmpty()) {
            return fromHeader;
        }

        String authorization = normalize(request.getHeader("Authorization"));
        if (authorization.startsWith("Bearer ")) {
            return normalize(authorization.substring(7));
        }

        // Fallback: if .ZAVAAUTH cookie exists, call WhoAmI.ashx to resolve the sessionToken.
        // Java cannot decrypt the .NET FormsAuth cookie, so we ask the auth gateway to do it.
        String cookieName = PayGatewayConfig.getSsoCookieName();
        String authCookieValue = "";
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    authCookieValue = normalize(cookie.getValue());
                    break;
                }
            }
        }

        if (!authCookieValue.isEmpty()) {
            String resolved = resolveTokenFromAuthGateway(cookieName, authCookieValue);
            if (!resolved.isEmpty()) {
                return resolved;
            }
        }

        return "";
    }

    /**
     * Calls WhoAmI.ashx on the .NET auth gateway, forwarding the .ZAVAAUTH cookie,
     * and extracts the sessionToken from the JSON response.
     */
    private String resolveTokenFromAuthGateway(String cookieName, String cookieValue) {
        String whoAmIUrl = normalize(PayGatewayConfig.getAuthGatewayWhoAmIUrl());
        if (whoAmIUrl.isEmpty()) {
            return "";
        }
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(whoAmIUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Cookie", cookieName + "=" + cookieValue);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            int status = conn.getResponseCode();
            if (status != 200) {
                return "";
            }

            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
            }
            return extractSessionTokenFromJson(body.toString());
        } catch (Exception ignored) {
            return "";
        }
    }

    /** Simple JSON extraction for "sessionToken":"<value>" without external libraries. */
    private String extractSessionTokenFromJson(String json) {
        String key = "\"sessionToken\":\"";
        int start = json.indexOf(key);
        if (start < 0) {
            return "";
        }
        start += key.length();
        int end = json.indexOf('"', start);
        if (end < 0) {
            return "";
        }
        return normalize(json.substring(start, end));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
