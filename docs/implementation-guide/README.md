# Guía de implementación del ERP por empresa

- Estado: Edición candidata; J11-S9-07 congela `purchasing` con G0–G6 verdes, demo oficial, fotografía y PDF; ADR-0040 planifica el plugin técnico opcional `legacy_migration`, ADR-0045 el funcional transversal opcional `business_process_management` y ADR-0046 la familia vertical Flota F1/F2; J11-S8-C07 implementa publicaciones completas, unidad menor opcional y búsqueda/paginación en servidor de `reference_data`; J11-S8-C06 mantiene políticas empresariales versionadas, V2 append-only y administración neutral; J11-S8-C02 mantiene retorno contextual seguro para plugins y los 11 usos nativos administrables, definiciones, familias de variantes y definiciones de socios,
  ciclo activo/inactivo, revisión/historial append-only y reemplazo seguro de definiciones simples, ciclo de perfiles tributarios y familias, revisión explícita e historial visible tributario, las cuatro clases empresariales de socios y asignación versionada de familias a artículos validadas; instalador interno J11-S9-08 creado; G7, Authenticode y matriz Windows independiente pendientes
- Edición: 1.0-rc103
- Fecha: 2026-08-14
- Compatibilidad: `PluginApiVersion.CURRENT = 0.4.3`; contratos `reference-data-api = 1.1.0`, `business-partners-api = 1.1.0`, `commercial-catalog-api = 1.1.0`, `inventory-api = 1.1.0` y `purchasing-api = 1.1.0`; Flyway `core` V1–V6, `plg_reference_data` V1–V4, `plg_business_partners` V1–V4, `plg_commercial_catalog` V1–V4, `plg_inventory` V1–V2, `plg_purchasing` V1–V2 validada en PostgreSQL 18.4/Testcontainers y fixture `plg_reference_plugin` V1; unidades JPA en `validate`; perfil físico `with-purchasing-demo` para WAR y migrador; imágenes verificadas `logixone/app:j11-s9-07-closing` y `logixone/migrator:j11-s9-07-closing`; Keycloak 26.7.0, WildFly 41 OIDC y Jakarta Faces 4.1; Maven, ArchUnit, PostgreSQL, migraciones, health, OIDC y Playwright acumulado verdes; instalador Windows `0.9.0-internal.1` restringido a `INTERNAL_UNSIGNED`; G7, Authenticode y matriz Windows independiente pendientes
- Audiencia: implementadores funcionales, desarrolladores de plugins, responsables de infraestructura y soporte de puesta en marcha
- Fuente canónica: este documento versionado junto al código

## Propósito

Enseñar, de forma progresiva y reproducible, cómo llevar Smart ERP desde una distribución validada hasta una implementación operativa para una empresa concreta. La guía debe permitir que un implementador comprenda el modelo antes de ejecutar comandos, tome decisiones dentro de los límites arquitectónicos y pueda verificar objetivamente el resultado.

La marca visible es **Smart ERP**. Las coordenadas, rutas, variables y recursos
que contienen `logixone` son identificadores técnicos heredados preservados por
compatibilidad; no deben renombrarse durante una instalación o actualización.

No es únicamente un runbook de instalación ni una referencia de API. Combina explicación conceptual, procedimiento paso a paso, ejemplos, controles de seguridad, pruebas, diagnóstico y criterios de entrega a la empresa.

## Política de mantenimiento

- La guía evoluciona con el producto y usa la misma versión que el baseline al que describe.
- Toda historia que cambie onboarding empresarial, plugins, personalización, configuración, datos, despliegue, seguridad u operación debe evaluar y, cuando corresponda, actualizar esta guía en el mismo cambio.
- Cada procedimiento debe indicar desde qué versión aplica, prerrequisitos, entradas, resultado esperado, validación, reversión y enlaces a ADR o runbooks detallados.
- No se documentan comandos o pantallas inexistentes como si estuvieran disponibles.
- Los ejemplos usan empresas, dominios, identificadores y secretos ficticios.
- La edición entregable debe poder publicarse como un documento único navegable; podrá generarse una versión PDF desde esta fuente sin convertir el PDF en la fuente mantenida.

## Cómo usar esta edición

Leer los capítulos en orden la primera vez. Los bloques se interpretan así:

- **Disponible:** existe en el baseline y puede verificarse con el comando indicado.
- **Interno:** existe como servicio Java, pero todavía no tiene endpoint o pantalla productivos; solo lo invoca un adaptador autorizado o el arnés de pruebas.
- **Ilustrativo:** muestra la forma esperada de código o decisión, pero debe adaptarse y probarse antes de incorporarlo.
- **Pendiente:** capacidad deliberadamente no implementada; no debe simularse con accesos directos o endpoints temporales.

La guía no reemplaza los [ADR](../adr/README.md), la [arquitectura vigente](../architecture/overview.md) ni los [runbooks](../runbooks/README.md). Cuando exista diferencia, manda el contrato y el procedimiento versionado junto al código.

## Recorrido de aprendizaje

1. Qué es Smart ERP y cómo se divide en kernel, plugins funcionales y plugin de personalización empresarial.
2. Qué puede configurar o extender un implementador y qué requiere cambiar contratos públicos o el producto base.
3. Relevamiento inicial de la empresa: procesos, módulos, usuarios futuros, datos, integraciones, pantallas y requisitos no funcionales.
4. Clasificación de cada necesidad como configuración, plugin funcional reutilizable o personalización exclusiva de la empresa.
5. Preparación reproducible del ambiente, configuración externa, secretos, PostgreSQL, migrator, aplicación y health checks.
6. Creación y ciclo de vida de la empresa dentro del ERP.
7. Selección, compatibilidad, dependencias y activación de plugins funcionales para esa empresa.
8. Creación, asignación obligatoria y evolución del plugin de personalización exclusivo.
9. Personalización de pantallas ajenas mediante contratos públicos, slots y overlays, sin importar internos ni relajar seguridad del servidor.
10. Migraciones, carga y validación de datos, respaldo, recuperación y rollback compatible.
11. Construcción de la distribución, pruebas requeridas, despliegue por digest y promoción entre ambientes.
12. Diagnóstico de empresa no disponible, plugin ausente o incompatible, dependencia inválida, migración pendiente y personalización rechazada.
13. Checklist de aceptación, evidencias, capacitación, traspaso y soporte posterior a la puesta en marcha.

## Capítulo 1 — Qué es Smart ERP y qué se implementa para una empresa

Smart ERP es un ERP construido como monolito modular: una única aplicación WildFly y un único WAR contienen un kernel transversal y los JAR de plugins seleccionados. La separación modular impide que el kernel se convierta en un controlador central con lógica de ventas, inventario o facturación.

| Pieza | Responsabilidad | Quién la mantiene |
|---|---|---|
| Kernel | empresa, contexto, activación, seguridad futura, configuración, auditoría y catálogo | equipo de plataforma |
| Plugin funcional | capacidad ERP reutilizable y sus contratos públicos | equipo propietario del dominio |
| Plugin técnico planificado | capacidad transversal opcional, como migración de legados; requiere que `PluginKind.TECHNICAL` sea aprobado antes de implementarse | equipo de plataforma y migración |
| Plugin `CUSTOMIZATION` | cambios exclusivos de una empresa sobre extensiones públicas | equipo de implementación empresarial |
| Migrator | evolución versionada del esquema | plataforma y propietarios de cada esquema |
| Distribución | selección física de JAR y construcción del WAR/imagen | release/infraestructura |

Cada empresa operativa debe tener exactamente una personalización propia. Esa asignación no está dentro del JAR: se persiste como `customization_plugin_id` en `core.company`. La misma imagen puede contener varias personalizaciones, pero cada consulta empresarial selecciona solo la asignada.

## Capítulo 2 — Qué se configura, qué se reutiliza y qué se desarrolla

Antes de crear código, clasificar cada pedido:

| Tipo de necesidad | Tratamiento correcto | Ejemplo |
|---|---|---|
| Configuración externa | variable, secreto o dato administrado por un caso de uso | URL de base, puerto, estado de activación |
| Capacidad reutilizable | plugin funcional | inventario disponible para varias empresas |
| Diferencia exclusiva | plugin de personalización | etiqueta, ayuda o fragmento propio en un slot publicado |
| Cambio del contrato funcional | nueva versión del contrato del plugin propietario | nueva columna extensible o acción pública |
| Regla de seguridad/negocio | servicio del plugin funcional o kernel | autorización, cálculo fiscal, validación de stock |

No convertir en personalización visual:

- una regla tributaria o cálculo;
- autorización o asignación de roles;
- lectura directa de una tabla ajena;
- un parche de XHTML, bean, DTO o entidad de otro plugin;
- CSS o JavaScript global para alterar pantallas no declaradas.

Si no existe un contrato público adecuado, registrar el requisito y solicitar al propietario una extensión versionada. `CUSTOMIZATION` no significa acceso general al sistema.

## Capítulo 3 — Relevamiento inicial de la empresa

Crear una ficha sin secretos ni datos personales innecesarios:

1. identidad técnica y nombre comercial usado solo en documentación funcional;
2. procesos incluidos y explícitamente excluidos;
3. sedes, zonas horarias, monedas e idiomas;
4. volúmenes estimados, concurrencia, disponibilidad y ventanas operativas;
5. datos maestros y transaccionales a migrar;
6. integraciones de entrada y salida;
7. pantallas o reportes que requieren diferencias;
8. requisitos de respaldo, retención, auditoría y recuperación;
9. responsables de aceptación funcional, técnica y operativa;
10. estrategia de despliegue, reversión y soporte.

Desde J11-S8-C03, documente además qué publicación de `reference_data` cubre los
países y monedas de la empresa. J11-S8-C07 agrega publicaciones `FULL`, pero su
presencia no sustituye la revisión de licencia, vigencia y necesidades de la
empresa ni constituye certificación. Active `reference_data` antes de `business_partners` y
`commercial_catalog`. Conceda `reference_data.policy.manage` sólo a quienes deban
administrar disponibilidad y revisar procedencia; una inhabilitación se aplica a
usos nuevos de la empresa y nunca reemplaza una corrección de la publicación.

Matriz mínima de requisitos:

| ID | Necesidad | Clasificación | Propietario | Contrato/versión | Prueba de aceptación |
|---|---|---|---|---|---|
| R-001 | capacidad reutilizable | plugin funcional | dominio | versión del plugin | caso funcional |
| R-002 | diferencia de pantalla | personalización | implementación | `ScreenId` y rango | composición + UI futura |
| R-003 | configuración | kernel/infra | operaciones | variable o caso de uso | smoke/readiness |

No comenzar un plugin empresarial mientras existan requisitos sin clasificación o sin propietario.

## Capítulo 4 — Diseñar la composición de plugins

Para cada plugin funcional seleccionado, registrar:

- `PluginId` y versión exacta;
- compatibilidad con `plugin-api`;
- dependencias requeridas y opcionales;
- capacidades, permisos y menús que publica;
- esquemas y migraciones cuando existan;
- pantallas y versiones que la empresa personalizará.

El orden no se decide manualmente: `PluginCatalogResolver` valida y ordena el grafo. Una dependencia requerida ausente, incompatible o cíclica invalida el catálogo completo. Todos los funcionales quedan antes de cualquier personalización.

El plugin empresarial debe depender de cada funcional cuyo contrato modifica. Esa dependencia es `REQUIRED`: evita que la empresa quede activa con un overlay cuyo objetivo físico no está presente.

### Crear el esqueleto con la plantilla productiva

Construir primero el generador versionado:

```powershell
.\mvnw.cmd -B -pl tools/plugin-scaffold -am package
```

Para un plugin funcional neutral:

```powershell
java -jar tools\plugin-scaffold\target\plugin-scaffold-0.1.0-SNAPSHOT-executable.jar `
  --project-root . `
  --output plugins\sample-capability `
  --artifact-id sample-capability `
  --plugin-id sample_capability `
  --package py.com.logixone.plugins.samplecapability `
  --display-name "Sample capability" `
  --kind functional
```

La salida contiene exactamente siete archivos: POM, definición neutral, prueba,
`beans.xml`, proveedor de `ServiceLoader`, README y checklist contractual. Nace
sin entidades, migraciones, permisos, menús ni pantallas. El generador se niega a
sobrescribir una carpeta y no edita el reactor ni el plugin set.

Una personalización se genera con `--kind customization` y requiere
`--target-plugin-id`, `--target-min-version` y `--target-max-version`. Esto crea la
dependencia `REQUIRED` que impide desplegar el overlay sin su funcional compatible.
El [runbook del generador](../runbooks/plugin-scaffold.md) contiene ambos comandos,
el inventario de salida y el procedimiento de composición y validación.

Después de generar, registrar explícitamente el módulo en el POM padre y
dependency management, y seleccionarlo desde un perfil de
`distribution/logixone-plugin-set`. El mismo perfil debe construir WAR y migrador;
la variante base también debe probarse para detectar dependencias transitivas.
Persistencia, contratos públicos y pantallas se agregan sólo cuando los requisitos
del dominio están aprobados. Toda pantalla usa Jakarta Faces, Material Design 3 y
los rangos responsive del shell.

### Elegir comunicación síncrona o eventos

Use un puerto público síncrono cuando la operación actual necesite una respuesta
para decidir su propio resultado. Use un evento de integración cuando propague un
hecho ya confirmado y el productor no deba esperar el trabajo del consumidor.
Auditoría, logs y eventos internos de dominio no sustituyen ese contrato.

[ADR-0013](../adr/0013-eventos-integracion-outbox-por-plugin.md) exige que el
productor posea el tipo/payload y su outbox en `plg_<producer>`, mientras el
consumidor posee inbox/deduplicación en `plg_<consumer>`. La escritura empresarial
y el outbox se confirman juntos; la entrega es `at-least-once` y el consumidor es
idempotente por `event_id`. No se promete orden global ni “exactly once”.

No agregue una tabla `core.outbox`, no publique antes del commit y no elija un
broker preventivamente. El [contrato operativo](../architecture/integration-events-outbox.md)
enumera sobre, activación, bootstrap, reintentos, cuarentena, replay, métricas y
pruebas obligatorias. La infraestructura se implementa junto al primer productor y
consumidor reales. `business_partners` no necesita outbox sólo por existir.

## Capítulo 5 — Preparar un ambiente reproducible

Quien trabaje con un IDE puede seguir el [runbook paso a paso para Visual Studio
Code](../runbooks/levantar-logixone-visual-studio-code.md) o el
[runbook para IntelliJ IDEA Ultimate 2026.2](../runbooks/levantar-logixone-intellij-idea-ultimate.md).
Ambos usan el editor para navegación, Maven, pruebas y control de Docker; la
ejecución completa conserva Docker/Compose como baseline oficial. Los recorridos
documentan JDK, Wrapper, extensiones o integraciones, secretos, imágenes, arranque,
health, persistencia y diagnóstico sin introducir una configuración manual
divergente de WildFly.

### Prerrequisitos disponibles

- Windows con PowerShell o un sistema POSIX equivalente;
- JDK 21 para build local;
- Docker Engine, Buildx y Compose compatibles con `linux/amd64`;
- acceso inicial a Maven Central, Docker Hub y Quay;
- espacio para `.tools/`, imágenes y los volúmenes PostgreSQL/Keycloak;
- puertos locales disponibles, por defecto `127.0.0.1:8080` y `127.0.0.1:8180`.

En Windows, desde la raíz:

```powershell
.\mvnw.cmd --version
```

Resultado esperado: Maven 3.9.16, Java 21 y Maven home bajo `.tools`. El Wrapper
de Windows selecciona automáticamente el JDK y su caché del proyecto aunque el
entorno global use Java 8. Nunca redirigir dependencias o binarios del proyecto al
perfil del usuario ni repetir el ajuste manual de variables antes de cada build.

### Configuración y secreto local

1. copiar `infra/compose/compose.env.example` como `infra/compose/compose.env.local`;
2. crear `postgres-password.txt`, `keycloak-admin-password.txt`, `oidc-client-secret.txt` y `demo-user-password.txt` bajo `.tools/secrets/`, cada uno con un valor aleatorio distinto de una sola línea;
3. restringir permisos de los cuatro archivos;
4. configurar tags de aplicación/migrator, issuer, redirect/origin/logout y un `LOGIXONE_TX_NODE_ID` único;
5. validar sin imprimir secretos:

```powershell
docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml config --quiet
```

Los secretos nunca se copian al archivo de variables, código, realm JSON, Dockerfile, documentación o argumentos de build. El runbook [Keycloak y OIDC para desarrollo y demo](../runbooks/keycloak-oidc.md) contiene la topología, variables, arranque, persistencia, bootstrap y matriz validada.

### Infraestructura OIDC validada para la demo

Keycloak 26.7.0 está fijado por versión y digest `linux/amd64`. Para desarrollo/demo usa un volumen `keycloak-data`, un realm declarativo sin usuarios/passwords y el issuer único `http://keycloak.localhost:8180/realms/logixone`. El alias funciona dentro de la red Compose `identity` y en el navegador por el comportamiento de `.localhost`.

WildFly configura `elytron-oidc-client` mediante expresiones externas, no mediante dependencias Java de Keycloak. `/app/*`, `/admin/*` y `/faces/*` redirigen al login y `/api/*` usa detección REST para responder `401`; health permanece público. Firma RS256, issuer, audience y expiración forman parte de la validación OIDC. `J11-S6-07` volvió a comprobar runtime positivo, rechazo de audience, issuer y expiración inválidos y la administración protegida.

El logout RP-Initiated de WildFly 41 está en estabilidad `preview`; la configuración
embebida y el arranque usan `--stability=preview` conforme a
[ADR-0008](../adr/0008-logout-oidc-estabilidad-preview-wildfly.md). La prueba E2E
exige que, después de cerrar sesión, una segunda visita protegida vuelva al login en
lugar de reutilizar la sesión de Keycloak.

Readiness comprueba únicamente que la configuración OIDC local sea coherente y nunca sondea Keycloak. Liveness no cambia. Si falta provider URL, cliente, secreto o URI de logout, el entrypoint falla cerrado antes de iniciar WildFly.

## Capítulo 6 — Crear y gobernar la empresa

