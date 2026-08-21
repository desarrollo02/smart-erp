# Documentación de Smart ERP

Esta carpeta es la fuente única de documentación del proyecto. Toda decisión, análisis, procedimiento, historia, prueba y evidencia debe quedar registrada aquí en el mismo cambio que la produce.

`AGENTS.md` permanece en la raíz porque es un archivo operativo que los agentes deben descubrir automáticamente; no sustituye la documentación mantenida en `docs/`.

## Estructura

| Carpeta | Contenido |
|---|---|
| `adr/` | Decisiones arquitectónicas y sus consecuencias. |
| `architecture/` | Diseño vigente, diagramas, contratos y límites de módulos. |
| `knowledge-base/` | Conocimiento extraído del legado, sin modificarlo ni copiarlo mecánicamente. |
| `backlog/` | Épicas, historias, criterios de aceptación y priorización. |
| `sprints/` | Objetivos, pasos, resultados, retrospectivas y estado de cada Sprint. |
| `evidence/` | Evidencias reproducibles de compilaciones, pruebas y validaciones. |
| `runbooks/` | Procedimientos operativos, recuperación y diagnóstico. |
| `implementation-guide/` | Guía didáctica y versionada para implementar el ERP en una empresa. |
| `user-guide/` | Manual de usuario orientado a tareas, permisos y recuperación. |
| `developer-guide/` | Manual técnico para desarrollar y revisar módulos y plugins. |
| `output/pdf/` | Documentos PDF generados y verificados para consulta y distribución. |

## Regla de documentación obligatoria

Cada paso de trabajo debe quedar asociado a una historia o tarea y documentar, como mínimo:

1. Identificador, fecha y estado.
2. Objetivo y criterios de aceptación.
3. Estado inicial y supuestos.
4. Acciones realizadas en orden.
5. Archivos creados o modificados.
6. Decisiones y motivos.
7. Comandos o pruebas ejecutadas y sus resultados.
8. Errores encontrados y cómo se resolvieron.
9. Riesgos, pendientes y siguiente paso permitido.

No basta con indicar que una prueba pasó: la evidencia debe permitir identificar qué se probó, con qué comando o procedimiento y cuál fue el resultado.

## Flujo documental

1. Crear o actualizar el documento de la historia antes de comenzar.
2. Registrar decisiones relevantes durante el trabajo; crear un ADR si afectan arquitectura, seguridad, datos, compatibilidad o despliegue.
3. Después de cada cambio coherente, registrar y ejecutar su prueba correspondiente.
4. Si una prueba falla, registrar el fallo y su corrección antes de continuar.
5. Cerrar la historia únicamente cuando los criterios y la Definition of Done estén verificados.
6. Al cerrar un Sprint, ejecutar una demo visual navegable del baseline real, con guion reproducible, evidencia en 375/720/1280 px y limitaciones explícitas.
7. Crear `sprints/sprint-XX/estructura-plugins-y-dependencias.md` desde POM,
   descriptores y migraciones reales, con gráfico y alternativa textual.
8. Revisar la guía de Visual Studio Code, el manual de usuario, el manual técnico y
   la guía de implementación; actualizar lo afectado o registrar por qué no cambia.
9. Regenerar y verificar visualmente `output/pdf/guia-estructura-repositorio-logixone.pdf`; registrar páginas, tamaño, SHA-256 y resultado de revisión en la evidencia del cierre.

Para la candidata visual de Sprint 3 existió una excepción temporal aprobada el
2026-07-28: las pruebas automatizadas de `J11-S3-01` a `J11-S3-07` podían ejecutarse
acumuladas en `J11-S3-08`. Las historias conservaron `Implementada pendiente de
validación` hasta que G2–G6 quedaron verdes y entonces cambiaron a `Completada`. La
excepción no autoriza cerrar el Sprint ni promover la candidata: G7 independiente y
el PDF continúan pendientes.

## Convenciones

