package py.com.logixone.web.admin;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;

final class AdminAuditMessages {

    private AdminAuditMessages() {
    }

    static void invalidQuery() {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(
                FacesMessage.SEVERITY_ERROR,
                "Consulta no válida",
                "Revise los filtros visibles y vuelva a intentar."));
    }
}
