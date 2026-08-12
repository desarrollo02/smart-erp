# Evidencia documental — ADR-0040 y módulo de migración de legados

- Fecha: 2026-08-11
- Alcance: planificación, sin código ejecutable
- Estado: revisión documental completada; decisiones LM-D01 a LM-D12 pendientes
- Pruebas automatizadas: no ejecutadas; producto autorizó acumularlas hasta la
  candidata comercializable

## Resultado

Se agregó al roadmap el plugin técnico opcional `legacy_migration`, con Oracle
Forms & Reports como primer perfil de origen. La capacidad no recibe número ERP,
no altera el orden funcional 1–19 y eleva el catálogo global futuro de veintinueve
a treinta plugins reutilizables.

La arquitectura separa:

- runner externo efímero para inventario y extracción de solo lectura;
- paquete inmutable con manifiesto, procedencia y checksums;
- plugin Jakarta para proyectos, mapeos, corridas, cuarentena, conciliación y
  corte;
- adaptadores que invocan contratos públicos tipados de cada plugin destino.

Se prohíben escritura al Oracle origen, acceso JPA/SQL a esquemas privados,
redistribución no autorizada de herramientas o drivers Oracle, dual-write por
defecto y transpilación automática de Forms/PLSQL a Jakarta Faces/Java.

## Fuentes y artefactos creados

- [ADR-0040](../adr/0040-modulo-tecnico-migracion-legados-oracle-forms-reports.md)
- [Épica LM-00 a LM-09](../backlog/epica-migracion-legados-oracle-forms-reports.md)
- [Perfil Oracle Forms & Reports](../knowledge-base/legacy-migration/oracle-forms-reports-source-profile.md)
- [Roadmap general](../backlog/epica-roadmap-plugins-productivos.md)

La documentación oficial de Oracle consultada fue Forms2XML, Forms Migration
Assistant y `rwconverter`/Reports XML. Las URL y la fecha de consulta quedaron
registradas en el perfil y el ADR.

## Validación realizada

La aceptación de este cambio es exclusivamente estática:

1. enlaces locales de los documentos nuevos y modificados;
2. consistencia del identificador `legacy_migration` y la secuencia LM-00–LM-09;
3. ausencia de caracteres UTF-8 dañados;
4. limpieza de espacios finales y conflictos de parche mediante `git diff --check`;
5. confirmación de que no se agregó código, dependencia, driver, migración SQL,
   descriptor ni composición ejecutable.

Resultado: `git diff --check` sin hallazgos; once documentos de alcance revisados
sin enlaces locales rotos; diez historias consecutivas LM-00–LM-09; una sola
entrada ADR-0040 en el índice; sin mojibake ni espacios finales en el conjunto
revisado. Ningún gate de Maven, PostgreSQL, Docker/Compose, seguridad o Playwright
se considera ejecutado por esta evidencia.
