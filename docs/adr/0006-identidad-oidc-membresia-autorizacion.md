# ADR-0006 — Identidad OIDC, membresía empresarial y autorización

- Estado: Aceptado
- Fecha: 2026-07-28
- Historia: `J11-S3-00`
- Reemplaza: ninguna decisión anterior

## Contexto

El cierre de Sprint 2 dejó empresas, activaciones, personalizaciones, permisos disponibles y pantallas neutrales, pero todavía no existe una identidad autenticada que pueda establecer de forma confiable el usuario ni la empresa activa. Exponer selección empresarial, administración o una UI funcional antes de resolver esa frontera permitiría suplantación mediante headers, parámetros o cookies aportados por el cliente.

El producto confirmó el 2026-07-28 que la primera demo visual debe usar Keycloak como proveedor OpenID Connect, el soporte OIDC nativo de WildFly y autorización empresarial propiedad del ERP. La decisión debe conservar independencia respecto de internos del proveedor, aislamiento por empresa, denegación segura y una ruta futura hacia proveedores corporativos federados.

## Decisión

### 1. Proveedor e integración

Keycloak será el proveedor OIDC externo inicial. No se empaqueta dentro del WAR ni de la imagen de aplicación. Para desarrollo y demo se declarará como servicio de infraestructura reproducible; en otros ambientes podrá ser un servicio administrado que cumpla el mismo contrato OIDC.

WildFly protegerá el despliegue mediante su subsistema nativo `elytron-oidc-client`. La aplicación no incorporará adaptadores Java propietarios de Keycloak. La configuración utilizará el `provider-url` estándar, el identificador del cliente y credenciales suministradas externamente.

La línea inicial evaluada para la demo será Keycloak 26.7.x. La historia de infraestructura deberá fijar una versión exacta y el digest de la imagen después de verificar compatibilidad; una etiqueta móvil como `latest` nunca será identidad promovible.

Se usará un único realm de Logixone y un cliente confidencial para el shell web. Una empresa de negocio no equivale a un realm. Keycloak puede federar posteriormente otros OIDC, SAML, LDAP o Active Directory sin cambiar el identificador empresarial ni trasladar la autorización funcional fuera del ERP.

### 2. Flujo web y límites del protocolo

El shell Jakarta Faces usará Authorization Code Flow mediante redirección del servidor. No se implementarán Implicit Flow, Resource Owner Password Credentials, formularios locales de contraseña ni almacenamiento de tokens en `localStorage` o `sessionStorage`.

La configuración deberá validar firma, emisor, audiencia, expiración y estado de la sesión conforme a las capacidades de WildFly. En producción se exige HTTPS y verificación normal de hostname y certificados; `allow-any-hostname` y `disable-trust-manager` quedan prohibidos fuera de ambientes efímeros expresamente documentados.

Las páginas web no autenticadas redirigen al proveedor. Los recursos REST protegidos responden `401` sin redirección y `403` cuando existe identidad pero falta autorización. Liveness permanece público; readiness conserva el contrato operativo documentado y no se convierte en un endpoint de diagnóstico de identidad.

### 3. Identidad local estable

La clave externa estable de una persona es el par normalizado `(issuer, subject)` obtenido de un token validado. Correo, nombre visible, username y grupos son atributos mutables y nunca claves de seguridad.

El esquema `core` mantendrá un usuario de aplicación con identificador UUID propio, identidad externa única, estado local, versión optimista y marcas UTC. El ERP no guarda hashes de contraseña, respuestas de autenticación, refresh tokens ni secretos de Keycloak.

Una identidad autenticada que no tenga usuario local activo ni bootstrap autorizado recibe denegación genérica. No se autoaprovisionan permisos por el simple hecho de completar un login.

### 4. Membresía y selección empresarial

La membresía pertenece al ERP y relaciona usuario y empresa. Su clave lógica es `(app_user_id, company_id)` y conserva estado y versión. Una empresa inactiva o no operacional nunca se vuelve utilizable por tener una membresía activa.

Después del login:

1. cero membresías activas producen una pantalla de acceso no habilitado, sin enumerar empresas;
2. una membresía válida puede seleccionarse automáticamente;
3. varias membresías válidas muestran un selector con únicamente las empresas autorizadas;
4. cambiar de empresa invalida el contexto anterior y vuelve a calcular permisos, plugins, menús y pantallas;
5. cada petición revalida usuario, membresía, empresa y autorización antes de ejecutar aplicación.

La selección se conserva en sesión HTTP del servidor como referencia técnica y no como prueba autosuficiente. Un `CompanyId` recibido en header, query string, formulario, JSON, cookie o URL nunca otorga acceso. En los límites funcionales, `CompanyContext` solo se construye después de resolver la sesión autenticada y comprobar la membresía vigente.

### 5. Roles y permisos

