# Shell Jakarta Faces y navegación autorizada

- Versión: 8
- Fecha: 2026-07-28
- Estado: shell empresarial validado en `J11-S3-08`; administración hasta `J11-S4-07` implementada pendiente de pruebas
- Aplica desde: candidata visual de Sprint 3 y candidata administrativa de Sprint 4

## Propósito

Explicar cómo se compone y se extiende el shell sin convertir la interfaz en fuente de
autorización ni acoplarla a vistas internas de plugins.

## Decisión visual

La primera candidata usa Jakarta Faces 4.1 y CSS propio. No incluye PrimeFaces u otra
biblioteca. Jakarta Faces forma parte de Jakarta EE 11 y WildFly lo proporciona; el
WAR no empaqueta una implementación duplicada.

Material Design 3 es el sistema de diseño, no una dependencia incorporada. Los roles
de color, forma, elevación y estado se declaran como tokens `--md-sys-*` en
`shell.css` y los componentes Faces consumen esos roles. No cargar fuentes, iconos,
scripts, Web Components o estilos desde un CDN.

El shell es propietario del tema y los renderers. Un plugin publica contratos
neutrales y una personalización usa overlays tipados; ninguno inyecta XHTML, CSS o
JavaScript global. Consultar
[ADR-0007](../adr/0007-material-design-responsive-sobre-jsf.md).

## Estructura

| Recurso | Responsabilidad |
|---|---|
| `ShellViewBean` | modelo request-scoped y guarda de ruta directa |
| `ShellTextCatalog` | traducción cerrada de claves públicas conocidas |
| `ShellScreenRegistry` | rutas, tipos, regiones, textos y fragmentos con renderer permitido |
| `ShellScreenView` | resultado request-scoped seguro para Jakarta Faces |
| `TrustedNavigationView` | actor de presentación, empresas y menús autorizados |
| `app/index.xhtml` | selección, sesión, contexto y workspace |
| `app/view.xhtml` | destino central de rutas públicas autorizadas |
| `shell.css` | estilos responsive y accesibles sin recursos externos |
| `SystemAuthorityAccessPort` | resolución neutral y actual de autoridad global |
| `AdminAuthorizationFilter` | guarda server-side de `/admin/*` y `/faces/admin/*` |
| `TrustedAdminWebAccess` | frontera request-scoped para permiso global exacto |
| `AdminViewBean` | proyección delgada de áreas globales permitidas |
| `CompanyAdministrationPort` | consultas y comandos neutrales para empresas/plugins |
| `CompanyAdminViewBean` | alta y estado de empresas sin acceso JPA |
| `PluginAdminViewBean` | catálogo, activaciones y personalización sin reglas de dominio |
| `BusinessSecurityAdministrationPort` | consultas y comandos neutrales de seguridad por empresa |
| `BusinessSecurityAdminViewBean` | usuarios, membresías, roles y permisos empresariales |
| `SystemAuthorityAdministrationPort` | consultas y comandos neutrales de autoridad global |
| `SystemAuthorityAdminViewBean` | roles, asignaciones y permisos de la instancia |
| `AuditQueryPort` | consulta neutral, paginada y con filtros cerrados |
| `AuditAdminViewBean` | adaptación read-only de auditoría para Jakarta Faces |
| `admin/index.xhtml` | landing administrativa y navegación permitida |
| `admin/companies.xhtml` | alta, activación e inactivación de empresas |
| `admin/plugins.xhtml` | catálogo físico y composición por empresa |
| `admin/security.xhtml` | usuarios y autorización propia de una empresa |
| `admin/system-authority.xhtml` | autoridad global sin contexto empresarial |
| `admin/audit.xhtml` | consulta técnica append-only desde V5 |
| `admin.css` | extensión Material 3 responsive del panel administrativo |

Las vistas y recursos pertenecen a `web-shell` bajo `META-INF/resources` como única
fuente mantenida. Al construir, `distribution` los copia explícitamente a la raíz
del WAR mediante `maven-war-plugin`; las clases permanecen en `web-shell.jar`. Así
el runtime no depende de que WildFly exponga XHTML o CSS desde un JAR anidado.

## Flujo de una petición

1. WildFly exige OIDC para `/app/*`.
2. `ShellViewBean` solicita el contexto actual a `TrustedWebAccess`.
3. Cero empresas produce denegación genérica; varias muestran selección; una queda
   seleccionada automáticamente.
4. `TrustedNavigationView` se calcula desde membresías, empresas operacionales,
   plugins efectivos y permisos vigentes.
5. El shell traduce la `labelKey` pública y crea un enlace al destino central.
6. `view.xhtml` compara el parámetro `route` exactamente con el menú actual.
7. `ShellScreenRegistry` resuelve el par exacto plugin/ruta a un `ScreenId` público.
8. `TrustedScreenAccess` vuelve a validar plugin/permiso, recompone la empresa y
   exige la pantalla exacta.
