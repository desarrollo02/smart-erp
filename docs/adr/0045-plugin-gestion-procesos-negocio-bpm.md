# ADR-0045 — Plugin de gestión de procesos de negocio BPM

- Estado: Aceptado para planificación; implementación no autorizada
- Fecha: 2026-08-11
- Decisión de producto: agregar al plan una capacidad transversal para que cada
  empresa configure, ejecute, siga y mejore sus procesos
- Plugin propuesto: `business_process_management`
- Épica: [Gestión de procesos de negocio](../backlog/epica-gestion-procesos-negocio-bpm.md)
- Piloto propuesto: aprobación de solicitudes de Compras

## Contexto

Los plugins funcionales conservan reglas y ciclos propios, pero cada empresa
puede necesitar una secuencia distinta de tareas, responsables, plazos,
escalamientos y decisiones alrededor de esas operaciones. Codificar cada flujo
en Compras, Ventas, Recursos Humanos o Soporte duplicaría lógica, haría rígida la
implantación y dificultaría medir tiempos y cuellos de botella.

Smart ERP ya posee empresa, identidad, autorización, auditoría, activación de
plugins y pantallas neutrales. ADR-0013 define eventos públicos, outbox/inbox,
idempotencia y recuperación, aunque su transporte todavía no está materializado.
Falta una capacidad empresarial que use esas bases para administrar procesos de
larga duración sin convertirse en dueño de los datos o invariantes de otros
plugins.

BPMN 2.0.2 es la versión formal publicada por OMG para modelar procesos de
negocio. DMN 1.5 es la versión formal aplicable para decisiones y reglas; versiones
posteriores permanecen en beta al registrar esta decisión. Adoptar estas
referencias no implica implementar todas sus construcciones ni certificar
conformidad completa.

## Decisión propuesta

### 1. Identidad y posición

Se planifica `business_process_management` como plugin `FUNCTIONAL`, transversal,
reutilizable y opcional.

- No recibe número en la secuencia ERP 1–19 ni desplaza `purchasing`, `sales` o
  los plugins posteriores.
- Eleva el catálogo global planificado de treinta a treinta y un plugins
  reutilizables.
- Puede estar físicamente presente y activarse sólo para las empresas que lo
  contraten o configuren.
- No pertenece al kernel ni se clasifica como `CUSTOMIZATION`.
- Los plugins operativos no dependen de su implementación y continúan operando
  con sus reglas esenciales cuando BPM está ausente o inactivo.

### 2. Responsabilidad

El plugin será dueño de:

- definiciones empresariales de proceso y versiones publicadas;
- actividades, transiciones, variables declaradas, responsables y plazos;
- instancias, tokens de ejecución, tareas humanas y temporizadores;
- suscripciones, inbox, correlación, reintentos e incidentes;
- historia append-only y evidencia de decisiones;
- métricas de duración, espera, SLA, retrabajo, carga y cuellos de botella;
- diseñador guiado, bandeja de trabajo, seguimiento y administración.

No será dueño de solicitudes, órdenes, clientes, artículos, inventario, facturas,
pagos, empleados ni estados propios de otros dominios. Una tarea completada no
modifica por sí sola un agregado ajeno.

### 3. Autoridad del dominio

BPM coordina; el plugin funcional decide. Toda acción sobre otro módulo debe:

1. usar un contrato público, tipado y versionado;
2. revalidar empresa, plugin efectivo, actor y permiso exacto;
3. conservar versión esperada e idempotencia;
4. aceptar que el dominio rechace la transición;
5. registrar el resultado sin forzar ni reescribir estado privado.

Un proceso de aprobación de Compras puede elegir responsables, plazos y
escalamientos, pero `purchasing` mantiene la separación solicitante/aprobador,
los estados válidos y el permiso `purchasing.requests.approve`.

### 4. Estándares y alcance inicial

Las definiciones usarán conceptos y, cuando se habilite intercambio, artefactos
compatibles con BPMN 2.0.2. La primera versión implementará y documentará un
subconjunto cerrado:

- inicio manual o por evento público;
- fin exitoso, cancelado o fallido;
- tarea humana;
- compuerta exclusiva y paralela;
- temporizador, vencimiento y escalamiento;
- variables declaradas y tipadas;
- asignación por usuario o permiso;
- acción pública registrada;
- reintento, incidente y recuperación.

Un elemento no soportado bloquea la publicación con un diagnóstico comprensible;
nunca se ignora silenciosamente. DMN 1.5 queda planificado para una fase posterior
de tablas de decisión versionadas. CMMN, coreografías, transacciones BPMN,
compensación genérica y procesos ad hoc completos quedan fuera de V1.