El kernel implementa el flujo interno y `J11-S4-05` agrega el adaptador web
administrativo autorizado para empresas y plugins. El implementador debe usar las
pantallas protegidas, no crear un REST temporal, aceptar un identificador de empresa
desde un header no confiable ni escribir directamente en las tablas `core`.

La secuencia válida en una distribución que ya contiene los JAR necesarios es:

1. incorporar físicamente a la distribución el plugin de personalización exclusivo de la empresa y todos los plugins funcionales seleccionados;
2. validar el catálogo completo y sus dependencias antes de registrar datos empresariales;
3. ingresar con un actor local que tenga `kernel.company.manage`, abrir
   `/logixone/faces/admin/companies.xhtml` y registrar la empresa seleccionando una
   personalización libre; el servidor la crea como `INACTIVE`;
4. ingresar a `/logixone/faces/admin/plugins.xhtml` con `kernel.plugin.manage`,
   seleccionar la empresa y habilitar los plugins funcionales en orden de
   dependencias; una dependencia requerida ausente o inactiva rechaza la operación;
5. volver a Empresas y cambiarla a `ACTIVE` únicamente cuando la composición
   resulte operativa;
6. hacer pasar cada operación funcional por `PluginOperationGuard`, que obtiene la empresa desde el `CompanyContext` confiable y verifica que el plugin sea efectivo antes de ejecutar el callback.

Los comandos devuelven uno de tres resultados estables: `CHANGED`, `UNCHANGED` o `REJECTED`. `UNCHANGED` expresa idempotencia y no incrementa artificialmente versiones. `REJECTED` incluye un código neutral, no una excepción JPA/SQL. Las mutaciones usan versión esperada para detectar concurrencia; el llamador debe releer el estado antes de decidir si reintenta.

El reemplazo de personalización valida primero presencia física, categoría, exclusividad y compatibilidad. Solo después cambia la asignación dentro de una transacción. Si la validación, persistencia o auditoría falla, la personalización anterior permanece íntegra. La desactivación común de la personalización asignada está prohibida.

Cada comando y verificación de guarda produce auditoría estructurada con IDs técnicos,
operación, resultado, código, versiones, instante UTC y actor. Las acciones web de
esta sección agregan `AppUserId` local y correlación generada por el servidor; no
registran token, claims completos, cookies, nombres comerciales ni credenciales.

Para validación técnica interna se dispone de pruebas unitarias, Testcontainers y un arnés JTA opt-in descrito en el [runbook de Compose](../runbooks/compose.md). El arnés es exclusivamente de pruebas y nunca forma parte del WAR o de la imagen normal.

### Seguridad empresarial y autoridad global

`J11-S4-06` agrega dos procedimientos que el implementador debe mantener
separados:

1. registrar el usuario local en `/admin/security.xhtml` usando su subject; el
   issuer se obtiene de la configuración OIDC validada y el usuario nace inactivo;
2. activar el usuario y seleccionar la empresa exacta;
3. registrar y activar su membresía empresarial;
4. registrar un rol de esa empresa, concederle únicamente permisos funcionales
   actualmente efectivos y asignarlo a la membresía;
5. usar `/admin/system-authority.xhtml` solamente cuando el usuario deba operar el
   kernel transversalmente; allí se crean roles globales y se conceden permisos
   cerrados de `SystemPermission`.

Un rol empresarial siempre contiene `CompanyId` y nunca concede administración
global. Un rol global nunca contiene `CompanyId` y no concede acceso funcional a
plugins. Keycloak autentica, pero sus roles no sustituyen ninguno de estos modelos.
Las pantallas no gestionan credenciales, contraseñas, MFA, realms o clientes.

Toda reducción de acceso requiere confirmación. El servidor vuelve a autorizar el
permiso exacto, relee IDs y versiones y conserva auditoría correlacionada. La
política de autoridad global rechaza inactivar, desasignar o revocar si el resultado
dejaría la instancia sin administrador efectivo. No reparar ese rechazo mediante
SQL o roles del proveedor OIDC: primero debe asignarse otra autoridad global válida.

Una nueva concesión empresarial sólo puede elegirse entre permisos de plugins
efectivos. Si un plugin deja de ser efectivo, su concesión histórica permanece
visible como no efectiva para permitir su revocación; no vuelve operativo al
plugin.

### Consultar auditoría técnica

`J11-S4-07` incorpora V5 y `/admin/audit.xhtml`. El implementador con
`kernel.audit.view` puede consultar eventos producidos desde esa migración mediante
páginas de 25 registros y filtros cerrados por categoría, resultado, ventana,
empresa técnica y correlación exacta.

La vista muestra UUID locales, IDs públicos, operaciones, resultados, códigos,
versiones, actor técnico e instante UTC. No muestra issuer, subject OIDC, nombres,
tokens, cookies, credenciales, claims, SQL, stacktraces o datos comerciales. Los
logs anteriores a V5 no aparecen porque no se realiza un backfill inseguro.

`core.audit_event` es append-only: no usar SQL para editar o borrar eventos. Esta
edición no define exportación ni retención. Cualquier política futura debe acordar
plazo, respaldo, acceso, recuperación y eliminación verificable antes de agregar
una migración o procedimiento operativo.

Las mutaciones y su auditoría confirman o revierten juntas. Las decisiones de
acceso usan una transacción corta para conservar también denegaciones. Los logs
estructurados continúan como señal operativa, pero la pantalla consulta PostgreSQL
mediante `AuditQueryPort`, nunca archivos del servidor.

### Identidad para la demo: modelo y persistencia disponibles

Los contratos neutrales, adaptadores de persistencia, infraestructura Keycloak/OIDC,
derivación confiable del actor/empresa, shell Faces y pantalla neutral personalizada
están **Disponibles y validados técnicamente**. Las pantallas administrativas hasta
auditoría están implementadas como candidata pendiente de las pruebas acumuladas de Sprint 4;
no deben tratarse todavía como panel aprobado para producción.

- `AppUserId` es el identificador UUID público del usuario local.
- `ExternalIdentity` conserva issuer OIDC canónico y subject exacto; correo y username no participan en igualdad.
- `CompanyMembership` representa autorización local y versionada para una empresa.
- `CompanyRole`, `MembershipRoleAssignment` y `RolePermissionGrant` mantienen el rol dentro de una única empresa.
- `CompanyAccessPolicy` produce `SELECTED`, `SELECTION_REQUIRED` o `DENIED` para cero, una o varias membresías.
- `EffectivePermissionPolicy` devuelve únicamente concesiones de roles activos que también estén disponibles en la composición efectiva de plugins.
- `AuthenticatedCompanyContext` transporta actor y empresa después de validación server-side; solo `TrustedAccessService` lo produce a partir del estado actual.

Los implementadores no deben instanciar estos tipos desde valores libres del navegador. `ValidatedOidcPrincipal` toma issuer/subject del principal ya validado por WildFly, y `TrustedAccessPort` resuelve la empresa seleccionada mediante estado local actual.

### Autoridad administrativa global y primeras pantallas

ADR-0009 separa la administración global de las membresías y roles empresariales.
`SystemPermission` publica cinco códigos iniciales del kernel: administrar empresas,
plugins, seguridad, auditoría y la propia autoridad global. Un rol global nunca
contiene `CompanyId` y una asignación relaciona únicamente `AppUserId` con
`SystemRoleId`.

`EffectiveSystemPermissionPolicy` devuelve solo permisos concedidos por roles
globales activos a un usuario activo y presentes en el vocabulario disponible. Una
referencia inconsistente produce denegación cerrada. `SystemAuthoritySafetyPolicy`
recibe el estado completo posterior a una mutación y exige que permanezca al menos
un usuario activo con un rol activo que conceda
`kernel.system_administration.manage`.

V4, el bootstrap neutral y su persistencia JPA/JTA están implementados pendientes
de pruebas. La migración crea roles globales, concesiones y asignaciones sin
empresa; readiness exige V4.
`SystemAuthorityBootstrapService` crea o verifica una declaración exacta que debe
contener `kernel.system_administration.manage`. `ConfiguredSystemAuthorityBootstrap`
permanece `false` por defecto, no expone endpoint y no registra issuer o subject.

`JpaSystemAuthorityRepository` mapea las tablas V4 con entidades privadas y
`TransactionalSystemAuthorityUseCases` delimita bootstrap y administración. Las
operaciones que pueden reducir autoridad adquieren un lock transaccional, leen el
snapshot completo y rechazan el cambio si eliminaría al último administrador
efectivo. Usuarios y roles conservan control de versión optimista.

El bootstrap continúa deshabilitado por defecto. Su adaptador ya existe, pero la
matriz PostgreSQL/JTA todavía está pendiente: no habilitarlo sobre un volumen
compartido o productivo. En el ambiente de desarrollo autorizado, el procedimiento
one-shot será aplicar V4, conservar backup, completar la declaración local exacta,
habilitar, recrear `app`, comprobar `CHANGED`/`UNCHANGED`, volver a `false` y recrear
de nuevo. No sustituirlo por SQL, primer login, roles de Keycloak o un endpoint
temporal.

`J11-S4-04` agrega la entrada `/admin/index.xhtml`. WildFly autentica por OIDC y un
filtro cubre tanto la ruta directa como `/faces/admin/*`. La autorización vuelve a
buscar al usuario local activo y sus permisos globales en cada request; no usa la
empresa seleccionada, roles empresariales, claims de rol ni una lista guardada en
sesión. Una denegación responde genéricamente y deja el diagnóstico solo en la
auditoría con correlación del servidor.

La landing muestra únicamente áreas para las que el actor tiene permiso. Empresas y
Plugins ya enlazan a pantallas operativas; las demás áreas continúan como siguientes
incrementos. El filtro exige el permiso exacto al abrir cada ruta y cada acción llama
de nuevo `TrustedAdminWebAccess.require(permission)` antes del caso de uso. Reemplazar
una personalización exige `kernel.company.manage` aunque la pantalla de plugins se
haya abierto con `kernel.plugin.manage`. Ocultar una tarjeta o botón nunca autoriza
un comando.

Las versiones de empresa y activación regresan en formularios JSF sólo como valores
candidatos. El caso de uso relee el estado; un conflicto obliga a recargar y nunca
sobrescribe silenciosamente. Inactivar una empresa, deshabilitar un plugin o
reemplazar una personalización requiere confirmación explícita. Ninguna de esas
acciones elimina datos. El catálogo físico es de sólo lectura: incorporar o retirar
un JAR sigue requiriendo reconstrucción y redespliegue.

### Contexto de empresa y autorización en una operación

El flujo disponible desde `J11-S3-05` es:

1. WildFly valida OIDC antes de permitir el acceso a `/app/*`, `/admin/*` o `/api/*`.
2. La frontera web acepta el principal solo cuando `authType` es `OIDC`; no procesa el token ni confía en headers.
3. `TrustedAccessService` busca al usuario local activo y sus membresías actuales.
4. Con cero empresas niega sin revelar opciones; con una selecciona automáticamente; con varias devuelve solo los IDs autorizados para que el shell solicite elección.
5. La sesión guarda exclusivamente los IDs locales de actor y empresa. Es una referencia que debe volver a validarse, no una autorización cacheada.
6. Antes de una acción funcional, `requireAuthorization(pluginId, permissionId)` exige empresa operacional, plugin efectivo, propiedad del permiso, rol activo, asignación actual y concesión vigente.
7. La auditoría registra el resultado con correlación generada por el servidor y omite token, cookie, claims, issuer/subject y datos personales.

Un `CompanyId` procedente de un selector, formulario o URL es únicamente un candidato.
El cambio de empresa limpia primero la referencia anterior. Si la nueva empresa no
pertenece al actor o dejó de estar operacional, la aplicación responde `403` genérico
y conserva la sesión sin empresa seleccionada.

No guardar en beans de sesión menús, pantallas, roles o permisos. El shell mantiene
modelos de vista por request y debe recomponerlos después de cada cambio
de empresa y conservar la guarda del servidor en la acción final. El procedimiento
operativo completo está en el [runbook de contexto confiable](../runbooks/trusted-context-authorization.md).

### Shell Faces y navegación disponibles

El shell inicial no incorpora PrimeFaces. Usa Jakarta Faces 4.1 proporcionado por
WildFly, CSS propio y estado de vista en servidor. Esta elección no agrega una
dependencia o licencia al baseline y puede revisarse mediante ADR si una capacidad
visual futura demuestra una necesidad concreta.

Material Design 3 es el sistema de diseño obligatorio sobre JSF conforme a
[ADR-0007](../adr/0007-material-design-responsive-sobre-jsf.md). El shell mantiene
tokens `--md-sys-*` y renderers permitidos; un plugin no entrega su propio tema,
XHTML, CSS o JavaScript global. Toda pantalla nueva o modificada debe resolver desde
su historia los rangos compacto (`0–599px`), medio (`600–839px`) y expandido
(`840px` o más), además de teclado, foco, estados y ausencia de overflow horizontal
normal.

Para una personalización empresarial, el implementador debe pedir cambios mediante
los IDs y operaciones públicas de la pantalla. Una futura personalización de marca
deberá exponer valores validados que el shell convierta a roles visuales permitidos;
no debe generar hojas de estilo libres.

Las rutas públicas del corte son:

| Ruta | Propósito | Protección |
|---|---|---|
| `/logixone/faces/app/index.xhtml` | selección, cabecera, empresa y menú | OIDC + contexto actual |
| `/logixone/faces/app/view.xhtml?route=/...` | destino central de una contribución | OIDC + menú actual + guarda plugin/permiso |
| `/logixone/faces/admin/index.xhtml` | landing de administración global | OIDC + al menos un permiso global actual |
| `/logixone/faces/admin/companies.xhtml` | alta y estado de empresas | OIDC + `kernel.company.manage` |
| `/logixone/faces/admin/plugins.xhtml?company=<uuid>` | catálogo, activaciones y personalización | OIDC + `kernel.plugin.manage`; reemplazo exige además `kernel.company.manage` |
| `/logixone/faces/admin/security.xhtml?company=<uuid>` | usuarios, membresías, roles y permisos empresariales | OIDC + `kernel.security.manage` |
| `/logixone/faces/admin/system-authority.xhtml` | roles, asignaciones y permisos globales | OIDC + `kernel.system_administration.manage` |
| `/logixone/faces/admin/audit.xhtml` | auditoría técnica paginada desde V5 | OIDC + `kernel.audit.view` |
| `/logixone/app/logout` | salida coordinada | mecanismo OIDC de WildFly |

La UI no navega hacia XHTML de un plugin. `MenuContribution.route` se compara con la
proyección actual y el shell conserva el plugin propietario y el permiso requerido.
Una URL copiada o manipulada vuelve a consultar autorización aunque el enlace no se
encuentre visible.

`core.company` todavía no contiene nombre comercial. Por eso la candidata muestra
una etiqueta técnica acotada sólo para empresas ya autorizadas; no se debe reemplazar
por una tabla paralela, una variable libre o el nombre como clave. El nombre comercial
se agregará mediante modelo y migración cuando exista su caso de uso.

Los recursos se mantienen en `web-shell/src/main/resources/META-INF/resources`; no
colocar vistas empresariales en `distribution`, no incluir fragmentos XHTML desde un
plugin y no guardar `TrustedNavigationView` en sesión. Para operar y extender este
corte consultar el [runbook del shell](../runbooks/shell-ui.md).

La ruta `/reference` ya tiene un adaptador cerrado para
`reference_plugin:dashboard`. Antes de renderizar, `TrustedScreenAccess` vuelve a
validar actor, empresa, plugin, permiso y pantalla, y recompone los overlays actuales.
El resultado vive sólo durante la petición. Un implementador no debe guardar
`ComposedScreen` o `ShellScreenView` en sesión ni asumir que haber visto el menú
autoriza un postback posterior.

La migración V3 está **empaquetada y validada sobre PostgreSQL 18.4**. Crea estas estructuras sin modificar V1/V2:

| Tabla | Propósito | Regla de aislamiento |
|---|---|---|
| `core.app_user` | usuario local por issuer/subject | identidad externa única, sin password/token |
| `core.company_membership` | acceso de un usuario a una empresa | PK usuario/empresa y estado cerrado |
| `core.security_role` | rol funcional propio de empresa | código único dentro de empresa |
| `core.role_permission` | concesión de `ContributionId` | FK compuesta al rol de la misma empresa |
| `core.membership_role` | roles asignados a membresía | FKs compuestas impiden cruzar empresas |

Los adaptadores JPA son privados de `kernel-infrastructure-jakarta`. `AppUserEntity`, `CompanyMembershipEntity`, `SecurityRoleEntity`, `RolePermissionEntity` y `MembershipRoleEntity` no son contratos de plugins y no deben importarse desde dominios o UI. La aplicación trabaja con puertos y convierte los conflictos de base a códigos tipados.

Las operaciones administrativas siguen cuatro reglas:

1. una alta normal crea usuario, membresía y rol como `INACTIVE`;
2. repetir un cambio cuyo estado ya coincide devuelve `UNCHANGED` y conserva la versión;
3. asignar rol valida primero membresía, empresa y pertenencia del rol;
4. una concesión desconocida puede conservarse, pero solo será efectiva cuando el catálogo público aporte ese `ContributionId`.

El bootstrap one-shot dispone ahora del adaptador interno `ConfiguredSecurityBootstrap`. Compose inicia `app` solamente después del migrador; el adaptador permanece deshabilitado por defecto y, al habilitarse, recibe issuer/subject, empresa, personalización, rol y permisos mediante configuración externa. Una declaración idéntica devuelve `UNCHANGED`; una empresa, identidad, membresía, rol, asignación o permiso incompatible aborta el despliegue. No ejecutar inserts manuales, no exponer un endpoint de bootstrap y no inventar identidades reales en Compose versionado.

Antes de habilitarlo, la empresa exacta debe existir, estar activa y conservar la personalización declarada. Completar todas las variables `LOGIXONE_SECURITY_BOOTSTRAP_*`, usar como issuer el mismo `LOGIXONE_OIDC_PROVIDER_URL`, ejecutar una sola recreación de `app`, revisar el evento técnico y volver a dejar `LOGIXONE_SECURITY_BOOTSTRAP_ENABLED=false`. Los valores reales pertenecen al archivo local ignorado, no a esta guía.