- Documentos en Markdown y codificación UTF-8.
- Fechas en formato `AAAA-MM-DD`.
- Historias con nombre `J11-S<numero>-<numero>-descripcion.md`.
- ADR con nombre `NNNN-descripcion.md` y estado explícito: Propuesto, Aceptado, Reemplazado o Rechazado.
- Rutas y comandos deben ser reproducibles; no incluir contraseñas, tokens ni secretos.
- Los archivos de evidencia grandes o generados no se versionan automáticamente. Documentar su ubicación, checksum o mecanismo de reproducción.
- El PDF de estructura del repositorio es la excepción obligatoria de cada cierre de Sprint: se actualiza en su ruta estable, pero no sustituye las fuentes Markdown ni el código.
- Cada Sprint requiere una demo visual real y documentada; no puede reemplazarse por diapositivas, mocks, capturas aisladas ni una explicación oral.
- Cada Sprint conserva una fotografía gráfica y textual de plugins y dependencias;
  el roadmap planificado debe distinguirse del baseline implementado.
- Los manuales se separan por audiencia y se actualizan junto al cambio que altera
  su procedimiento o contrato.

## Índice inicial

- [Gobierno documental del Sprint 1](sprints/sprint-01/J11-S1-00-gobierno-documental.md)
- [Sprint 1](sprints/sprint-01/README.md)
- [J11-S1-01 — Baseline y decisiones arquitectónicas](sprints/sprint-01/J11-S1-01-baseline-arquitectonico.md)
- [J11-S1-02 — Esqueleto Maven reproducible](sprints/sprint-01/J11-S1-02-esqueleto-maven.md)
- [Índice de ADR](adr/README.md)
- [ADR-0005 — Contexto empresarial, activación y personalización obligatoria](adr/0005-contexto-empresarial-activacion-personalizacion.md)
- [Vista general de arquitectura](architecture/overview.md)
- [Inventario de selectores y datos administrables](architecture/inventario-selectores-y-datos-administrables.md)
- [ADR-0032 — Plugin para estaciones de servicio de combustible](adr/0032-plugin-estaciones-servicio-combustible.md)
- [ADR-0033 — Dominio independiente de facturación recurrente](adr/0033-dominio-facturacion-recurrente.md)
- [Épica — Planes, prorrateo y consumo medido](backlog/epica-facturacion-recurrente.md)
- [Épica — Ventas: presupuestos, pedidos y compromisos](backlog/epica-ventas.md)
- [Caracterización de Ventas](knowledge-base/sales/legacy-characterization.md)
- [Sprint 11 — Ventas](sprints/sprint-11/README.md)
- [Épica — Estaciones de servicio de combustible](backlog/epica-estaciones-servicio-combustible.md)
- [Caracterización de estaciones de servicio](knowledge-base/fuel-station/legacy-characterization.md)
- [Estrategia y matriz de pruebas](architecture/test-strategy.md)
- [Guía de implementación del ERP por empresa](implementation-guide/README.md)
- [Manual de usuario](user-guide/README.md)
- [Manual integrado del instalador Windows y puesta en marcha](user-guide/operations/instalador-windows-puesta-en-marcha.md)
- [Manual técnico para desarrolladores](developer-guide/README.md)
- [J11-S8-C04 — Gobierno Git y ramas por Sprint](sprints/sprint-08/J11-S8-C04-gobierno-git-ramas.md)
- [Guía para levantar Smart ERP con Visual Studio Code](runbooks/levantar-logixone-visual-studio-code.md)
- [Estructura de plugins y dependencias de Sprint 7](sprints/sprint-07/estructura-plugins-y-dependencias.md)
- [Estructura de plugins y dependencias de Sprint 8](sprints/sprint-08/estructura-plugins-y-dependencias.md)
- [Construcción local con Maven Wrapper](runbooks/build-local.md)
- [Relocalización de descargas dentro del proyecto](sprints/sprint-01/J11-S1-02-descargas-proyecto.md)
- [J11-S1-03 — Docker e infraestructura como código](sprints/sprint-01/J11-S1-03-docker-iac.md)
- [Diagnóstico inicial de Docker](evidence/J11-S1-03-diagnostico-docker.md)
- [Construcción de la imagen Docker](runbooks/docker-build.md)
- [Validación y operación de Compose](runbooks/compose.md)
- [Validación estática de Compose](evidence/J11-S1-03-compose-estatico.md)
- [Construcción y operación del migrador](runbooks/migrator.md)
- [Migrador one-shot y fallo seguro](evidence/J11-S1-03-migrator-one-shot.md)
- [Explicación en PDF de migrator.jar y la estructura del repositorio](output/pdf/explicacion-migrator-y-estructura-repositorio.pdf)
- [Guía PDF verificada del baseline J11-S8-C07](output/pdf/guia-estructura-repositorio-logixone.pdf)
- [Evidencia de generación y revisión del PDF](evidence/J11-S1-03-guia-pdf-migrator-repositorio.md)
- [Persistencia, health y smoke del sexto incremento](evidence/J11-S1-03-persistencia-smoke.md)
- [Backup y restauración controlada de PostgreSQL](runbooks/postgresql-backup-restore.md)
- [Cierre formal de J11-S1-03](evidence/J11-S1-03-cierre.md)
- [J11-S1-04 — Contratos de plugins y validaciones](sprints/sprint-01/J11-S1-04-contratos-plugins.md)
- [J11-S1-05 — Kernel, descubrimiento CDI y plugin de referencia](sprints/sprint-01/J11-S1-05-kernel-cdi-plugin-referencia.md)
- [Evidencia de J11-S1-05 — Kernel, CDI y plugin de referencia](evidence/J11-S1-05-kernel-cdi-plugin-referencia.md)
- [J11-S1-06 — Aplicación mínima y endpoints semánticos de salud](sprints/sprint-01/J11-S1-06-aplicacion-minima-health.md)
- [Evidencia de J11-S1-06 — Aplicación mínima y health semántico](evidence/J11-S1-06-aplicacion-minima-health.md)
- [J11-S1-07 — Validación integral y cierre del Sprint 1](sprints/sprint-01/J11-S1-07-validacion-integral-cierre.md)
- [Evidencia de J11-S1-07 — Validación integral y cierre del Sprint 1](evidence/J11-S1-07-validacion-integral-cierre.md)
- [Épica — Kernel multiempresa y activación de plugins](backlog/epica-kernel-multiempresa-activacion-plugins.md)
- [Épica — Personalización obligatoria por empresa](backlog/epica-personalizacion-pantallas-por-empresa.md)
- [Sprint 2 — Kernel multiempresa, activación y personalización](sprints/sprint-02/README.md)
- [Evidencia de J11-S2-00 — Planificación del Sprint 2](evidence/J11-S2-00-planificacion-sprint-02.md)
- [Evidencia de J11-S2-01 — Elaboración de ADR-0005](evidence/J11-S2-01-adr-contexto-activacion-personalizacion.md)
- [J11-S2-02 — Contratos y modelo neutral multiempresa](sprints/sprint-02/J11-S2-02-modelo-neutral-multiempresa.md)
- [Evidencia de J11-S2-02 — Modelo neutral multiempresa](evidence/J11-S2-02-modelo-neutral-multiempresa.md)
- [J11-S2-03 — Migración `core` V2 y evolución segura](sprints/sprint-02/J11-S2-03-migracion-core-v2.md)
- [Evidencia de J11-S2-03 — Migración `core` V2](evidence/J11-S2-03-migracion-core-v2.md)
- [J11-S2-04 — Persistencia JPA/JTA](sprints/sprint-02/J11-S2-04-persistencia-jpa-jta.md)
- [Evidencia de J11-S2-04 — Persistencia JPA/JTA](evidence/J11-S2-04-persistencia-jpa-jta.md)
- [J11-S2-05 — Casos de uso y guardas de activación](sprints/sprint-02/J11-S2-05-casos-uso-guardas-activacion.md)
- [Evidencia de J11-S2-05 — Casos de uso y guardas de activación](evidence/J11-S2-05-casos-uso-guardas-activacion.md)
- [J11-S2-06 — Filtrado de contribuciones por empresa](sprints/sprint-02/J11-S2-06-filtrado-contribuciones-empresa.md)
- [Evidencia de J11-S2-06 — Filtrado de contribuciones por empresa](evidence/J11-S2-06-filtrado-contribuciones-empresa.md)
- [J11-S2-07 — Contrato y composición de personalizaciones de pantalla](sprints/sprint-02/J11-S2-07-contrato-personalizacion-pantallas.md)
- [Evidencia de J11-S2-07 — Contrato y composición de personalizaciones de pantalla](evidence/J11-S2-07-contrato-personalizacion-pantallas.md)
- [J11-S2-08 — Validación integral y cierre del Sprint 2](sprints/sprint-02/J11-S2-08-validacion-integral-cierre.md)
- [Evidencia provisional de J11-S2-08 — Validación integral y cierre](evidence/J11-S2-08-validacion-integral-cierre.md)
- [Ficha de validación independiente de la guía candidata](implementation-guide/VALIDATION.md)
- [ADR-0006 — Identidad OIDC, membresía empresarial y autorización](adr/0006-identidad-oidc-membresia-autorizacion.md)
- [ADR-0007 — Material Design 3 y pantallas responsive sobre Jakarta Faces](adr/0007-material-design-responsive-sobre-jsf.md)
- [ADR-0008 — Logout OIDC y estabilidad preview de WildFly](adr/0008-logout-oidc-estabilidad-preview-wildfly.md)
- [Épica — Identidad, autorización y primera demo visual](backlog/epica-identidad-autorizacion-demo-visual.md)
- [Sprint 3 — Identidad segura y primera demo visual](sprints/sprint-03/README.md)
- [J11-S3-00 — Gobierno, ADR y planificación](sprints/sprint-03/J11-S3-00-gobierno-adr-planificacion.md)
- [Evidencia de J11-S3-00 — Planificación del Sprint 3](evidence/J11-S3-00-planificacion-sprint-03.md)
- [J11-S3-01 — Modelo neutral de identidad y autorización](sprints/sprint-03/J11-S3-01-modelo-identidad-autorizacion.md)
- [Evidencia de J11-S3-01 — Modelo neutral de identidad y autorización](evidence/J11-S3-01-modelo-identidad-autorizacion.md)
- [J11-S3-02 — Migración `core` V3 de seguridad](sprints/sprint-03/J11-S3-02-migracion-core-v3-seguridad.md)
- [Evidencia de J11-S3-02 — Migración `core` V3 de seguridad](evidence/J11-S3-02-migracion-core-v3-seguridad.md)
- [J11-S3-03 — Persistencia y casos de uso de seguridad](sprints/sprint-03/J11-S3-03-persistencia-casos-uso-seguridad.md)
- [Evidencia de J11-S3-03 — Persistencia y casos de uso de seguridad](evidence/J11-S3-03-persistencia-casos-uso-seguridad.md)
- [J11-S3-04 — Keycloak y WildFly OIDC reproducibles](sprints/sprint-03/J11-S3-04-keycloak-wildfly-oidc.md)
- [Evidencia de J11-S3-04 — Keycloak y WildFly OIDC](evidence/J11-S3-04-keycloak-wildfly-oidc.md)
- [J11-S3-05 — Contexto confiable, autorización y auditoría](sprints/sprint-03/J11-S3-05-contexto-confiable-autorizacion.md)
- [Evidencia de J11-S3-05 — Contexto confiable, autorización y auditoría](evidence/J11-S3-05-contexto-confiable-autorizacion.md)
- [Runbook de contexto empresarial confiable y autorización](runbooks/trusted-context-authorization.md)
- [J11-S3-06 — Shell UI y navegación por empresa](sprints/sprint-03/J11-S3-06-shell-ui-navegacion.md)
- [Evidencia de J11-S3-06 — Shell UI y navegación por empresa](evidence/J11-S3-06-shell-ui-navegacion.md)
- [J11-S3-07 — Renderer JSF y personalización visual A/B](sprints/sprint-03/J11-S3-07-render-pantalla-personalizada.md)
- [Evidencia de J11-S3-07 — Renderer JSF y personalización visual A/B](evidence/J11-S3-07-render-pantalla-personalizada.md)
- [Evidencia técnica acumulada de J11-S3-08 — Demo y gates G2–G6](evidence/J11-S3-08-validacion-demo-cierre.md)
- [Runbook del shell Jakarta Faces](runbooks/shell-ui.md)
- [Runbook — Keycloak y OIDC para desarrollo/demo](runbooks/keycloak-oidc.md)
- [Manual para presentar la demo visual de Sprint 3](runbooks/manual-demo-visual-sprint-03.md)
- [PDF verificado del manual de demo visual](output/pdf/manual-demo-visual-sprint-03.pdf)
- [Evidencia de generación y revisión del manual PDF](evidence/manual-demo-visual-sprint-03-pdf.md)
- [ADR-0009 — Autoridad administrativa global y panel operativo](adr/0009-autoridad-administrativa-global-kernel.md)
- [Épica — Administración operativa segura del kernel](backlog/epica-administracion-operativa-kernel.md)
- [Sprint 4 — Administración operativa del kernel](sprints/sprint-04/README.md)
- [Evidencia de planificación de Sprint 4](evidence/J11-S4-00-planificacion-administracion-kernel.md)
- [J11-S4-01 — Modelo neutral de autoridad global](sprints/sprint-04/J11-S4-01-modelo-autoridad-global.md)
- [Evidencia de J11-S4-01](evidence/J11-S4-01-modelo-autoridad-global.md)
- [J11-S4-02 — Migración `core` V4 y bootstrap global](sprints/sprint-04/J11-S4-02-migracion-core-v4-bootstrap-global.md)
- [Evidencia de J11-S4-02](evidence/J11-S4-02-migracion-core-v4-bootstrap-global.md)
- [J11-S4-03 — Persistencia JPA/JTA y casos de uso globales](sprints/sprint-04/J11-S4-03-persistencia-casos-uso-autoridad-global.md)
- [Evidencia de J11-S4-03](evidence/J11-S4-03-persistencia-casos-uso-autoridad-global.md)
- [J11-S4-04 — Frontera web administrativa confiable](sprints/sprint-04/J11-S4-04-frontera-web-administrativa-confiable.md)
- [Evidencia de J11-S4-04](evidence/J11-S4-04-frontera-web-administrativa-confiable.md)
- [J11-S4-08 — Validación acumulada, demo y cierre](sprints/sprint-04/J11-S4-08-validacion-demo-cierre.md)
- [Evidencia de J11-S4-08](evidence/J11-S4-08-validacion-demo-cierre.md)
- [Manual paso a paso de pruebas integrales J11-S4-08](runbooks/manual-pruebas-j11-s4-08.md)
- [PDF verificado del manual de pruebas J11-S4-08](output/pdf/manual-pruebas-j11-s4-08.pdf)
- [Evidencia de generación y revisión del manual PDF de pruebas](evidence/manual-pruebas-j11-s4-08-pdf.md)
- [Metodología — Demo visual obligatoria al cerrar cada Sprint](evidence/metodologia-demo-visual-cierre-sprint.md)
- [Épica — Instalador Windows reproducible por Sprint](backlog/epica-instalador-windows-reproducible.md)
- [Metodología — Decisión e instalador Windows al cerrar cada Sprint](runbooks/metodologia-instalador-windows-cierre-sprint.md)
- [ADR-0028 — Gobierno de selectores y datos administrables](adr/0028-gobierno-de-selectores-y-datos-administrables.md)
- [ADR-0029 — Confirmación del instalador en cada cierre](adr/0029-confirmacion-instalador-por-cierre-sprint.md)
- [Evidencia — Decisión de instalador Windows por Sprint](evidence/metodologia-instalador-windows-cierre-sprint.md)
- [ADR-0010 — Modelo canónico de documentos y SIFEN como referencia estructural](adr/0010-modelo-canonico-documentos-referencia-sifen.md)
- [ADR-0031 — Facturación masiva dentro de documentos comerciales](adr/0031-facturacion-masiva-en-documentos-comerciales.md)
- [Base de conocimiento — SIFEN v150 y estructura de documentos](knowledge-base/sifen-v150-estructura-documentos.md)
- [Base de conocimiento — Facturación masiva en los legados](knowledge-base/commercial-documents/facturacion-masiva-legacy-characterization.md)
- [Épica — Documentos comerciales canónicos e integración SIFEN](backlog/epica-documentos-comerciales-y-sifen.md)
- [Épica — Facturación masiva, recuperable e idempotente](backlog/epica-facturacion-masiva.md)
- [Evidencia — Análisis SIFEN v150 para persistencia](evidence/analisis-sifen-v150-persistencia-documentos.md)
- [Evidencia — Análisis de facturación masiva](evidence/analisis-facturacion-masiva.md)
- [ADR-0011 — Roadmap y dependencias de plugins productivos](adr/0011-roadmap-dependencias-plugins-productivos.md)
- [Épica — Roadmap inicial de plugins productivos](backlog/epica-roadmap-plugins-productivos.md)
- [ADR-0034 — Plugin de telemetría vehicular y seguimiento GPS](adr/0034-plugin-telemetria-vehicular.md)
- [Base de conocimiento — Telemetría vehicular en el legado](knowledge-base/vehicle-telemetry/legacy-characterization.md)
- [Épica — Telemetría vehicular y seguimiento GPS](backlog/epica-telemetria-vehicular.md)
- [Evidencia — Incorporación de telemetría vehicular](evidence/analisis-telemetria-vehicular.md)
- [ADR-0012 — Composición física única y migraciones de plugins](adr/0012-composicion-unica-y-migraciones-de-plugins.md)
- [Sprint 5 — Fundaciones ejecutables para plugins productivos](sprints/sprint-05/README.md)
- [J11-S5-00 — Gobierno y planificación](sprints/sprint-05/J11-S5-00-gobierno-planificacion.md)
- [Evidencia de J11-S5-00](evidence/J11-S5-00-planificacion-fundaciones-plugins.md)
- [J11-S5-01 — Composición única y migraciones de plugins](sprints/sprint-05/J11-S5-01-migraciones-plugins-composicion.md)
- [Evidencia de J11-S5-01](evidence/J11-S5-01-migraciones-plugins-composicion.md)
- [J11-S5-02 — Plantilla mínima de plugin productivo](sprints/sprint-05/J11-S5-02-plantilla-plugin-productivo.md)
- [Runbook — Generar un plugin neutral](runbooks/plugin-scaffold.md)
- [Evidencia de J11-S5-02 — Plantilla mínima de plugin productivo](evidence/J11-S5-02-plantilla-plugin-productivo.md)
- [J11-S5-03 — Contrato de eventos de integración y outbox](sprints/sprint-05/J11-S5-03-eventos-integracion-outbox.md)
- [ADR-0013 — Eventos de integración y outbox por plugin](adr/0013-eventos-integracion-outbox-por-plugin.md)
- [Contrato operativo de eventos y outbox](architecture/integration-events-outbox.md)
- [Evidencia de J11-S5-03](evidence/J11-S5-03-eventos-integracion-outbox.md)
- [J11-S5-04 — Validación, demo visual y corte técnico](sprints/sprint-05/J11-S5-04-validacion-demo-cierre.md)
- [Evidencia de J11-S5-04](evidence/J11-S5-04-validacion-demo-cierre.md)
- [Sprint 6 — Primer plugin productivo `business_partners`](sprints/sprint-06/README.md)
- [J11-S6-00 — Gobierno y planificación de `business_partners`](sprints/sprint-06/J11-S6-00-gobierno-planificacion.md)
- [Evidencia de J11-S6-00](evidence/J11-S6-00-gobierno-planificacion.md)
- [J11-S6-01 — Caracterización de `business_partners`](sprints/sprint-06/J11-S6-01-caracterizacion-business-partners.md)
- [Base de conocimiento — personas, clientes y proveedores del legado](knowledge-base/business-partners/legacy-characterization.md)
- [Evidencia de J11-S6-01](evidence/J11-S6-01-caracterizacion-business-partners.md)
- [ADR-0014 — Modelo de participante comercial y contrato público](adr/0014-modelo-participante-comercial-y-contrato-publico.md)
- [J11-S6-02 — Dominio y contratos de `business_partners`](sprints/sprint-06/J11-S6-02-dominio-contratos-business-partners.md)
- [Evidencia de J11-S6-02](evidence/J11-S6-02-dominio-contratos-business-partners.md)
- [Sprint 7 — Catálogo comercial `commercial_catalog`](sprints/sprint-07/README.md)
- [J11-S7-01 — Caracterización de `commercial_catalog`](sprints/sprint-07/J11-S7-01-caracterizacion-commercial-catalog.md)
- [ADR-0019 — Modelo de catálogo comercial y contratos públicos](adr/0019-modelo-catalogo-comercial-y-contratos-publicos.md)
- [J11-S7-02 — Dominio y contratos de `commercial_catalog`](sprints/sprint-07/J11-S7-02-dominio-contratos-commercial-catalog.md)
- [ADR-0020 — Persistencia privada de `commercial_catalog`](adr/0020-persistencia-privada-commercial-catalog.md)
- [J11-S7-03 — Persistencia de `commercial_catalog`](sprints/sprint-07/J11-S7-03-persistencia-commercial-catalog.md)
- [Evidencia de J11-S7-03](evidence/J11-S7-03-persistencia-commercial-catalog.md)
- [Sprint 8 — Inventario `inventory`](sprints/sprint-08/README.md)
- [J11-S8-07 — Validación integral y demo oficial](sprints/sprint-08/J11-S8-07-validacion-demo-cierre.md)
- [Evidencia de J11-S8-07](evidence/J11-S8-07-validacion-demo-cierre.md)
- [Demo oficial de cierre técnico de Sprint 8](runbooks/demo-cierre-sprint-08.md)
- [J11-S8-08 — Instalador Windows interno y cierre formal](sprints/sprint-08/J11-S8-08-instalador-windows-cierre.md)
- [Evidencia del instalador Windows interno](evidence/J11-S8-08-instalador-windows-cierre.md)
- [Demo visual del instalador Windows interno](runbooks/demo-instalador-windows-sprint-08.md)
- [Sprint 9 — Compras `purchasing`](sprints/sprint-09/README.md)
- [Épica — Compras](backlog/epica-compras.md)
- [J11-S9-01 — Caracterización de `purchasing`](sprints/sprint-09/J11-S9-01-caracterizacion-purchasing.md)
- [Base de conocimiento — solicitudes, órdenes, recepciones y devoluciones](knowledge-base/purchasing/legacy-characterization.md)
- [Evidencia de J11-S9-01](evidence/J11-S9-01-caracterizacion-purchasing.md)
- [ADR-0041 — Modelo de `purchasing` y contratos públicos](adr/0041-modelo-purchasing-y-contratos-publicos.md)
- [J11-S9-02 — Dominio y contratos de `purchasing`](sprints/sprint-09/J11-S9-02-dominio-contratos-purchasing.md)
- [Evidencia de J11-S9-02](evidence/J11-S9-02-dominio-contratos-purchasing.md)
- [ADR-0042 — Persistencia privada de `purchasing`](adr/0042-persistencia-privada-purchasing.md)
- [J11-S9-03 — Persistencia de `purchasing`](sprints/sprint-09/J11-S9-03-persistencia-purchasing.md)
- [Evidencia de J11-S9-03](evidence/J11-S9-03-persistencia-purchasing.md)
- [ADR-0043 — Aplicación JTA e idempotencia de `purchasing`](adr/0043-aplicacion-jta-idempotencia-purchasing.md)
- [J11-S9-04 — Aplicación de `purchasing`](sprints/sprint-09/J11-S9-04-aplicacion-purchasing.md)
- [Evidencia de J11-S9-04](evidence/J11-S9-04-aplicacion-purchasing.md)
- [ADR-0044 — Recorridos visuales de `purchasing`](adr/0044-recorridos-visuales-purchasing.md)
- [J11-S9-05 — Interfaz de `purchasing`](sprints/sprint-09/J11-S9-05-interfaz-purchasing.md)
- [Evidencia de J11-S9-05](evidence/J11-S9-05-interfaz-purchasing.md)
- [J11-S9-06 — Integración y demo candidata de `purchasing`](sprints/sprint-09/J11-S9-06-integracion-composicion-purchasing.md)
- [Evidencia de J11-S9-06](evidence/J11-S9-06-integracion-composicion-purchasing.md)
- [J11-S9-07 — Validación integral y demo oficial](sprints/sprint-09/J11-S9-07-validacion-demo-cierre.md)
- [Evidencia de J11-S9-07](evidence/J11-S9-07-validacion-demo-cierre.md)
- [J11-S9-08 — Instalador Windows interno](sprints/sprint-09/J11-S9-08-instalador-windows-cierre.md)
- [Evidencia del instalador Windows de Sprint 9](evidence/J11-S9-08-instalador-windows-cierre.md)
- [Demo segura del instalador Windows de Sprint 9](runbooks/demo-instalador-windows-sprint-09.md)
- [Fotografía de plugins de Sprint 9](sprints/sprint-09/estructura-plugins-y-dependencias.md)
- [Demo oficial de cierre técnico de Sprint 9](runbooks/demo-cierre-sprint-09.md)
- [Manual 07 — Compras](user-guide/modules/compras.md)
- [ADR-0040 — Migración de legados Oracle Forms & Reports](adr/0040-modulo-tecnico-migracion-legados-oracle-forms-reports.md)
- [Épica — Migración de legados](backlog/epica-migracion-legados-oracle-forms-reports.md)
- [Evidencia documental — Plan del módulo de migración](evidence/ADR-0040-plan-migracion-legados.md)
- [ADR-0045 — Plugin de gestión de procesos de negocio BPM](adr/0045-plugin-gestion-procesos-negocio-bpm.md)
- [Épica — Gestión de procesos de negocio BPM](backlog/epica-gestion-procesos-negocio-bpm.md)
- [Evidencia documental — Plan del plugin BPM](evidence/ADR-0045-plan-bpm.md)
- [ADR-0046 — Familia de mantenimiento de flota y taller automotriz](adr/0046-familia-mantenimiento-flota-taller-automotriz.md)
- [Caracterización — Taller y mantenimiento vehicular](knowledge-base/vehicle-maintenance/legacy-characterization.md)
- [Épica F1 — Mantenimiento de flota](backlog/epica-mantenimiento-flota.md)
- [Épica F2 — Taller automotriz comercial](backlog/epica-taller-automotriz-comercial.md)
- [Evidencia documental — Plan de la familia Flota](evidence/ADR-0046-plan-flota-taller.md)
- [ADR-0048 — Plugin de gestión inmobiliaria](adr/0048-plugin-gestion-inmobiliaria.md)
- [Épica — Gestión inmobiliaria](backlog/epica-gestion-inmobiliaria.md)
- [Evidencia documental — Plan de gestión inmobiliaria](evidence/ADR-0048-plan-gestion-inmobiliaria.md)
- [PDF 00 — Plugins y orden de construcción](output/pdf/00-roadmap-plugins-y-orden-construccion.pdf)
- [Fuente web — Plugins y orden de construcción](user-guide/roadmap-plugins-y-orden-construccion.html)
- [Evidencia — PDF de plugins y orden](evidence/pdf-roadmap-plugins-orden-construccion.md)
- [Perfil de origen — Oracle Forms & Reports](knowledge-base/legacy-migration/oracle-forms-reports-source-profile.md)