Keycloak autentica y puede limitar quién ingresa al cliente, pero no será la fuente de roles funcionales del ERP. Las membresías, roles empresariales, asignaciones y concesiones de permisos se persisten en `core`.

Un rol funcional es propio de una empresa. Una membresía puede recibir varios roles y un rol puede conceder códigos de permiso publicados por el kernel o por plugins. La autorización efectiva exige simultáneamente:

1. identidad OIDC válida y usuario local activo;
2. membresía activa en la empresa seleccionada;
3. empresa activa y operacional;
4. plugin propietario físicamente presente, compatible y efectivo para esa empresa;
5. permiso concedido por al menos un rol vigente;
6. guarda de aplicación y validaciones de dominio satisfechas.

Desactivar o retirar un plugin hace inefectivos sus permisos, menús y pantallas, pero no elimina las concesiones históricas. Si el plugin compatible vuelve a ser efectivo, las concesiones se recalculan. Ocultar controles de UI nunca sustituye esta autorización del servidor.

La administración global del sistema será una autoridad separada de los roles empresariales. No se inferirá desde un rol de realm genérico ni desde pertenecer a todas las empresas.

### 6. Esquema `core` V3

La evolución inicial será aditiva y versionada mediante Flyway. V1 y V2 permanecen inmutables. V3 deberá representar, como mínimo:

- `core.app_user`: UUID local, `issuer`, `subject`, estado, datos de presentación mínimos, versión y marcas UTC;
- `core.company_membership`: usuario, empresa, estado, versión y marcas UTC;
- `core.security_role`: rol empresarial estable, nombre de presentación, estado, versión y marcas UTC;
- `core.role_permission`: relación entre rol y código de permiso;
- `core.membership_role`: relación entre membresía y rol de la misma empresa.

Las restricciones de unicidad, claves foráneas y consistencia empresarial se expresarán en PostgreSQL además de Java. No habrá relaciones JPA hacia entidades de plugins ni copias del catálogo físico. JPA continuará en modo `validate`.

### 7. Bootstrap inicial

La primera autoridad administrativa se crea mediante una operación one-shot, idempotente y declarada como infraestructura. Recibirá externamente la identidad OIDC exacta y las asignaciones iniciales después de migraciones. No contendrá contraseña, no se versionará con datos reales y no expondrá un endpoint anónimo de “primer administrador”.

El proceso debe fallar de forma segura si la empresa, personalización, identidad o rol no coinciden con la declaración. Una repetición idéntica no duplica filas; una repetición incompatible se rechaza y queda auditada.

Para la demo podrán existir identidades y empresas ficticias reproducibles, pero sus contraseñas y secretos se inyectarán por archivos locales ignorados. Ningún dato ficticio se presentará como configuración productiva.

### 8. Sesión, auditoría y datos sensibles

El login debe rotar el identificador de sesión. El logout local invalida la sesión y coordina el cierre OIDC cuando corresponda. Las acciones mutables del shell deberán protegerse contra CSRF mediante las capacidades de Jakarta Faces y controles adicionales si aparece un endpoint no Faces.

La auditoría sustituirá el actor `SYSTEM` o `TEST` por el `AppUserId` autenticado cuando la operación provenga de una persona. Registrará también empresa, correlación, plugin, operación, resultado y código estable cuando corresponda. No registrará tokens, cookies, secretos, passwords, claims completos ni datos personales innecesarios.

### 9. UI de la primera demo

El shell inicial será server-side con Jakarta Faces 4.1, tecnología incluida en el baseline Jakarta EE 11. Antes de agregar PrimeFaces u otra biblioteca visual, la historia de UI deberá documentar versión, licencia, necesidad y superficie de mantenimiento.

La demo debe mostrar capacidades reales:

- login y logout OIDC;
- usuario autenticado y empresa activa;
- selector cuando el usuario tenga más de una membresía;
- navegación calculada desde plugins efectivos y permisos concedidos;
- primera pantalla neutral renderizada desde `ScreenDefinition`;
- diferencia visible entre las personalizaciones de dos empresas;
- denegaciones reales para usuario, membresía, empresa, plugin o permiso inválidos.

No se simularán ventas, facturación, inventario u otro dominio ERP productivo. La pantalla de referencia demuestra composición, seguridad y personalización, no una función de negocio terminada.

### 10. Salud y fallos externos

Liveness nunca consulta Keycloak, PostgreSQL, catálogo ni sesión. Readiness verifica la configuración local necesaria y las dependencias ya definidas, pero no realizará una llamada síncrona a Keycloak en cada sondeo: una caída externa no debe provocar una cascada de reinicios.

Los fallos de discovery, claves, login o logout producirán denegación segura y observabilidad operativa sin exponer detalles al navegador. Si al arrancar no puede establecerse la configuración OIDC mínima requerida para proteger el despliegue, la aplicación no se considera lista.

