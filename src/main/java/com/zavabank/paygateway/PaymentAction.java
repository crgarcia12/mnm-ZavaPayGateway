package com.zavabank.paygateway;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

public class PaymentAction extends Action {
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");
    private final SsoSessionService ssoSessionService = new SsoSessionService();

    @Override
    public ActionForward execute(
        ActionMapping mapping,
        ActionForm form,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        SessionUser user = ensureUserSession(request);
        if (user == null) {
            return mapping.findForward("login");
        }

        request.setAttribute("username", user.getUsername());
        request.setAttribute("accountOptions", loadAccounts());

        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return mapping.findForward("success");
        }

        PaymentForm paymentForm = form instanceof PaymentForm ? (PaymentForm) form : new PaymentForm();
        String amountValue = safe(paymentForm.getAmount());
        String accountValue = safe(paymentForm.getAccountId());
        String paymentType = safe(paymentForm.getPaymentType());
        String memo = safe(paymentForm.getMemo());
        if (amountValue.isEmpty() || accountValue.isEmpty() || paymentType.isEmpty()) {
            request.setAttribute("statusError", "Please select an account, amount, and payment type.");
            return mapping.findForward("success");
        }

        try {
            int debitAccountId = Integer.parseInt(accountValue);
            BigDecimal amount = new BigDecimal(amountValue);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                request.setAttribute("statusError", "Payment amount must be greater than zero.");
                return mapping.findForward("success");
            }

            String referenceNumber = "PGW-" + System.currentTimeMillis();
            int settlementAccountId = PayGatewayConfig.getSettlementAccountId();
            if (settlementAccountId == debitAccountId) {
                settlementAccountId = debitAccountId == 1 ? 2 : 1;
            }

            boolean posted = postToLedger(debitAccountId, settlementAccountId, amount, paymentType, referenceNumber, memo);
            if (!posted) {
                request.setAttribute("statusError", "Unable to post payment to ZavaLedger.");
                return mapping.findForward("success");
            }

            request.setAttribute(
                "statusMessage",
                "Payment posted. Reference " + referenceNumber + ", amount $" + MONEY_FORMAT.format(amount)
            );
            return mapping.findForward("history");
        } catch (NumberFormatException ignored) {
            request.setAttribute("statusError", "Amount or account format is invalid.");
            return mapping.findForward("success");
        }
    }

    private SessionUser ensureUserSession(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        Object existing = session.getAttribute(SsoSessionService.SESSION_USER);
        if (existing instanceof SessionUser) {
            return (SessionUser) existing;
        }
        SessionUser resolved = ssoSessionService.resolveSessionUser(request, "");
        if (resolved != null) {
            session.setAttribute(SsoSessionService.SESSION_USER, resolved);
        }
        return resolved;
    }

    private List<AccountOption> loadAccounts() {
        List<AccountOption> options = new ArrayList<AccountOption>();
        try (Connection connection = PayGatewayConnectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT TOP 30 a.AccountID, a.AccountNumber, at.TypeName, a.Balance " +
                     "FROM Accounts a INNER JOIN AccountTypes at ON a.AccountTypeID = at.AccountTypeID " +
                     "WHERE a.Status = 'Active' ORDER BY a.AccountNumber");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                AccountOption option = new AccountOption();
                option.setAccountId(resultSet.getInt(1));
                option.setAccountNumber(resultSet.getString(2));
                option.setAccountType(resultSet.getString(3));
                option.setBalance(MONEY_FORMAT.format(resultSet.getBigDecimal(4)));
                options.add(option);
            }
        } catch (SQLException ignored) {
        }
        return options;
    }

    private boolean postToLedger(
        int debitAccountId,
        int creditAccountId,
        BigDecimal amount,
        String paymentType,
        String referenceNumber,
        String memo
    ) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(PayGatewayConfig.getLedgerUrl());
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/xml");
            connection.setDoOutput(true);
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);

            String description = "ZavaPayGateway:" + paymentType + ":" + memo;
            String body = "<transactionRequest>" +
                "<debitAccountId>" + debitAccountId + "</debitAccountId>" +
                "<creditAccountId>" + creditAccountId + "</creditAccountId>" +
                "<amount>" + amount + "</amount>" +
                "<description>" + xmlEscape(description) + "</description>" +
                "<referenceNumber>" + xmlEscape(referenceNumber) + "</referenceNumber>" +
                "</transactionRequest>";
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(body.getBytes(StandardCharsets.UTF_8));
            }

            if (connection.getResponseCode() != HttpServletResponse.SC_OK) {
                return false;
            }

            byte[] responseBytes = readAll(connection);
            String responseText = new String(responseBytes, StandardCharsets.UTF_8);
            return responseText.contains("<status>POSTED</status>");
        } catch (IOException ignored) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private byte[] readAll(HttpURLConnection connection) throws IOException {
        byte[] buffer = new byte[4096];
        int read;
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        try (java.io.InputStream inputStream = connection.getInputStream()) {
            while ((read = inputStream.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
        }
        return out.toByteArray();
    }

    private String xmlEscape(String value) {
        return safe(value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
