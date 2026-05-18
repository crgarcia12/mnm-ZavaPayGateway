package com.zavabank.paygateway;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

public class PaymentHistoryAction extends Action {
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
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
        request.setAttribute("historyRows", readHistory());
        return mapping.findForward("success");
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

    private List<PaymentHistoryRecord> readHistory() {
        List<PaymentHistoryRecord> rows = new ArrayList<PaymentHistoryRecord>();
        try (java.sql.Connection connection = PayGatewayConnectionFactory.openConnection();
             java.sql.PreparedStatement statement = connection.prepareStatement(
                 "SELECT TOP 75 t.TransactionID, a.AccountNumber, t.Amount, t.Description, " +
                     "t.ReferenceNumber, t.TransactionDate, t.Status " +
                     "FROM Transactions t INNER JOIN Accounts a ON t.AccountID = a.AccountID " +
                     "WHERE t.Description LIKE 'ZavaPayGateway:%' ORDER BY t.TransactionDate DESC");
             java.sql.ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                PaymentHistoryRecord record = new PaymentHistoryRecord();
                record.setTransactionId(resultSet.getLong(1));
                record.setAccountNumber(resultSet.getString(2));
                BigDecimal amount = resultSet.getBigDecimal(3);
                record.setAmount(amount == null ? "0.00" : MONEY_FORMAT.format(amount));
                String description = resultSet.getString(4);
                String paymentType = "UNKNOWN";
                if (description != null && description.startsWith("ZavaPayGateway:")) {
                    String[] split = description.split(":");
                    if (split.length >= 2) {
                        paymentType = split[1];
                    }
                }
                record.setPaymentType(paymentType);
                record.setReferenceNumber(resultSet.getString(5));
                java.util.Date txDate = resultSet.getTimestamp(6);
                record.setTransactionDate(txDate == null ? "" : DATE_FORMAT.format(txDate));
                record.setStatus(resultSet.getString(7));
                rows.add(record);
            }
        } catch (java.sql.SQLException ignored) {
        }
        return rows;
    }
}