### 5. Versionado y publicación

- Una definición comienza `DRAFT` y puede revisarse sin afectar ejecuciones.
- Publicar crea una versión inmutable con autor, instante, formato y checksum.
- Las nuevas instancias usan la versión vigente al comenzar.
- Una instancia continúa sobre su versión original.
- Migrar una instancia exige un plan explícito, compatible, autorizado y
  auditable; no se hace automáticamente.
- Retirar una versión impide nuevos inicios, pero conserva instancias, tareas,
  historia y métricas.

### 6. Integración neutral

Los eventos que inician o continúan procesos deben cumplir ADR-0013. BPM conserva
un inbox idempotente por `event_id`; no consulta outboxes ni esquemas privados.

BPM-01 debe decidir un contrato Java puro para contribuciones de proceso, con
opciones como un módulo neutral `process-contract-api`. El contrato describirá,
sin Jakarta ni clases internas:

- evento o disparador público;
- sujeto e identificador opaco;
- acción permitida y versión;
- parámetros y resultados tipados;
- permiso exigido;
- política de idempotencia y correlación.

No se admite registrar nombres de clases para reflexión, SQL, URL arbitraria,
scripts, EL, JavaScript, Groovy ni código suministrado por una empresa.

### 7. Ejecución durable

Las instancias, tareas y temporizadores se persisten en
`plg_business_process_management`. La ejecución debe tolerar reinicios y entrega
`at-least-once` mediante reclamación con lease, versión optimista e idempotencia.
No mantiene una transacción abierta mientras espera una tarea humana.

Jakarta Concurrency o un temporizador administrado puede despertar el trabajo,
pero la base es la fuente durable. Jakarta Batch puede apoyar reprocesamientos o
proyecciones masivas; no sustituye el motor BPM. No se prometen transacciones
distribuidas. Acciones entre plugins usan confirmación, eventos y compensaciones
de negocio explícitas.

### 8. Persistencia conceptual

El esquema debe separar al menos:

- `process_definition` y `process_version`;
- `activity_definition`, `transition_definition` y `action_binding`;
- `process_instance` y `execution_token`;
- `work_item` y asignaciones;
- variables declaradas con tipos controlados;
- `timer_job`, `event_subscription` y `event_inbox`;
- `process_history`, `process_incident` y proyecciones métricas.

El XML BPMN original puede conservarse como artefacto inmutable de intercambio,
pero no será la única fuente operativa. El runtime usa un modelo validado y
normalizado. Ninguna tabla contiene FK o relación JPA hacia otro plugin.

### 9. Seguridad

La administración separará permisos para ver, crear, editar, publicar y retirar
definiciones; iniciar/cancelar instancias; ver, reclamar, completar o reasignar
tareas; recuperar incidentes y consultar métricas.

- Una asignación BPM no concede permisos del dominio destino.
- Completar la tarea y ejecutar la acción empresarial son autorizaciones
  diferentes.
- Los selectores de usuarios, permisos y sujetos se resuelven por contratos
  públicos y siempre por empresa.
- Variables, historial y métricas minimizan datos personales y aplican retención.
- Secretos, tokens y credenciales nunca son variables de proceso.
- La publicación de una definición puede configurarse con separación
  autor/publicador.

### 10. Interfaz

La UI seguirá Jakarta Faces 4.1, Material Design 3, contratos neutrales y los tres
rangos responsive. V1 comienza con un diseñador guiado del subconjunto soportado,
bandeja de tareas, ficha de instancia, incidentes y métricas.

Un lienzo BPMN avanzado puede requerir una biblioteca visual. Antes de agregarla
se deben documentar versión, licencia, seguridad, accesibilidad, funcionamiento
offline, compatibilidad JSF y ADR. No se carga JavaScript remoto ni se permite
inyectar XHTML, CSS, EL o scripts desde una definición.

### 11. Motor

Esta planificación no selecciona todavía un motor. BPM-00 debe comparar:

1. motor acotado propio, alineado con el subconjunto y el monolito modular;
2. biblioteca embebida detrás de puertos propios;
3. servicio externo, sólo si un ADR futuro cambia la topología oficial.

La evaluación debe cubrir Java 21, Jakarta EE 11, WildFly 41, PostgreSQL,
transacciones JTA, esquema privado, tamaño, CVE, licencia, actualización,
portabilidad de definiciones, operación y pruebas. Incorporar Spring, otro runtime
o un servicio externo no queda autorizado por este ADR.

