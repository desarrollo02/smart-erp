# Contexto empresarial confiable y autorización

- Versión: 2
- Fecha: 2026-07-28
- Estado: Implementado y validado en `J11-S3-08`
- Aplica desde: candidata `J11-S3-05`

## Propósito

Explicar cómo consumir el actor OIDC y el contexto empresarial sin convertir datos
del navegador o de la sesión en autoridad. Este procedimiento sirve a quienes creen
recursos REST, acciones Faces o adaptadores de un plugin.

## Prerrequisitos

- WildFly protege `/app/*` y `/api/*` con `elytron-oidc-client`.
- `principal-attribute=sub` y `LOGIXONE_OIDC_PROVIDER_URL` coinciden con el proveedor.
- el usuario local, la membresía, la empresa, el rol y las concesiones se crearon por
  un caso de uso autorizado o por el bootstrap one-shot;
- la empresa está activa y conserva exactamente una personalización efectiva;
- el plugin funcional está presente, compatible y habilitado para esa empresa.

## Flujo obligatorio

1. Inyectar `TrustedWebAccess` en la frontera web.
2. Usar `current()` solo para presentar selección o contexto; no construir
   `AuthenticatedCompanyContext` desde un parámetro.
3. Para elegir empresa, pasar el UUID candidato a `selectCompany`. El servicio lo
   contrasta con las membresías y el estado operacional actuales.
4. Antes de cada acción de un plugin llamar
   `requireAuthorization(PluginId, ContributionId)` con IDs constantes publicados
   por el contrato del plugin.
5. Usar el `AuthenticatedCompanyContext` devuelto para el caso de uso. No aceptar un
   segundo `CompanyId` desde el request.
6. Al cambiar empresa llamar siempre al método de selección, que limpia la referencia
   anterior antes de validar la nueva.
7. Para logout navegar a `/app/logout`; WildFly coordina la salida OIDC conforme a
   [ADR-0008](../adr/0008-logout-oidc-estabilidad-preview-wildfly.md).

El endpoint `GET /api/company-context` permite comprobar únicamente que existe un
contexto local actual. Devuelve `204` y deliberadamente no revela actor, empresa,
roles ni permisos. No reemplaza la guarda funcional.

## Estado de sesión permitido

`TrustedCompanySession` guarda `AppUserId`, `CompanyId` y revisión. Está prohibido
agregar token, cookie, issuer, subject, claims, roles, permisos, menú, pantalla
compuesta o datos personales. La referencia se revalida en cada operación.

Una revocación de usuario, membresía, empresa, rol, concesión o plugin produce efecto
en la siguiente operación. Si deja inválido el contexto, la referencia se limpia.

## Respuestas y diagnóstico

| Situación pública | Respuesta | Diagnóstico interno |
|---|---|---|
| falta principal validado | `401 {"error":"unauthorized"}` | autenticación OIDC ausente/inválida |
| usuario, membresía o empresa no autorizados | `403 {"error":"forbidden"}` | código estable en auditoría |
| plugin o permiso inefectivo | `403 {"error":"forbidden"}` | plugin, permiso y código en auditoría |
| contexto correcto | `204` en el probe | evento permitido con correlación |

Las respuestas llevan `Cache-Control: no-store`. No mostrar el código interno en UI,
no registrar el valor de un token/cookie y no copiar claims completos al diagnóstico.
Buscar en logs `event=trusted_access` y correlacionar por `correlation_id` generado en
el servidor.

## Validación ejecutada

`J11-S3-08` ejecutó la matriz de la
[historia J11-S3-05](../sprints/sprint-03/J11-S3-05-contexto-confiable-autorizacion.md)
con pruebas unitarias de revocación, PostgreSQL/JTA, WildFly, Keycloak y Playwright.
Se comprobaron selección entre empresas autorizadas, rechazo de una empresa
inyectada, denegación sin membresía, relectura de membresía/rol/plugin/permiso y
logout sin reutilización de sesión. La validación independiente de la guía continúa
siendo el gate de cierre pendiente.

## Reversión

El cambio es aditivo y no introduce migración. Para volver a una imagen anterior se
promueve su digest compatible. No borrar V3, usuarios, membresías, roles, concesiones
ni volúmenes. Las sesiones HTTP de la candidata deben invalidarse al cambiar de
versión para no conservar referencias creadas por otro baseline.
