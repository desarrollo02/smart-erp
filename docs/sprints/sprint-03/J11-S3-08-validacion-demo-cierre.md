# J11-S3-08 — Validación de demo y cierre del Sprint 3

- Estado: En validación — G2–G6 verdes; G7 pendiente
- Dependencia: `J11-S3-07` completada

## Objetivo

Ejecutar la validación acumulada de Sprint 3, corregir cualquier hallazgo y producir una demo visual reproducible cuya seguridad, aislamiento, persistencia y personalización estén demostrados antes del cierre.

## Alcance

- todas las pruebas unitarias diferidas de `J11-S3-01` a `J11-S3-07`;
- ArchUnit y composición física de WAR/imágenes;
- PostgreSQL V1→V2→V3, JPA/JTA, concurrencia y rollback;
- Keycloak/WildFly OIDC positivo y negativo;
- bootstrap, secretos, logs, health y recreación Compose;
- matriz de cero, una y múltiples membresías;
- autorización, revocación y cambio de empresa;
- Playwright para login, selector, navegación, pantalla y A/B;
- demostración guiada y capturas/evidencia sin secretos;
- recorrido independiente de la edición candidata vigente de la guía;
- actualización de guía, arquitectura, runbooks, retrospectiva y siguiente incremento;
- regeneración y revisión integral del PDF obligatorio.

## Fuera de alcance

- omitir o relajar pruebas para aceptar la demo;
- registrar un hallazgo conocido como verde;
- usar datos o credenciales empresariales reales;
- presentar dominios ERP no implementados;
- desplegar o promover a producción.

## Criterios de aceptación

- **CA-01:** todas las historias previas conservan trazabilidad entre criterios y pruebas.
- **CA-02:** pruebas unitarias acumuladas terminan con cero fallos y cero omisiones no justificadas.
- **CA-03:** ArchUnit confirma límites de módulos y ausencia de dependencias Keycloak en dominio/WAR.
- **CA-04:** build limpio y WAR reproducible quedan verdes.
- **CA-05:** V1→V2→V3, base vacía, reejecución y checksums quedan verificados.
- **CA-06:** JPA `validate`, JTA, concurrencia, idempotencia y rollback quedan verdes.
- **CA-07:** login/logout real funciona y casos de issuer, audience, expiración y sesión inválidos deniegan.
- **CA-08:** secretos y tokens no aparecen en fuentes, imágenes, respuestas o logs.
- **CA-09:** cero, una y varias membresías se comportan según ADR-0006.
- **CA-10:** manipular empresa desde el navegador no concede acceso.
- **CA-11:** revocación de membresía/rol y desactivación de plugin eliminan acceso efectivo.
- **CA-12:** Playwright demuestra shell, selector, menú y links directos protegidos.
- **CA-13:** personalizaciones A/B son visibles y no existe filtración cruzada.
- **CA-14:** liveness/readiness conservan su semántica y no filtran diagnósticos OIDC.
- **CA-15:** recrear contenedores sin borrar volúmenes conserva estado; limpieza elimina solo recursos efímeros propios.
- **CA-16:** la demo está rotulada como técnica y no presenta un dominio ERP ficticio.
- **CA-17:** un implementador independiente completa `VALIDATION.md` y los hallazgos se resuelven.
- **CA-18:** la guía se eleva a `1.0` únicamente si la validación independiente es satisfactoria.
- **CA-19:** evidencia registra comandos, versiones, digests, fallos, correcciones y resultados.
- **CA-20:** el PDF estable se regenera contra el baseline final, se renderizan todas sus páginas y se registra páginas, bytes y SHA-256.
- **CA-21:** historias `J11-S3-01` a `J11-S3-07` solo cambian a `Completada` después de sus gates verdes.
- **CA-22:** retrospectiva y siguiente trabajo autorizado quedan documentados.

## Gates

- G0: documentación, UTF-8, enlaces, estados y trazabilidad.
- G2: unitarias y ArchUnit acumuladas.
- G3: PostgreSQL, Flyway, JPA/JTA y rollback.
- G4: OIDC, sesión y seguridad negativa.
- G5: imágenes, Compose, secretos, health y persistencia.
- G6: Playwright y demo visual A/B.
- G7: validación independiente, guía, evidencia, retrospectiva y PDF.

## Orden operativo confirmado

Por decisión del responsable de producto, esta historia se ejecutará en dos cortes:

1. aprovisionar secretos e identidades ficticias ignoradas, empresas A/B y permisos
   mediante automatización cerrada; construir y arrancar la candidata sin promoverla;
2. confirmar manualmente que login, shell y pantalla A/B son observables y avisar de
   inmediato al responsable de producto;
3. ejecutar después la matriz automatizada acumulada G2–G6;
4. corregir cualquier fallo y repetir el alcance afectado;
5. completar G7, elevar la guía y regenerar el PDF sólo cuando todo esté verde.

Que la candidata sea observable habilita la demostración solicitada, pero no la
aceptación técnica ni el cierre del Sprint.

## Regla de cierre

No basta con que la pantalla se vea. Cualquier gate fallido mantiene la demo como candidata y las historias de código como pendientes de validación. Se corrige la causa y se repite el alcance afectado antes de cerrar.

## Resultado técnico acumulado

G2–G6 terminaron verdes el 2026-07-28: 145 pruebas Maven sin fallos u omisiones, 7
reglas ArchUnit, PostgreSQL V3/Testcontainers, repositorios, 4 pruebas JTA, 4 pruebas
OIDC, imágenes verificadas, Compose/persistencia/health, seguridad negativa y 3
escenarios Playwright A/B. El WAR resultó idéntico entre host e imagen después de
normalizar permisos del contexto Docker. La evidencia completa está en
[J11-S3-08 — gates G2–G6](../../evidence/J11-S3-08-validacion-demo-cierre.md).

G7 no se considera ejecutado: `VALIDATION.md` no tiene todavía validador ni dictamen,
la guía permanece `1.0-rc11`, no existe retrospectiva final y el PDF no se regeneró.

## Siguiente paso

Una persona que no haya implementado Sprint 2/3 debe completar
`docs/implementation-guide/VALIDATION.md` usando la guía y el repositorio. Los
hallazgos se corrigen y se repite el alcance afectado. Solo con dictamen satisfactorio
se define en retrospectiva el siguiente incremento, se eleva la guía a `1.0` y se
regenera/verifica el PDF de cierre.