V3–V6 fueron validadas con PostgreSQL/Testcontainers, JPA/JTA y Compose. Antes de
aplicarlas en un entorno persistente se debe identificar la imagen, conservar un
backup según el runbook y entender que un rollback de aplicación no elimina las
migraciones aplicadas. Readiness exige V6 y Hibernate continúa en `validate`; no se
autoriza generación automática de DDL.

- Keycloak autentica externamente mediante OIDC y WildFly está configurado con `elytron-oidc-client`; login, logout y casos negativos fueron validados en runtime.
- El ERP identifica a la persona por `(issuer, subject)` y no guarda contraseñas ni tokens.
- Usuarios locales, membresías, roles empresariales y permisos funcionales pertenecen a `core`.
- Una empresa no será un realm. El usuario solo podrá seleccionar empresas donde mantenga membresía activa.
- La selección se conserva en sesión del servidor y se revalida en cada operación; un `CompanyId` enviado por el navegador nunca concede acceso.
- La autorización combina actor, membresía, empresa operacional, plugin efectivo y permiso.
- El shell Jakarta Faces muestra únicamente navegación autorizada y una pantalla técnica compuesta con personalización A/B.

No se debe configurar manualmente un header empresarial, crear usuarios directamente por SQL ni abrir endpoints de bootstrap.

## Capítulo 7 — Seleccionar y activar plugins funcionales

Después de resolver el estado efectivo, `CompanyContributionService` produce una vista inmutable para una sola empresa. La vista conserva el plugin propietario y reúne, en orden, sus capacidades, permisos y menús. No consulta tablas privadas de plugins ni guarda copias en `core`: siempre proyecta el descriptor físicamente presente y validado.

Un implementador debe interpretar el resultado con estas reglas:

1. primero aparecen únicamente los plugins funcionales habilitados y efectivos, respetando dependencias;
2. el último plugin es siempre la personalización exclusiva asignada a esa empresa;
3. otra empresa puede obtener una lista distinta sobre la misma imagen desplegada;
4. una fila histórica de activación no recrea un plugin cuyo JAR ya no está en la distribución;
5. empresa inactiva o personalización ausente/incompatible devuelve cero contribuciones para evitar una experiencia estándar silenciosa;
6. los permisos enumerados son códigos disponibles, no concesiones a roles o usuarios;
7. ocultar un menú solo evita ofrecer navegación: toda operación debe seguir pasando por la guarda y, cuando exista identidad, por autorización del servidor.

El adaptador administrativo es una interfaz JSF interna y protegida; no constituye
una API REST pública ni permite instalación dinámica. `J11-S3-07` agregó el primer
renderer JSF cerrado para una pantalla técnica de referencia. No es un motor genérico
ni autoriza simular personalizaciones reemplazando XHTML, importando beans ajenos o
escribiendo directamente en tablas `core`.

## Capítulo 8 — Crear el plugin de personalización exclusivo

Desde `J11-S2-07`, una personalización empresarial es un módulo Maven JAR normal incluido físicamente en la distribución. No es un archivo que se sube en ejecución. Su descriptor debe declarar `PluginKind.CUSTOMIZATION`, una identidad propia, compatibilidad con `PluginApiVersion.CURRENT = 0.4.3` y dependencias requeridas sobre cada plugin funcional cuya pantalla modifica.

El recorrido técnico para una empresa nueva es:

1. crear un módulo separado, por ejemplo `plugins/acme-customization`, sin copiar clases de los plugins funcionales;
2. declarar una única implementación CDI de `PluginDefinition`, un `PluginId`
   estable y registrar la misma clase en
   `META-INF/services/py.com.logixone.plugin.api.PluginDefinition`;
3. declarar como `REQUIRED` cada dependencia funcional y limitarla con un rango compatible conocido;
4. publicar overlays tipados con IDs únicos; el `ScreenId` objetivo siempre pertenece al plugin funcional;
5. aportar únicamente fragmentos cuyo `ScreenFragmentId` pertenezca al propio plugin de personalización;
6. agregar el módulo al perfil de `distribution/logixone-plugin-set` y reconstruir
   con ese mismo perfil las imágenes de aplicación y migrador;
7. registrar la empresa inactiva, asignarle exclusivamente ese `PluginId`, habilitar funcionales y activar la empresa mediante los casos de uso autorizados;
8. ejecutar pruebas del módulo, ArchUnit, matriz del WAR, gate integral y smoke de la imagen antes de promoverla.

El JAR no debe contener un `CompanyId`: la relación exclusiva se persiste en `core.company`. Esto permite validar identidad y compatibilidad del artefacto sin convertirlo en una copia de datos de una empresa. Aun así, la misma personalización no puede asignarse a dos empresas.

Los módulos `reference-customization-a` y `reference-customization-b` son ejemplos técnicos mínimos. Enseñan aislamiento y operaciones permitidas; no son plantillas de negocio ni deben renombrarse y copiarse mecánicamente para una implementación real.

## Capítulo 9 — Personalizar una pantalla publicada por otro plugin

El propietario funcional decide la superficie extensible. Su descriptor publica un `ScreenDefinition` con:

- `ScreenId`, formado por el `PluginId` propietario y un nombre local estable;
- versión semántica independiente del aspecto visual;
- elementos con ID, región, orden, claves de etiqueta/ayuda y estado estándar;
- conjunto exacto de `ScreenCustomizationOperation` autorizado por elemento;
- slots con región, orden y capacidad máxima.

Una personalización publica un `ScreenOverlay` que indica pantalla objetivo, rango de versión compatible y una lista no vacía de `ScreenChange`. Las operaciones disponibles son deliberadamente pequeñas:

| Operación | Efecto permitido |
|---|---|
| `CHANGE_LABEL` / `CHANGE_HELP` | Sustituir una clave de recurso autorizada, no texto ejecutable ni EL |
| `HIDE` | Hacer menos visible un elemento que el propietario marcó como ocultable |
| `DISABLE` | Impedir interacción visual sin sustituir la guarda del servidor |
| `REQUIRE` | Endurecer la presentación; nunca volver opcional un requisito estándar |
| `REORDER` | Mover dentro de la misma región y dentro de sus posiciones válidas |
| `SlotContent` | Insertar un fragmento propio en un slot público con capacidad disponible |

No existen operaciones `SHOW`, `ENABLE` u `OPTIONAL`. Tampoco se admiten rutas XHTML, nombres de beans, expresiones EL, clases Java, CSS/JavaScript global ni referencias a repositorios o entidades ajenas.

### Procedimiento de diseño

1. pedir al equipo propietario el contrato público de la pantalla y su versión; no inspeccionar la vista interna para inventar IDs;
2. confirmar que el elemento autoriza cada operación necesaria y que el slot permite la cantidad de fragmentos requerida;
3. decidir si el pedido es realmente presentación; cambios de cálculo, autorización, validación de negocio o datos requieren otro puerto público y otra historia;
4. declarar el rango mínimo/máximo que la personalización ha probado;
5. usar claves de recursos propias para textos y un `ScreenFragmentId` del plugin empresarial para contenido de slot;
6. probar el overlay válido y también pantalla ausente, versión incompatible, ID desconocido, operación prohibida, conflicto y exceso de capacidad;
7. demostrar dos empresas sobre el mismo catálogo y verificar que ninguna recibe el overlay o fragmento de la otra.

`CompanyScreenService` recibe la `CompanyContributions` ya filtrada. Compone primero todas las `ScreenDefinition` funcionales y después los overlays de la única personalización efectiva. Si cualquier cambio es inválido, devuelve diagnósticos y cero pantallas; no degrada silenciosamente a la pantalla estándar ni aplica una parte del overlay.

### Cómo lo consumirá el adaptador JSF con Material Design 3

El primer adaptador existe en `ShellScreenRegistry` y consume únicamente
`ComposedScreen`, nunca `PluginDescriptor` ni clases internas de los plugins. Para
registrar otra pantalla, el implementador debe:

1. resolver `ScreenTextKey` mediante el sistema de recursos autorizado;
2. renderizar elementos conocidos desde una lista cerrada de componentes JSF que consumen tokens Material 3 del shell;
3. respetar visibilidad, habilitación, required y orden ya compuestos;
4. resolver `ScreenFragmentId` mediante un registro público de fragmentos del plugin propietario y colocarlo solo en el slot resultante;
5. mantener autorización, validación de negocio, auditoría y `PluginOperationGuard` en los servicios del servidor;
6. fallar de forma segura si un tipo visual o fragmento no tiene adaptador;
7. adaptar cada layout a compacto, medio y expandido sin overflow horizontal de página;
8. añadir pruebas de componente y Playwright para la pantalla estándar y cada variante soportada.

`ScreenElementType` ofrece `DISPLAY_TEXT`, `TEXT_INPUT`, `SELECT`, `ACTION` y
`DATA_TABLE`. No
inferir un renderer desde `ScreenElementId`, una clase, el nombre de la empresa o una
ruta XHTML. Si hace falta un tipo nuevo, primero se versiona el contrato neutral,
después se registra su renderer shell-owned y finalmente se prueban el caso válido y
el rechazo cuando el adaptador falta.

Este diseño evita que Jakarta Faces, Material Design, tokens CSS o una biblioteca
visual formen parte de `plugin-api`. También permite cambiar el renderer sin cambiar
el contrato empresarial. `J11-S3-08` validó por Playwright las variantes A/B en
compacto, medio y expandido; sigue siendo una demostración técnica, no un dominio ERP
productivo.

## Capítulo 10 — Migraciones, datos, respaldo y rollback

El kernel es propietario del esquema `core`; cada futuro plugin persistente será propietario de su esquema `plg_<plugin_id>`. Ningún plugin puede leer o escribir tablas privadas de otro. Referencias cruzadas usan identificadores y contratos públicos, no relaciones JPA entre plugins.

### Migraciones disponibles

- V1 crea `core.system_metadata`.
- V2 crea empresas y activaciones.
- V3 crea identidad local, membresías, roles y permisos empresariales.
- V4 crea roles globales, permisos administrativos y asignaciones a usuarios locales.
- V5 agrega auditoría técnica append-only.
- Flyway mantiene `core.flyway_schema_history` y cada plugin persistente mantiene
  `plg_<plugin_id>.flyway_schema_history`; todos validan checksums.
- Hibernate usa `validate`: nunca crea ni actualiza el esquema.

Una migración aplicada no se modifica. El siguiente cambio de `core` debe agregarse
como V6 o posterior; cada plugin conserva su propia secuencia V1, V2, etc. Todo
cambio documenta compatibilidad, respaldo y recuperación.

### Secuencia segura

1. respaldar antes de un cambio destructivo o una actualización de datos;
2. probar restauración según el [runbook de backup](../runbooks/postgresql-backup-restore.md);
3. construir una imagen de migrator y una de aplicación desde el mismo baseline y
   el mismo perfil de `logixone-plugin-set`;
4. ejecutar migrator una sola vez antes de la aplicación;
5. comprobar `schema_version`, readiness y logs sin secretos;
6. conservar el volumen al reemplazar o recrear solo la aplicación.

El fixture `reference-plugin` ya demuestra migraciones `plg_*`; su tabla
`migration_fixture` es únicamente técnica y no modela un dominio ERP. Todavía no
existen importadores productivos. No cargar datos escribiendo SQL directamente en
`core` ni en esquemas privados. Cada importador futuro deberá validar formato,
propiedad empresarial, idempotencia, errores por fila, auditoría y reversión.

### Rollback

El rollback de aplicación consiste en volver a la imagen anterior compatible, no revertir migraciones automáticamente. Las migraciones aditivas permanecen. Un rollback destructivo de datos requiere procedimiento específico, respaldo validado y autorización.

```powershell
# Conserva PostgreSQL y su volumen.
docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml down

# Ajustar LOGIXONE_APP_IMAGE a la referencia anterior compatible y recrear.
docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml up -d --wait
```

No usar `down --volumes` en un ambiente que deba conservar datos.

## Capítulo 11 — Construir, probar, desplegar y promover

### Gate integral disponible

```powershell
.\mvnw.cmd -B "-Dlogixone.postgres.integration=true" clean verify
```

Este gate ejecuta unitarias, ArchUnit, migraciones y repositorios sobre PostgreSQL real. No cerrar un corte con pruebas omitidas o relajadas.

### Variantes de la pareja WAR + migrador

```powershell
# Base: cero plugins de referencia.
.\mvnw.cmd -B -pl migrator,distribution/logixone-war -am clean package

# Solo funcional de referencia.
.\mvnw.cmd -B -Pwith-reference-plugin `
  -pl migrator,distribution/logixone-war -am clean package

# Funcional más personalizaciones A/B.
.\mvnw.cmd -B -Pwith-screen-customization-plugins `
  -pl migrator,distribution/logixone-war -am clean package
```

Usar `clean` al alternar perfiles. Inspeccionar `WEB-INF/lib` y los proveedores SPI
del ejecutable; un JAR o migración de la variante anterior no puede sobrevivir
accidentalmente.

### Imágenes candidatas

```powershell
docker buildx build --load --platform linux/amd64 `
  --build-arg LOGIXONE_MAVEN_PROFILE=with-screen-customization-plugins `
  -f infra/docker/Dockerfile.migrator `
  -t logixone/migrator:j11-s5-01 .

docker buildx build --load --platform linux/amd64 `
  --build-arg LOGIXONE_MAVEN_PROFILE=with-screen-customization-plugins `
  -f infra/docker/Dockerfile `
  -t logixone/app:j11-s5-01 .
```

### Arranque candidato

```powershell
$env:COMPOSE_PROJECT_NAME = 'logixone-candidate'
$env:LOGIXONE_APP_IMAGE = 'logixone/app:j11-s3-08'
$env:LOGIXONE_MIGRATOR_IMAGE = 'logixone/migrator:j11-s3-08'
$env:LOGIXONE_HTTP_PORT = '18080'

docker compose -f infra/compose/compose.yaml up -d --wait
curl.exe http://127.0.0.1:18080/logixone/health/live
curl.exe http://127.0.0.1:18080/logixone/health/ready
```

Resultado esperado: ambos endpoints responden `200` y `UP`; readiness contiene
`catalog`, `configuration`, `database`, `migrations` y `oidc-configuration` en verde.

Los tags son solo referencias locales. En ambientes compartidos, publicar una vez y promover exactamente `repositorio@sha256:digest`. No reconstruir la imagen por ambiente y no incorporar configuración o secretos al artefacto.

## Capítulo 12 — Diagnóstico operativo

| Síntoma | Comprobación | Respuesta segura |
|---|---|---|
| liveness 200, readiness 503 | checks públicos y logs técnicos | corregir configuración/base/migración; no reiniciar a ciegas |
| `catalog=DOWN` | `plugin_catalog_initialization_failed` | corregir descriptor, dependencia, ciclo o duplicado y reconstruir |
| empresa sin contribuciones | estado, personalización y activaciones mediante casos de uso | corregir la causa; no mostrar una interfaz estándar silenciosa |
| `CUSTOMIZATION_NOT_PRESENT` | JAR físico y perfil de distribución | incorporar artefacto compatible y redesplegar |
| `CUSTOMIZATION_ALREADY_ASSIGNED` | exclusividad persistida | crear/seleccionar otra personalización; no compartirla |
| dependencia no efectiva | grafo y decisiones de la misma empresa | activar requeridas en orden |
| overlay rechazado | diagnóstico de pantalla, versión, elemento, slot u operación | corregir todo el overlay; no aplicar parcialmente |
| conflicto de versión | releer empresa/activación | decidir si reintentar con la versión nueva |
| migrator falla por checksum | evidencia y recurso versionado | restaurar la migración original; agregar una versión nueva |
| PostgreSQL no disponible | estado del servicio y red `backend` | recuperar base; liveness debe permanecer independiente |

Comandos de diagnóstico que no muestran secretos:

```powershell
docker compose -f infra/compose/compose.yaml ps
docker compose -f infra/compose/compose.yaml logs --no-color postgres migrator app
```

No imprimir variables completas, no abrir `/run/secrets` en evidencias y no registrar SQL con datos empresariales.

## Capítulo 13 — Checklist de aceptación y entrega

### Funcional y contractual

- [ ] matriz de requisitos aprobada y cada necesidad clasificada;
- [ ] plugins funcionales, versiones y dependencias identificados;
- [ ] personalización exclusiva con identidad propia;
- [ ] overlays dentro de contratos públicos y rangos probados;
- [ ] ninguna regla de negocio o seguridad depende solo de la UI.

### Técnica

- [ ] Maven/Java correctos y gate limpio verde;
- [ ] ArchUnit y matriz WAR verdes;
- [ ] migraciones desde base vacía y versión anterior verificadas;
- [ ] repositorios, JPA `validate`, JTA, rollback e aislamiento verdes;
- [ ] imagen identificada por digest;
- [ ] liveness/readiness y smoke aprobados;
- [ ] recreación conserva volumen y datos;
- [ ] arnés de pruebas ausente del WAR normal.

### Seguridad y operación

- [ ] secretos solo por mecanismo externo;
- [ ] logs y respuestas revisados;
- [ ] respaldo/restauración probados cuando corresponde;
- [ ] rollback de aplicación documentado;
- [ ] responsables, monitoreo, escalamiento y ventana acordados;
- [ ] temporales y entornos efímeros eliminados sin tocar datos previos.

### Traspaso

- [ ] versión y compatibilidad registradas;
- [ ] runbooks y limitaciones entregados;
- [ ] capacitación realizada;
- [ ] evidencia reproducible archivada;
- [ ] pendientes y siguiente incremento priorizados.

### Demo visual obligatoria del Sprint

- [ ] demo ejecutada sobre el artefacto o digest exacto que se pretende cerrar;
- [ ] guion reproducible versionado en `docs/runbooks/`;
- [ ] recorrido real en 375, 720 y 1280 px sin overflow horizontal normal;
- [ ] autorización positiva/negativa y estados vacío/error pertinentes mostrados;
- [ ] datos exclusivamente ficticios y ausencia de secretos en pantalla/evidencia;
- [ ] capacidades implementadas y limitaciones pendientes explicadas sin simular;
- [ ] procedimiento de preparación y restauración del estado documentado.

Una presentación o mock puede acompañar la explicación, pero no cuenta como demo.
El Sprint solo puede cerrar cuando una persona puede seguir el guion contra el
sistema real y obtener los resultados documentados. Este gate se suma a pruebas,
seguridad, retrospectiva y PDF obligatorio.

### Decisión de instalador Windows al cerrar el Sprint

