# Épica — Gestión de procesos de negocio BPM

- Estado: Planificada; BPM-D01 a BPM-D12 pendientes
- Plugin: `business_process_management`
- Clasificación: funcional transversal, reutilizable y opcional por empresa
- Prioridad: planificar ahora; implementar después de componer Compras y antes de
  generalizar workflows configurables en plugins posteriores
- Decisión: [ADR-0045](../adr/0045-plugin-gestion-procesos-negocio-bpm.md)
- Piloto: aprobación de solicitudes de `purchasing`

## Objetivo

Permitir que cada empresa defina, publique, ejecute y mida procesos propios con
tareas, responsables, decisiones, plazos y escalamiento, conservando la autoridad
de cada plugin funcional y una historia completa de las ejecuciones.

## Valor de negocio

- adapta procesos por empresa sin copiar ni bifurcar módulos;
- permite saber dónde está cada caso y quién debe actuar;
- mide tiempos de ciclo, espera, SLA, retrabajo e incidentes;
- compara versiones para comprobar si un cambio realmente mejoró el proceso;
- reduce seguimientos manuales por correo o planillas;
- conserva evidencia de responsables, decisiones y vencimientos;
- reutiliza una capacidad común en Compras, Ventas, RR. HH. y Soporte.

## Audiencias

| Actor | Necesidad |
|---|---|
| diseñador de procesos | modelar tareas, decisiones, responsables y plazos |
| dueño del proceso | aprobar/publicar versiones y revisar resultados |
| participante | consultar, reclamar y completar tareas autorizadas |
| supervisor | reasignar, escalar y resolver excepciones permitidas |
| auditoría/soporte | reconstruir historia, reglas, versión y correlación |
| dirección | observar SLA, carga, tendencias y cuellos de botella |
| implementador | registrar eventos y acciones públicas sin romper plugins |

## Capacidades

### Definiciones y versiones

- directorio por empresa, código, nombre, propietario y estado;
- borrador editable y versión publicada inmutable;
- subconjunto BPMN 2.0.2 explícito y validado;
- responsables, permisos, variables tipadas, SLA y escalamiento;
- importación/exportación sólo de elementos soportados;
- retiro sin borrar historia o instancias.

### Ejecución

- inicio manual o por evento público;
- tareas humanas, compuertas exclusivas/paralelas y temporizadores;
- tokens persistentes y reinicio seguro;
- acciones públicas allowlist con autorización e idempotencia;
- pausa, cancelación y recuperación según permisos;
- incidentes, reintentos, cuarentena y diagnóstico comprensible.

### Trabajo humano

- bandeja `Mis tareas`, disponibles, reclamadas y vencidas;
- asignación por usuario o permiso empresarial;
- separación autor/aprobador y otras incompatibilidades declaradas;
- reclamar, liberar, completar, devolver, reasignar o escalar;
- comentario y evidencia controlados, sin almacenar secretos;
- enlace seguro a la pantalla del dominio propietario.

### Seguimiento y mejora

- instancia, etapa actual, participantes, fechas y versión;
- duración total, tiempo activo y espera;
- cumplimiento de SLA y vencimientos;
- retrabajo, reasignaciones, rechazos e incidentes;
- carga y antigüedad por actividad;
- comparación de versiones del mismo proceso;
- proyecciones y exportaciones autorizadas, sin joins privados.

## Límites

- BPM no modifica directamente esquemas de otros plugins;
- una tarea no reemplaza permisos o invariantes del dominio;
- no se permiten SQL, scripts, EL, JavaScript, Groovy, reflexión por nombre de
  clase ni HTTP arbitrario configurados por la empresa;
- no se prometen todos los elementos de BPMN ni conformidad completa en V1;
- no se guardan tokens, contraseñas o secretos como variables;
- no se migran instancias activas silenciosamente;
- no existe una transacción distribuida de larga duración;
- BPM inactivo no impide el funcionamiento esencial de los dominios.

## Decisiones pendientes BPM-D01 a BPM-D12

| ID | Decisión requerida |
|---|---|
| BPM-D01 | motor acotado propio, biblioteca embebida o alternativa aprobada |
| BPM-D02 | subconjunto exacto BPMN 2.0.2 de V1 |
| BPM-D03 | formato canónico normalizado y artefacto de intercambio |
| BPM-D04 | ubicación/versionado de `process-contract-api` o contrato equivalente |
| BPM-D05 | catálogo de eventos y acciones allowlist del piloto |
| BPM-D06 | asignación por usuario, permiso, separación y delegación |
| BPM-D07 | variables permitidas, sensibilidad, cifrado y retención |
| BPM-D08 | temporizadores, lease, reloj, nodos múltiples y recuperación |
| BPM-D09 | política de cambios e instancias entre versiones |
| BPM-D10 | diseñador guiado y eventual biblioteca BPMN accesible/offline |
| BPM-D11 | métricas, retención, anonimización y exportación |
| BPM-D12 | perfiles de composición, licencia y operación presente/ausente |

