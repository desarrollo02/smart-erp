# Evidencia — Demo visual obligatoria al cerrar cada Sprint

- Fecha: 2026-07-28
- Tipo: decisión de metodología de producto
- Estado: incorporada

## Decisión

El responsable de producto estableció que todo Sprint debe finalizar con una demo
visual. La regla se incorporó como gate obligatorio y acumulativo: no reemplaza
pruebas, seguridad, evidencia, retrospectiva, guía ni PDF.

## Forma de cumplimiento

- demo navegable sobre el baseline real que se pretende cerrar;
- guion reproducible versionado en `docs/runbooks/`;
- Jakarta Faces y Material Design 3 cuando se agregue una vista;
- evidencia en compacto 375 px, medio 720 px y expandido 1280 px;
- datos ficticios, autorización y estados relevantes;
- capacidades y límites declarados sin mocks engañosos;
- preparación y restauración del ambiente documentadas.

Para incrementos técnicos se requiere una visualización segura del resultado dentro
del producto. No se obliga a inventar un dominio funcional: puede mostrarse estado,
diagnóstico, configuración no sensible o administración, siempre mediante contratos
reales y sin exponer secretos.

## Archivos actualizados

- `AGENTS.md` y Definition of Done;
- flujo documental e índice de Sprints;
- estrategia de pruebas versión 14;
- criterio global de Sprint 4;
- guía de implementación `1.0-rc15` y su ficha de validación.

## Verificación

El cambio es documental. Se revisaron enlaces locales, versión de guía, regla de
Definition of Done y coherencia con el PDF obligatorio. No se ejecutaron pruebas de
código ni runtime.