- [x] se preguntó explícitamente `¿Crearemos un nuevo instalador Windows para este Sprint?`;
- [x] se registró respuesta `SÍ`, fecha 2026-08-14, responsable de producto y
  razón: crear una edición interna representativa del baseline de Compras;
- [x] con `SÍ`, `current` se sustituyó de forma acotada por
  `0.9.0-internal.1` después de validar el candidato;

Si la respuesta es `SÍ`, completar además:

- [x] baseline funcional, documental, demo y PDF congelados antes de construir;
- [x] preflight de solo lectura declara compatible, advertencia o bloqueo;
- [x] máquina bloqueada/cancelación no producen cambios ni UAC en pruebas
  deterministas; falta confirmación en VM real;
- [x] lista previa declara componentes, versiones, descargas, licencias, tamaño,
  rutas, puertos, reinicios y acciones;
- [x] consentimiento explícito verificado; elevación no fue necesaria en la máquina
  adoptada y falta el escenario UAC real;
- [x] descargas y binarios fijados y validados por hash; Authenticode pendiente;
- [ ] instalación limpia en VM termina con migrator, Compose y health verdes;
- [ ] instalación, actualización y reparación reales de J11-S9-08 en VM compatible;
- [x] versión, baseline/digest, SHA-256, estado de firma y terceros evidenciados;
- [x] el directorio generado `current` contiene sólo ocho derivados del Sprint;
- [x] fuentes, pruebas, evidencias y releases publicados permanecen intactos.

La metodología está en el
[runbook de instalador Windows](../runbooks/metodologia-instalador-windows-cierre-sprint.md).
La candidata interna está en `installer/windows/current/`. Ejecute primero el
preflight y revise el plan; no distribuya el EXE porque su canal es
`INTERNAL_UNSIGNED`. El montaje manual documentado continúa siendo la alternativa
para implementación y desarrollo. La evidencia y los pendientes externos están en
[J11-S9-08](../evidence/J11-S9-08-instalador-windows-cierre.md).

Una empresa no se declara operativa solo porque WildFly arrancó. Deben cumplirse composición empresarial, datos, aceptación funcional, seguridad aplicable y operación.

## Capítulo 14 — Ejemplo ficticio completo: Distribuidora Boreal

Este ejemplo usa nombres y datos ficticios. Como el baseline aún no tiene dominios ERP productivos ni adaptador administrativo, `reference-plugin` representa una capacidad reutilizable y `reference-customization-a` representa el JAR exclusivo de Boreal. Los comandos de build, pruebas y despliegue son ejecutables; el alta empresarial se valida mediante servicios internos y el arnés opt-in.

### 14.1 Relevamiento

| ID | Pedido Boreal | Clasificación | Solución del ejemplo |
|---|---|---|---|
| BOR-001 | habilitar panel funcional | plugin funcional | `reference_plugin@1.0.0` |
| BOR-002 | etiqueta y ayuda propias | personalización visual | overlay A sobre `reference_plugin:dashboard@1.0.0` |
| BOR-003 | hacer resumen obligatorio | personalización más estricta | `ScreenChange.Require` |
| BOR-004 | aviso fiscal propio | fragmento de slot | `reference_custom_a:tax_notice` |
| BOR-005 | conservar datos al recrear | operación | volumen PostgreSQL nombrado |

Quedan fuera del ejemplo: administración productiva de usuarios/roles, facturación,
inventario y migración de datos comerciales. La UI renderizada es una demostración
técnica y no una pantalla de dominio ERP.

### 14.2 Selección y personalización

La distribución contiene físicamente el funcional y A/B para demostrar un catálogo común. La empresa Boreal se asigna solo a `reference_custom_a`; B puede pertenecer a otra empresa, pero nunca participa en la composición de Boreal.

El descriptor A:

- declara `PluginKind.CUSTOMIZATION`;
- es compatible con `plugin-api` `[0.4.0,0.5.0)`;
- depende de `reference_plugin` `[1.0.0,2.0.0)` como `REQUIRED`;
- apunta a `reference_plugin:dashboard` versión `[1.0.0,2.0.0)`;
- usa únicamente elementos/slots públicos;
- aporta un fragmento cuyo propietario es A.

### 14.3 Pruebas antes de construir la imagen

```powershell
.\mvnw.cmd -B -pl plugins/reference-plugin,plugins/reference-customization-a `
  -am test

.\mvnw.cmd -B -pl kernel-application,tests/architecture-tests -am test

.\mvnw.cmd -B "-Dlogixone.postgres.integration=true" clean verify
```

Casos negativos que deben permanecer verdes: versión incompatible, elemento/slot desconocido, operación prohibida, conflicto de cambio, posición inválida, exceso de capacidad y fragmento de otro propietario.

### 14.4 Construcción y arranque

Ejecutar los comandos del capítulo 11 con el perfil `with-screen-customization-plugins`. Verificar en el log:

```text
plugin_count=3 plugins=reference_plugin@1.0.0,reference_custom_a@1.0.0,reference_custom_b@1.0.0
```

Esa línea solo demuestra presencia física; no asigna B a Boreal.

### 14.5 Alta y activación interna

**Interno:** un adaptador administrativo autorizado debe:

1. registrar Boreal `INACTIVE` con `reference_custom_a`;
2. habilitar `reference_plugin`;
3. cambiar Boreal a `ACTIVE` con la versión esperada;
4. consultar contribuciones y pantallas;
5. ejecutar operaciones funcionales mediante `PluginOperationGuard`.

No existe endpoint productivo para estos pasos. Para certificar el flujo dentro de WildFly se usa exclusivamente el arnés:

```powershell
.\mvnw.cmd -B -Pjta-runtime-harness `
  -pl tests/runtime-persistence-harness -am package

.\mvnw.cmd -B -pl tests/integration-tests `
  "-Dlogixone.base-uri=http://127.0.0.1:18080" `
  "-Dlogixone.jta-probe=true" verify
```

El escenario A esperado conserva el mismo `ScreenId`, cambia etiqueta/ayuda, marca el resumen requerido y contiene solo el fragmento A. Otra empresa con B obtiene su propio resultado sin filtración cruzada.

### 14.6 Persistencia, recreación y rollback

1. conservar una empresa/activación sintética mediante el escenario de prueba;
2. ejecutar `docker compose down` sin `--volumes`;
3. recrear la composición y comprobar migrator sin cambios y filas conservadas;
4. para rollback, seleccionar la imagen anterior compatible por digest y recrear solo aplicación/migrator según el runbook;
5. eliminar el volumen únicamente si el proyecto Compose fue creado expresamente como efímero y la evidencia ya fue guardada.

### 14.7 Entrega del ejemplo

La evidencia debe contener requisitos BOR-001–005, versiones, resultado de gates,
digest, health, persistencia, rollback, limitaciones y responsables. Boreal no puede
considerarse una implementación ERP productiva hasta incorporar dominios reales,
administración operativa y aceptación empresarial; identidad, autorización y shell
ya forman parte del baseline técnico.

## Capítulo 15 — Diseñar documentos comerciales con referencia SIFEN

Factura, nota de crédito, nota de débito y nota de remisión son capacidades futuras
de plugins funcionales. Para diseñarlas, el implementador debe consultar
[ADR-0010](../adr/0010-modelo-canonico-documentos-referencia-sifen.md) y el
[análisis estructural de SIFEN v150](../knowledge-base/sifen-v150-estructura-documentos.md).
La referencia de 2019 sirve para aprender la organización de datos; no autoriza una
integración fiscal ni reemplaza la documentación oficial vigente.

El procedimiento obligatorio será:

1. obtener del canal oficial el manual, XSD, catálogos y reglas vigentes; registrar
   origen, versión y checksum sin versionar material externo innecesario;
2. caracterizar los casos de uso y el ciclo de vida de cada tipo de documento;
3. diseñar un agregado canónico con UUID interno, cabecera común, snapshots de
   participantes, ítems, ajustes, impuestos, pagos, cuotas, totales y referencias;
4. agregar extensiones tipadas para factura, nota de crédito/débito y remisión, con
   transporte y ubicaciones cuando corresponda;
5. separar estados comercial, fiscal y logístico y registrar eventos append-only;
6. implementar un adaptador versionado que traduzca el agregado al SIFEN aplicable;
7. conservar el XML firmado, hash, CDC, versión, envíos y respuestas como evidencia
   inmutable, además de las tablas relacionales operativas;
8. probar precisión, redondeo, numeración, concurrencia, snapshots, referencias,
   firma, respuestas y migraciones antes de declarar compatibilidad.

No se debe copiar el XSD a una tabla de cientos de columnas, guardar el negocio solo
como XML/JSON ni usar EAV como sustituto del modelo. Tampoco se recalcula un
documento emitido desde maestros actuales: emisor, receptor, direcciones, conceptos
y valores quedan congelados. Las correcciones se representan mediante notas o
eventos relacionados.

El kernel no contiene estas tablas ni cálculos. El plugin propietario conserva su
esquema y migraciones; otro plugin lo referencia mediante IDs y contratos públicos,
nunca importando entidades JPA o consultando tablas privadas. La épica futura está
registrada en el
[backlog de documentos comerciales y SIFEN](../backlog/epica-documentos-comerciales-y-sifen.md).

### Preparar facturación masiva

La generación de muchas facturas en una fecha común pertenece a
`commercial_documents`; no se crea un plugin `bulk_billing`. El implementador debe
mantener dos procesos distintos:

1. el lote comercial recibe candidatos versionados, prevalida, congela, aprueba y
   crea facturas canónicas independientes;
2. el plugin `sifen` toma los documentos emitidos y forma sus lotes técnicos de
   firma, transmisión y consulta.

El lote comercial debe ser persistente y recuperable. Cada ítem usa una clave
idempotente por empresa, origen, versión, período y tipo documental; se ejecuta en
una transacción corta y conserva su propio resultado. Una caída o repetición no
puede duplicar facturas. No use `MAX + 1`, una transacción para todo el lote, un
bucle controlado por la vista ni tasas tributarias embebidas.

Separe período facturado, fecha de corte, fecha comercial propuesta, instante de
emisión, firma, transmisión y vencimiento. Una fecha común no autoriza
retrofechado: antes de numerar se revalidan empresa, permiso, zona horaria,
autorización fiscal, establecimiento/punto, moneda, receptor, impuestos y totales.

Los plugins de origen llaman al contrato público de documentos con IDs y versiones;
no comparten entidades. ADR-0033 confirma `recurring_billing` como propietario de
planes, suscripciones, prorrateos, consumo facturable y corridas de cargos. El
plugin solamente produce candidatos; documentos conserva el lote de emisión y la
factura.

Consulte [ADR-0031](../adr/0031-facturacion-masiva-en-documentos-comerciales.md),
la [caracterización](../knowledge-base/commercial-documents/facturacion-masiva-legacy-characterization.md)
y la [épica específica](../backlog/epica-facturacion-masiva.md). Para planes y uso,
consulte [ADR-0033](../adr/0033-dominio-facturacion-recurrente.md) y la
[épica recurrente](../backlog/epica-facturacion-recurrente.md).

## Capítulo 16 — Aplicar el roadmap de plugins productivos

Después de cerrar `J11-S4-08`, el implementador no debe comenzar directamente por
facturación ni copiar el plugin de referencia.
[ADR-0011](../adr/0011-roadmap-dependencias-plugins-productivos.md) y
[ADR-0027](../adr/0027-terminal-punto-venta-y-ampliacion-roadmap.md), ampliados por
[ADR-0030](../adr/0030-familia-recursos-humanos-nomina-paraguay.md) y
[ADR-0032](../adr/0032-plugin-estaciones-servicio-combustible.md) y
[ADR-0033](../adr/0033-dominio-facturacion-recurrente.md) y
[ADR-0034](../adr/0034-plugin-telemetria-vehicular.md), definen diecinueve
plugins ERP reutilizables y una personalización distinta por empresa.
[ADR-0035](../adr/0035-operacion-offline-terminal-punto-venta.md) no cambia la
cantidad ni el orden: hace obligatoria la venta offline de la primera versión
productiva del POS. [ADR-0036](../adr/0036-operaciones-proveedor-soporte-lanzamientos-conector.md)
agrega una familia separada de operaciones del proveedor sin renumerar esta
secuencia. [ADR-0037](../adr/0037-familia-cooperativa-ahorro-credito-paraguay.md)
agrega un perfil vertical cooperativo separado. [ADR-0038](../adr/0038-plugin-datos-referencia-normativos.md)
agrega una fundación R0 sin renumerar los diecinueve. Finalmente,
[ADR-0040](../adr/0040-modulo-tecnico-migracion-legados-oracle-forms-reports.md)
planifica `legacy_migration` como técnico transversal opcional, también sin orden
ERP ni cambio de la secuencia funcional. [ADR-0045](../adr/0045-plugin-gestion-procesos-negocio-bpm.md)
planifica además `business_process_management` como funcional transversal,
reutilizable y opcional por empresa. [ADR-0046](../adr/0046-familia-mantenimiento-flota-taller-automotriz.md)
agrega la familia vertical F1 `fleet_maintenance` y F2 `automotive_workshop`, sin
renumerar ERP 1–19:

| Orden | Plugin | Capacidad que estabiliza |
|---:|---|---|
| R0 | `reference_data` | países, monedas, procedencia y políticas por empresa |
| 1 | `business_partners` | clientes, proveedores, contactos y direcciones |
| 2 | `commercial_catalog` | productos, servicios, unidades, impuestos y precios |
| 3 | `inventory` | depósitos, existencias, movimientos y reservas |
| 4 | `purchasing` | órdenes, recepciones y devoluciones a proveedores |
| 5 | `sales` | presupuestos, pedidos y compromisos de venta |
| 6 | `logistics` | preparación, despacho, transporte y entrega |
| 7 | `vehicle_telemetry` | dispositivos, posición, recorridos, sensores, geocercas y seguimiento |
| 8 | `commercial_documents` | factura, notas, remisión, snapshots y totales |
| 9 | `recurring_billing` | planes, suscripciones, prorrateo, consumo y cargos |
| 10 | `sifen` | proyección, firma, transmisión y eventos fiscales |
| 11 | `treasury` | cajas, bancos, cobros, pagos y conciliación |
| 12 | `point_of_sale` | terminales, sesiones de cajero y checkout rápido online/offline con sincronización idempotente |
| 13 | `fuel_station` | tanques, surtidores, picos, turnos, lecturas y conciliación húmeda |
| 14 | `accounts_receivable` | deuda de clientes, cuotas y cobranzas |
| 15 | `accounts_payable` | obligaciones y pagos a proveedores |
| 16 | `accounting` | asientos, períodos, mayores y cierres |
| 17 | `human_resources` | legajo, relación laboral, organización, ausencias y tiempo |
| 18 | `payroll` | conceptos, períodos, liquidaciones y recibos neutrales |
| 19 | `payroll_paraguay` | reglas y artefactos IPS/MTESS versionados |
| último | `<empresa>_customization` | cambios exclusivos sobre contratos ya estabilizados |

Para `N` empresas, una distribución completa puede contener `20 + N` plugins
productivos. La fórmula describe presencia física; cada empresa activa solamente
las capacidades seleccionadas y usa exactamente su personalización asignada.

La familia de operaciones del proveedor amplía el catálogo global futuro a
veintitrés reutilizables, pero usa perfiles distintos:

| Plugin | Dónde se compone | Uso |
|---|---|---|
| `customer_support` | instancia central del proveedor | cobertura, instalaciones, casos, SLA y resolución |
| `release_management` | instancia central del proveedor | mejoras, correcciones, candidatos, gates y releases |
| `support_connector` | ERP del cliente, opcional | conexión HTTPS saliente y diagnósticos consentidos |

La clase técnica es todavía conceptual: `PluginKind` sólo expone `FUNCTIONAL` y
`CUSTOMIZATION` en el baseline. Antes de generar el conector debe aprobarse y
versionarse de forma compatible una eventual clase `TECHNICAL`; no se usa
`CUSTOMIZATION` como sustituto.

Los dos plugins centrales no se entregan por defecto al cliente. Si una
distribución ERP incluye el único JAR `support_connector`, la fórmula física pasa
a `21 + N`; su activación y consentimiento siguen siendo independientes por
empresa. El conector no abre puertos administrativos, no ejecuta shell, SQL o
scripts, no instala actualizaciones y no bloquea el ERP si soporte central está
caído. Para detalles y planificación consulte las épicas de
[soporte](../backlog/epica-soporte-clientes-erp.md),
[releases](../backlog/epica-gestion-lanzamientos-erp.md) y
[conector](../backlog/epica-conector-soporte-seguro.md).

La familia cooperativa amplía el catálogo global futuro a veintinueve reutilizables
y tampoco renumera ERP 1–19:

| Orden interno | Plugin | Uso |
|---:|---|---|
| C1 | `cooperative_membership` | socios, estado, aportes y desvinculación |
| C2 | `cooperative_governance` | asambleas, órganos, mandatos y decisiones |
| C3 | `aml_compliance` | debida diligencia, riesgo, alertas y casos LA/FT |
| C4 | `cooperative_savings` | productos, cuentas, saldos, intereses y restricciones |
| C5 | `cooperative_credit` | solicitud, aprobación, cartera, garantías y cobranza |
| C6 | `cooperative_regulatory_paraguay` | reglas, cálculos y artefactos INCOOP/SEPRELAD |

Para una cooperativa no se debe activar el conjunto ERP completo por costumbre.
El perfil mínimo futuro reutilizará `business_partners`, `treasury`, `accounting`
y los seis anteriores; ventas, inventario, POS, nómina o soporte se agregan sólo
si la institución realmente los necesita. Esta tabla es planificación, no una
composición disponible.

Durante el relevamiento, el implementador deberá distinguir expresamente:

- persona/organización en `business_partners` y condición de socio en membresía;
- aporte social, cuenta de ahorro, caja/banco y asiento contable;
- préstamo cooperativo y deuda comercial de una factura;
- regla del dominio y parámetro/regla paraguaya versionada;
- dato fuente, proyección regulatoria y artefacto presentado.

COOP-00 debe confirmar tipo/nivel, estatuto, productos, fuentes oficiales,
checksums, matriz LA/FT, plan de cuentas, privacidad, segregación, migración y
reconciliación. Ningún dato real ni operación financiera se habilita antes de
cerrar esos gates y los contratos de tesorería/contabilidad. Consulte la
[épica cooperativa](../backlog/epica-cooperativa-ahorro-credito-paraguay.md).