### 12. Piloto

El primer piloto será la aprobación de solicitudes de Compras después de que
J11-S9-06 componga `purchasing`. El piloto materializará el primer productor,
consumidor y evento real de ADR-0013 y demostrará:

- inicio por solicitud enviada;
- asignación a un aprobador distinto del solicitante;
- vencimiento y escalamiento;
- aprobación o rechazo mediante contrato público;
- duplicados, reinicios y reintentos idempotentes;
- seguimiento de duración y SLA;
- operación normal de Compras cuando BPM está inactivo.

## Secuencia planificada

| Orden | Historia | Resultado |
|---:|---|---|
| 1 | BPM-00 | caracterización, decisiones y spike construir/adoptar |
| 2 | BPM-01 | API neutral, contribuciones, subconjunto y versionado |
| 3 | BPM-02 | esquema privado, definiciones, instancias y tareas |
| 4 | BPM-03 | motor durable de tareas humanas y compuertas |
| 5 | BPM-04 | eventos, temporizadores, incidentes y recuperación |
| 6 | BPM-05 | diseñador guiado, bandeja y administración responsive |
| 7 | BPM-06 | SLA, métricas, cuellos de botella y comparación de versiones |
| 8 | BPM-07 | piloto autorizado con solicitudes de Compras |
| 9 | BPM-08 | matriz integral, demo, manuales, PDF y decisión de instalador |

## Consecuencias

### Positivas

- cada empresa adapta recorridos sin bifurcar el código de los dominios;
- procesos y responsables quedan versionados y auditables;
- métricas permiten mejorar tiempos y detectar esperas;
- un mismo motor transversal sirve a distintos plugins;
- los dominios conservan autoridad, datos y seguridad.

### Costes y riesgos

- un motor durable, temporizadores y recuperación agregan complejidad operativa;
- BPMN completo es demasiado amplio para una primera versión;
- acciones genéricas mal diseñadas podrían eludir autorización o crear ciclos;
- datos de proceso pueden duplicar información sensible si no se minimizan;
- el modelador visual puede introducir una dependencia grande o inaccesible;
- cambiar versiones con instancias activas exige reglas estrictas;
- el primer piloto obliga a materializar y operar outbox/inbox.

## Alternativas descartadas

### Guardar workflows dentro del kernel

Se descarta porque el kernel no es dueño de procesos empresariales ni de sus
datos, tareas o métricas.

### Permitir scripts, SQL o endpoints arbitrarios

Se descarta porque rompe límites de plugins, autorización, reproducibilidad y
seguridad, y convierte la configuración empresarial en ejecución remota.

### Reemplazar las reglas del dominio con BPM

Se descarta porque permitiría que una definición omita invariantes, permisos,
versiones o controles fiscales y contables.

### Exigir BPM como dependencia de todos los plugins

Se descarta porque dejaría de ser opcional y haría que dominios operativos
dependan de un orquestador transversal.

## Gates antes de implementar

- [ ] aprobar BPM-D01 a BPM-D12;
- [ ] decidir motor mediante spike reproducible;
- [ ] fijar subconjunto BPMN y política de importación/exportación;
- [ ] decidir ubicación y versión del contrato neutral de acciones;
- [ ] materializar ADR-0013 con productor y consumidor reales;
- [ ] aprobar modelo de autorización y retención de variables;
- [ ] decidir biblioteca de modelado o diseñador guiado sin dependencia adicional;
- [ ] aprobar perfiles físicos y operación con BPM presente/ausente;
- [ ] definir pruebas de reinicio, lease, duplicados, reloj y recuperación.

## Referencias

- [ADR-0002 — Arquitectura de plugins](0002-arquitectura-plugins.md)
- [ADR-0005 — Contexto empresarial y activación](0005-contexto-empresarial-activacion-personalizacion.md)
- [ADR-0011 — Roadmap de plugins](0011-roadmap-dependencias-plugins-productivos.md)
- [ADR-0013 — Eventos de integración y outbox](0013-eventos-integracion-outbox-por-plugin.md)
- [ADR-0016 — Autorización y auditoría de plugins](0016-autorizacion-y-auditoria-operaciones-plugin.md)
- [OMG BPMN 2.0.2](https://www.omg.org/spec/BPMN/)
- [OMG DMN](https://www.omg.org/spec/DMN/)
- [Jakarta Batch 2.1](https://jakarta.ee/specifications/batch/2.1/)