9. El registro acepta sólo tipos, regiones, textos, slots y fragmentos conocidos y
   produce un `ShellScreenView` nuevo para esa petición.

El bean `RequestScoped` resuelve en `@PostConstruct` el contexto base necesario para
que JSF pueda decodificar componentes cuyo `rendered` depende del estado. El
`f:viewParam` invalida esa preparación y `preRenderView` completa después la guarda
de ruta. Los botones vuelven a validar empresa, plugin y permiso en servicios del
servidor y redirigen a una solicitud nueva cuando cambian el contexto. Esto evita
estado de vista serializado y no oculta un componente antes de que procese su POST.

No aceptar una ruta que no esté en la proyección, no formar el `PluginId` desde un
parámetro y no considerar que un menú oculto protege la operación.

### Flujo administrativo

1. WildFly exige OIDC para `/admin/*` y `/faces/*`.
2. El filtro cubre la ruta directa y la variante `/faces/admin/*`.
3. `ValidatedOidcPrincipal` acepta solo `authType=OIDC` y construye la identidad
   neutral desde issuer configurado y subject validado por el contenedor.
4. `SystemAuthorityAccessService` vuelve a buscar usuario, roles, asignaciones y
   permisos; no consulta empresa ni confía en roles del proveedor.
5. Sin al menos un permiso conocido responde genéricamente y audita el diagnóstico
   interno con correlación generada por el servidor.
6. La landing usa el contexto de ese request y muestra solo áreas permitidas.
7. `companies.xhtml`, `plugins.xhtml`, `security.xhtml`,
   `system-authority.xhtml` y `audit.xhtml` exigen respectivamente `kernel.company.manage`,
   `kernel.plugin.manage`, `kernel.security.manage` y
   `kernel.system_administration.manage` y `kernel.audit.view` tanto en filtro como en su bean.
8. Cada acción llama `require(permission)` otra vez. Reemplazar personalización
   exige `kernel.company.manage`, aun desde la pantalla de plugins.
9. El bean parsea IDs/versiones como candidatos; `CompanyAdministrationPort` relee
   el estado dentro de la transacción y devuelve un resultado cerrado.
10. Inactivar empresa, deshabilitar plugin o reemplazar personalización muestra una
    confirmación explícita y nunca elimina datos.
11. Seguridad empresarial usa siempre `CompanyId`; autoridad global usa tipos
    distintos que no aceptan contexto empresarial.
12. Inactivar usuarios/roles/membresías, desasignar o revocar exige confirmación;
    el servidor protege además al último administrador global efectivo.
13. Auditoría usa páginas y filtros cerrados, muestra únicamente IDs técnicos desde
    V5 y no permite editar, borrar ni exportar.
14. El filtro agrega cabeceras `no-store`, anti-frame, `nosniff`, referencia y CSP a
    toda respuesta administrativa permitida o denegada.

No conservar autoridad global en sesión. Una revocación debe afectar la siguiente
petición aun cuando la sesión OIDC siga activa.

## Renderer de pantalla y variantes A/B

El contrato neutral inicial admite `DISPLAY_TEXT`, `TEXT_INPUT` y `ACTION`.
`ShellScreenRegistry` conoce solamente `reference_plugin:dashboard` para la ruta
`/reference` y los fragmentos públicos `reference_custom_a:tax_notice` y
`reference_custom_b:company_notice`.

- A renombra/reordena `summary`, agrega ayuda, lo vuelve requerido e inserta su
  tarjeta tributaria.
- B oculta `summary`, deshabilita `refresh` e inserta su aviso empresarial.
- desconocer tipo, región, texto, slot o fragmento rechaza toda la vista;
- cambiar de empresa genera otra composición y otro modelo request-scoped;
- `HIDE`, `DISABLE` y `REQUIRE` afectan presentación, nunca la guarda del servidor.

Para registrar una pantalla nueva se agregan explícitamente su par plugin/ruta, ID,
textos, regiones, slots, fragmentos y componentes permitidos. No usar reflexión,
convenciones de nombres, includes, factories aportadas por plugins ni fallbacks que
muestren la pantalla estándar cuando el overlay fue rechazado.

## Floorplan para entidades ERP

Desde [ADR-0018](../adr/0018-floorplan-erp-directorio-alta-ficha.md), una pantalla
de entidad con búsqueda y mantenimiento no debe acumular todos sus formularios en
una única página. El renderer del shell separa:

- `directory`: filtros y un conjunto de resultados;
- `create`: datos mínimos del alta;
- `detail`: resumen y pestañas por tarea.

