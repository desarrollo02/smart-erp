# Sprint 4 — Administración operativa del kernel

- Estado: En curso; gates técnicos G0–G6 y PDF verdes, G7 independiente pendiente
- Fecha de inicio: 2026-07-28
- Duración propuesta: cuatro semanas
- Dependencia: candidata visual empresarial y administrativa disponible; validación independiente continúa pendiente
- ADR rector: [ADR-0009](../../adr/0009-autoridad-administrativa-global-kernel.md)

## Objetivo

Agregar una zona administrativa segura y responsive que permita operar las
capacidades transversales ya presentes en el kernel sin usar SQL directo,
bootstrap permanente ni privilegios empresariales como autoridad global.

Al validar el Sprint se podrá hablar de un **kernel operativo inicial completo**,
no de un kernel definitivo ni listo para producción.

## Autorización de continuidad

El Sprint 3 conserva pendiente G7: recorrido independiente de la guía y cierre
documental. El responsable de producto autorizó continuar con Sprint 4 y decidió
que las pruebas de sus historias de código se acumulen. Esta autorización no cierra
Sprint 3, no eleva la guía a `1.0` y no permite promover la candidata.

## Alcance

- contratos y modelo neutral de autoridad administrativa global;
- migración aditiva `core` V4;
- migración aditiva `core` V5 para auditoría técnica append-only;
- bootstrap one-shot del primer administrador global;
- persistencia JPA/JTA y casos de uso transaccionales;
- ruta `/admin/*` protegida por OIDC y permisos globales;
- panel de empresas, plugins y personalización exclusiva;
- panel de usuarios, membresías, roles y permisos;
- consulta segura de auditoría;
- Material Design 3 responsive sobre Jakarta Faces;
- validación acumulada y demo administrativa.

## Fuera de alcance

- dominios ERP productivos;
- API administrativa pública;
- gestión de contraseñas o configuración interna de Keycloak;
- instalación dinámica de JAR;
- edición arbitraria de XHTML/CSS/JavaScript;
- borrado de datos de plugins;
- promoción a producción.

## Secuencia de historias

| Orden | Historia | Resultado | Estado |
|---:|---|---|---|
| 1 | [J11-S4-00](J11-S4-00-gobierno-planificacion.md) | ADR, épica, alcance y gates | Completada documentalmente |
| 2 | [J11-S4-01](J11-S4-01-modelo-autoridad-global.md) | modelo neutral de autoridad global | Completada; validada en S4-08 |
| 3 | [J11-S4-02](J11-S4-02-migracion-core-v4-bootstrap-global.md) | migración `core` V4 y bootstrap global | Completada; validada en S4-08 |
| 4 | [J11-S4-03](J11-S4-03-persistencia-casos-uso-autoridad-global.md) | persistencia y casos de uso JPA/JTA | Completada; validada en S4-08 |
| 5 | [J11-S4-04](J11-S4-04-frontera-web-administrativa-confiable.md) | frontera web administrativa confiable | Completada; validada en S4-08 |
| 6 | [J11-S4-05](J11-S4-05-ui-empresas-plugins-personalizacion.md) | UI de empresas, plugins y personalización | Completada; validada en S4-08 |
| 7 | [J11-S4-06](J11-S4-06-ui-usuarios-membresias-roles-permisos.md) | UI de usuarios, membresías, roles y permisos | Completada; validada en S4-08 |
| 8 | [J11-S4-07](J11-S4-07-auditoria-visual-endurecimiento.md) | auditoría visual y endurecimiento | Completada; validada en S4-08 |
| 9 | [J11-S4-08](J11-S4-08-validacion-demo-cierre.md) | pruebas acumuladas, demo y cierre | G0–G6 verdes; G7 pendiente |

No se inicia una historia de código antes de crear su documento con criterios,
límites, migraciones, documentación afectada y matriz de pruebas pendiente.

## Criterios globales de éxito

- **CS-01:** ADR-0009 permanece trazable y compatible con ADR-0005/0006/0007.
- **CS-02:** autoridad global pertenece al kernel y no a claims o roles empresariales.
- **CS-03:** V4/V5 son aditivas; V1-V3 permanecen inmutables y JPA usa `validate`.
- **CS-04:** bootstrap global es one-shot, idempotente y sin endpoint anónimo.
- **CS-05:** el último administrador global efectivo no puede ser revocado.
- **CS-06:** cada ruta y comando administrativo exige autorización del servidor.
- **CS-07:** empresas, plugins y personalización conservan invariantes existentes.
- **CS-08:** usuarios, membresías y roles respetan empresa y concurrencia optimista.
- **CS-09:** auditoría identifica actor, operación y resultado sin secretos.
- **CS-10:** backing beans no contienen dominio ni acceso JPA directo.
- **CS-11:** UI accesible y responsive a 375, 720 y 1280 px.
- **CS-12:** demo empresarial A/B permanece operativa.
- **CS-13:** guía y runbooks se actualizan en el mismo cambio.
- **CS-14:** las pruebas pendientes quedan verdes antes del cierre.
- **CS-15:** PDF obligatorio se regenera y verifica al cerrar el Sprint.
- **CS-16:** el cierre ejecuta una demo visual administrativa reproducible en
  375/720/1280 px mediante un guion versionado, sin sustituir pruebas ni seguridad.

