# Épica — Conector seguro de soporte

- Estado: Planificada como plugin técnico opcional `support_connector`
- Fecha: 2026-08-04
- Perfil: instalación del cliente con soporte integrado contratado
- Decisión: [ADR-0036](../adr/0036-operaciones-proveedor-soporte-lanzamientos-conector.md)
- Prioridad: futura; no modifica Sprint 8 ni habilita acceso remoto

## Objetivo

Permitir que una empresa enlace voluntariamente su instalación de Logixone con el
servicio central, presente solicitudes y entregue diagnósticos estrictamente
tipados, acotados, sanitizados y auditados, sin abrir acceso administrativo
entrante ni ejecutar código remoto.

## Alcance inicial

- identidad opaca de instalación y empresa enlazada;
- conexión HTTPS siempre saliente;
- credencial de máquina rotatoria, revocable y externa al código;
- negociación de protocolo y capacidades;
- solicitud de soporte y seguimiento mediante referencia central;
- inventario permitido de release, digest y versiones de plugins;
- diagnóstico por perfiles allowlist versionados;
- consentimiento, aprobación/rechazo y revocación;
- cola local idempotente, reintento, backoff y operación offline;
- pantalla local de estado, enlace, diagnóstico y auditoría responsive.

## Prohibiciones explícitas

- no shell, terminal, PowerShell, SQL ni script remoto;
- no listener administrativo o puerto entrante;
- no escritorio remoto ni control del sistema operativo;
- no lectura arbitraria de archivo, tabla, log o dato personal;
- no descarga/ejecución de JAR, binario o código;
- no autoactualización, reinicio, instalación o rollback;
- no desactivar UAC, antivirus, firewall, TLS o políticas corporativas;
- no bloquear el ERP cuando soporte central no esté disponible.

El esquema previsto es `plg_support_connector`. `Técnico` es una clasificación de
producto; el `PluginKind` ejecutable todavía sólo admite `FUNCTIONAL` y
`CUSTOMIZATION`. SC-00 decidirá y versionará de forma compatible si se agrega
`TECHNICAL`; el conector no se presentará como personalización. Como el servicio central vive en
otra distribución, el descriptor no requiere el plugin runtime
`customer_support`. La integración usa su contrato público Java puro y un
protocolo HTTPS versionado.

## Historias propuestas

| Historia | Resultado |
|---|---|
| SC-00 | threat model, `PluginKind`, clasificación de datos, consentimiento, protocolo, retención y recuperación confirmados |
| SC-01 | evolución compatible del tipo si aplica, descriptor, dominio neutral, identidad local y contrato público de protocolo |
| SC-02 | esquema privado, migraciones, enlace, consentimiento, outbox y auditoría local |
| SC-03 | transporte HTTPS saliente, autenticación de máquina, rotación, idempotencia, cuotas y backoff |
| SC-04 | perfiles allowlist, sanitización, límites, aprobación y resultado con checksum |
| SC-05 | solicitud local de soporte y pantalla JSF Material 3 responsive |
| SC-06 | simulador central, compatibilidad, desconexión, revocación y actualización de protocolo |
| SC-07 | pruebas ofensivas/negativas, composición presente/ausente, demo, runbook, manuales y PDF |

## Criterios de aceptación

- **SC-CE01:** toda conexión se origina localmente por HTTPS a un endpoint aprobado;
  no existe listener administrativo entrante.
- **SC-CE02:** identidad de máquina y credenciales humanas son distintas; la
  credencial rota y se revoca sin reinstalar ni registrar su valor.
- **SC-CE03:** cada mensaje lleva empresa, instalación, versión de protocolo,
  correlación, idempotencia y expiración verificadas.
- **SC-CE04:** una petición libre, desconocida, repetida, expirada o
  sobredimensionada se rechaza sin ejecutar acciones.
- **SC-CE05:** cada perfil de diagnóstico declara campos, fuentes, ventana,
  tamaño, sanitización y permiso; no admite rutas, SQL o comandos recibidos.
- **SC-CE06:** la pantalla muestra categorías y destino antes de aprobar y conserva
  checksum, conteos, actor, instante y resultado después.
- **SC-CE07:** rechazo, revocación, desconexión, credencial vencida, TLS inválido o
  servicio central caído no impiden operar el ERP.
- **SC-CE08:** reintentos y reinicios no duplican casos, mensajes, diagnósticos ni
  eventos.
- **SC-CE09:** la cola local cifra o protege datos temporales según clasificación,
  aplica retención y nunca contiene secretos capturados.
- **SC-CE10:** desactivar o retirar conserva auditoría y no elimina ni modifica
  datos de otros plugins.
- **SC-CE11:** logs no contienen credenciales, payloads completos, mensajes de
  clientes, fragmentos no sanitizados ni datos personales innecesarios.
- **SC-CE12:** interfaz y consentimiento cubren 375, 720 y 1280 px, teclado, foco,
  vacío, offline, error, rechazo y acceso denegado.
- **SC-CE13:** ArchUnit demuestra que el conector no importa internos del kernel,
  soporte u otros plugins.
- **SC-CE14:** composición y pruebas demuestran que el ERP funciona igual cuando
  el conector está ausente, inactivo o desconectado.
- **SC-CE15:** descriptor, registro, scaffold y consumidores reconocen su clase
  técnica sin confundirla con `CUSTOMIZATION` ni romper plugins existentes.

## Perfiles de diagnóstico iniciales propuestos

| Perfil | Datos máximos | Requiere aprobación |
|---|---|---|
| `installation_identity` | release, digest, sistema operativo general y versión de protocolo | según política empresarial |
| `plugin_inventory` | IDs, versiones, estado físico/activo autorizado y compatibilidad | según política empresarial |
| `health_snapshot` | liveness/readiness, códigos y timestamps | según política empresarial |
| `migration_status` | versión/checksum/resultado, nunca credencial o SQL de negocio | sí en el alcance inicial |
| `sanitized_log_window` | ventana y tamaño acotados después de redacción local | siempre |

Los nombres y campos definitivos se congelarán en SC-00. Ningún perfil autoriza
una consulta arbitraria ni sustituye el consentimiento informado.

No se inicia esta épica durante Sprint 8. Antes del código deben aprobarse la
clasificación ejecutable, el threat model, el protocolo, la autoridad que puede
consentir y la política de retención/transferencia de datos.