El modo y la pestaña pueden viajar en query string porque no conceden autoridad. La
ruta pública, el recurso, el plugin, la empresa y el permiso se vuelven a resolver
en el servidor. En expandido se mantiene navegación lateral y tabla; en medio y
compacto se usa menú colapsable y lista adaptable. La pantalla operativa no muestra
`ScreenId`, slots, versión optimista ni explicaciones transaccionales.

Agregar otra entidad requiere reutilizar este floorplan o documentar por qué necesita
un patrón distinto. No copiar XHTML de `business_partners`: registrar el contrato
neutral y ampliar el renderer cerrado del shell con pruebas de los tres modos y los
tres rangos responsive.

## Estados públicos

- preparación de sesión;
- selección empresarial;
- workspace con menú;
- workspace sin funciones autorizadas;
- acceso no habilitado;
- fallo temporal genérico;
- ruta no disponible.

Ningún estado muestra stacktrace, SQL, token, claim, subject, cookie o un UUID ajeno.
Los errores inesperados registran sólo el tipo de excepción en el evento del shell;
los detalles de autorización permanecen en `event=trusted_access`.

## Accesibilidad y responsive

- idioma español y títulos por vista;
- skip link al contenido;
- orden semántico de encabezado, navegación, contenido y pie;
- labels de formularios y mensajes de validación;
- foco visible;
- rango compacto `0–599px`: contenido apilado y acciones alcanzables;
- rango medio `600–839px`: composición para tableta y densidad intermedia;
- rango expandido desde `840px`: contenido limitado para conservar legibilidad;
- sin overflow horizontal de página para contenido normal;
- `prefers-reduced-motion` deshabilita transiciones.

Playwright usa `375px`, `720px` y `1280px` como muestras de compacto, medio y
expandido. La matriz ejecutada en `J11-S3-08` comprobó que
`document.documentElement.scrollWidth <= window.innerWidth`, generó capturas A/B y
recorrió selector, cambio de empresa, ruta protegida, denegación y logout. Los
límites `599px`, `600px`, `839px` y `840px`, teclado, foco y reduced motion se
mantienen como criterios obligatorios para cada pantalla nueva y deben ampliarse en
la suite cuando exista una interacción que los ejercite.

## Agregar una contribución de menú

El plugin funcional declara `MenuContribution` con ID, clave de texto, ruta pública y
permiso opcional. El shell necesita incorporar una traducción segura para la clave en
`ShellTextCatalog`; una clave desconocida muestra el fallback genérico y no cambia la
autorización.

La ruta debe seguir siendo un identificador público de navegación. No debe contener
una ubicación XHTML, expresión EL, bean, clase Java, script o CSS del plugin.

## Arranque y acceso

La URL pública por defecto es
`http://localhost:8080/logixone/faces/app/index.xhtml`. En la composición local
validada se usa el puerto alternativo `18080` para evitar colisiones. El servicio se
publica exclusivamente en `127.0.0.1`; `localhost` es el nombre público local usado
por el retorno OIDC.

La landing administrativa se solicita en
`http://localhost:8080/logixone/faces/admin/index.xhtml`. Esta URL todavía no fue
validada en runtime. Una identidad empresarial normal debe recibir denegación
genérica; una identidad con al menos un permiso global efectivo debe ver únicamente
las áreas concedidas.

Las nuevas rutas son
`http://localhost:8080/logixone/faces/admin/companies.xhtml` y
`http://localhost:8080/logixone/faces/admin/plugins.xhtml`, además de
`http://localhost:8080/logixone/faces/admin/security.xhtml` y
`http://localhost:8080/logixone/faces/admin/system-authority.xhtml`, más
`http://localhost:8080/logixone/faces/admin/audit.xhtml`. Las cinco
están dentro del WAR hasta `J11-S4-07`, pero su recorrido con OIDC/PostgreSQL, las
confirmaciones y los breakpoints 375/720/1280 siguen pendientes para `J11-S4-08`.
No anunciar todavía una demo administrativa validada.

La demo está observable cuando PostgreSQL, migrator, Keycloak y aplicación están en
su estado esperado y existen las identidades/datos ficticios aprovisionados. La
matriz `J11-S3-08` recorrió una identidad sin empresa, otra con una empresa y otra con
dos empresas; también comprobó las personalizaciones A/B. Las credenciales se leen
del archivo local ignorado y no se documentan.

## Reversión

V5 es aditiva y append-only. Revertir la aplicación requiere promover una imagen
anterior compatible, recrearla e invalidar sesiones; no borrar `core.audit_event`,
V5, PostgreSQL, Keycloak ni sus volúmenes. Un artefacto anterior que exige V3/V4
puede ignorar la tabla adicional.
