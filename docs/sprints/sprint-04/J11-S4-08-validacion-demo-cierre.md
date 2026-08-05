# J11-S4-08 — Validación acumulada, demo y cierre del Sprint 4

- Estado: Gates técnicos G0–G6 y PDF verdes; G7 pendiente únicamente de validación independiente
- Sprint: 4
- Fecha de inicio: 2026-07-28
- Dependencias: `J11-S4-01` a `J11-S4-07` completadas y validadas en este gate
- ADR rectores: [ADR-0009](../../adr/0009-autoridad-administrativa-global-kernel.md) y [ADR-0011](../../adr/0011-roadmap-dependencias-plugins-productivos.md)

## Objetivo

Ejecutar la validación acumulada de la administración operativa del kernel,
corregir cada hallazgo real, demostrar visualmente el recorrido administrativo y
producir la evidencia y los documentos necesarios para cerrar Sprint 4. El cierre
es el último gate antes de preparar el primer plugin productivo.

## Alcance

- pruebas unitarias diferidas de `J11-S4-01` a `J11-S4-07`;
- ArchUnit y composición física del WAR;
- migraciones PostgreSQL `core` V1→V5, checksums, reejecución y protección
  append-only;
- repositorios JPA, transacciones JTA, concurrencia, idempotencia y rollback;
- bootstrap one-shot y protección del último administrador global;
- OIDC y autorización administrativa positiva y negativa;
- cabeceras defensivas, privacidad, logs y ausencia de secretos;
- Docker/Compose, health, migraciones y persistencia de volúmenes;
- Playwright a 375, 720 y 1280 px, accesibilidad básica y regresión A/B;
- demo visual administrativa reproducible;
- recorrido independiente de la guía de implementación;
- actualización de documentación, retrospectiva y siguiente incremento;
- regeneración y revisión del PDF obligatorio de estructura del repositorio.

## Fuera de alcance

- omitir, desactivar o relajar pruebas para conseguir un cierre verde;
- aceptar como verde una compilación realizada con pruebas omitidas;
- iniciar un plugin productivo antes de completar esta historia;
- elevar la guía a `1.0` sin validación independiente satisfactoria;
- promover imágenes o desplegar a producción;
- presentar el panel como un ERP productivo terminado.

## Criterios de aceptación

- **CA-01:** documentación, ADR, estados, enlaces y trazabilidad son consistentes.
- **CA-02:** las pruebas unitarias acumuladas terminan sin fallos ni omisiones.
- **CA-03:** ArchUnit conserva los límites del kernel, web y plugins.
- **CA-04:** el build completo y el WAR verificable terminan verdes.
- **CA-05:** PostgreSQL valida V1→V5, instalación vacía, reejecución, checksums,
  restricciones e inmutabilidad de migraciones aplicadas.
- **CA-06:** JPA `validate`, JTA, concurrencia, idempotencia y rollback terminan
  verdes, incluida la atomicidad entre mutaciones y auditoría.
- **CA-07:** bootstrap global es one-shot y no deja una ruta anónima permanente.
- **CA-08:** el último administrador global efectivo no puede ser revocado.
- **CA-09:** OIDC y la autorización administrativa aceptan únicamente identidades y
  permisos válidos; manipulación de IDs, filtros o rutas falla cerrada.
- **CA-10:** auditoría registra decisiones permitidas y denegadas sin secretos,
  identidad externa ni datos comerciales.
- **CA-11:** `/admin/*` conserva las cabeceras defensivas en respuestas permitidas y
  denegadas.
- **CA-12:** liveness/readiness mantienen semántica, diagnóstico seguro y exigencia
  de la versión `core` V5.
- **CA-13:** Docker/Compose, migraciones y recreación sin borrar volúmenes conservan
  el estado esperado.
- **CA-14:** Playwright demuestra el recorrido administrativo en 375, 720 y 1280 px
  y no rompe la demo empresarial A/B.
- **CA-15:** la demo visual usa el baseline real y un guion reproducible.
- **CA-16:** un implementador independiente completa `VALIDATION.md` y los
  hallazgos se resuelven y revalidan.
- **CA-17:** la guía solo cambia a `1.0` si el dictamen independiente es
  satisfactorio.
- **CA-18:** la evidencia registra ambiente, comandos, resultados, fallos y
  correcciones sin incluir secretos.
- **CA-19:** el PDF obligatorio se regenera contra el baseline final, se renderiza
  por completo y registra páginas, bytes, SHA-256 y revisión visual.
- **CA-20:** `J11-S4-01` a `J11-S4-07` cambian a `Completada` únicamente después de
  que sus pruebas correspondientes estén verdes.
- **CA-21:** retrospectiva y próximo incremento autorizado quedan documentados.

## Gates y regla de parada

| Gate | Alcance | Estado inicial |
|---|---|---|
| G0 | documentación, UTF-8, enlaces, estados y trazabilidad | En ejecución |
| G1 | compilación, empaquetado y migraciones V4/V5 presentes | Evidencia previa con pruebas omitidas; debe reconfirmarse |
| G2 | JUnit y ArchUnit acumulados | Pendiente |
| G3 | PostgreSQL V1→V5, JPA/JTA, concurrencia y rollback | Pendiente |
| G4 | OIDC, autorización y seguridad administrativa negativa | Pendiente |
| G5 | Docker/Compose, health, secretos, migraciones y persistencia | Pendiente |
| G6 | Playwright, accesibilidad y demo visual responsive A/B | Pendiente |
| G7 | validación independiente, guía, evidencia, retrospectiva y PDF | Pendiente |

Una prueba ejecutada y fallida detiene el avance hacia el siguiente gate. Se
documenta el hallazgo, se corrige su causa y se repite primero la prueba mínima y
después el alcance afectado.

## Orden operativo

Se sigue el [manual paso a paso de pruebas](../../runbooks/manual-pruebas-j11-s4-08.md):

1. congelar e identificar la candidata local sin promoverla;
2. validar G0 y G1;
3. ejecutar G2 desde el módulo más pequeño hacia `mvn verify`;
4. ejecutar G3 y G4 sobre infraestructura real;
5. construir y validar imágenes y Compose en G5;
6. ejecutar Playwright, revisar la interfaz y realizar la demo en G6;
7. completar el recorrido independiente y los artefactos de cierre en G7.

## Condición para iniciar plugins productivos

No se implementará `business_partners` ni otro plugin del roadmap mientras esta
historia conserve un gate técnico o documental pendiente. Después del cierre se
habilita únicamente la preparación transversal acordada en ADR-0011: migraciones
`plg_*`, plantilla de plugin productivo y contrato de outbox/eventos cuando resulte
necesario.

## Evidencia

Los comandos, resultados y hallazgos se registran en
[la evidencia de J11-S4-08](../../evidence/J11-S4-08-validacion-demo-cierre.md).