El primer instrumento de relevamiento es
[COOP-00](../backlog/COOP-00-gobierno-alcance-matriz-normativa.md). Solicita
estatuto, tipo/nivel, reglamentos, productos, plan de cuentas, matriz LA/FT,
canales regulatorios y fuentes de migración; define COOP-D01–D15 y gates G0–G5.
El implementador debe usar también el
[registro regulatorio inicial](../knowledge-base/cooperative-savings-credit/regulatory-scope-analysis.md)
y el [mapa de límites](../architecture/cooperative-savings-credit-boundaries.md).
Estos documentos no autorizan pedir bases productivas o secretos por canales no
aprobados ni empezar módulos mientras las decisiones continúen pendientes.

### Cómo planificar una migración desde Oracle Forms & Reports

> **Pendiente:** `legacy_migration` no existe todavía en el reactor, no aporta
> pantallas ni puede ejecutar importaciones. Esta sección define el trabajo de
> implantación futuro; no es un procedimiento operativo disponible.

Con ADR-0040, el catálogo global futuro pasa de veintinueve a treinta plugins
reutilizables. El nuevo plugin no sustituye `purchasing`, ventas, inventario ni
otro dominio: coordina descubrimiento, mapeo, simulación, cuarentena,
reconciliación y corte. Cada destino sigue siendo dueño de sus reglas y recibe
datos únicamente mediante comandos o puertos públicos tipados.

Antes de cotizar o ejecutar una migración, el implementador deberá:

1. inventariar módulos Forms (`.fmb`), menús (`.mmb`), bibliotecas (`.olb`,
   `.pll`/`.pld`), reportes (`.rdf`/`.rex`), objetos de base y volúmenes;
2. registrar versión de Oracle, codificación, esquemas, dependencias, jobs,
   integraciones, reglas PL/SQL, permisos y ventanas de indisponibilidad;
3. obtener una extracción reproducible y de solo lectura, preferentemente como
   paquete portátil inmutable con manifiesto, checksums y evidencia de origen;
4. clasificar cada objeto como dato, regla, pantalla, reporte, integración o
   artefacto sin destino; una pantalla o trigger no se transpila automáticamente;
5. acordar por entidad el mapeo versionado, transformaciones, catálogos, claves,
   procedencia, criterio de aceptación, propietario funcional y tratamiento de
   rechazos;
6. exigir que el plugin destino publique una API de importación idempotente y
   autorizada; queda prohibido insertar en sus tablas privadas o compartir JPA;
7. ejecutar perfilado, simulación, importaciones reanudables, cuarentena y
   reconciliación antes del corte;
8. ensayar respaldo del destino, congelación o punto consistente del origen,
   carga base/delta, decisión `go/no-go`, observación y rollback sin escribir ni
   borrar masivamente el Oracle original.

Las herramientas Oracle y sus drivers se ejecutarán en un runner externo efímero,
con licencia y secretos suministrados fuera del código. No se incorporarán al WAR,
a los repositorios ni a una imagen comercial sin autorización de redistribución.
El plugin Jakarta administrará proyectos, paquetes, mapeos, corridas, rechazos,
conciliaciones y aprobaciones; el runner sólo inspeccionará y producirá artefactos
de entrada verificables.

La oferta de reemplazo de Oracle sólo será comercializable después de completar
LM-00 a LM-09, adaptar los módulos funcionales incluidos, ensayar el corte y
demostrar idempotencia, reanudación, aislamiento empresarial, seguridad negativa,
reconciliación sin diferencias críticas y rollback. Las fuentes canónicas son la
[épica de migración](../backlog/epica-migracion-legados-oracle-forms-reports.md) y
el [perfil Oracle Forms & Reports](../knowledge-base/legacy-migration/oracle-forms-reports-source-profile.md).

### Cómo planificar procesos configurables con BPM

> **Pendiente:** `business_process_management` no existe en el reactor y no debe
> prometerse como capacidad disponible. La planificación no selecciona motor ni
> biblioteca de modelado y no modifica J11-S9-06.

Con ADR-0045, el catálogo global futuro pasa de treinta a treinta y un plugins
reutilizables. BPM permite que una empresa configure tareas, responsables, plazos,
escalamientos y seguimiento, pero el plugin funcional conserva sus datos, estados,
reglas y autorización. Una tarea BPM completada no cambia por sí sola una orden,
factura o legajo.

Antes de ofrecer esta capacidad, el implementador deberá:

1. completar BPM-00 y aceptar BPM-D01 a BPM-D12;
2. seleccionar un motor mediante un spike reproducible compatible con Java 21,
   Jakarta EE 11, WildFly 41, PostgreSQL, licencia y operación del producto;
3. fijar un subconjunto BPMN 2.0.2 cerrado y rechazar elementos no soportados;
4. registrar sólo eventos y acciones públicas tipadas, versionadas, autorizadas e
   idempotentes, sin SQL, scripts o HTTP arbitrario;
5. modelar definiciones publicadas e instancias como versiones inmutables y
   preservar tareas, temporizadores, incidentes e historia tras reinicios;
6. definir retención y clasificación de variables, evitando secretos y datos
   personales innecesarios;
7. probar composición y operación con BPM presente, ausente, activo e inactivo;
8. ejecutar el piloto de aprobación de solicitudes de Compras sin hacer que
   `purchasing` dependa de BPM.

Consulte la [épica BPM](../backlog/epica-gestion-procesos-negocio-bpm.md) para las
historias BPM-00 a BPM-08, criterios y matriz automatizada.

### Cómo planificar mantenimiento de flota y taller automotriz

> **Pendiente:** F1 y F2 están planificados, pero no existen en el reactor, no
> tienen descriptores, esquemas ni pantallas ejecutables y no modifican J11-S9-06.

Con ADR-0046, el catálogo global futuro pasa de treinta y uno a treinta y tres
plugins reutilizables. La familia usa una secuencia vertical propia:

| Orden | Plugin | Inicio permitido |
|---:|---|---|
| F1 | `fleet_maintenance` | después de estabilizar `logistics-api` y `VehicleId` |
| F2 | `automotive_workshop` | después de F1, `sales` y `commercial_documents` |

Para implantar F1, el implementador debe:

1. relevar categorías, cantidad de vehículos, talleres, puestos, responsables y
   políticas por fecha, kilómetros u horas;
2. mantener vehículo/categoría en Logística y guardar sólo IDs/snapshots en F1;
3. definir planes publicados inmutables y disparos idempotentes por plan, versión,
   vehículo, umbral y ciclo;
4. validar fuente, unidad, instante, calidad y plausibilidad de una lectura de
   Telemetría, manteniendo captura manual autorizada cuando esté ausente;
5. solicitar reservas/consumos a Inventario y reposición/terceros a Compras sin
   acceder a sus tablas;
6. usar `ActorId` para técnicos y conservar únicamente asignación/tiempo de OT;
7. proteger acceso móvil con autenticación, vencimiento, revocación y
   reautorización; no reutilizar tokens permanentes del legado;
8. separar cargas de combustible de lubricantes consumidos como repuesto;
9. probar generación preventiva, concurrencia, reinicio, rechazo externo,
   inmutabilidad de cierre, disponibilidad y composición con integraciones
   presentes/ausentes.

Para F2, el implementador debe mantener recepción, condición inicial,
autorizaciones y entrega, pero referenciar la única OT de F1, presupuesto/pedido de
Ventas y factura/notas de Documentos Comerciales. Tesorería y Cuentas por Cobrar
conservan pago y deuda. Una autorización externa identifica versión y alcance,
vence y puede revocarse; nunca concede permisos internos ni acceso a otra empresa.

Producto aceptó FM-D01 a FM-D12 y AW-D01 a AW-D10 sin cambios el 2026-08-12. Las
fuentes canónicas son el [ADR-0046](../adr/0046-familia-mantenimiento-flota-taller-automotriz.md),
la [caracterización](../knowledge-base/vehicle-maintenance/legacy-characterization.md),
la [épica F1](../backlog/epica-mantenimiento-flota.md) y la
[épica F2](../backlog/epica-taller-automotriz-comercial.md).

### Qué está disponible de Compras en J11-S9-07

> **Compuesta y validada automáticamente:** dominio, tablas, repositorios,
> permisos, aplicación y cinco recorridos visuales forman parte del WAR y del
> migrador mediante `with-purchasing-demo`. Maven, PostgreSQL, ArchUnit,
> Docker/Compose, migraciones, health/OIDC y Playwright están verdes. La
> validación independiente, Authenticode y la matriz Windows de J11-S9-08 siguen
> pendientes.

El reactor contiene `purchasing-api@1.1.0` y `purchasing@1.1.0`. La API publica
identidades, consultas y comandos controlados para importar solicitudes u órdenes
abiertas con procedencia. El dominio separa solicitud, orden, recepción y
devolución; conserva snapshots de proveedor, artículo/unidad y moneda; impide
sobre-recepción y devolución mayor al neto recibido; y exige movimiento público de
inventario para líneas `STOCK`.

El plugin declara `plg_purchasing` V1–V2 con once tablas y una unidad JPA en
`validate`. Cuatro repositorios de agregado y dos ledgers reciben siempre empresa,
conservan orden de líneas,
asignaciones y snapshots, y no exponen borrado. Solicitudes finalizadas,
recepciones confirmadas y devoluciones confirmadas son inmutables; una confirmación
de stock exige una referencia pública de movimiento. No cree el esquema a mano ni
active actualización automática de Hibernate.

La aplicación declara doce permisos. Un receptor que confirme stock necesita
`purchasing.receipts.confirm` y el permiso acotado
`inventory.movements.purchase.post`; no necesita el permiso general de movimiento
manual. V2 deduplica mutaciones e importaciones por empresa, huella y procedencia.

J11-S9-05 agrega las rutas de solicitudes, órdenes, recepciones, devoluciones y
seguimiento. Los handlers revalidan empresa y permiso en el servidor, consultan
directorios paginados y usan selectores públicos de proveedores, artículos,
depósitos y ubicaciones. El shell conserva el renderer, Material Design 3 y los
tres rangos responsive. El recorrido de recepción y el de devolución crean una
línea por documento en este primer corte; para más líneas se repite la operación
antes de confirmar. Consulte el
[manual 07 de Compras](../user-guide/modules/compras.md) para datos, acciones,
permisos y recuperación ante errores.

Un implementador no debe registrar manualmente el descriptor, ejecutar V1/V2
fuera del migrador oficial ni copiar JARs al WAR. Debe seleccionar el perfil
`with-purchasing-demo`, que mantiene aplicación y migrador coherentes. Consulte
[ADR-0041](../adr/0041-modelo-purchasing-y-contratos-publicos.md),
[ADR-0042](../adr/0042-persistencia-privada-purchasing.md) y
[J11-S9-03](../sprints/sprint-09/J11-S9-03-persistencia-purchasing.md),
[ADR-0043](../adr/0043-aplicacion-jta-idempotencia-purchasing.md) y
[J11-S9-04](../sprints/sprint-09/J11-S9-04-aplicacion-purchasing.md),
[ADR-0044](../adr/0044-recorridos-visuales-purchasing.md) y
[J11-S9-05](../sprints/sprint-09/J11-S9-05-interfaz-purchasing.md), además de
[J11-S9-06](../sprints/sprint-09/J11-S9-06-integracion-composicion-purchasing.md) y
[J11-S9-07](../sprints/sprint-09/J11-S9-07-validacion-demo-cierre.md).

Este es un orden de construcción. En runtime, `PluginCatalogResolver` valida el
grafo y calcula el orden topológico. Un plugin declara únicamente dependencias
públicas necesarias y nunca importa entidades, repositorios, beans o XHTML de otro.

La secuencia evita modelos provisionales:

- documentos usan participantes y conceptos estables y conservan snapshots;
- telemetría referencia `VehicleId` de logística y posee dispositivos,
  observaciones, recorridos y seguimiento; pausar o detener conserva historia y
  no ejecuta control físico remoto;
- facturación recurrente calcula cargos reproducibles desde planes, prorrateo y
  consumo, y entrega candidatos versionados a documentos sin poseer facturas;
- SIFEN depende de la proyección fiscal pública del documento canónico;
- tesorería publica liquidaciones para cobrar/pagar sin compartir tablas;
- punto de venta coordina catálogo, inventario, ventas, documentos y tesorería,
  pero no reemplaza sus fuentes de verdad ni obliga a usar SIFEN; offline conserva
  sólo proyecciones acotadas y un diario cifrado, y sincroniza efectos idempotentes
  al reconectar;
- estaciones de servicio conserva la topología física, lecturas, despachos y
  conciliación húmeda; catálogo conserva productos/precios, inventario el stock
  contable y POS/documentos/tesorería la venta, el comprobante y el cobro;
- contabilidad consume eventos y no dirige ventas, inventario o tesorería;
- recursos humanos conserva una identidad laboral distinta del participante
  comercial y del usuario de acceso;
- nómina consume contratos públicos de recursos humanos y publica hechos hacia
  finanzas sin compartir entidades;
- `payroll_paraguay` adapta reglas oficiales versionadas sin invadir nómina;
- la personalización se crea al final porque necesita `ScreenId`, slots y rangos de
  versión ya probados.

El descubrimiento y la ejecución de migraciones `plg_*` y la plantilla reproducible
de plugins ya están habilitados y validados. ADR-0013 fijó el contrato mínimo de
eventos/outbox sin inventar infraestructura. J11-S5-04 reconfirmó los gates, la demo
visual responsive y el PDF del corte técnico.
Sprint 6 comenzó `business_partners` por caracterización antes de dominio, esquema,
contratos, permisos, pantallas responsive, pruebas, demo visual y PDF de cierre.
El detalle de alcance y criterios se conserva en la
[épica del roadmap](../backlog/epica-roadmap-plugins-productivos.md) y en el
[plan de Sprint 6](../sprints/sprint-06/README.md).

### Cómo consumir el contrato neutral de catálogo

Desde J11-S7-02, `commercial_catalog` es otro plugin y expone únicamente
`commercial-catalog-api`, actualmente `1.1.0`. Un plugin consumidor puede solicitar por empresa
una `CatalogItemReference`, convertir cantidades con `CatalogUnitConversions` o
cotizar una lista explícita mediante `CatalogPricing`. Nunca importa
`CatalogItem`, `PriceList`, entidades futuras o repositorios internos.

Desde J11-S9-05, `CatalogSearchCriteria` también acepta alcances `PURCHASE` y
`SALE`. El repositorio aplica el alcance en PostgreSQL antes de contar y paginar;
un selector no debe recuperar una página amplia para filtrarla localmente.

Inventario, compras, ventas y documentos deben persistir su propio snapshot de
descripción, unidad/factor, cantidad, moneda, modo tributario, importe, vigencia y
versión efectivamente usados. Guardar solo un FK lógico al ítem o recalcular un
documento histórico con el catálogo actual no es aceptable.

El tipo producto/servicio es inmutable, pero compra y venta son alcances que pueden
coexistir. La unidad base tiene factor `1`; cada conversión adicional es específica
del ítem. Una lista fija moneda, impuesto, escala y redondeo; el puerto no decide
qué lista corresponde a un cliente ni aplica promociones.

J11-S7-03 materializa internamente el esquema privado, repositorios y secuencia de
códigos. La V1 crea veinte tablas en `plg_commercial_catalog`; V2 agrega cuatro
tablas de revisión append-only para definiciones simples y retroalimenta la
versión vigente. V3 agrega un vínculo opcional de reemplazo por cada tipo simple,
con FK privada de misma empresa y restricciones que exigen origen inactivo. V4
agrega cabecera y atributos de familias por revisión, retroalimenta las filas
vigentes y versiona ambas tablas de asignación para que apunten a una estructura
inmutable. JPA
sólo valida el esquema y nunca ejecuta DDL. Los repositorios reconstruyen ítems y listas completos,
siempre por `CompanyId`, preservan historia y convierten conflictos de versión,
unicidad, referencia y vigencia a códigos estables. La secuencia es atómica por
empresa/ámbito, es tolerante a huecos y nunca usa `MAX + 1`.

Desde J11-S7-04 existen internamente cuatro permisos separados: `view`,
`items.manage`, `prices.manage` y `definitions.manage`. Los casos de uso autorizados
permiten alta y mantenimiento optimista de ítems, identificadores, unidades,
clasificación, variantes, listas y entradas de precio. Las seis familias de
definiciones se registran y consultan por servicios propios; los plugins consumidores
pueden usar directorio, conversión y cotización neutrales. Toda mutación produce
auditoría técnica sin nombres, identificadores comerciales ni importes.

La empresa se toma exclusivamente del contexto autenticado y cada llamada exige el
plugin y permiso exactos antes del repositorio. La UI no recibe un `companyId`
confiable del navegador, no conserva pruebas de autorización en sesión ni usa SQL
para crear definiciones. Los resultados de acceso, versión, código,
identificador, referencia y vigencia son estables y no exponen excepciones SQL.

Desde J11-S8-C02 el descriptor aporta cinco recorridos separados: `/catalog` para
artículos/servicios, `/catalog/price-lists` para listas y
`/catalog/tax-profiles` para perfiles internos, más `/catalog/definitions` para
consultar, registrar, revisar, consultar historial, inactivar, reactivar y reemplazar unidades, categorías, marcas y
etiquetas, y `/catalog/variant-families` para alta, consulta, revisión completa,
historial y ciclo de sus plantillas. Cada recorrido separa
directorio, alta y ficha; artículos y listas requieren `commercial_catalog.view`,
mientras que los maestros requieren `commercial_catalog.definitions.manage` y las
demás mutaciones exigen el permiso específico de artículos o precios. El shell
conserva XHTML, Material Design 3, responsive y accesibilidad; el plugin sólo aporta
contratos e interacción neutrales y publica slots acotados de directorio/detalle.

El recorrido de definiciones, incluido el ciclo activo/inactivo de una unidad, fue
validado en 375, 720 y 1280 px. Su guion y
evidencia están en el
[runbook J11-S8-C02](../runbooks/demo-definiciones-catalogo-j11-s8-c02.md).