## Alternativas consideradas

### Contraseñas locales en PostgreSQL

Se descarta porque duplicaría gestión de credenciales, recuperación, MFA y políticas de seguridad dentro del ERP.

### Roles y membresías empresariales administrados solamente en Keycloak

Se descarta porque acoplaría reglas de negocio y activación de plugins a claims externos, volvería obsoletos los tokens ante cambios operativos y complicaría el aislamiento y la auditoría transaccional por empresa.

### Un realm por empresa

Se descarta para el baseline porque confunde tenancy del ERP con aislamiento del proveedor, complica usuarios que trabajan en varias empresas y multiplica clientes, configuración y operación. Un futuro requisito regulatorio podría autorizarlo mediante otro ADR.

### Adaptador Java específico de Keycloak dentro del WAR

Se descarta porque WildFly 41 ya ofrece soporte OIDC nativo y el contrato estándar reduce acoplamiento al proveedor.

### Empresa confiada desde header o claim sin membresía local

Se descarta porque conocer o recibir un UUID no demuestra autorización vigente y no contempla estado operacional, plugins efectivos ni revocación local.

### Primer usuario autenticado convertido automáticamente en administrador

Se descarta porque una carrera de despliegue podría conceder privilegios irreversibles al actor equivocado.

### SPA con tokens en almacenamiento del navegador

Se descarta para la primera demo porque el shell server-side ya cubre navegación y personalización sin introducir una segunda arquitectura de sesión y exposición de tokens.

## Consecuencias

### Positivas

- las credenciales y MFA quedan fuera del ERP;
- el proveedor puede cambiar o federarse detrás de OIDC;
- membresías y permisos siguen el estado transaccional de empresas y plugins;
- una selección enviada por el cliente nunca sustituye autorización;
- la UI puede demostrar personalización real sin exponer endpoints administrativos anónimos;
- el WAR no incorpora adaptadores propietarios de Keycloak.

### Costes y riesgos aceptados

- la demo agrega un servicio externo, certificados, secretos y configuración de redirect URI;
- se necesita un bootstrap one-shot antes del primer uso;
- cambios de membresía o roles requieren invalidación o revalidación efectiva de sesión;
- la matriz de pruebas deberá cubrir navegador, proveedor, WildFly, PostgreSQL y dos empresas;
- una mala configuración de issuer, audiencia o proxy puede impedir login aunque la aplicación esté viva;
- mantener roles en el ERP exige UI o procedimiento administrativo posterior.

## Plan de verificación

Por decisión de producto, las pruebas automatizadas del Sprint 3 se acumularán y se ejecutarán después de terminar la candidata de demo visual. Hasta ese gate, las historias de código se registrarán como implementadas pendientes de validación y no como completadas.

El cierre deberá ejecutar, como mínimo:

- unitarias para identidad externa, membresía, roles, permisos y resolución de contexto;
- ArchUnit para independencia de dominio/aplicación y ausencia de adaptadores propietarios;
- Testcontainers PostgreSQL para V1→V2→V3, restricciones, concurrencia y rollback;
- pruebas runtime con WildFly y Keycloak para login, logout, sesión, issuer, audiencia y denegaciones;
- casos de cero, una y varias membresías;
- casos de revocación y cambio de empresa sin filtración cruzada;
- Playwright para shell, selector, navegación y personalizaciones A/B;
- Docker/Compose, secretos externos, health, persistencia y recreación;
- recorrido independiente de la guía `1.0-rc1` y regeneración del PDF obligatorio.

## Compatibilidad con decisiones anteriores

Este ADR especializa [ADR-0002](0002-arquitectura-plugins.md), [ADR-0003](0003-persistencia-migraciones.md), [ADR-0004](0004-docker-iac-promocion-digest.md) y [ADR-0005](0005-contexto-empresarial-activacion-personalizacion.md). Conserva el kernel como propietario de identidad, empresas, seguridad y auditoría; mantiene migraciones inmutables, secretos externos, contexto empresarial no confiado al cliente y personalización aplicada como última capa.

## Referencias verificadas

- [WildFly 41 — `elytron-oidc-client` secure deployment](https://docs.wildfly.org/41/feature-pack/doc/reference/subsystem/elytron-oidc-client/secure-deployment/index.html), consultada el 2026-07-28.
- [Keycloak — planificación para proteger aplicaciones y servicios](https://www.keycloak.org/securing-apps/overview), consultada el 2026-07-28.
- [Keycloak — guía de administración](https://www.keycloak.org/docs/latest/server_admin/), consultada el 2026-07-28.
- [Keycloak 26.7.0 — anuncio de versión](https://www.keycloak.org/2026/07/keycloak-2670-released), consultada el 2026-07-28.

La aceptación de este ADR autoriza iniciar `J11-S3-01` siguiendo el orden y los estados de validación definidos por Sprint 3.