## Política excepcional de pruebas

Durante `J11-S4-01` a `J11-S4-07`:

- la implementación puede avanzar sin ejecutar inmediatamente pruebas automatizadas;
- cada historia queda `Implementada pendiente de pruebas`;
- solamente las pruebas pueden quedar pendientes: no código conocido, migración,
  decisión, documentación o defecto observado;
- una prueba ejecutada y fallida bloquea el avance;
- no se cierra el Sprint ni se promueve una imagen;
- `J11-S4-08` ejecuta toda la matriz acumulada.

## Gates acumulados

| Gate | Resultado requerido |
|---|---|
| G0 | documentación, ADR, enlaces y trazabilidad |
| G1 | módulos y migraciones V4/V5 compilables, sin afirmar pruebas verdes |
| G2 | JUnit y ArchUnit de autoridad y límites |
| G3 | PostgreSQL V1→V5, JPA/JTA, concurrencia y rollback |
| G4 | OIDC y autorización administrativa positiva/negativa |
| G5 | Docker/Compose, health, migraciones, secretos y persistencia |
| G6 | Playwright administrativo a 375/720/1280 y regresión A/B |
| G7 | guía, evidencia, retrospectiva y PDF final |

G0–G6 y el PDF obligatorio quedaron verdes el 2026-07-29. G7 conserva pendiente
únicamente el recorrido por una persona independiente.
La evidencia ejecutable está en
[J11-S4-08](../../evidence/J11-S4-08-validacion-demo-cierre.md).

## Escenario visual objetivo

1. una identidad empresarial normal intenta `/admin/*` y recibe denegación;
2. un administrador global entra al panel;
3. registra una empresa con su personalización obligatoria;
4. habilita un plugin funcional compatible;
5. crea una membresía y asigna un rol empresarial;
6. cambia de empresa y comprueba el menú efectivo;
7. consulta la auditoría de las operaciones;
8. intenta revocar al último administrador y el servidor lo rechaza;
9. repite el recorrido en compacto, medio y expandido.

## Riesgos

- mezclar autoridad global con roles empresariales;
- permitir enumeración mediante IDs manipulados;
- dejar la instancia sin administrador;
- mover reglas a beans JSF;
- reemplazar personalización sin validar dependencias;
- acumular demasiados cambios antes del gate de pruebas;
- presentar el panel como preparación productiva completa.

## Retrospectiva técnica provisional

- Las pruebas acumuladas detectaron defectos reales de navegación Faces, carga de
  recursos y overflow que no aparecían en compilación ni en pruebas de aplicación.
- Probar 599/600 y 839/840 px, además de 375/720/1280, evitó declarar responsive una
  pantalla que solo funcionaba en tres tamaños cómodos.
- El arnés JTA debe usar siempre una composición desechable distinta de la demo:
  limpia sus entidades operativas, pero la auditoría confirmada permanece por su
  contrato append-only.
- Comparar el ID del contenedor con el digest de la etiqueta evita demostrar una
  imagen anterior por accidente.
- La interrupción final de Docker Desktop mostró que el preflight de una demo debe
  incluir motor, contenedores y health inmediatamente antes de compartir pantalla.
- La ausencia de metadata Git continúa impidiendo certificar diff, commit y
  procedencia del árbol; no afecta las pruebas del contenido presente, pero sigue
  siendo un riesgo de gobierno.

## Continuidad autorizada

Por decisión de producto del 2026-07-29, [Sprint 5](../sprint-05/README.md) puede
avanzar con la preparación transversal de ADR-0011 mientras G7 independiente sigue
pendiente:

1. convención de esquemas y migraciones `plg_*` por plugin;
2. plantilla reproducible de plugin productivo;
3. contrato de eventos/outbox mínimo si la primera integración lo necesita;
4. planificación posterior del primer plugin productivo `business_partners`;
5. demo visual y pruebas incrementales normales, sin conservar la excepción de
   acumulación de Sprint 3/4.

Esta continuidad no cierra Sprint 4, no convierte en pendientes sus gates técnicos
verdes y no autoriza promoción o producción. El recorrido independiente de
[VALIDATION.md](../../implementation-guide/VALIDATION.md) continúa pendiente. El
orden operativo del cierre actual permanece documentado en el
[manual paso a paso](../../runbooks/manual-pruebas-j11-s4-08.md).
