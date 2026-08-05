# J11-S3-00 — Gobierno, ADR y planificación del Sprint 3

- Fecha de inicio: 2026-07-28
- Fecha de cierre: 2026-07-28
- Estado: Completada
- Dependencia: Sprint 2 cerrado y decisión de identidad confirmada

## Objetivo

Convertir la decisión confirmada sobre Keycloak, OIDC nativo, membresías y autorización propia del ERP en un ADR aceptado y un backlog lineal que termine en una demo visual segura y verificable.

## Alcance

- aceptar las fronteras entre proveedor de identidad y ERP;
- decidir identidad estable, membresía, roles, permisos, selección empresarial, sesión y bootstrap;
- definir la evolución aditiva `core` V3;
- delimitar lo que la primera demo debe y no debe mostrar;
- crear épica, Sprint, historias, criterios, gates, riesgos y trazabilidad;
- incorporar la decisión temporal de ejecutar las pruebas acumuladas al terminar la candidata visual;
- actualizar metodología, arquitectura, backlog, guía e índices.

## Fuera de alcance

- agregar dependencias o modificar POM;
- crear clases Java, migraciones, configuración de Keycloak, XHTML o CSS;
- descargar imágenes o seleccionar todavía su digest ejecutable;
- ejecutar Maven, Docker, PostgreSQL, Keycloak o Playwright;
- presentar una prueba futura como ya ejecutada.

## Criterios de aceptación

- **CA-01:** ADR-0006 queda aceptado con fecha, contexto, decisión, alternativas, consecuencias y verificación.
- **CA-02:** Keycloak autentica y el ERP conserva membresías, roles y permisos funcionales.
- **CA-03:** la identidad se basa en `(issuer, subject)` y no en correo o username.
- **CA-04:** un único realm inicial no se confunde con empresas del ERP.
- **CA-05:** WildFly usa soporte OIDC nativo y no se autoriza un adaptador propietario en el WAR.
- **CA-06:** la empresa activa solo se resuelve desde una sesión autenticada y membresía revalidada.
- **CA-07:** V3 se define aditiva sin modificar V1 o V2.
- **CA-08:** el bootstrap inicial es one-shot, idempotente y sin endpoint anónimo.
- **CA-09:** la demo incluye login, selector, navegación, pantalla neutral y personalización A/B.
- **CA-10:** el plan excluye dominios ERP productivos, SPA, passwords locales y administración pública completa.
- **CA-11:** las nueve historias tienen dependencia lineal, alcance, exclusiones, criterios y gates.
- **CA-12:** las pruebas diferidas se concentran en `J11-S3-08` y las historias previas no se consideran completadas sin ellas.
- **CA-13:** la guía independiente y el PDF obligatorio siguen siendo gates del cierre.
- **CA-14:** los índices locales enlazan ADR, épica, Sprint, historias y evidencia.
- **CA-15:** G0 final confirma UTF-8, enlaces y estructura documental.

## Gates

- G0 documental sobre todos los Markdown y enlaces locales.
- Auditoría estructural de las nueve historias del Sprint 3.
- Auditoría de trazabilidad entre ADR-0006, épica, criterios globales e historias.
- No aplica Maven ni Docker: la historia no modifica código, POM, SQL o infraestructura ejecutable.

## Resultado final

ADR-0006 quedó aceptado según la confirmación explícita del responsable de producto. El Sprint 3 se organizó en nueve historias y conserva una única meta de producto: demo visual segura de identidad, empresa, navegación y personalización.

La excepción temporal de pruebas no convierte trabajo no validado en terminado. Las historias `J11-S3-01` a `J11-S3-07` deberán usar un estado intermedio hasta que `J11-S3-08` ejecute todos los gates acumulados.

## Siguiente paso

Iniciar `J11-S3-01` y mantener dominio y aplicación libres de Jakarta, Keycloak, HTTP, JPA y UI.
