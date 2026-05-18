<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<html>
<head>
    <title>ZavaPayGateway - Payment History</title>
</head>
<body bgcolor="#f4f4f4">
<table width="980" align="center" cellpadding="8" cellspacing="0" border="1" bgcolor="#ffffff">
    <tr bgcolor="#003366">
        <td colspan="7">
            <font color="#ffffff"><b>ZavaPayGateway - Payment History</b></font>
            <span style="float:right;color:#ffffff;">User: <%= request.getAttribute("username") == null ? "Unknown" : request.getAttribute("username") %></span>
        </td>
    </tr>
    <tr bgcolor="#eeeeee">
        <td colspan="7">
            <a href="/" style="font-weight:bold;">&#9664; ZavaBank Portal</a> |
            <a href="<%= request.getContextPath() %>/makePayment.do">Make Payment</a> |
            <a href="<%= request.getContextPath() %>/paymentHistory.do">Refresh History</a>
        </td>
    </tr>
    <tr bgcolor="#d7d7d7">
        <th align="left">Transaction ID</th>
        <th align="left">Account</th>
        <th align="right">Amount</th>
        <th align="left">Payment Type</th>
        <th align="left">Reference #</th>
        <th align="left">Transaction Date</th>
        <th align="left">Status</th>
    </tr>
    <logic:iterate id="row" name="historyRows">
        <tr>
            <td><bean:write name="row" property="transactionId"/></td>
            <td><bean:write name="row" property="accountNumber"/></td>
            <td align="right">$<bean:write name="row" property="amount"/></td>
            <td><bean:write name="row" property="paymentType"/></td>
            <td><bean:write name="row" property="referenceNumber"/></td>
            <td><bean:write name="row" property="transactionDate"/></td>
            <td><bean:write name="row" property="status"/></td>
        </tr>
    </logic:iterate>
    <logic:empty name="historyRows">
        <tr>
            <td colspan="7"><i>No posted payments found yet.</i></td>
        </tr>
    </logic:empty>
</table>
</body>
</html>
