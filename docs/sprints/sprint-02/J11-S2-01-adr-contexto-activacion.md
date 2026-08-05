# J11-S2-01 — ADR de contexto empresarial y activación

- Estado: Completada
- Dependencia: `J11-S2-00` completada y backlog aceptado el 2026-07-27
- Tipo: Decisión arquitectónica, seguridad y datos

## Objetivo

Aceptar las invariantes de identidad de empresa, ciclo de vida, contexto confiable, activación persistida, personalización obligatoria, concurrencia y diagnóstico antes de crear contratos, tablas o adaptadores.

## Preguntas que debe resolver

1. ¿Qué tipo y formato canónico tendrá `CompanyId` y quién lo genera?
2. ¿Qué estados mínimos puede tener una empresa y qué operaciones permite cada uno?
3. ¿Una activación ausente significa desactivado y debe persistirse también el estado desactivado?
4. ¿Cómo se conserva la intención cuando un plugin se retira y luego vuelve a estar presente?
5. ¿Cómo se evita perder actualizaciones concurrentes y cómo se define la idempotencia?
6. ¿Qué recibe un caso de uso administrativo y qué recibe una operación funcional como contexto de empresa?
7. ¿Qué fuente puede establecer el contexto antes de existir identidad en Sprint 3?
8. ¿Qué diagnósticos neutrales distinguen empresa inexistente/inactiva, plugin ausente, dependencia inactiva y dependiente activo?
9. ¿Qué auditoría mínima puede registrarse sin inventar identidad de usuario?
10. ¿Qué datos son públicos entre módulos y cuáles permanecen internos al kernel?
11. ¿Cómo declara un plugin si es funcional o de personalización y cómo se garantiza la relación exclusiva empresa/personalización?
12. ¿Qué estado operativo y qué diagnóstico produce una empresa cuando su personalización asignada falta o es incompatible, y cómo impacta readiness global?
13. ¿Cómo se reemplaza una personalización de forma atómica, auditable y sin un estado intermedio válido sin plugin obligatorio?
14. ¿Cómo se garantiza que toda personalización se componga después de los plugins funcionales y solo para su empresa asignada?
15. ¿Qué contrato público y versionado puede modificar una pantalla ajena sin permitir acceso a internos ni debilitamiento de seguridad?

## Resolución propuesta

- identificador UUID opaco, sin significado empresarial incorporado;
- ciclo mínimo `ACTIVE`/`INACTIVE`, sin borrado físico en este Sprint;
- ausencia de fila igual a desactivado;
- fila explícita con estado deseado y versión optimista, sin eliminación al desactivar;
- comandos administrativos con `CompanyId` explícito y operaciones funcionales mediante un puerto neutral de contexto;
- adaptadores de contexto únicamente en pruebas durante Sprint 2; ningún header HTTP confiable hasta integrar identidad;
- activación y desactivación idempotentes, con rechazo transaccional ante dependencias inválidas;
- categoría explícita `FUNCTIONAL`/`CUSTOMIZATION` en el descriptor, sin inferirla del nombre;
- relación persistida uno a uno: una empresa operativa tiene una personalización y una personalización pertenece a una sola empresa;
- personalización fuera del flujo normal de desactivación y reemplazo mediante una única transición validada;
- orden de composición global con todos los plugins funcionales antes de cualquier personalización y selección empresarial de una sola capa final;
- extensiones de pantalla mediante identificadores, versiones, slots y propiedades públicas; nunca mediante reemplazo arbitrario de recursos o importación de internos;
- códigos de error estables y logs estructurados sin nombres comerciales ni datos sensibles.

Estas decisiones quedaron aceptadas en [ADR-0005 — Contexto empresarial, activación y personalización obligatoria](../../adr/0005-contexto-empresarial-activacion-personalizacion.md).

## Alcance

- requisitos, invariantes y ejemplos positivos/negativos;
- decisión de propiedad por módulo;
- modelo conceptual de datos y restricciones;
- política de concurrencia e idempotencia;
- frontera de confianza del contexto empresarial;
- propiedad, obligatoriedad, compatibilidad, orden y sustitución del plugin de personalización;
- límites de los contratos de pantalla y de los cambios que un overlay puede realizar;
- impacto en migraciones, JPA, pruebas y futura identidad;
- plan de compatibilidad y recuperación de V1 a V2.

## Fuera de alcance

- código Java, SQL, POM o configuración WildFly;
- selección de proveedor OIDC;
- usuario, rol, permiso asignado o sesión;
- API HTTP o UI administrativa;
- hard delete o fusión de empresas.

## Resultado de la elaboración

- `CompanyId` se propone como UUID opaco generado por un puerto del kernel.
- toda empresa se registra `INACTIVE` y con `customization_plugin_id` no nulo y único.
- la activación común aplica solo a plugins `FUNCTIONAL`; ausencia de fila significa desactivado.
- una personalización ausente o incompatible pone en cuarentena a su empresa, sin bajar readiness global.
- un catálogo físico inválido continúa bajando readiness para toda la instancia.
- reemplazar una personalización requiere una transición optimista, atómica y auditable.
- `plugin-api` sube de `0.1.0` a `0.2.0` al introducir categoría obligatoria sin valor predeterminado.
- los contratos de pantalla son Java puro, cerrados por defecto y aplican el overlay empresarial al final.

La evidencia está en [J11-S2-01 — ADR-0005](../../evidence/J11-S2-01-adr-contexto-activacion-personalizacion.md).

## Criterios de aceptación

- **CA-01:** el ADR enumera alternativas y justifica la representación de `CompanyId`.
- **CA-02:** define estados de empresa y comportamiento operativo de cada uno.
- **CA-03:** define de manera inequívoca el estado ausente y la activación efectiva.
- **CA-04:** conserva decisiones y datos al desactivar o retirar un plugin.
- **CA-05:** activación/desactivación y dependencias tienen invariantes transaccionales explícitas.
- **CA-06:** concurrencia, idempotencia y conflictos tienen resultado observable y estable.
- **CA-07:** ninguna fuente HTTP no autenticada se declara confiable para el contexto.
- **CA-08:** separa comandos administrativos de contexto usado por operaciones funcionales.
- **CA-09:** define diagnósticos sin filtrar información entre empresas.
- **CA-10:** ubica cada contrato público en `plugin-api` o `kernel-api` según su responsabilidad y mantiene entidades/adaptadores internos.
- **CA-11:** documenta estrategia V1→V2, compatibilidad, recuperación y prohibición de DDL automático.
- **CA-12:** actualiza arquitectura, estrategia de pruebas y backlog sin contradicciones ni enlaces rotos.
- **CA-13:** formaliza la categoría de plugin y la relación uno a uno entre empresa y personalización.
- **CA-14:** define el estado de una empresa sin personalización válida y su efecto explícito sobre readiness.
- **CA-15:** define una sustitución atómica y auditable que no usa la desactivación común.
- **CA-16:** establece el orden final y el aislamiento empresarial de la personalización.
- **CA-17:** delimita un contrato público versionado de pantalla y prohíbe acceso a internos, tablas ajenas y relajación de controles del servidor.

## Gates

- G0 completo.
- Revisión de coherencia contra ADR-0002, ADR-0003 y AGENTS.md.
- Cero cambio de código o datos antes de aceptar el ADR.

## Siguiente historia permitida

`J11-S2-02`, habilitada después de aceptar ADR-0005 y completar los gates documentales.
