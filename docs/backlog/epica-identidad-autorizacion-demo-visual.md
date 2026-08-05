# Épica — Identidad, autorización y primera demo visual

- Estado: Implementación y gates técnicos de Sprint 3 verdes; validación independiente G7 de la guía candidata pendiente
- Prioridad: Alta
- Inicio: Sprint 3
- Decisión base: [ADR-0006](../adr/0006-identidad-oidc-membresia-autorizacion.md)

## Propósito

Entregar una primera versión navegable que demuestre autenticación real, aislamiento empresarial, autorización del servidor, navegación por plugins efectivos y personalización visual distinta por empresa, sin presentar todavía un dominio ERP productivo.

## Resultados de producto

- una persona inicia y termina sesión mediante Keycloak/OIDC;
- el ERP vincula la identidad externa con un usuario local estable;
- el usuario solo puede seleccionar empresas donde tiene membresía activa;
- roles empresariales conceden permisos publicados por el kernel y los plugins;
- el contexto empresarial se deriva en el servidor y se revalida en cada operación;
- el shell muestra únicamente menús permitidos y plugins efectivos;
- una pantalla neutral se renderiza con diferencias visibles entre dos personalizaciones empresariales;
- la demo puede levantarse de forma reproducible sin secretos versionados.

## Invariantes

1. Keycloak autentica; el ERP decide membresías, roles y permisos funcionales.
2. La identidad estable es `(issuer, subject)`; correo y username no son claves.
3. Un `CompanyId` aportado por el navegador nunca constituye autorización.
4. Toda operación combina usuario, membresía, empresa, plugin y permiso efectivos.
5. El último plugin aplicado continúa siendo la personalización exclusiva de la empresa.
6. La UI no reemplaza las guardas de aplicación.
7. Tokens, cookies, passwords y secretos no se registran ni se versionan.
8. La demo muestra capacidades existentes y no simula facturación, ventas o inventario terminados.

## Fuera de alcance

- almacenar o recuperar contraseñas dentro de Logixone;
- un realm separado por empresa;
- SCIM, autoservicio de registro, recuperación de contraseña o MFA administrados por el ERP;
- administración completa de usuarios y roles mediante API pública;
- una SPA o aplicación móvil;
- primer dominio ERP productivo;
- integración real con LDAP, Active Directory u otros IdP, aunque Keycloak pueda federarlos;
- autorización basada únicamente en roles o grupos incluidos en el token.

## Hitos

1. modelo neutral de identidad, membresía, roles y permisos;
2. esquema `core` V3 y persistencia transaccional;
3. Keycloak y WildFly OIDC declarados como infraestructura;
4. contexto empresarial confiable y auditoría con actor real;
5. shell Jakarta Faces con selector y navegación efectiva;
6. pantalla compuesta y personalización A/B;
7. validación acumulada, Playwright, guía aceptada y PDF de cierre.

## Criterios de aceptación de la épica

- **CE-01:** un usuario sin sesión no accede al shell protegido.
- **CE-02:** issuer o audience inválidos no crean una identidad local.
- **CE-03:** un usuario sin membresía no puede enumerar empresas.
- **CE-04:** un usuario multiempresa solo ve y selecciona sus membresías vigentes.
- **CE-05:** cambiar de empresa recalcula contexto, permisos, menús y pantallas sin conservar datos de la anterior.
- **CE-06:** una revocación de membresía o rol se refleja sin confiar indefinidamente en claims antiguos.
- **CE-07:** un plugin desactivado no aporta permiso, menú ni pantalla aunque existan concesiones persistidas.
- **CE-08:** las personalizaciones A y B producen resultados visuales distintos y aislados.
- **CE-09:** la distribución no empaqueta un adaptador Java propietario de Keycloak.
- **CE-10:** Compose, migraciones, OIDC, seguridad negativa, Playwright y persistencia quedan verificados antes de aceptar la demo.
- **CE-11:** el recorrido independiente de la guía candidata queda completado y la edición puede pasar a `1.0`.
- **CE-12:** el Sprint se cierra con el PDF obligatorio regenerado y revisado.

## Política temporal de validación

Por decisión del responsable de producto del 2026-07-28, las pruebas automatizadas se ejecutarán después de terminar la candidata visual. Las historias de implementación anteriores al cierre no se consideran terminadas: usarán el estado `Implementada pendiente de validación` hasta superar el gate acumulado de `J11-S3-08`.
