package py.com.logixone.web.admin;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import py.com.logixone.kernel.application.company.CompanyOperationCode;
import py.com.logixone.kernel.application.company.CompanyOperationStatus;
import py.com.logixone.kernel.application.company.admin.CompanyAdministrationActionResult;

/** Safe browser messages: no SQL, stack traces or foreign technical identifiers. */
final class AdminCompanyOperationMessages {

    private AdminCompanyOperationMessages() {
    }

    static String finish(
            CompanyAdministrationActionResult result,
            String changedMessage,
            String unchangedMessage,
            String redirect) {
        FacesContext faces = FacesContext.getCurrentInstance();
        FacesMessage message;
        if (result.status() == CompanyOperationStatus.REJECTED) {
            message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "No se pudo aplicar el cambio",
                    safeFailure(result.failure().orElseThrow()));
        } else {
            String detail = result.changed() ? changedMessage : unchangedMessage;
            message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Operación procesada", detail);
        }
        faces.addMessage(null, message);
        faces.getExternalContext().getFlash().setKeepMessages(true);
        return redirect;
    }

    static void invalidInput() {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(
                FacesMessage.SEVERITY_ERROR,
                "Entrada no válida",
                "Revise los datos visibles y vuelva a intentar."));
    }

    static void denied() {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(
                FacesMessage.SEVERITY_ERROR,
                "Operación no disponible",
                "La sesión actual no está autorizada para realizar este cambio."));
    }

    static void targetUnavailable() {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(
                FacesMessage.SEVERITY_ERROR,
                "Recurso no disponible",
                "No fue posible resolver el recurso solicitado. Regrese al listado y recargue."));
    }

    private static String safeFailure(CompanyOperationCode code) {
        return switch (code) {
            case COMPANY_VERSION_CONFLICT,
                    ACTIVATION_VERSION_CONFLICT,
                    CUSTOMIZATION_VERSION_CONFLICT ->
                    "El estado cambió en otra sesión. Recargue la pantalla antes de reintentar.";
            case REQUIRED_DEPENDENCY_NOT_EFFECTIVE, ACTIVE_DEPENDENT_EXISTS,
                    CUSTOMIZATION_INCOMPATIBLE, CUSTOMIZATION_CONTRACT_INVALID ->
                    "La composición solicitada no cumple sus dependencias. Revise los estados actuales.";
            case CUSTOMIZATION_ALREADY_ASSIGNED ->
                    "La personalización seleccionada ya no está disponible. Recargue las opciones.";
            case COMPANY_INACTIVE ->
                    "La empresa debe estar activa para completar esta operación.";
            default ->
                    "La operación fue rechazada con el estado actual. Recargue y verifique la selección.";
        };
    }
}