Ninguna se presupone resuelta por aceptar esta épica.

## Mapa de historias

| Orden | Historia | Entregable |
|---:|---|---|
| 1 | BPM-00 | caracterización, BPM-D01 a BPM-D12 y spike de motor |
| 2 | BPM-01 | API neutral, contribuciones y modelo versionado |
| 3 | BPM-02 | esquema privado, persistencia e idempotencia |
| 4 | BPM-03 | ejecución durable, tareas humanas y compuertas |
| 5 | BPM-04 | eventos, outbox/inbox, temporizadores e incidentes |
| 6 | BPM-05 | diseñador guiado, bandeja y administración responsive |
| 7 | BPM-06 | seguimiento, SLA, métricas y comparación |
| 8 | BPM-07 | piloto con aprobación de solicitudes de Compras |
| 9 | BPM-08 | gates integrales, demo, manuales, PDF y cierre |

## Piloto de Compras

El piloto comienza con una solicitud enviada y crea una instancia correlacionada
por empresa e identificador público. Selecciona un aprobador distinto del
solicitante, genera una tarea con vencimiento y permite aprobar o rechazar usando
un comando público de Compras. El resultado del dominio confirma o rechaza el
avance; BPM no escribe `plg_purchasing`.

Debe demostrar:

- evento de inicio confirmado mediante outbox;
- duplicación y reentrega inocuas;
- reinicio con tarea/temporizador pendientes;
- autorización negativa y empresa ajena;
- plugin BPM inactivo y Compras operativa;
- escalamiento por SLA;
- versión de proceso y dominio trazables;
- métricas sin datos sensibles innecesarios.

## Criterios de aceptación de la épica

- **CE-01:** cada empresa ve y ejecuta sólo sus definiciones, instancias y tareas.
- **CE-02:** una versión publicada es inmutable y conserva checksum.
- **CE-03:** elementos no soportados impiden publicar; no se ignoran.
- **CE-04:** una instancia queda ligada a una versión concreta.
- **CE-05:** tareas humanas exigen actor, empresa, permiso y versión vigentes.
- **CE-06:** el dominio destino vuelve a autorizar y puede rechazar la acción.
- **CE-07:** no existen FK, JPA o SQL hacia esquemas ajenos.
- **CE-08:** eventos duplicados no duplican instancias ni efectos.
- **CE-09:** reinicios no pierden tokens, tareas, temporizadores o incidentes.
- **CE-10:** BPM presente/ausente e inactivo se prueba por empresa.
- **CE-11:** variables y logs no contienen secretos o datos personales excesivos.
- **CE-12:** métricas explican versión, período y población sin reescribir historia.
- **CE-13:** UI funciona en 375, 720 y 1280 px con teclado y foco visible.
- **CE-14:** el piloto de Compras conserva todas las invariantes de `purchasing`.
- **CE-15:** documentación, demo y PDF distinguen estándar soportado de trabajo
  futuro.

## Matriz automatizada mínima

- dominio del modelo y validación del subconjunto;
- publicación concurrente y checksum;
- PostgreSQL/Testcontainers, Flyway e idempotencia;
- empresa ajena, permisos, plugin inactivo y separación de funciones;
- tokens paralelos, timers, lease, reloj y dos nodos;
- outbox/inbox, duplicado, desorden, reinicio, cuarentena y replay;
- acciones de dominio aceptadas/rechazadas y compensación explícita;
- ArchUnit y cero acceso a implementaciones/esquemas ajenos;
- composición WAR/migrador con BPM presente y ausente;
- Docker/Compose, health, persistencia y recreación;
- Playwright responsive del diseñador, bandeja, instancia y monitoreo;
- regresión de Compras con BPM activo e inactivo.

La prueba de aceptación por otra persona sigue siendo independiente de esta
matriz automatizada.

## Dependencias y secuencia

- Requiere kernel vigente, contrato de autorización y ADR-0013 materializado.
- El piloto requiere `purchasing` compuesto y un contrato público de aprobación.
- No requiere `sales`, RR. HH. ni plugins financieros para comenzar.
- No cambia la secuencia ERP 1–19.
- Planificar BPM no habilita código durante J11-S9-06; primero deben aceptarse
  BPM-D01 a BPM-D12 y crearse una iteración propia.

## Resultado esperado

Una empresa puede publicar una versión controlada, iniciar casos, trabajar tareas
y observar SLA sin acceso técnico a tablas o código. El mismo proceso puede
mejorarse mediante una nueva versión y compararse objetivamente con la anterior.
