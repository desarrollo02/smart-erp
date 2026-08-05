# J11-S2-00 — Gobierno y planificación del Sprint 2

- Fecha de inicio: 2026-07-27
- Estado: Completada
- Dependencia: Sprint 1 completado

## Objetivo

Convertir los mandatos de activación persistida y personalización obligatoria por empresa en épicas y un backlog ordenado, verificable y seguro antes de modificar código o base de datos.

## Alcance

- relevar decisiones aceptadas y riesgos residuales del Sprint 1;
- delimitar Sprint 2 respecto de identidad, UI renderizada y dominios ERP;
- incorporar el requisito confirmado de un plugin de personalización exclusivo por empresa y aplicado como última capa;
- definir historias, dependencias, criterios y gates;
- identificar las decisiones de arquitectura y datos que deben resolverse antes del código;
- actualizar los índices documentales.

## Fuera de alcance

- aceptar las decisiones todavía abiertas de `J11-S2-01`;
- seleccionar versiones o agregar dependencias;
- crear migraciones, clases Java, endpoints o imágenes nuevas;
- iniciar `J11-S2-01` sin aceptación del backlog.

## Criterios de aceptación

- **CA-01:** las épicas derivan explícitamente de ADR-0002, ADR-0003, los riesgos del cierre anterior y la decisión de producto sobre personalización.
- **CA-02:** el objetivo del Sprint es uno, demostrable y no incluye autenticación, UI renderizada ni dominio ERP.
- **CA-03:** las historias tienen orden lineal, dependencia inmediata y resultado verificable.
- **CA-04:** cada historia define alcance, fuera de alcance, criterios y gates mínimos.
- **CA-05:** las decisiones sobre arquitectura, seguridad y datos se concentran en `J11-S2-01` antes del código.
- **CA-06:** Testcontainers es obligatorio donde se prueben repositorios o SQL PostgreSQL.
- **CA-07:** no se usa un header HTTP arbitrario ni un endpoint sin autorización para simular empresa activa.
- **CA-08:** los índices locales enlazan la épica, el Sprint y su evidencia sin enlaces rotos.
- **CA-09:** G0 confirma UTF-8 estricto, metadatos coherentes y ausencia de caracteres de reemplazo.
- **CA-10:** el resultado identifica `J11-S2-01` como primera historia y exige aceptación explícita antes de iniciarla.
- **CA-11:** el plan exige exactamente una personalización distinta y obligatoria por empresa.
- **CA-12:** modificar pantallas ajenas se limita a contratos públicos versionados y no permite importar internos ni relajar seguridad.
- **CA-13:** una historia propia precede al cierre para implementar y probar la composición de pantalla neutral.
- **CA-14:** duración, riesgos, gates, métricas y secuencia reflejan el alcance agregado.

## Gates

- G0 sobre todos los Markdown del repositorio.
- Auditoría estructural de estado, dependencia, objetivo, alcance, exclusiones, criterios y gates en las nueve historias.
- Revisión cruzada contra ADR-0002, ADR-0003, estrategia de pruebas y cierre del Sprint 1.
- No aplica Maven: esta historia solo modifica documentación y no cambia contratos ni código.

## Resultado final

Los 14 criterios de aceptación quedaron cumplidos. Las adendas posteriores conservaron las nueve historias y ampliaron el plan vigente a 17 criterios globales y 153 criterios de historia: primero se incorporó la personalización exclusiva por empresa y después la guía para implementadores dentro de `J11-S2-08`. La auditoría estructural terminó sin brechas y G0 quedó verde.

La evidencia está en [J11-S2-00 — Planificación del Sprint 2](../../evidence/J11-S2-00-planificacion-sprint-02.md). El backlog fue aceptado posteriormente el 2026-07-27 y habilitó la elaboración documental de `J11-S2-01`; ningún cambio de código queda autorizado antes de aceptar su ADR.
