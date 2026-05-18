<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<html>
<head>
    <title>ZavaPayGateway Login</title>
</head>
<body bgcolor="#f4f4f4">
<table width="760" align="center" cellpadding="8" cellspacing="0" border="1" bgcolor="#ffffff">
    <tr bgcolor="#003366">
        <td><font color="#ffffff"><b>ZavaPayGateway - Payment Processing Portal</b></font></td>
    </tr>
    <tr bgcolor="#d0d8e0">
        <td style="padding:3px 8px;font-size:11px;font-family:Verdana,Arial;"><a href="/" style="color:#003366;text-decoration:none;font-weight:bold;">&#9664; ZavaBank Portal</a></td>
    </tr>
    <tr>
        <td>
            <p>Sign in using your cross-ecosystem session token from ZavaAuthGateway.</p>
            <%
                Object loginError = request.getAttribute("loginError");
                if (loginError != null) {
            %>
            <p><font color="#cc0000"><b><%= loginError %></b></font></p>
            <%
                }
            %>
            <html:form action="/login.do" method="post">
                <table cellpadding="6" cellspacing="0" border="0">
                    <tr>
                        <td><b>Session Token:</b></td>
                        <td><html:text property="sessionToken" size="56" maxlength="128"/></td>
                    </tr>
                    <tr>
                        <td>&nbsp;</td>
                        <td><html:submit value="Sign In"/></td>
                    </tr>
                </table>
            </html:form>
            <p><small>Token sources: <code>.ZAVAAUTH</code> cookie, <code>sessionToken</code> query parameter, or <code>X-Session-Token</code> header.</small></p>
        </td>
    </tr>
</table>
</body>
</html>