El decimocuarto corte aplica a las familias el mismo límite de empresa, permiso y
versión esperada. La inactivación conserva identidad y atributos ordenados; la
reactivación incrementa nuevamente la versión. La administración consulta ambos
estados. El decimonoveno corte permite asignarlas desde Artículos y servicios:
el navegador envía identidad/revisión y valores neutrales, y la aplicación vuelve
a resolver la familia dentro de la empresa, bloquea su revisión vigente y valida
estado, versión, atributos obligatorios, atributos desconocidos y tipos antes de
persistir. No se implementa mediante SQL manual ni se confía en el tipo enviado
por el cliente.

Desde J11-S7-06 el perfil `with-commercial-catalog-demo` compone físicamente el
catálogo junto con participantes y usa la misma selección en WAR y migrador. La
empresa A puede habilitarlo desde “Plugins por empresa” y conceder sus cuatro
permisos desde “Seguridad empresarial”; el shell fusiona ambos menús productivos
sin una lista fija adicional. El fixture controlado aporta unidades, categoría,
marca y tres perfiles tributarios ficticios —general, reducido y exento— sólo para
demostración. No representan tasas oficiales ni una configuración SIFEN
certificada.

La candidata visual ejecuta alta de artículo, identificador, clasificación, lista
y precio mediante casos de uso reales, además de denegación al desactivar el
plugin. Fue validada en 375, 599, 600, 720, 839, 840 y 1280 px. Para reproducirla,
seguir el [runbook J11-S7-06](../runbooks/demo-commercial-catalog-j11-s7-06.md).
El detalle contractual permanece en
[`plugin-contract.md`](../../plugins/commercial-catalog/docs/plugin-contract.md).

### Cómo integrar inventario sin acceder al catálogo privado

Desde J11-S8-02, `inventory` es un plugin funcional separado y su descriptor exige
`commercial_catalog` compatible en el rango `[1.0.0,2.0.0)`. El implementador no
debe crear FK, relación JPA, consulta SQL o import hacia clases internas del
catálogo. La inscripción local recibe un `CatalogItemReference`, valida que sea un
`PRODUCT` activo y conserva identidad, código, nombre, unidad base y versión.

Cada depósito crea una ubicación `GENERAL`. Empresas que no necesitan posiciones
físicas la preseleccionarán; las demás podrán crear ubicaciones de almacenamiento,
recepción o despacho. Nunca se usa ubicación nula. La política del concepto define
si la clave admite lote, exige una serie o requiere vencimiento; la condición
operativa es disponible, cuarentena o dañada.

Los consumidores autorizados usan `inventory-api@1.1.0` para consultar una clave
exacta, contabilizar un movimiento idempotente o administrar una reserva. Las
cantidades se guardan en unidad base con hasta seis decimales y el factor de
conversión con hasta doce. Cada movimiento conserva la entrada presentada y la
versión del catálogo utilizada; no se recalcula el pasado.

Compras usa `postCatalogItem`: entrega `CatalogItemId`, bucket y trazabilidad, e
Inventario resuelve su `InventoryItemId` privado. El rol necesita
`inventory.movements.purchase.post`; no se permite resolver la identidad local con
SQL o un contrato de persistencia.

Una salida o reserva que vuelva negativo el físico o el disponible debe rechazarse
en la misma transacción. Las correcciones se expresan como reversión o ajuste; un
conteo nunca sustituye el saldo silenciosamente. Costos, monedas y valoración no
pertenecen al primer contrato. La persistencia ya está disponible dentro del JAR:
V1 crea nueve tablas y V2 agrega el recibo inmutable de operaciones de reserva en
`plg_inventory`. La unidad `logixone-inventory-pu` valida diez entidades y los siete
repositorios requieren empresa, conservan snapshots y no permiten borrado físico.

La aplicación publica `availability`, `movements` y `reservations` y separa siete
permisos: lectura, depósitos, artículos, movimientos, reservas, conteos y ajustes.
Los adaptadores CDI revalidan empresa y permiso exactos y la frontera JTA marca
rollback ante cualquier mutación fallida. J11-S8-05 publica los menús/pantallas
`Existencias`, `Depósitos` y `Conteos`; sus handlers usan únicamente estos casos de
uso y contratos públicos.

Desde J11-S8-C03, `with-inventory-demo` incorpora físicamente datos normativos,
participantes, catálogo e inventario con la misma selección para WAR y migrador. El registro
descubre siete descriptores: cuatro productivos y tres fixtures. Habilite primero
`reference_data`, luego sus consumidores, y conceda permisos por administración;
después debe iniciarse una sesión
nueva para obtener el snapshot actualizado de autoridades. El shell fusiona sus
opciones sin importar código privado del plugin.

Desde J11-S9-06, `with-purchasing-demo` extiende esa misma selección con Compras
y conserva idéntico cierre físico en WAR y migrador. Active sus dependencias antes
de `purchasing`, conceda los doce permisos según función y renueve la sesión tras
cambiar autoridades. No combine una aplicación construida con este perfil con un
migrador de `with-inventory-demo`.

La candidata valida depósito/`GENERAL`, inscripción, entrada 12, reserva 3,
disponibilidad 9, conteo contabilizado y denegación al desactivar. Se ejecutó en
375, 599, 600, 720, 839, 840 y 1280 px. Para reproducirla, seguir el
[runbook J11-S8-06](../runbooks/demo-inventory-j11-s8-06.md). No usar el fixture o
los datos operativos ficticios como mecanismo de carga productiva.

### Cómo relevar participantes antes de implementar una empresa

La caracterización del legado confirmó que “persona”, “cliente” y “proveedor” no
deben copiarse como tres maestros independientes. El implementador debe relevar una
identidad de participante por empresa y después sus roles comerciales, que pueden
coexistir y tener estados propios.

Clasificar cada campo solicitado antes de cargarlo o personalizarlo:

| Si el dato describe... | Propietario previsto |
|---|---|
| identidad, nombre, identificación, dirección o contacto actual | `business_partners` |
| condición, precio, vendedor o compromiso de venta | `commercial_catalog`/`sales` |
| crédito, saldo, deuda o cobranza | `accounts_receivable` |
| plazo, obligación o pago a proveedor | `purchasing`/`accounts_payable`/`treasury` |
| ruta, transporte o especialización logística | `logistics` |
| cuenta o imputación | `accounting` |
| dato emitido en factura, nota o remisión | snapshot de `commercial_documents` |
| XML, CDC, firma, envío o respuesta fiscal | `sifen` |

No usar RUC, cédula o código visible como identidad técnica entre plugins. No
resolver duplicados automáticamente ni eliminar físicamente participantes para
“limpiar” una migración. Antes de importar datos antiguos se requiere perfilado por
empresa, reglas de precedencia, respaldo y ensayo reversible.

La fuente detallada es la
[caracterización de `business_partners`](../knowledge-base/business-partners/legacy-characterization.md).
BP-D01 a BP-D10 fueron aceptadas sin cambios. ADR-0014 y J11-S6-02 implementan el
dominio y el contrato público `1.0.0`: `BusinessPartnerDirectory` recibe empresa e
ID opaco y devuelve una referencia mínima. ADR-0015 y J11-S6-03 materializan las
tablas y repositorios. ADR-0016 y J11-S6-04 agregan comandos, consultas, permisos,
autorización actual y auditoría técnica. ADR-0017 y J11-S6-05 publican menú,
pantalla `business_partners:directory`, handler neutral y renderer JSF responsive.
J11-S6-06 incorpora esa capacidad al perfil físico reproducible
`with-business-partners-demo`, compartido por WAR y migrador.
J11-S8-C02 agrega `business_partners:definitions`, la ruta
`/business-partners/definitions` y el catálogo empresarial `CHANNEL_KIND`; la
ficha de socios resuelve sus opciones activas mediante el caso de uso del mismo
plugin.

### Cómo preparar la persistencia de participantes

El implementador no debe crear tablas manualmente ni permitir que Hibernate las
actualice. La distribución que incluya el plugin debe ejecutar primero el migrador;
su descriptor aporta `classpath:db/migration/business_partners` y Flyway crea
`plg_business_partners.flyway_schema_history` junto con V1–V4.

La V1 separa raíz, roles, identificaciones, direcciones, canales, contactos y
secuencias. `company_id` está presente en todas las claves propietarias, pero no hay
FK hacia `core.company` ni joins privados. RUC/cédula duplicados se consultan como
candidatos de revisión; no se rechazan universalmente. Código general y código de
rol sí tienen unicidad por su ámbito.

V2 agrega `business_partner_definition`. Su clave compuesta incluye empresa, clase
y código; el estado y la versión permiten evolucionar el catálogo sin reescribir
canales históricos. La migración inicializa correo, teléfono, WhatsApp y sitio web
para empresas que ya tienen participantes. Una empresa nueva administra sus
propios valores desde **Definiciones de socios**; no se consulta `core.company` desde la
migración privada.

El decimotercer corte de J11-S8-C02 permite inactivar y reactivar esos tipos con la
versión vigente. La mutación revalida empresa y permiso, conserva la fila, audita
identidad/versiones y rechaza una versión obsoleta o un código ajeno. La pantalla
administrativa mantiene visibles los inactivos; el selector de un canal nuevo sólo
recibe definiciones activas. No edite el estado directamente en SQL.

V3 agrega `business_partner_definition_revision`. La migración retroalimenta la
versión vigente de cada definición V2 y, desde entonces, cada alta, revisión de
nombre o cambio de estado conserva una fila append-only por
empresa/clase/código/versión. El decimoquinto corte permite cambiar sólo el nombre
visible desde **Nueva revisión** y consultar todas las versiones en **Historial**;
empresa, clase y código permanecen estables. No edite ni elimine revisiones por
SQL.

V4 amplía la misma raíz e historial a `IDENTIFICATION_TYPE`, `ADDRESS_TYPE` y
`ADDRESS_PURPOSE`. Retroalimenta cada código usado en identificaciones/direcciones,
crea revisiones iniciales y siembra tipos mínimos por empresa sin agregar tablas.
Los formularios ofrecen sólo definiciones activas y la aplicación vuelve a
resolver empresa, clase y estado con bloqueo compartido antes de guardar. País
continúa como código ISO textual hasta decidir una fuente normativa versionada.

Para validar el corte en una instalación:

```powershell
.\mvnw.cmd -B -pl plugins/business-partners -am verify `
  "-Dlogixone.postgres.integration=true"
