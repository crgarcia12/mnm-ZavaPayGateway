package com.zavabank.paygateway;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

public class LoginAction extends Action {
    private final SsoSessionService ssoSessionService = new SsoSessionService();

    @Override
    public ActionForward execute(
        ActionMapping mapping,
        ActionForm form,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        HttpSession session = request.getSession(true);
        Object existing = session.getAttribute(SsoSessionService.SESSION_USER);
        if (existing instanceof SessionUser) {
            return mapping.findForward("success");
        }

        String tokenFromForm = "";
        if (form instanceof LoginForm) {
            tokenFromForm = ((LoginForm) form).getSessionToken();
        }

        SessionUser sessionUser = ssoSessionService.resolveSessionUser(request, tokenFromForm);
        if (sessionUser != null) {
            session.setAttribute(SsoSessionService.SESSION_USER, sessionUser);
            return mapping.findForward("success");
        }

        if ("POST".equalsIgnoreCase(request.getMethod()) || (tokenFromForm != null && !tokenFromForm.trim().isEmpty())) {
            request.setAttribute("loginError", "Session token was not found or has expired.");
        }
        return mapping.findForward("login");
    }
}
