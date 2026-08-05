# J11-S8-06 - Integración, composición y demo candidata de `inventory`

- Estado: Completa
- Fecha: 2026-08-01
- Gate: G5 composición y operación
- Dependencia: J11-S8-05 verde

## Objetivo

Incorporar `inventory` junto con `commercial_catalog` y `business_partners` al WAR
y al migrador mediante una selección física única, verificar su dependencia
funcional, migraciones, seguridad y persistencia, y entregar una demo visual
responsive reproducible sobre el runtime real.

## Composición de demo

El perfil Maven `with-inventory-demo` se declara únicamente en
`distribution/logixone-plugin-set` e incluye:

- `business-partners` y su API pública;
- `commercial-catalog` y su API pública;
- `inventory` y su API pública;
- `reference-plugin` como fixture transversal;
- `reference-customization-a` y `reference-customization-b` para las empresas
  ficticias.

El WAR y el migrador consumen el mismo `logixone-plugin-set`. En runtime existen
seis descriptores: tres plugins productivos y tres fixtures. Los JAR de API no son
plugins y no aportan descriptores, menús ni migraciones.

Este perfil es exclusivo de desarrollo y demostración. Una implementación real
debe seleccionar sólo los plugins funcionales contratados y el plugin de
personalización obligatorio y diferente de cada empresa.

## Criterios de aceptación

1. el perfil físico se declara una vez y lo consumen WAR y migrador;
2. la dependencia requerida `inventory -> commercial_catalog [1.0.0,2.0.0)` se
   resuelve sin acceso JPA, SQL ni importación de clases privadas del catálogo;
3. la distribución contiene exactamente los seis descriptores esperados y las
   APIs públicas necesarias;
4. los Dockerfiles aceptan el perfil en modos `verified` y `visual-candidate`;
5. el migrador aplica `plg_inventory` V1–V2 y una segunda ejecución informa cero
   cambios;
6. recrear `app` conserva PostgreSQL y Keycloak; no se eliminan volúmenes;
7. la empresa A habilita los tres plugins productivos y recibe los permisos por
   las pantallas administrativas reales;
8. una sesión renovada muestra un único menú fusionado con socios, catálogo e
   inventario;
9. el recorrido crea depósito, ubicación, inscripción, entrada, reserva y conteo
   contabilizado mediante la UI y casos de uso reales;
10. la consulta final muestra físico 12, reservado 3 y disponible 9;
11. desactivar `inventory` impide acceder a su ruta directa y restaurarlo recupera
    el recorrido sin borrar datos;
12. 375, 599, 600, 720, 839, 840 y 1280 px no presentan overflow horizontal;
13. reactor, arquitectura, PostgreSQL estricto, Docker/Compose, health, OIDC,
    Playwright, guías y evidencia quedan verdes.

## Resultado

Los trece criterios quedaron satisfechos. La candidata se ejecuta en
`http://localhost:18080/logixone/faces/app/index.xhtml` con tres plugins
productivos físicamente presentes. El shell fusiona sus aportes después de evaluar
presencia, compatibilidad, dependencia, activación empresarial y permisos de la
sesión actual.

El recorrido visual registró datos ficticios por la interfaz: creó un depósito y
su ubicación `GENERAL`, inscribió un producto activo del catálogo, contabilizó una
entrada de 12 unidades, reservó 3, comprobó disponibilidad 9 y completó un conteo
físico sin diferencias. La desactivación negó la ruta y la restauración conservó
los datos.

La validación encontró una consulta JPQL con `count` como alias, palabra reservada
bajo el modo estricto de Hibernate. Se renombró el alias a `stockCount` y se agregó
un escenario PostgreSQL que evalúa el bloqueo de conteos con cumplimiento JPA
estricto. También se ajustó el shell compacto para envolver correctamente el panel
de sesión y evitar overflow horizontal.

La activación y las concesiones modifican autoridad persistida, pero la sesión
autenticada conserva un snapshot de seguridad. El recorrido debe iniciar una sesión
nueva después de esos cambios; refrescar sólo la página no concede autoridad.

Sprint 8 continúa abierto. `J11-S8-07` debe ejecutar el gate integral, presentar la
demo oficial, registrar retrospectiva, congelar el baseline y regenerar/verificar
el PDF. Después, `J11-S8-08` construirá y validará el instalador Windows antes del
cierre formal.

Evidencia: [J11-S8-06](../../evidence/J11-S8-06-integracion-composicion-inventory.md).

Guion: [demo reproducible J11-S8-06](../../runbooks/demo-inventory-j11-s8-06.md).
