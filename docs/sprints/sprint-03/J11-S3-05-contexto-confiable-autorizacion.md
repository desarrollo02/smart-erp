# J11-S3-05 — Contexto confiable, autorización y auditoría

- Estado: Completada
- Dependencia: `J11-S3-04` implementada para la candidata

## Objetivo

Transformar una identidad OIDC validada en actor local y contexto empresarial confiable, aplicando autorización del servidor y auditoría con actor real en cada operación protegida.

## Alcance

- adaptación de principal validado a `ExternalIdentity`;
- resolución de usuario local activo;
- selección server-side para cero, una y varias membresías;
- almacenamiento mínimo de empresa seleccionada en sesión HTTP;
- revalidación de usuario, membresía y empresa en cada operación;
- intersección de roles/permisos con plugins efectivos;
- extensión de guardas de aplicación y auditoría;
- respuestas web `401`/`403` y diagnósticos seguros;
- invalidación del contexto al cambiar empresa, revocar acceso o cerrar sesión.

## Fuera de alcance

- aceptar empresa desde header, query, JSON, cookie o campo oculto como autoridad;
- autorización únicamente visual;
- exponer claims o diagnósticos internos al navegador;
- endpoint administrativo completo;
- almacenar access/refresh tokens en tablas o JavaScript.

## Criterios de aceptación

- **CA-01:** solo un principal OIDC ya validado puede originar un actor autenticado.
- **CA-02:** usuario inexistente, inactivo o ambiguo recibe denegación genérica.
- **CA-03:** cero membresías no revela nombres ni IDs de empresas.
- **CA-04:** una membresía válida puede seleccionarse automáticamente.
- **CA-05:** varias membresías requieren elección entre opciones autorizadas.
- **CA-06:** alterar el `CompanyId` enviado por el navegador no concede acceso.
- **CA-07:** cada operación revalida membresía, empresa operacional, plugin efectivo y permiso.
- **CA-08:** cambiar empresa elimina cualquier vista, menú o pantalla calculada para la anterior.
- **CA-09:** revocar membresía o rol invalida la autorización sin esperar un nuevo login completo.
- **CA-10:** desactivar un plugin vuelve inefectivos sus permisos aunque persista la concesión.
- **CA-11:** auditoría registra `AppUserId`, empresa, plugin, operación, resultado y correlación.
- **CA-12:** auditoría y logs excluyen tokens, cookies, claims completos y datos personales innecesarios.
- **CA-13:** fallos distinguen `401` de `403` sin facilitar enumeración.
- **CA-14:** la matriz de aislamiento, revocación y cambio de empresa queda definida para G2/G4.

## Gates

- G1: candidata integrable con contexto confiable.
- G2/G4 diferidos: políticas, runtime de sesión y seguridad negativa en `J11-S3-08`.
- G0 documental inmediato.

## Estado provisional aplicado

Se usó `Implementada pendiente de validación` hasta superar G2 y G4.

## Resultado implementado

La frontera web convierte exclusivamente un `Principal` que WildFly marque con
`authType=OIDC`. El issuer procede de la misma variable
`LOGIXONE_OIDC_PROVIDER_URL` usada por `elytron-oidc-client` y el nombre del
principal representa `sub`, conforme a `principal-attribute=sub`. No se leen
headers, cookies, parámetros, access tokens, refresh tokens ni claims completos.

`TrustedAccessService` y su adaptador JTA vuelven a consultar en cada uso:

1. usuario local por `(issuer, subject)` y estado activo;
2. membresías activas del usuario;
3. estado operacional de cada empresa;
4. plugins físicos, compatibles, habilitados y efectivos para la empresa;
5. roles, asignaciones y permisos vigentes;
6. pertenencia del permiso requerido al plugin efectivo requerido.

La sesión HTTP guarda solamente `AppUserId`, `CompanyId` y una revisión local. Esa
referencia es histórica, no una prueba de acceso. No se conservan roles, permisos,
menús, pantallas ni claims; por eso una revocación se observa en la siguiente
operación protegida. Cambiar empresa limpia primero la referencia anterior y solo
vincula la nueva después de validarla en el servidor.

