package py.com.logixone.web.admin;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import py.com.logixone.kernel.application.security.SecurityOperationCode;
import py.com.logixone.kernel.application.security.SecurityOperationStatus;
import py.com.logixone.kernel.application.security.admin.SecurityAdministrationActionResult;

final class AdminSecurityOperationMessages {

    private AdminSecurityOperationMessages() {
    }

    static String finish(
            SecurityAdministrationActionResult result,
            String changed,
            String unchanged,
            String redirect) {
        FacesMessage message;
        if (result.status() == SecurityOperationStatus.REJECTED) {
            message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "No se pudo aplicar el cambio",
                    safeFailure(result.failure().orElseThrow()));
        } else {
            message = new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Operación procesada",
                    result.changed() ? changed : unchanged);
        }
        FacesContext faces = FacesContext.getCurrentInstance();
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

    private static String safeFailure(SecurityOperationCode code) {
        return switch (code) {
            case USER_VERSION_CONFLICT, MEMBERSHIP_VERSION_CONFLICT,
                    ROLE_VERSION_CONFLICT, SYSTEM_ROLE_VERSION_CONFLICT ->
                    "El estado cambió en otra sesión. Recargue antes de reintentar.";
            case SYSTEM_LAST_ADMINISTRATOR_REQUIRED ->
                    "La operación dejaría la instancia sin un administrador global efectivo.";
            case PERMISSION_NOT_AVAILABLE ->
                    "El permiso ya no está disponible en la composición efectiva de la empresa.";
            case EXTERNAL_IDENTITY_ALREADY_EXISTS, ROLE_CODE_ALREADY_EXISTS,
                    SYSTEM_ROLE_CODE_ALREADY_EXISTS ->
                    "Ya existe un recurso con esa identidad o código. Recargue el listado.";
            case ROLE_COMPANY_MISMATCH ->
                    "El rol no pertenece a la empresa seleccionada.";
            default ->
                    "La operación fue rechazada con el estado actual. Recargue y verifique la selección.";
        };
    }
}
