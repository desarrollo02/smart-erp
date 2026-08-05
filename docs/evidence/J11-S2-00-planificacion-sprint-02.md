# J11-S2-00 — Evidencia de planificación del Sprint 2

- Fecha: 2026-07-27
- Estado: Verde
- Tipo de cambio: documentación y gobierno; sin código, SQL, dependencias ni infraestructura

## Fuentes revisadas

- `AGENTS.md` y su orden, límites de kernel, plugins, persistencia y pruebas;
- ADR-0002, incluida la obligación explícita de probar activación persistida y filtrado por empresa en Sprint 2;
- ADR-0003 sobre propiedad del esquema `core`, unidades de persistencia, Flyway y prohibición de DDL automático;
- arquitectura y estrategia de pruebas vigentes;
- contratos `PluginDescriptor`, `PluginDependency`, `MigrationContribution` y `MenuContribution`;
- `PluginCatalogResolver`, `PluginRegistry` y migrador implementados;
- riesgos residuales y conclusión de `J11-S1-07`.
- decisión de producto confirmada el 2026-07-27: cada empresa tiene un plugin de personalización distinto, obligatorio y capaz de modificar pantallas de otros plugins.

## Resultado

Se crearon las épicas de kernel multiempresa y personalización obligatoria. El Sprint 2 propuesto conserva una sola meta demostrable: persistir y aplicar activación y personalización por empresa sin introducir autenticación, UI renderizada ni dominios ERP productivos.

El backlog contiene nueve historias lineales:

1. `J11-S2-00`: gobierno y planificación;
2. `J11-S2-01`: ADR e invariantes;
3. `J11-S2-02`: contratos y modelo neutral;
4. `J11-S2-03`: migración `core` V2;
5. `J11-S2-04`: persistencia JPA/JTA;
6. `J11-S2-05`: casos de uso y guardas;
7. `J11-S2-06`: filtrado de contribuciones;
8. `J11-S2-07`: contrato y composición de personalizaciones de pantalla;
9. `J11-S2-08`: validación integral y cierre.

Cada historia tiene dependencia inmediata, objetivo, alcance, fuera de alcance, criterios de aceptación, gates y siguiente paso. El Sprint define 16 criterios globales y las historias suman 147 criterios verificables.

## Adenda de personalización por empresa

La planificación inicial tenía ocho historias, 12 criterios globales y 98 criterios de historia. Tras la nueva decisión de producto se realizó una revisión transversal y se estableció que:

- una empresa operativa debe tener exactamente una personalización propia;
- un plugin de personalización no se comparte con otra empresa ni se desactiva por el flujo común;
- todos los plugins funcionales se componen antes de la personalización asignada;
- modificar una pantalla ajena exige un contrato público, tipado y versionado publicado por su propietario;
- las personalizaciones no pueden importar internos, acceder a tablas ajenas, reemplazar recursos arbitrariamente ni relajar autorización, validación o auditoría;
- el renderizado JSF/PrimeFaces y Playwright quedan para el incremento que implemente la primera UI, pero el contrato neutral y su compositor se prueban en Sprint 2.

La duración propuesta cambió de dos a tres semanas. `J11-S2-07` pasó a ser la historia específica de personalización y el cierre integral se renumeró como `J11-S2-08`.

## Adenda de guía para implementadores

El 2026-07-27 se agregó como entregable obligatorio una guía que pueda entregarse a quienes implementarán el ERP para empresas concretas. No se creó una historia posterior al cierre: la primera edición utilizable forma parte de `J11-S2-08`, porque debe escribirse y validarse contra todas las capacidades reales del Sprint antes de certificar el baseline.

La fuente canónica se creó en `docs/implementation-guide/README.md` y define:

- audiencia y propósito didáctico;
- recorrido desde relevamiento hasta entrega y soporte;
- clasificación entre configuración, plugin funcional reutilizable y personalización exclusiva;
- preparación de ambiente, empresa, plugins, datos, pruebas, despliegue, diagnóstico y rollback;
- ejemplo completo con una empresa ficticia;
- validación por un implementador que no haya escrito la funcionalidad;
- versionado y obligación de actualización en historias futuras.

La adenda agrega `CS-17` al Sprint y `CA-19` a `CA-24` en la historia de cierre. El plan vigente conserva nueve historias y pasa a 17 criterios globales y 153 criterios de aceptación de historias.

## Decisiones de seguridad y alcance

- Identidad/OIDC continúa diferida a Sprint 3 según la arquitectura vigente.
- Sprint 2 no confiará en un header HTTP arbitrario para establecer empresa.
- No habrá endpoint administrativo o funcional público sin autorización.
- Las pruebas de comportamiento usarán servicios directamente y, solo si es imprescindible para WildFly, un arnés exclusivo de tests ausente del WAR normal.
- Testcontainers será obligatorio para SQL y repositorios PostgreSQL.
- El plugin de referencia seguirá sin persistencia propia; el descubrimiento de migraciones `plg_*` permanece fuera del Sprint.
- La asignación de personalización pertenece al kernel; el descriptor no incorpora `CompanyId` ni datos comerciales.
- La ausencia o incompatibilidad del plugin obligatorio se resuelve de forma segura y su efecto exacto sobre readiness debe cerrarse en `J11-S2-01`.
- Ocultar o habilitar componentes visuales nunca sustituye las guardas y validaciones del servidor.