```

El resultado esperado es V1–V4 aplicadas una vez, una segunda migración con cero
cambios y 21 escenarios PostgreSQL verdes entre migración y repositorios. No
editar una migración aplicada; una evolución usa V5 o superior. Desactivar o retirar el JAR conserva tablas e
información. La composición de demo del plugin en WAR/migrador se construye con
`with-business-partners-demo` y se opera mediante el
[runbook J11-S6-06](../runbooks/demo-business-partners-j11-s6-06.md). El recorrido
actual de las cuatro clases se reproduce con la
[demo J11-S8-C02](../runbooks/demo-definiciones-socios-j11-s8-c02.md).

### Cómo invocar casos de uso de participantes

Un adaptador JSF o de integración no construye una empresa ni concede permisos por
su cuenta. Debe inyectar `CurrentCompanyAuthorization`, solicitar una prueba para
`business_partners` y exactamente uno de estos permisos, convertirla mediante
`BusinessPartnerOperationContext.from(...)` y recién entonces llamar al límite
`TransactionalBusinessPartnerUseCases`:

| Permiso | Uso |
|---|---|
| `business_partners.view` | buscar, detalle y candidatos duplicados |
| `business_partners.manage` | alta y datos generales, identificaciones, direcciones y contactos |
| `business_partners.roles.manage` | asignar o cambiar cliente/proveedor |
| `business_partners.lifecycle.manage` | inactivar o reactivar el participante completo |

El kernel revalida OIDC, sesión, membresía, empresa, activación del plugin y permiso
en cada solicitud. La aplicación vuelve a comprobar plugin y permiso exactos antes
de leer el repositorio. No guardar `AuthorizedCompanyOperation` en sesión ni
reutilizarla para otra acción.

Las mutaciones devuelven códigos estables, usan versión esperada y auditan operación,
resultado, IDs técnicos y correlación. El sobre no incluye nombre, RUC/cédula,
dirección, email ni teléfono. Código automático usa el contador transaccional
`general`; un código manual sigue sujeto a unicidad por empresa. Las coincidencias
de identificación producen una advertencia y no un rechazo automático.

Esta edición permite agregar detalles, pero todavía no corregir/vencer una
identificación ni editar/desactivar direcciones o canales. Esas operaciones deben
preservar historia y no se simulan en la pantalla de J11-S6-05.

### Cómo habilitar y demostrar la pantalla de participantes

La pantalla sólo aparece cuando el JAR está presente, la empresa tiene
`business_partners` activo y el rol actual posee al menos
`business_partners.view`. Las acciones de alta/datos, roles y ciclo de vida exigen
además sus permisos separados; mostrar el formulario nunca concede la operación.

El implementador debe usar la ruta pública `/business-partners`, no enlazar
directamente un XHTML. Conforme a [ADR-0018](../adr/0018-floorplan-erp-directorio-alta-ficha.md),
el shell representa esa ruta en modos `directory`, `create` y `detail`; la ficha usa
pestañas semánticas y muestra una sola tarea a la vez. En expandido el directorio usa
tabla y navegación lateral; en medio y compacto usa lista adaptable y menú
colapsable. Los parámetros `mode` y `tab` sólo controlan presentación.

`ShellViewBean` recompone `ComposedScreen`, selecciona el único
`ScreenInteraction.Handler` del `ScreenId` y conserva identidad/versión del detalle
sólo durante el postback. El handler vuelve a obtener autorización actual y traduce
la acción al caso de uso. La empresa, el recurso, el modo, la pestaña o la versión
recibidos del navegador no se consideran autoridad.

La pantalla operativa no debe mostrar `ScreenId`, versión optimista, slots ni
explicaciones de JTA/auditoría. Esa información pertenece a contratos,
documentación, logs y diagnóstico autorizado; retirarla de la vista no elimina sus
controles server-side.

Antes de aceptar una implementación:

1. active el plugin mediante la administración protegida;
2. conceda `view`, `manage`, `roles.manage` y `lifecycle.manage` únicamente a roles
   que los necesiten;
3. abra el alta separada, registre un participante ficticio y confirme que se abre
   su ficha;
4. vuelva al directorio, búsquelo, abra el detalle, cambie de pestaña y ejecute una
   mutación; confirme aviso, estado de negocio y auditoría;
5. pruebe permisos faltantes y plugin desactivado;
6. valide 375, 599, 600, 720, 839, 840 y 1280 px sin overflow;
7. use sólo `directory_extensions` y `detail_extensions` para una futura
   personalización, respetando la versión `1.0.0` de la pantalla.

El guion actual está en el
[runbook J11-S6-06](../runbooks/demo-business-partners-j11-s6-06.md). La composición
está verificada para desarrollo/demo, pero no autoriza promover la imagen ni usar
los fixtures A/B como personalización de una empresa real.

## Capítulo 17 — Gobernar selectores y sus datos

Antes de implementar un formulario, cree un inventario de cada selector. Para cada
uno registre fuente, propietario, clasificación, ruta de administración, permiso,
tratamiento de vacío/inactivos y tamaño esperado. Use las cinco clases de
[ADR-0028](../adr/0028-gobierno-de-selectores-y-datos-administrables.md): estado
cerrado, catálogo empresarial, referencia operativa, catálogo normativo o
composición/despliegue.

Reglas para una implementación empresarial:

1. un catálogo empresarial debe tener pantalla autorizada de alta, consulta,
   edición permitida e inactivación antes de aparecer como selector terminado;
2. una referencia a otro maestro enlaza al recorrido propietario y usa su contrato
   público, nunca su tabla o entidad JPA;
3. el shell muestra `Administrar`/`Agregar` sólo con permiso, preserva un borrador
   seguro y refresca opciones al volver;
4. estados de proceso, permisos y códigos normativos no aceptan valores arbitrarios;
5. un valor inactivo no se ofrece para operaciones nuevas, pero sigue visible en
   registros históricos;
6. listas grandes se buscan y paginan en servidor;
7. Playwright cubre alta, retorno, actualización, permiso negativo, vacío,
   inactivo, teclado y 375/720/1280 px.

El baseline actual tiene 91 selectores lógicos. `plugin-api` 0.4.3 y los renderers
autorizados cubren 73 selectores de plugins y 18 del kernel/shell, incluidos los
cuatro tipos de `business_partners` mediante **Definiciones de socios**. Los nativos declaran
propietario `PLATFORM`, muestran origen/clase y sólo exponen rutas administrativas
cuando la autoridad global contiene el permiso declarado. La
[auditoría](../architecture/inventario-selectores-y-datos-administrables.md)
confirma el reemplazo seguro de unidades, categorías, marcas y etiquetas, la
revisión/historial de familias, su asignación versionada a artículos y las
definiciones de identificación/dirección como resueltos. País y moneda pertenecen
a `reference_data`; los consumidores usan `reference-data-api` 1.1.0, buscan sólo
valores habilitados en servidor, limitan cada página a 50 y vuelven a validarlos
dentro de la transacción. La pantalla
propietaria exige `reference_data.policy.manage`, utiliza versión optimista y
conserva historia append-only por empresa; no admite altas de códigos.
Los selectores renderizados por plugins ya navegan al propietario con un contexto
efímero de un uso, conservan por POST sólo el borrador permitido y refrescan las
opciones al volver. Los 11 usos nativos administrables ofrecen la misma vuelta
segura mediante planes cerrados del shell; los siete restantes son cerrados o de
despliegue y no admiten altas arbitrarias.
El detalle de gobierno permanece en la
[épica transversal](../backlog/epica-gobierno-selectores-datos-administrables.md)
y el trabajo normativo en la
[épica de datos de referencia](../backlog/epica-datos-referencia-normativos.md).
El seed `BOOTSTRAP_SUBSET` continúa visible sólo como publicación histórica. V4
publica 248 países y 178 códigos únicos de moneda o fondo; los 13 valores `N.A.`
no se convierten en cero. La implementación local todavía debe superar PostgreSQL,
Docker/Compose y Playwright antes de considerarse cerrada.

## Capacidades y límites de esta edición

| Disponible en el baseline documentado | Pendiente de historias futuras |
|---|---|
| empresas, activación y personalización persistidas; V4–V6, autoridad global y auditoría append-only para kernel/plugins | validación independiente de la guía candidata |
| actor/empresa OIDC, shell y paneles administrativos validados con OIDC/Servlet/Playwright | administración productiva y operación real de una empresa futura |
| contrato neutral 0.4.3, fuentes de plugin/plataforma tipadas 91/91, búsqueda bajo demanda y página máxima 50, renderers JSF Material 3, interacción cerrada y retorno seguro de plugins y nativos administrables validados | tipos visuales futuros que requieran nueva versión |
| `reference_data` con API `1.1.0`, V1–V4 privadas, procedencia, políticas optimistas, publicaciones `FULL` 248/178, unidad menor opcional, importador determinista y paginación en servidor | gate PostgreSQL/Compose/Playwright de políticas, publicación y responsive |
| JPA/JTA, PostgreSQL, dominio/API, aplicación, seguridad, UI responsive, composición, gate integral, demo oficial y PDF de `business_partners` verdes | dieciocho plugins productivos posteriores |
| `business_partners` con API `1.1.0`, V1–V4 privadas, participantes, búsqueda pública paginada y cuatro clases de definiciones por empresa, país resuelto por `reference-data-api`, ciclo activo/inactivo, revisión de nombre e historial append-only versionados, permisos y UI responsive | operaciones futuras sobre datos históricos y pruebas acumuladas de la ampliación 1.1 |
| `commercial_catalog` con API `1.1.0`, búsqueda paginada por alcance comercial, V1–V4 privadas, repositorios, permisos, casos de uso, auditoría, alta/consulta visual y ciclo activo/inactivo de unidades, categorías, marcas, etiquetas, perfiles tributarios y familias de variantes; revisión/historial append-only y reemplazo seguro de definiciones simples, revisión explícita/historial de perfiles, revisión estructural/historial de familias y asignación versionada a artículos | pruebas acumuladas de la ampliación 1.1 y diecisiete plugins productivos posteriores |
| `inventory` con API `1.1.0`, V1–V2 privadas, diez entidades, siete repositorios, ocho permisos y movimiento por identidad pública de catálogo; baseline 1.0 previo con perfil físico, imágenes, gate integral, demo oficial y PDF verdes | validar la ampliación 1.1 junto con Compras y dieciséis plugins productivos posteriores |
| `purchasing-api` y `purchasing` `1.1.0`, V1–V2 privadas de once tablas, JPA, repositorios, doce permisos, aplicación auditada, CDI/JTA, perfil físico WAR/migrador y cinco recorridos visuales; Maven, PostgreSQL, ArchUnit, Docker/Compose, migraciones, health, OIDC y Playwright verdes | gate acumulado J11-S9-07 y validación independiente |
| Compose con Keycloak 26.7.0, realm declarativo, login/logout y WildFly OIDC | proveedor OIDC y topología de identidad productivos |
| ADR-0010 y análisis estructural del manual SIFEN v150 | documentos comerciales, adaptador SIFEN y verificación de la especificación oficial vigente |
| ADR-0031 y caracterización de facturación masiva; portal oficial verificado con MT 150 + NT-027 | lote comercial idempotente dentro de `commercial_documents` y lotes fiscales separados; implementación futura |
| ADR-0033, análisis y épica de `recurring_billing` | planes/versiones, suscripciones, prorrateo, consumo facturable, cargos reproducibles e integración por candidatos; implementación futura |
| ADR-0034, caracterización y épica de `vehicle_telemetry` | dispositivos, asignaciones, observaciones, recorridos, geocercas, alertas y seguimiento `ACTIVE/PAUSED/STOPPED`; implementación futura después de `logistics` |
| ADR-0011/ADR-0027/ADR-0030/ADR-0032/ADR-0033/ADR-0034/ADR-0035/ADR-0036/ADR-0037/ADR-0038/ADR-0040/ADR-0045/ADR-0046/ADR-0013: fundación R0, diecinueve plugins ERP, tres de operaciones del proveedor, seis cooperativos, un técnico de migración, un funcional transversal BPM, familia Flota F1/F2, telemetría, facturación recurrente, POS offline, estaciones de servicio, familia de RR. HH., personalización y contrato outbox | implementación progresiva desde `inventory`; secuencia ERP 1–19 sin cambios y familias futuras separadas de proveedor, cooperativa, migración, BPM y Flota; `legacy_migration`, `business_process_management`, `fleet_maintenance` y `automotive_workshop` aún no están implementados |
| ADR-0026 e instalador Windows interno con preflight, consentimiento, reparación, health y `current` verificados | Authenticode, VM limpia/incompatible y escenarios reales de UAC/cancelación |

## Validación editorial y técnica

Antes de entregar una edición:

1. un implementador que no haya escrito la funcionalidad debe seguir el recorrido en un ambiente limpio;
2. los comandos, rutas, nombres de contratos y resultados deben coincidir con el baseline publicado;
3. los enlaces locales, UTF-8 y ejemplos deben pasar G0;
4. el recorrido no puede requerir acceso a internos de otros plugins, tablas privadas, credenciales embebidas ni pasos manuales no declarados;
5. toda limitación o capacidad todavía no implementada debe aparecer como tal;
6. la edición debe registrar versión, fecha, compatibilidad y cambios respecto de la edición anterior.

El recorrido en limpio de la edición candidata vigente se registra en la [ficha de validación independiente](VALIDATION.md). La existencia de la ficha no equivale a una prueba ejecutada. Los gates técnicos y la demo visual ya están disponibles; el recorrido debe completarlo y firmarlo una persona que no haya desarrollado las capacidades evaluadas.

## Estado de la primera edición

La edición 1.0-rc47 conserva el recorrido anterior, el análisis SIFEN de ADR-0010 y
el roadmap de ADR-0011. Agrega la composición física única y el procedimiento de
migraciones de plugins de ADR-0012. Los gates técnicos de Sprint 4 quedaron verdes
sobre PostgreSQL, WildFly, Keycloak, Docker y navegador real. J11-S5-01 agregó 12
escenarios PostgreSQL/Testcontainers y validó en Compose migración `plg_*`,
idempotencia, health, recreación y retirada física sin borrar datos. J11-S5-02
agregó el generador neutral de plugins, sus validaciones y la prueba de composición
real en WAR y migrador. J11-S5-03 diferenció sincronía, integración y auditoría y
fijó propiedad/recuperación del futuro outbox sin agregar infraestructura
especulativa. J11-S5-04 volvió a ejecutar reactor, PostgreSQL, composición A/B,
Docker/Compose, health, persistencia, OIDC y Playwright; las 22 capturas responsive
fueron revisadas sin defectos de layout. Sprint 6 caracterizó el legado, aceptó
BP-D01 a BP-D10 y creó API pública/dominio en dos módulos. J11-S6-03 agregó
ADR-0015, ocho tablas privadas, una unidad JPA y repositorios validados con
PostgreSQL. J11-S6-04 cerró aplicación, permisos y auditoría. J11-S6-05 agregó
ADR-0017, `plugin-api` 0.4.0 y la primera UI productiva con Playwright verde en
siete anchos. J11-S6-06 agregó el perfil físico único, el par de imágenes verificado,
idempotencia, conservación de datos y una nueva demo responsive. La corrección de
aceptación de J11-S6-06 agregó ADR-0018 y reorganizó la UI productiva en directorio,
alta y ficha con pestañas, eliminando la página vertical y los metadatos técnicos de
la superficie operativa. J11-S6-07 volvió a validar reactor, arquitectura,
PostgreSQL, imágenes, Compose, health, OIDC y la demo final con 35 capturas; también
regeneró y revisó el PDF obligatorio. Sprint 7 completó gobierno y caracterización
de `commercial_catalog`; CC-D01 a CC-D10 fueron confirmadas, J11-S7-02 creó su
API pública `1.0.0` y dominio neutral, y J11-S7-03 agregó ADR-0020, V1 con veinte
tablas, JPA privada, repositorios empresariales y secuencia atómica validados con
PostgreSQL. J11-S7-04 agregó ADR-0021, cuatro permisos, comandos y consultas
empresariales, administración de definiciones, directorio/conversión/cotización,
auditoría técnica y límites CDI/JTA validados. Esta edición conserva el onboarding
reproducible con IntelliJ IDEA Ultimate 2026.2, manteniendo Docker/Compose como
ejecución oficial. J11-S7-05 agregó ADR-0022, dos menús y dos pantallas neutrales,
handlers autorizados de artículos/listas de precios y un floorplan JSF generalizado.
J11-S7-06 añadió el perfil físico con ambos plugins, WAR/migrador e imágenes
coherentes, fixture Unicode idempotente, activación/permisos por administración y
una demo Playwright verde en siete anchos. J11-S7-07 repitió reactor base/completo,
ArchUnit, PostgreSQL, imágenes verificadas, migraciones idempotentes, Compose,
health, OIDC y la demo acumulada con 47 capturas; regeneró el PDF y planificó
`inventory` sin adelantar su dominio. G7 independiente permanece pendiente.
La edición agrega el paquete documental mantenible: fotografía gráfica de plugins
de Sprint 7, guía de Visual Studio Code, manual de usuario orientado a tareas y
manual técnico para desarrolladores. Los manuales distinguen audiencias y la guía
de usuario queda alineada, sin afirmar certificación, con ISO/IEC/IEEE 26514,
IEC/IEEE 82079-1, ISO 24495-1, ISO 9241-210 y WCAG 2.2.
J11-S8-01 caracteriza el inventario legado, fija la frontera candidata y presenta
IN-D01 a IN-D10 con alternativas, impacto y recomendación. Producto confirmó las
diez recomendaciones sin cambios el 2026-07-31. J11-S8-02 agregó ADR-0023,
`inventory-api@1.0.0`, el módulo funcional, la dependencia requerida del catálogo y
el dominio neutral. J11-S8-03 agregó ADR-0024, V1, nueve tablas/entidades y seis
repositorios. J11-S8-04 agregó V2, diez entidades, siete repositorios, tres
capacidades, siete permisos, casos de uso auditados y contratos CDI/JTA. J11-S8-05
agregó ADR-0025, tres menús/pantallas neutrales, directorios empresariales, handlers
autorizados y presentación mediante el renderer único del shell. J11-S8-06 agregó
el perfil físico único con los tres productivos, WAR/migrador e imágenes coherentes,
migraciones idempotentes, activación/permisos administrativos y una demo Playwright
verde de depósito, existencias, reserva, disponibilidad, conteo y seguridad
negativa. J11-S8-07 repitió el gate integral, congeló las imágenes, ejecutó la demo
oficial y regeneró la fotografía, retrospectiva y PDF. J11-S8-08 agregó ADR-0026,
fuentes nativas Windows Forms/CLI, manifiesto, 54 aserciones deterministas,
preflight/plan/consentimiento y ejecución reparable. La instalación local y dos
reparaciones terminaron con migración y health verdes, sin cambiar cuatro secretos
ni nueve conteos de datos. `current` contiene ocho archivos íntegros; el EXE está
`NotSigned` y restringido a `INTERNAL_UNSIGNED`.
J11-S8-C01 reabrió el baseline por el hallazgo de selectores sin administración:
agrega `/catalog/tax-profiles`, alta autorizada y tres perfiles ficticios de demo.
Los tests, composición y demo responsive afectada están verdes; recongelación y
PDF siguen pendientes. J11-S8-C02 agregó `/catalog/definitions`, consulta/alta y
ciclo activo/inactivo autorizado de unidades, categorías, marcas y etiquetas;
agregó además
`/catalog/variant-families` y `/business-partners/definitions`. La cobertura es
71/71 selectores de plugins. El octavo corte permite crear una nueva revisión de
tratamiento, descripción y vigencia tributaria sin cambiar código, nombre o
identidad, y conserva referencias a versiones anteriores. El noveno corte permite
consultar esas versiones de forma autorizada, descendente y aislada por empresa,
sin exponer entidades ni tablas privadas. ADR-0028 documenta
ahora 89 selectores lógicos en ese corte y las brechas de
administración que deben resolverse antes de `purchasing`. ADR-0029 reemplazó la
regeneración automática: al llegar al cierre se preguntará si se creará un
instalador nuevo. Con `SÍ` serán aplicables VM limpia/incompatible, UAC/cancelación,
firma y la matriz acordada; con `NO`, `current` quedará intacto y no representará
el nuevo baseline. G7 independiente continúa pendiente.
El duodécimo corte agregó retorno seguro a los 11 usos nativos administrables con
whitelist de rutas/inputs, POST, UUID opaco, binding de sesión y reautorización. El
gate `verified` quedó 24/24, health y logs verdes, y Playwright validó ida/retorno y
restauración en 1280/720/375 px. El decimotercer corte agregó el ciclo versionado y
auditado de tipos de canal; PostgreSQL, módulo, reactor, imagen, health y Playwright
responsive quedaron verdes. El decimocuarto corte agregó el ciclo de familias de
variantes con aislamiento empresarial, versión optimista, auditoría y atributos
preservados; PostgreSQL, reactor, imagen, health y Playwright responsive quedaron
verdes. El decimoquinto corte agregó revisión de nombre e historial append-only de
tipos de canal mediante V3 privada; módulo 46/46, PostgreSQL 19/19, gate 24/24,
imágenes, migración idempotente, health y Playwright responsive quedaron verdes.
El decimosexto corte agregó V2 y revisión/historial append-only de definiciones
simples sin cambiar código ni identidad. El decimoséptimo agregó V3 y reemplazo
seguro mediante identidad sucesora, origen inactivo e inmutable y referencias
históricas conservadas. El decimoctavo agregó V4, revisión completa e historial
append-only de familias y versión inmutable en asignaciones existentes. El
decimonoveno expone la asignación neutral y vuelve a validar familia activa,
revisión y estructura dentro de la transacción. El vigésimo amplía
`business_partners` a cuatro clases empresariales, aplica V4 con backfill y
revalida las referencias activas de identificación/dirección antes de persistir.
J11-S8-C03 agrega `reference_data`, lleva el inventario a 91 selectores y registra
el subconjunto `PY/PYG/USD`. J11-S8-C07 conserva ese historial, incorpora
publicaciones `FULL` 248/178, representa `N.A.` como ausencia y convierte país y
moneda en búsquedas paginadas con revalidación transaccional mediante la API
pública. Los gates PostgreSQL, Docker/Compose, JTA/OIDC y Playwright del corte
están verdes; el Sprint continúa abierto por los gates formales de cierre.
La edición no se etiqueta como `1.0` hasta
completar el recorrido independiente y resolver cualquier hallazgo que produzca.
Como Sprint 8 continúa formalmente abierto, el PDF estable verificado en el corte
interno deberá regenerarse si la matriz o G7 modifica el baseline final de cierre.

## Historial

| Edición | Fecha | Cambio |
|---|---|---|
| 0.x | 2026-07-27 | contrato editorial y capítulos incrementales 6–9 |
| 1.0-rc1 | 2026-07-27 | recorrido completo, operación, diagnóstico, checklist y ejemplo Distribuidora Boreal |
| 1.0-rc2 | 2026-07-28 | adenda planificada de Keycloak/OIDC, identidad local, membresía, autorización y demo visual |
| 1.0-rc3 | 2026-07-28 | contratos neutrales de usuario, membresía, rol, selección empresarial y permisos efectivos |
| 1.0-rc4 | 2026-07-28 | esquema `core` V3 aditivo para identidad, membresía, roles y permisos |
| 1.0-rc5 | 2026-07-28 | adaptadores JPA/JTA, administración tipada, consultas actuales y bootstrap interno de seguridad |
| 1.0-rc6 | 2026-07-28 | Keycloak 26.7.0 por digest, realm/cliente declarativos, WildFly OIDC, secretos, volumen y bootstrap externo cerrado |
| 1.0-rc7 | 2026-07-28 | principal OIDC confiable, empresa en sesión mínima, revalidación por operación, guarda plugin/permiso y auditoría segura |
| 1.0-rc8 | 2026-07-28 | shell Faces, selector multiempresa, menú filtrado, logout, estados accesibles y ruta directa autorizada |
| 1.0-rc9 | 2026-07-28 | Material Design 3 sobre JSF, tokens del shell y responsive compacto/medio/expandido obligatorio |
| 1.0-rc10 | 2026-07-28 | renderer cerrado de `ComposedScreen`, tipos neutrales, guarda de pantalla y personalización visual A/B |
| 1.0-rc11 | 2026-07-28 | gates técnicos G2–G6 verdes, logout OIDC preview validado, WAR reproducible y demo A/B operativa |
| 1.0-rc12 | 2026-07-28 | ADR-0009 y modelo neutral de autoridad administrativa global, implementado pendiente de pruebas |
| 1.0-rc13 | 2026-07-28 | análisis SIFEN v150, ADR-0010 y método de persistencia canónica para futuros documentos comerciales |
| 1.0-rc14 | 2026-07-28 | V4 aditiva, readiness 4 y bootstrap global neutral cerrado, implementados pendientes de pruebas |
| 1.0-rc15 | 2026-07-28 | demo visual navegable y reproducible obligatoria para el cierre de cada Sprint |
| 1.0-rc16 | 2026-07-28 | mapeos JPA V4, casos JTA de autoridad global y protección serializada del último administrador |
| 1.0-rc17 | 2026-07-28 | autorización global actual por request, filtro OIDC y landing administrativa Faces responsive |
| 1.0-rc18 | 2026-07-28 | UI de empresas, catálogo físico, activaciones y personalización obligatoria por empresa |
| 1.0-rc19 | 2026-07-28 | UI separadas de seguridad empresarial y autoridad global, con confirmaciones y reautorización por comando |
| 1.0-rc20 | 2026-07-28 | V5 append-only, consulta visual paginada de auditoría y cabeceras defensivas administrativas |
| 1.0-rc21 | 2026-07-28 | roadmap aprobado de doce plugins reutilizables, orden de construcción y personalización distinta por empresa |
| 1.0-rc22 | 2026-07-29 | gates técnicos de Sprint 4 verdes, administración visual validada, seguridad negativa, persistencia y demo responsive verificadas |
| 1.0-rc23 | 2026-07-29 | ADR-0012, plugin set único, SPI neutral y migraciones `plg_*`; gates PostgreSQL/Docker pendientes |
| 1.0-rc24 | 2026-07-29 | J11-S5-01 cerrada: PostgreSQL/Testcontainers, imágenes, Compose, idempotencia y retirada sin pérdida verdes |
| 1.0-rc25 | 2026-07-29 | J11-S5-02 cerrada: generador neutral reproducible, composición real y guía para funcionales/personalizaciones |
| 1.0-rc26 | 2026-07-29 | J11-S5-03 cerrada: ADR-0013, contrato at-least-once y outbox/inbox propiedad de cada plugin |
| 1.0-rc27 | 2026-07-29 | J11-S5-04: gates técnicos y demo responsive verdes; Sprint 6 planificado; G7 independiente pendiente |
| 1.0-rc28 | 2026-07-29 | J11-S6-01: caracterización de participantes y roles, separación de dominios y decisiones BP-D01 a BP-D10 pendientes |
| 1.0-rc29 | 2026-07-29 | J11-S6-02: ADR-0014, API pública `1.0.0`, dominio neutral y 212 pruebas verdes; persistencia/UI pendientes |
| 1.0-rc30 | 2026-07-29 | J11-S6-03: ADR-0015, V1 con ocho tablas, JPA/repositorios y 13 escenarios PostgreSQL verdes; aplicación/UI pendientes |
| 1.0-rc31 | 2026-07-29 | J11-S6-04: ADR-0016, autorización neutral, cuatro permisos, comandos/consultas, auditoría `core` V6 y 14 escenarios PostgreSQL verdes; UI pendiente |
| 1.0-rc32 | 2026-07-29 | J11-S6-05: ADR-0017, `plugin-api` 0.4.0, menú y pantalla interactiva de participantes, Playwright responsive y demo técnica verde; composición física pendiente |
| 1.0-rc33 | 2026-07-29 | J11-S6-06: perfil físico único, WAR/migrador e imágenes verificadas, migraciones idempotentes, volúmenes conservados y nueva demo visual |
| 1.0-rc34 | 2026-07-29 | Corrección visual J11-S6-06: ADR-0018, navegación ERP, directorio/alta/ficha separados, pestañas y adaptación tabla/lista por rango |
| 1.0-rc35 | 2026-07-30 | J11-S6-07: gates integrales, imagen final, demo responsive, conservación de datos, PDF de Sprint 6 y planificación de `commercial_catalog` |
| 1.0-rc36 | 2026-07-30 | J11-S7-01: caracterización del catálogo legado, fronteras, casos, invariantes, snapshots y recomendaciones CC-D01 a CC-D10 |
| 1.0-rc37 | 2026-07-30 | Guía reproducible para abrir, construir, levantar y diagnosticar Smart ERP con IntelliJ IDEA Ultimate 2026.2 |
| 1.0-rc38 | 2026-07-30 | J11-S7-02: ADR-0019, API pública, dominio neutral y separación física de `commercial_catalog`; persistencia/UI pendientes |
| 1.0-rc39 | 2026-07-30 | J11-S7-03: ADR-0020, V1 privada con veinte tablas, JPA/repositorios empresariales y secuencia atómica; aplicación/UI pendientes |
| 1.0-rc40 | 2026-07-30 | J11-S7-04: ADR-0021, cuatro permisos, aplicación autorizada, definiciones, consultas, contratos CDI/JTA y auditoría; UI/composición pendientes |
| 1.0-rc41 | 2026-07-30 | J11-S7-05: ADR-0022, dos menús y pantallas neutrales, handlers autorizados y floorplan JSF generalizado; composición/Playwright pendientes |
| 1.0-rc42 | 2026-07-30 | J11-S7-06: perfil físico con dos plugins productivos, imágenes/migraciones coherentes, permisos administrados y nueva demo responsive verde |
| 1.0-rc43 | 2026-07-31 | J11-S7-07: gates G0-G6, imágenes finales, demo acumulada de dos plugins, PDF de Sprint 7 y planificación de `inventory` |
| 1.0-rc44 | 2026-07-31 | Paquete documental de cierre: gráfico de plugins, guía VS Code, manual de usuario basado en referencias internacionales y manual técnico de desarrollo |
| 1.0-rc45 | 2026-07-31 | J11-S8-01: caracterización de inventario, fronteras, casos, invariantes, snapshots y recomendaciones IN-D01 a IN-D10 |
| 1.0-rc46 | 2026-07-31 | Instalador Windows obligatorio desde Sprint 8: preflight, consentimiento/UAC, montaje, progreso, conservación y regeneración por baseline |
| 1.0-rc47 | 2026-07-31 | Confirmación de IN-D01 a IN-D10 sin cambios y autorización de J11-S8-02 para dominio y contratos de inventario |
| 1.0-rc48 | 2026-07-31 | J11-S8-02: ADR-0023, API pública `1.0.0`, dependencia requerida de catálogo y dominio neutral de inventario; persistencia/UI pendientes |
| 1.0-rc49 | 2026-07-31 | J11-S8-03: ADR-0024, V1 privada con nueve tablas, JPA, snapshots y seis repositorios empresariales; aplicación/UI pendientes |
| 1.0-rc50 | 2026-07-31 | J11-S8-04: V2, siete permisos, aplicación autorizada y auditada, contratos CDI, rollback JTA e idempotencia de movimientos y reservas; UI/composición pendientes |
| 1.0-rc51 | 2026-07-31 | J11-S8-05: ADR-0025, tres menús/pantallas neutrales de inventario, directorios, handlers autorizados y renderer JSF; composición/demo pendientes |
| 1.0-rc52 | 2026-08-01 | J11-S8-06: perfil físico con tres plugins productivos, imágenes y migraciones coherentes, permisos administrados y nueva demo responsive de inventario verde |
| 1.0-rc53 | 2026-08-01 | J11-S8-07: reactor, PostgreSQL, imágenes, Compose, OIDC y demo oficial verdes; fotografía, retrospectiva y PDF; baseline congelado para el instalador |
| 1.0-rc54 | 2026-08-01 | J11-S8-08: ADR-0026, instalador Windows interno, preflight/plan/consentimiento, instalación/reparación sin pérdida, demo y `current`; firma, VM y G7 pendientes |
| 1.0-rc55 | 2026-08-01 | J11-S8-C01: baseline reabierto; maestro autorizado de perfiles tributarios implementado, composición/Playwright/PDF/instalador pendientes |
| 1.0-rc56 | 2026-08-01 | ADR-0027: `point_of_sale` agregado como plugin 10; roadmap ampliado a trece reutilizables más una personalización por empresa |
| 1.0-rc57 | 2026-08-01 | ADR-0028/0029: inventario y gobierno de selectores; confirmación `SÍ`/`NO` antes de crear el instalador de cada Sprint |
| 1.0-rc58 | 2026-08-01 | J11-S8-C02 parcial: `plugin-api` 0.4.1, metadatos de selector, renderer autorizado, 45/51 fuentes de plugins declaradas y reactor verde |
| 1.0-rc59 | 2026-08-01 | J11-S8-C02: Definiciones del catálogo para unidades, categorías y marcas; 55/56 fuentes de plugins declaradas; Playwright y ciclos completos pendientes |
| 1.0-rc60 | 2026-08-01 | J11-S8-C02 tercer corte: etiquetas incorporadas a Definiciones del catálogo; imagen, health y Playwright focal verdes; familias de variantes y ciclos completos pendientes |
| 1.0-rc61 | 2026-08-01 | J11-S8-C02 cuarto corte: familias de variantes con atributos ordenados, imagen y Playwright focal verdes |
| 1.0-rc62 | 2026-08-01 | J11-S8-C02 quinto corte: tipos de canal empresariales, V2 privada y 59/59 fuentes de plugins declaradas |
| 1.0-rc63 | 2026-08-02 | J11-S8-C02 sexto corte: inactivación/reactivación de unidades, categorías, marcas y etiquetas con versión, empresa, auditoría, PostgreSQL y Playwright verdes |
| 1.0-rc64 | 2026-08-02 | ADR-0030: aprobadas HR-D01 a HR-D10; `human_resources`, `payroll` y `payroll_paraguay` agregados como órdenes 14 a 16 sin iniciar código |
| 1.0-rc65 | 2026-08-02 | J11-S8-C02 séptimo corte: ciclo versionado de perfiles tributarios; PostgreSQL, reactor, imagen, health y Playwright responsive verdes |
| 1.0-rc66 | 2026-08-02 | ADR-0031: facturación masiva asignada a `commercial_documents`, con lote recuperable/idempotente y transmisión SIFEN separada; sin iniciar código ni agregar plugin |
| 1.0-rc67 | 2026-08-02 | ADR-0032: `fuel_station` agregado como orden 11; roadmap ampliado a diecisiete reutilizables, con operación física separada de inventario, POS, documentos y tesorería |
| 1.0-rc68 | 2026-08-02 | ADR-0033: `recurring_billing` agregado como orden 8 para planes, prorrateo y consumo; roadmap ampliado a dieciocho sin transferir factura ni lote de emisión |
| 1.0-rc69 | 2026-08-02 | J11-S8-C02 octavo corte: revisión explícita de perfiles tributarios con identidad estable, historial relacional, versión, auditoría, PostgreSQL y UI neutral |
| 1.0-rc70 | 2026-08-02 | J11-S8-C02 noveno corte: historial tributario de solo lectura, aislado por empresa y responsive, con PostgreSQL, reactor, imagen, health y Playwright verdes |
| 1.0-rc71 | 2026-08-03 | J11-S8-C02 décimo corte: `plugin-api` 0.4.2, propietario de plataforma y metadatos nativos 18/18; cobertura 77/77, renderer autorizado y 342 pruebas de regresión verdes |
| 1.0-rc72 | 2026-08-03 | J11-S8-C02 undécimo corte: retorno seguro de selectores de plugins con POST, token opaco de un uso, borrador ligado a sesión/usuario/empresa, refresco de opciones, imagen y Playwright responsive verdes |
| 1.0-rc73 | 2026-08-03 | J11-S8-C02 duodécimo corte: retorno seguro de los 11 usos nativos administrables, CSP estricta, continuidad JSF, restauración cerrada, build 24/24 y Playwright 1280/720/375 verdes |
| 1.0-rc74 | 2026-08-03 | J11-S8-C02 decimotercer corte: inactivación/reactivación versionada y auditada de tipos de canal, exclusión de inactivos en altas, PostgreSQL, reactor, imagen, health y Playwright responsive verdes |
| 1.0-rc75 | 2026-08-03 | J11-S8-C02 decimocuarto corte: ciclo versionado, auditado y aislado por empresa de familias de variantes, preservación de atributos, PostgreSQL, reactor, imagen, health y Playwright responsive verdes |
| 1.0-rc76 | 2026-08-03 | ADR-0034: `vehicle_telemetry` agregado como orden 7 después de `logistics`; roadmap ampliado a diecinueve, con GPS, sensores y pausa/finalización auditada sin control físico remoto |
| 1.0-rc77 | 2026-08-03 | J11-S8-C02 decimoquinto corte: revisión de nombre e historial visible append-only de tipos de canal, V3 privada, PostgreSQL, reactor, imágenes, migración, health y Playwright responsive verdes |
| 1.0-rc78 | 2026-08-04 | ADR-0035: venta offline obligatoria para la primera versión productiva de POS, con plan `POS-OFF-00` a `POS-OFF-06`, almacenamiento cifrado y sincronización idempotente |
| 1.0-rc79 | 2026-08-04 | ADR-0036: familia futura de operaciones del proveedor con soporte central, gestión de releases y `support_connector` técnico opcional, sólo saliente y sin ejecución remota |
| 1.0-rc80 | 2026-08-04 | J11-S8-C02 decimosexto corte: V2 privada, revisión e historial append-only de definiciones simples, 71 pruebas de módulo, 17 PostgreSQL, reactor, imágenes, health y Playwright responsive verdes |
| 1.0-rc81 | 2026-08-04 | J11-S8-C02 decimoséptimo corte: V3 privada, reemplazo seguro de definiciones simples, 81/81 selectores, PostgreSQL 19/19, reactor, imágenes, health y Playwright responsive verdes |
| 1.0-rc82 | 2026-08-04 | J11-S8-C02 decimoctavo corte: V4 privada, revisión estructural e historial append-only de familias, asignaciones versionadas, 83/83 selectores, PostgreSQL 22/22, reactor, imágenes, health y Playwright responsive verdes |
| 1.0-rc83 | 2026-08-04 | ADR-0037: seis plugins para cooperativa de ahorro y crédito paraguaya agregados como familia separada; socios, gobierno, LA/FT, ahorros, créditos y regulación versionada, sin iniciar código |
| 1.0-rc84 | 2026-08-04 | COOP-00 refinada con quince decisiones, registro normativo COOP-N01–N15, grafo de dependencias, flujos monetarios y gates G0–G5; ejecución pendiente de datos y prioridad |
| 1.0-rc85 | 2026-08-04 | J11-S8-C02 decimonoveno corte: asignación neutral y versionada de familias a artículos, selector gobernado 84/84, revalidación empresarial/estructural, PostgreSQL, reactor, imagen, health y Playwright responsive verdes |
| 1.0-rc86 | 2026-08-04 | J11-S8-C02 vigésimo corte: definiciones empresariales de identificación/dirección, `business_partners` V4, selectores 89/89, PostgreSQL, reactor, imágenes, migración idempotente, health y Playwright responsive verdes |
| 1.0-rc87 | 2026-08-04 | J11-S8-C03: fundación `reference_data`, API 1.0.0, V1 privada, procedencia `PY/PYG/USD`, selectores 91/91 y revalidación transaccional; Docker/Playwright y recongelación pendientes |
| 1.0-rc88 | 2026-08-05 | J11-S8-C05: Smart ERP como marca visible; identificadores técnicos compatibles conservados y Playwright pendiente por Docker no disponible |
| 1.0-rc89 | 2026-08-05 | J11-S8-C06: políticas de `reference_data` con permiso dedicado, versión optimista, V2 append-only, auditoría y UI neutral; gates runtime pendientes |
| 1.0-rc90 | 2026-08-05 | J11-S8-C07: `reference-data-api` 1.1.0, `plugin-api` 0.4.3, V3–V4, publicaciones completas 248/178, `N.A.` opcional y búsqueda paginada; gates runtime pendientes |
| 1.0-rc91 | 2026-08-05 | J11-S8-C06/C07: PostgreSQL, `clean verify`, Compose, health, JTA/OIDC y Playwright verdes; 30 capturas responsive y PDF de 98 páginas verificado; instalador `NO` hasta una versión comercializable útil; G7 pendiente |
| 1.0-rc92 | 2026-08-11 | ADR-0040: `legacy_migration` agregado como plugin técnico transversal planificado; perfil inicial Oracle Forms & Reports, runner externo, importación por contratos públicos, reconciliación y corte reversible; sin código ni pruebas ejecutadas |
| 1.0-rc93 | 2026-08-11 | J11-S9-02: `purchasing-api@1.0.0`, descriptor y dominio neutral de solicitud, orden, recepción/devolución e importación abierta; implementada pendiente de pruebas y fuera de la composición |
| 1.0-rc94 | 2026-08-11 | J11-S9-03: `plg_purchasing` V1, nueve tablas, JPA `validate`, cuatro repositorios, inmutabilidad y movimiento de stock obligatorio; implementada pendiente de pruebas y fuera de la composición |
| 1.0-rc95 | 2026-08-11 | J11-S9-04: APIs 1.1, doce permisos de Compras, permiso acotado de Inventario, V2 con dos ledgers, aplicación auditada, CDI/JTA e integración por catálogo; pruebas acumuladas pendientes y UI/composición aún ausentes |
| 1.0-rc96 | 2026-08-11 | J11-S9-05: cinco pantallas neutrales de Compras, directorios paginados, selectores públicos gobernados, renderer del shell y manual 07/PDF; pruebas acumuladas y composición aún pendientes |
| 1.0-rc97 | 2026-08-11 | Revisión J11-S9-05: `commercial-catalog-api@1.1.0` filtra `PURCHASE` en servidor antes de total/paginación y el manual 07 explicita permisos auxiliares de Inventario y rutas Administrar; pruebas acumuladas pendientes |
| 1.0-rc98 | 2026-08-11 | Aclaración de producto: sólo se difiere la validación independiente. Gates automatizados ejecutados sobre corte materializado; se corrigieron dos errores de tipado de handlers y el trigger de confirmación por tabla; Compras 19 unitarias + 6 PostgreSQL, Catálogo 106, Inventario 71, Socios 74, ArchUnit 32 y `mvn verify` de 28 módulos verdes; composición/runtime/Playwright aún corresponden a J11-S9-06 |
| 1.0-rc99 | 2026-08-11 | ADR-0045: `business_process_management` agregado como plugin funcional transversal planificado; BPMN 2.0.2 acotado, ejecución durable, acciones públicas autorizadas, BPM-D01 a BPM-D12 y piloto futuro con solicitudes de Compras; sin código ni pruebas ejecutables afectadas |
| 1.0-rc100 | 2026-08-12 | ADR-0046: familia Flota planificada con F1 `fleet_maintenance` y F2 `automotive_workshop`; FM-D01 a FM-D12 y AW-D01 a AW-D10 aceptadas; catálogo 31→33, ERP 1–19 y J11-S9-06 sin cambios; sin código ejecutable afectado |
| 1.0-rc101 | 2026-08-12 | J11-S9-06: perfil físico `with-purchasing-demo`, WAR/migrador e imagen app final `sha256:4e7e84da913b64ae08cdd72188640af5a023e824db67dfb0aecdc2d40c38fba8`; 549 pruebas materializadas verdes, raíz protegida, migración idempotente, health/OIDC, Playwright integral y 18 capturas responsive; validación independiente y J11-S9-07 pendientes |
| 1.0-rc102 | 2026-08-13 | J11-S9-07: G0–G6 verdes, imágenes de cierre identificadas, Maven/ArchUnit/PostgreSQL/migraciones/health/OIDC/Playwright acumulado, demo oficial con 170 capturas, fotografía de ocho plugins y PDF regenerado/revisado; G7 independiente y decisión J11-S9-08 pendientes |
| 1.0-rc103 | 2026-08-14 | J11-S9-08: decisión `SÍ`, instalador interno `0.9.0-internal.1`, manifiesto `with-purchasing-demo`, 58 aserciones, preflight bloqueado sin cambios, integridad y UI smoke verdes; Authenticode, VM y G7 pendientes |
