# ADR-0036 — Operaciones del proveedor, soporte y conector seguro

- Estado: Aceptado
- Fecha: 2026-08-04
- Decisión de producto: incorporar `customer_support`, `release_management` y
  `support_connector` al plan de Logixone
- Modifica: catálogo futuro de plugins y perfiles de composición; no cambia el
  orden 1–19 del roadmap ERP vigente
- Implementación: futura; no autoriza código durante Sprint 8

> Nota vigente: esta ADR conserva la decisión histórica que elevó el catálogo de
> diecinueve a veintidós reutilizables. Posteriormente,
> [ADR-0037](0037-familia-cooperativa-ahorro-credito-paraguay.md) agregó seis
> plugins para cooperativas de ahorro y crédito; el catálogo global actual
> planificado contiene veintiocho, sin cambiar la familia definida aquí.

## Contexto

Logixone necesita atender a las empresas que compren o alquilen el ERP y gobernar
las mejoras, correcciones y versiones que se les entreguen. El roadmap actual
modela los dominios operativos del ERP, pero no posee todavía el ciclo de un caso
de soporte, el catálogo de instalaciones atendidas ni el ciclo de vida de una
versión del propio producto.

El soporte central también puede beneficiarse de información técnica de una
instalación. Resolverlo mediante acceso directo a la base de datos, una VPN
permanente, un shell remoto o scripts enviados por el proveedor crearía una vía
privilegiada difícil de limitar, auditar y revocar. Incluir tickets y releases en
el kernel trasladaría lógica de producto a una capacidad transversal que debe
seguir siendo pequeña.

Se necesita separar tres responsabilidades:

1. atender al cliente y conservar el caso de soporte;
2. planificar y publicar cambios del producto;
3. enlazar de forma voluntaria una instalación con el servicio central.

## Decisión

### 1. Familia de operaciones del proveedor

Se agregan tres plugins reutilizables al catálogo futuro:

| Plugin | Tipo | Despliegue principal | Responsabilidad |
|---|---|---|---|
| `customer_support` | funcional de operaciones del proveedor | instancia central del proveedor | clientes atendidos, cobertura, instalaciones, casos, SLA, conversaciones y resolución |
| `release_management` | funcional de operaciones del proveedor | instancia central del proveedor | defectos, mejoras, candidatos, versiones, compatibilidad, gates, notas y publicación |
| `support_connector` | técnico opcional | instalación del cliente | enlace saliente, registro local, solicitudes de soporte y diagnósticos consentidos |

El catálogo global planificado pasa de diecinueve a **veintidós plugins
reutilizables**: diecinueve plugins ERP, dos plugins funcionales de operaciones del
proveedor y un conector técnico. Esta cifra no significa que deban empaquetarse
juntos.

`Técnico` es aquí una clasificación arquitectónica y de producto. El baseline
actual de `PluginKind` sólo contiene `FUNCTIONAL` y `CUSTOMIZATION`; no se afirma
que `TECHNICAL` ya exista en el contrato ejecutable. Antes de crear el descriptor
del conector, SC-00 deberá aprobar mediante una evolución compatible de
`plugin-api` si se agrega `PluginKind.TECHNICAL` y cómo lo interpretan registro,
activación, scaffold, permisos, menús y consumidores con `switch`. No se lo
marcará silenciosamente como personalización.

La secuencia ERP 1–19 definida por ADR-0011 y sus ampliaciones no se renumera. La
familia nueva constituye una línea de producto y composición distinta. Su
prioridad de implementación se decidirá en un Sprint futuro después de cerrar
Sprint 8; planificarla ahora no autoriza adelantar código ni omitir predecesores
vigentes.

### 2. Perfiles de composición

Se distinguen al menos estos perfiles conceptuales:

- **ERP de cliente:** contiene los plugins ERP contratados, la personalización de
  la empresa y, si existe consentimiento y contrato de soporte integrado,
  `support_connector`;
- **operaciones del proveedor:** contiene `business_partners`,
  `customer_support`, `release_management`, los plugins comerciales que el
  proveedor realmente necesite y su personalización propia;
- **base o desarrollo:** puede omitir los tres plugins nuevos.

Una distribución ERP completa que antes contenía `19 + N` plugins productivos
continuará usando esa fórmula si no incorpora el conector. Con un único JAR
`support_connector` físicamente presente será `20 + N`, aunque su activación y
consentimiento se resuelvan por empresa. No se adopta `22 + N` como distribución
objetivo: los dos plugins centrales no deben enviarse por defecto a clientes.