## Gates ejecutados

### G0 incremental

Después de cada corte documental se validaron UTF-8 estricto, caracteres de reemplazo y enlaces locales. La planificación original terminó con 55 archivos Markdown válidos. La adenda terminó con 57 archivos válidos y cero enlaces rotos.

La adenda de guía para implementadores terminó con 62 Markdown, 153 enlaces locales, nueve historias, 153 criterios de historia, 17 criterios globales y cero errores de estructura, UTF-8 o enlaces.

### Auditoría estructural

El primer control encontró dos brechas:

- `J11-S2-00` no tenía un encabezado explícito de gates;
- `J11-S2-05` detallaba casos de uso, pero no una sección explícita de alcance.

Se corrigieron ambas y la repetición terminó con:

```text
stories=9 structural_errors=0
```

Todos los documentos de historia tienen estado, dependencia, objetivo, alcance, exclusiones, criterios y gates.

### Auditoría de coherencia

Resultado:

```text
sprint_success_criteria=16 linked_story_rows=9
total_story_acceptance_criteria=147
```

La búsqueda cruzada confirmó que OIDC, headers, endpoints, UI renderizada, Playwright y migraciones de plugins solo aparecen como límites, prohibiciones o trabajo futuro. El contrato neutral de pantalla es alcance explícito. Testcontainers está requerido para migración, repositorios y cierre integral.

El primer intento de resumir estas cantidades tuvo una subexpresión PowerShell inválida y no llegó a evaluar documentos. Se reemplazó por variables intermedias y la repetición produjo los valores anteriores.

El primer intento de aplicar la adenda en un único parche no encontró una línea de contexto que había cambiado y terminó sin modificar archivos. Se dividió el cambio por documento, se aplicó de forma incremental y cada corte posterior pasó G0.

El primer lanzamiento de la auditoría final no llegó a ejecutar PowerShell porque un carácter de acento grave dentro de la expresión regular cerró prematuramente la plantilla JavaScript del orquestador. Se eliminó ese carácter de la expresión y la repetición final produjo los conteos verdes registrados arriba.

## Cobertura de aceptación

| Criterio | Evidencia |
|---|---|
| `CA-01` | Épicas trazadas a ADR-0002, ADR-0003, cierre de Sprint 1 y decisión de producto. |
| `CA-02` | Un objetivo: activación y personalización persistidas por empresa; identidad/UI renderizada/dominio fuera. |
| `CA-03` | Nueve historias enlazadas con dependencia lineal. |
| `CA-04` | Auditoría estructural final: 9 historias, cero brechas. |
| `CA-05` | Las decisiones críticas se concentran en `J11-S2-01`. |
| `CA-06` | Testcontainers obligatorio desde migración/repositorios. |
| `CA-07` | Header arbitrario y endpoints no autorizados expresamente prohibidos. |
| `CA-08` | Índices de backlog, Sprints y documentación actualizados. |
| `CA-09` | G0 final ejecutado después de esta evidencia. |
| `CA-10` | Sprint permanece propuesto; `J11-S2-01` exige aceptación explícita. |
| `CA-11` | Épica, Sprint e historias exigen una personalización exclusiva y obligatoria por empresa. |
| `CA-12` | Contratos de pantalla públicos/versionados y prohibiciones de seguridad documentados. |
| `CA-13` | `J11-S2-07` implementa la composición neutral antes de `J11-S2-08`. |
| `CA-14` | Duración, riesgos, gates, métricas y secuencia fueron recalculados. |

## Pruebas no ejecutadas

No se ejecutó Maven, Docker, Compose ni PostgreSQL porque esta historia solo modifica Markdown y no cambia contratos, código, POM, SQL ni procedimientos operativos. Según la estrategia aceptada, su gate aplicable es G0.

## Archivos creados o modificados

- dos épicas e índice de backlog;
- README del Sprint 2;
- nueve documentos `J11-S2-00` a `J11-S2-08`;
- índices generales de documentación, Sprints y evidencias;
- enlace de continuidad desde el cierre del Sprint 1;
- esta evidencia.

## Conclusión

`J11-S2-00` y sus adendas quedan completadas. En el corte original el Sprint 2 permanecía propuesto y no habilitaba código. El usuario aceptó posteriormente continuar el 2026-07-27; al incorporarse la guía, `J11-S2-03` ya estaba cerrada y `J11-S2-04` era la siguiente historia permitida. La nueva adenda no adelanta implementación: obliga a producir y validar la primera edición durante `J11-S2-08`.
