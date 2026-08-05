# Incorporación de soporte, lanzamientos y conector seguro

- Fecha: 2026-08-04
- Estado: planificación aceptada; implementación futura
- Decisión: [ADR-0036](../adr/0036-operaciones-proveedor-soporte-lanzamientos-conector.md)

## Alcance realizado

Se incorporaron al catálogo futuro:

- `customer_support`, funcional y central para casos, cobertura, SLA y resolución;
- `release_management`, funcional y central para mejoras, correcciones, gates y
  publicación;
- `support_connector`, técnico y opcional en el ERP del cliente.

La decisión conserva la secuencia ERP 1–19 y separa las composiciones del
proveedor y del cliente. El conector sólo inicia HTTPS saliente, usa diagnósticos
allowlist con consentimiento y prohíbe shell, SQL, scripts, listeners
administrativos, autoactualización y control remoto.

La revisión del contrato ejecutable detectó que `PluginKind` sólo contiene
`FUNCTIONAL` y `CUSTOMIZATION`. ADR-0036 y la épica SC-00 dejaron como gate una
evolución compatible a una posible clase `TECHNICAL`; el plan no afirma que exista
ni clasifica al conector como personalización.

Se crearon tres épicas con historias, criterios de aceptación, límites, esquemas
previstos y decisiones pendientes. También se actualizaron ADR-0011, el roadmap,
los índices, el estado de Sprint 8, la guía de implementación y el manual
técnico.

Se revisaron el manual de usuario, la guía de Visual Studio Code y los runbooks
vigentes. No requieren cambios porque los tres plugins están solamente
planificados: no existe todavía pantalla, permiso operativo, instalación, comando
o recorrido que un usuario pueda ejecutar. El PDF de repositorio se regenera en
la recongelación obligatoria de Sprint 8, no en esta decisión intermedia.

## Gate documental G0

El validador mantenido `tmp/validate_docs.py` se ejecutó con el runtime Python
bundled de Codex porque `python` no estaba disponible en `PATH`. La repetición
final, posterior a esta evidencia y su índice, recorrió **280 archivos Markdown**:

- enlaces locales rotos: 0;
- errores UTF-8: 0;
- archivos con mojibake: 0;
- coincidencias con secretos locales: 0.

## Pruebas no aplicables

No se ejecutaron Maven, JUnit, ArchUnit, PostgreSQL, Docker, Compose ni Playwright.
El cambio no crea código, POM, descriptor, migración, endpoint, pantalla, perfil
de composición o artefacto ejecutable. El ADR prohíbe iniciar implementación
durante Sprint 8; el gate aplicable a este corte es exclusivamente G0 documental.

## Resultado

El plan distingue propiedad, despliegue y seguridad de los tres plugins sin
representarlos como implementados. Las decisiones de identidad externa, SLA,
threat model, protocolo, consentimiento, retención y fuente de verdad de releases
siguen siendo gates explícitos antes del código.