WAR y migrador continuarán recibiendo la misma selección física mediante
`distribution/logixone-plugin-set`. Agregar cualquiera de los tres exige build y
redespliegue; activarlo o desactivarlo por empresa no elimina datos.

### 3. Propiedad de `customer_support`

`customer_support` será dueño de:

- cuenta y contactos habilitados para soporte por referencias públicas;
- cobertura, vigencia, plan de atención y snapshot del SLA aplicable;
- registro lógico de instalación, ambiente, canal y versión declarada;
- casos de consulta, incidente, solicitud y problema;
- impacto, urgencia, prioridad, cola, asignación y vencimientos;
- conversaciones, adjuntos, escalaciones, solución y satisfacción;
- solicitudes y resultados de diagnóstico recibidos;
- artículos de conocimiento y comunicación vinculada al caso;
- historia, auditoría funcional y retención propias.

No será dueño de la persona u organización comercial, factura, cobro, plan de
facturación recurrente, binario de release, repositorio de código ni credencial de
la instalación. Usará `business-partners-api` como dependencia funcional
requerida. Podrá consultar contratos públicos de `recurring_billing`, ventas o
documentos cuando estén presentes, pero conservará únicamente referencias y el
snapshot de cobertura necesario para explicar por qué se atendió un caso.

Su esquema privado será `plg_customer_support`.

La identidad de contactos externos del portal requiere una decisión de seguridad
específica antes de implementar: no se convertirá cada cliente en administrador
global, no se abrirá una consulta sin empresa y no se inventará una excepción de
aislamiento en el plugin. La instancia del proveedor operará en su propio contexto
empresarial y representará a los clientes mediante participantes comerciales y
autorizaciones explícitas del portal.

### 4. Propiedad de `release_management`

`release_management` será dueño de:

- solicitudes de cambio de tipo defecto, mejora, cambio técnico o seguridad;
- componente/plugin afectado, severidad, prioridad, estado y trazabilidad;
- versión planificada, candidato, canal y ciclo de publicación;
- contenido de una versión y relación con cambios resueltos;
- compatibilidad con kernel, `plugin-api`, plugins, migraciones y datos;
- gates, aprobaciones, evidencias y excepciones explícitas;
- notas de versión y avisos de actualización;
- metadatos de artefactos, digest, SHA-256, firma y procedencia;
- publicación, retiro, soporte y eventos de versión.

Su esquema privado será `plg_release_management`.

El plugin no reemplaza Git, Maven, CI/CD, Docker, el registro de imágenes ni el
instalador Windows. En su primer alcance no compila, firma, promueve, instala ni
revierte artefactos. Registra resultados verificables producidos por esos sistemas
y rechaza presentar como publicada una versión cuyos gates obligatorios no estén
verdes.

Estados, severidades, canales y tipos de cambio serán catálogos cerrados y
versionados, no texto libre administrable por cada empresa.

### 5. Propiedad de `support_connector`

`support_connector` será un plugin técnico pequeño y opcional. Será dueño de:

- identidad local opaca de la instalación y de cada empresa enlazada;
- configuración no secreta del endpoint y versión del protocolo;
- estado de enlace, consentimiento, revocación y última sincronización;
- cola local idempotente de solicitudes y resultados pendientes;
- inventario técnico permitido: release, digest y versiones de plugins;
- solicitudes de diagnóstico, aprobación local y resultado sanitizado;
- auditoría local de qué categorías y cantidades de datos se transmitieron;
- referencias de casos centrales necesarias para seguimiento local.

Su esquema privado será `plg_support_connector`. No replicará el historial completo
de tickets ni las tablas centrales.

Quedan expresamente fuera:

- shell, PowerShell, terminal, SQL o ejecución de scripts remotos;
- lectura arbitraria de archivos o tablas;
- carga de clases, JAR o código recibido desde soporte;
- escritorio remoto, control del sistema operativo o apertura de puertos entrantes;
- actualización, instalación, reinicio o rollback automáticos;
- desactivación de UAC, antivirus, firewall, TLS u otros controles;
- captura de contraseñas, tokens, secretos, datos comerciales o personales no
  necesarios para el diagnóstico aprobado.

Desconectar o desactivar el plugin detiene nuevas transmisiones, revoca la sesión
del enlace cuando sea posible y conserva la evidencia local según la política de
retención. Nunca elimina datos de negocio ni impide que el ERP opere.

### 6. Protocolo entre instalaciones

`support_connector` y `customer_support` viven normalmente en distribuciones
distintas. Por tanto, el descriptor de `support_connector` **no** declarará una
dependencia runtime requerida de `customer_support`.

