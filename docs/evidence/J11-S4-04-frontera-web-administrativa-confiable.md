# Evidencia de J11-S4-04 — Frontera web administrativa confiable

- Fecha: 2026-07-28
- Estado: implementada pendiente de pruebas
- Historia: [J11-S4-04](../sprints/sprint-04/J11-S4-04-frontera-web-administrativa-confiable.md)

## Resultado

Se agregó una frontera administrativa global separada del contexto empresarial:

- `SystemAuthorityAccessService` resuelve la identidad externa contra `AppUser`
  local y vuelve a calcular permisos globales actuales;
- `SystemAuthorityAccessPort` permite exigir cualquier permiso global o uno exacto;
- `TransactionalSystemAuthorityUseCases` materializa el puerto en una lectura JTA;
- `AdminAuthorizationFilter` protege solicitudes `/admin/*` y
  `/faces/admin/*` antes de ejecutar Faces;
- `TrustedAdminWebAccess` reduce toda denegación a `401/403` genérico;
- `AdminViewBean` proyecta únicamente las áreas permitidas y no accede a JPA;
- `admin/index.xhtml` y `admin.css` aportan la primera landing Material Design 3
  responsive del Sprint 4.

No se agregó un recurso REST administrativo, endpoint de bootstrap ni mutación de
datos. Las tarjetas informan áreas autorizadas, pero todavía no enlazan a operaciones
que pertenecen a `J11-S4-05` y posteriores.

## Autenticación y autorización

`WEB-INF/web.xml` incluye `/admin/*` en el mismo constraint OIDC que protege el
workspace. La variante `/faces/admin/*` ya estaba autenticada por `/faces/*` y ahora
también pasa por el filtro global, evitando una ruta alternativa sin la guarda.

El filtro exige al menos un permiso conocido. Cada futura pantalla o comando puede
usar `TrustedAdminWebAccess.require(SystemPermission)` para releer y auditar el
permiso exacto. El contexto se reutiliza solamente dentro del request entre filtro y
landing; no se guarda en sesión. Una revocación deberá afectar la siguiente
petición, aun cuando la sesión OIDC continúe activa.

La decisión no usa `CompanyId`, membresías, roles empresariales, headers ni claims
de rol del proveedor. `ValidatedOidcPrincipal` continúa aceptando únicamente un
principal que WildFly marcó con `authType=OIDC`.

## Respuesta y auditoría seguras

Identidad local ausente, usuario inactivo, permiso ausente o contexto inconsistente
producen el mismo documento “Acceso no disponible”. La respuesta usa `no-store`,
`nosniff`, `no-referrer` y una CSP cerrada. Un fallo inesperado de la guarda devuelve
`503` genérico y no absorbe excepciones producidas después por Faces.

`StructuredSystemAuthorityAccessAudit` registra resultado, `AppUserId` cuando
existe, permiso requerido, diagnóstico interno, correlación generada por el servidor
e instante. No registra issuer, subject, token, cookie, nombre, email ni empresa.

## Compilación y empaquetado

Compilaciones principales, siempre con tests omitidos:

```powershell
.\mvnw.cmd -pl web-shell -am "-Dmaven.test.skip=true" compile
.\mvnw.cmd -pl kernel-infrastructure-jakarta -am "-Dmaven.test.skip=true" compile
```

Resultados:

- web y dependencias: `BUILD SUCCESS`, seis de seis módulos; 113 fuentes de
  aplicación y 23 de web;
- infraestructura y dependencias: `BUILD SUCCESS`, siete de siete módulos; 52
  fuentes de infraestructura.

La candidata visual se empaquetó con:

```powershell
.\mvnw.cmd -pl distribution/logixone-war -am `
  -Pwith-screen-customization-plugins `
  "-Dmaven.test.skip=true" package
```

Resultado: `BUILD SUCCESS`, doce de doce módulos. El WAR contiene la landing y CSS
administrativos, web/infraestructura y los plugins de referencia A/B. SHA-256 local:
`C9EE3166734C93D42D7E8A2E679793FD71F7CDCB0B6B36EA9AD014319296DCD5`.

## Revisión estática

- `web.xml` y `admin/index.xhtml`: XML bien formado;
- imports Jakarta/Hibernate/PostgreSQL en módulos puros: `0`;
- acceso JPA o `EntityManager` desde `web-shell`: `0`;
- archivos UTF-8 inválidos: `0`;
- enlaces Markdown locales rotos: `0`;
- V4 permaneció inmutable con SHA-256
  `8C35EF550FFC0949915758389781B25F9243A1E49AEC8AC2AFC16F26CB46B67A`;
- el WAR contiene `admin/index.xhtml`, `admin.css`, web, infraestructura, plugin de
  referencia y personalizaciones A/B.

## Pruebas pendientes

No se ejecutaron ni compilaron JUnit, ArchUnit, Servlet, OIDC runtime,
PostgreSQL/JPA/JTA, Docker/Compose, revocación en sesión ni Playwright. Permanecen
pendientes CA-01 a CA-10 y gates G2–G7.

No se observó una prueba fallida. La compilación y el empaquetado no convierten esta
historia en verde ni certifican que la landing ya pueda demostrarse en runtime.

## Siguiente paso

`J11-S4-05`: agregar pantallas y acciones para empresas, catálogo/activación de
plugins y personalización exclusiva, reautorizando cada comando con su permiso
global exacto.