El recurso mínimo `GET /api/company-context` responde `204` únicamente cuando el
principal sigue resolviendo un usuario y una empresa válidos. Las denegaciones
públicas se reducen a `401 {"error":"unauthorized"}` o
`403 {"error":"forbidden"}` con `Cache-Control: no-store`. Los códigos internos
solo llegan a la auditoría estructurada.

La guarda `TrustedWebAccess.requireAuthorization(pluginId, permissionId)` queda
disponible para el shell y los endpoints funcionales posteriores. Ocultar una
opción visual nunca sustituirá esta llamada.

## Matriz acumulada para J11-S3-08

| Escenario | Resultado esperado |
|---|---|
| sin principal OIDC validado | `401` genérico; sin contexto de empresa |
| identidad sin usuario local o usuario inactivo | `403` genérico; sin enumeración |
| usuario sin membresías activas | `403`; no expone IDs de empresas |
| una membresía y empresa operacional | selección automática y referencia mínima en sesión |
| varias membresías operacionales | `SELECTION_REQUIRED` con solo IDs autorizados |
| `CompanyId` ajeno, alterado o mal formado | `403`; no cambia el contexto |
| referencia de sesión de otro actor | `403` y limpieza de sesión |
| membresía o empresa revocada durante la sesión | siguiente operación `403` y limpieza |
| rol o concesión revocada durante la sesión | siguiente operación funcional `403` |
| plugin desactivado con concesión histórica persistente | plugin y permiso inefectivos; `403` |
| permiso perteneciente a otro plugin | `403` aunque exista la concesión textual |
| cambio válido de empresa | invalida referencia anterior antes de vincular la nueva |
| logout OIDC | sesión HTTP invalidada por WildFly; sin referencia reutilizable |
| acceso permitido o denegado | evento con actor local cuando existe, empresa, plugin/permiso cuando aplican, resultado y correlación del servidor |
| inspección de logs | ausencia de token, cookie, issuer/subject, claims completos y datos personales innecesarios |

La matriz todavía no se ejecutó. Será cubierta con JUnit/ArchUnit, PostgreSQL/JTA,
WildFly/Keycloak, REST Assured y navegador según corresponda en `J11-S3-08`.

## Validación candidata ejecutada

Con Java 21 se empaquetó primero `web-shell` y después el corte integrado:

```powershell
.\mvnw.cmd -B -DskipTests -pl web-shell -am package
.\mvnw.cmd -B -DskipTests `
  -pl kernel-infrastructure-jakarta,web-shell,distribution/logixone-war `
  -am package
.\mvnw.cmd -B -DskipTests -Pwith-screen-customization-plugins `
  -pl distribution/logixone-war -am package
```

Los tres comandos finalizaron con `BUILD SUCCESS`; el segundo ensambló nueve proyectos
y el tercero doce, incluida la composición funcional más personalizaciones A/B.
Maven informó `Tests are skipped` en todos los módulos. La inspección del WAR confirmó
`kernel-api`, `kernel-application`, `kernel-infrastructure-jakarta`, `web-shell` y
`WEB-INF/web.xml`; el perfil de demo agregó exactamente `reference-plugin`,
`reference-customization-a` y `reference-customization-b`. La búsqueda estática no encontró lectura de headers, parámetros o
cookies como fuente de empresa/autorización ni introdujo dependencias `javax.*`.

G2 y G4 continúan pendientes. Esta historia no satisface todavía la Definition of
Done y no autoriza promoción ni despliegue productivo.

## Validación acumulada

`J11-S3-08` validó políticas de revocación, relectura de estado, empresa manipulada,
cero/una/múltiples membresías, cambio de contexto y respuestas cerradas. G2/G4
quedaron verdes. Evidencia:
[gates G2–G6](../../evidence/J11-S3-08-validacion-demo-cierre.md).

## Siguiente paso

La historia está completada. El Sprint continúa con G7 de `J11-S3-08`.