El intercambio usará un contrato público Java puro y un protocolo HTTPS
versionado. El módulo público podrá vivir en `customer-support-api` o en un módulo
neutral separado si la historia de diseño demuestra que esa frontera es más
estable. Esa dependencia Maven no se confundirá con una dependencia funcional del
catálogo CDI de la misma distribución.

El protocolo deberá incluir:

- identidad de instalación y empresa sin reutilizar credenciales humanas;
- autenticación de máquina rotatoria y revocable, inyectada externamente;
- TLS validado y conexión iniciada siempre desde la instalación cliente;
- compatibilidad explícita de versión y negociación de capacidades;
- claves de idempotencia, correlación, expiración y protección contra repetición;
- paginación, tamaños máximos, cuotas y backoff;
- cifrado en tránsito y clasificación de cada campo transmitido;
- respuesta segura ante reloj incorrecto, caída de red o servicio central ausente;
- evidencia local y central sin registrar secretos ni payloads completos.

No se abrirá un listener administrativo en la instalación cliente. Una petición
central de diagnóstico será un mensaje tipado de una capacidad cerrada ya
implementada localmente, nunca un comando o script libre.

### 7. Consentimiento y diagnóstico

Los diagnósticos se basarán en perfiles allowlist versionados, por ejemplo:

- health y readiness;
- versión de release, digest y composición de plugins;
- estado de migraciones sin credenciales ni SQL de negocio;
- conteos y códigos de error por ventana temporal;
- fragmentos de log sanitizados por reglas locales y límites estrictos.

Cada solicitud declarará caso, motivo, categorías, ventana temporal, tamaño
máximo, vencimiento y destinatario. La política empresarial decidirá si requiere
aprobación humana en cada ocasión o si ciertas categorías no sensibles están
preautorizadas. Datos fuera del perfil se rechazan localmente.

La interfaz mostrará antes de aprobar qué se enviará y después conservará checksum,
conteos, instante, actor y resultado. La revocación impide nuevas recolecciones; no
falsifica ni borra la auditoría previa.

### 8. Dirección de dependencias e integración

La dirección prevista es:

```text
customer_support --REQUIRED--> business-partners-api
customer_support --OPTIONAL--> release-management-api
customer_support --OPTIONAL--> contratos comerciales públicos

support_connector --Maven/protocolo--> contrato público de soporte
support_connector --HTTPS saliente--> servicio central customer_support

release_management --sin dependencia--> customer_support
```

Soporte podrá presentar a `release_management` una solicitud tipada de cambio y
consumir eventos `ChangeFixed` o `ReleasePublished`. La API de releases será dueña
de esos comandos y eventos, de modo que `release_management` no necesite importar
tipos internos de soporte. No habrá ciclo, relación JPA, join ni lectura de esquema
ajeno.

### 9. Interfaz y permisos

Las pantallas centrales y locales usarán Jakarta Faces 4.1, Material Design 3,
contratos neutrales del shell y los rangos 375, 720 y 1280 px.

Recorridos centrales previstos:

- portal del cliente para crear y seguir casos propios;
- consola de agentes para cola, SLA, asignación, diagnóstico y solución;
- consola de releases para cambios, candidatos, gates, notas y publicación.

Recorridos locales del conector:

- estado del enlace y última sincronización;
- vincular o revocar una empresa;
- crear una solicitud de soporte;
- revisar y aprobar/rechazar un diagnóstico;
- consultar exactamente qué se transmitió.

Permisos preliminares:

- `customer_support.case.create`, `case.view_own`, `case.assign`, `case.resolve`,
  `sla.manage`, `diagnostics.request` y `knowledge.manage`;
- `release_management.change.manage`, `candidate.manage`, `gate.record`,
  `release.approve`, `release.publish` y `release.withdraw`;
- `support_connector.view`, `configure`, `case.submit`, `diagnostics.approve` y
  `disconnect`.

Los nombres definitivos se congelarán antes del código y cada servicio revalidará
actor, empresa, plugin, permiso, recurso y versión; ocultar un botón no será el
control de autorización.

### 10. Gate de comercialización y orden futuro

Antes de la primera entrega externa que prometa soporte integrado deberán estar
aprobados y probados:

1. modelo de identidad del portal y aislamiento de clientes;
2. alcance contractual y SLA;
3. clasificación ejecutable y compatibilidad de `PluginKind` para el conector;
4. protocolo, threat model, consentimiento y retención;
5. versión mínima de `customer_support` y `release_management`;
6. `support_connector` si la oferta incluye enlace desde el ERP;
7. recuperación, revocación, rotación de credenciales y operación degradada;
8. documentación de cliente, soporte, implementación y desarrolladores.

El conector sigue siendo opcional por empresa. Si no está contratado, ausente,
inactivo, desconectado o sin red, el ERP continúa operando y el cliente puede usar
el canal central de soporte definido por el proveedor.

## Consecuencias

### Positivas

- tickets, releases y enlace técnico tienen propietarios distintos;
- el kernel no incorpora lógica de mesa de ayuda ni entrega de software;
- las instalaciones de clientes no reciben la consola interna del proveedor;
- el diagnóstico puede limitarse, explicarse, auditarse y revocarse;
- soporte vincula un caso con una corrección sin acceder a tablas de releases;
- una caída del servicio central no bloquea la operación del ERP.

### Costes y riesgos

- el catálogo futuro crece a veintidós plugins reutilizables y suma perfiles de
  composición y matrices presente/ausente;
- identidad externa, adjuntos y diagnósticos aumentan la superficie de seguridad y
  privacidad;
- compatibilidad entre instalaciones y servicio central requiere versionado y
  pruebas de actualización;
- una allowlist demasiado amplia puede filtrar información aunque no exista shell
  remoto;
- release management puede duplicar herramientas de ingeniería si no se define la
  fuente de verdad y la integración;
- soporte 24/7, SLA contractual y residencia de datos requieren decisiones
  operativas y legales fuera del código.

## Alternativas descartadas

### Un único plugin para soporte, releases y conexión remota

Se descarta porque mezclaría datos de clientes, decisiones de publicación y
privilegios técnicos, además de obligar a enviar funcionalidad interna del
proveedor a cada instalación.

### Incluir soporte y releases en el kernel

Se descarta porque no son capacidades transversales necesarias para que los demás
dominios ERP operen.

### Acceso remoto general o agente ejecutor

Se descarta por su radio de impacto, dificultad de consentimiento, riesgo de
credenciales y posibilidad de eludir controles de la instalación.

### Replicar todos los tickets en la instalación cliente

Se descarta porque duplica la fuente de verdad, amplía datos personales locales y
complica sincronización y retención. El conector conserva referencias y cola
operativa mínima.

### Usar solamente correo y hojas de cálculo

Se admite como contingencia operativa temporal, pero no satisface SLA auditable,
trazabilidad entre caso y release, compatibilidad ni diagnóstico consentido.

## Verificación futura obligatoria

Cada épica deberá ejecutar unitarias, ArchUnit, PostgreSQL/Testcontainers, JPA/JTA,
OIDC, seguridad negativa, adjuntos maliciosos, límites, idempotencia, rotación y
revocación, incompatibilidad de protocolo, red intermitente, Docker/Compose,
health, Playwright responsive y demo navegable.

`support_connector` se probará además sin el servicio central, con certificado no
válido, endpoint no aprobado, credencial vencida, petición repetida, solicitud
expirada, payload sobredimensionado, diagnóstico rechazado, sanitización y
desconexión. Las pruebas demostrarán ausencia de listeners administrativos,
ejecución remota y secretos en logs.

No corresponde ejecutar Maven, Docker ni Playwright al aceptar este ADR porque el
cambio es exclusivamente de planificación. Su gate actual es documental.

## Referencias

- [ADR-0002 — Arquitectura de plugins](0002-arquitectura-plugins.md)
- [ADR-0005 — Contexto empresarial y activación](0005-contexto-empresarial-activacion-personalizacion.md)
- [ADR-0011 — Roadmap de plugins productivos](0011-roadmap-dependencias-plugins-productivos.md)
- [ADR-0013 — Eventos e idempotencia](0013-eventos-integracion-outbox-por-plugin.md)
- [ADR-0016 — Autorización y auditoría](0016-autorizacion-y-auditoria-operaciones-plugin.md)
- [ADR-0017 — Interacción visual neutral](0017-interaccion-visual-neutral-de-plugins.md)
- [ADR-0026 — Instalador Windows](0026-instalador-windows-bootstrapper-nativo.md)
- [Épica de soporte a clientes](../backlog/epica-soporte-clientes-erp.md)
- [Épica de gestión de lanzamientos](../backlog/epica-gestion-lanzamientos-erp.md)
- [Épica del conector seguro](../backlog/epica-conector-soporte-seguro.md)
- [Evidencia de incorporación y G0](../evidence/analisis-operaciones-proveedor-soporte-lanzamientos.md)
