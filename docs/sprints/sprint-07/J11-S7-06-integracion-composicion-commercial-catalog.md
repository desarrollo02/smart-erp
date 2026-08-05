# J11-S7-06 - Integración, composición y demo candidata de `commercial_catalog`

- Estado: Completa
- Fecha: 2026-07-30
- Gate: G5 composición y operación
- Dependencia: J11-S7-05 verde en código

## Objetivo

Incorporar `commercial_catalog` junto con `business_partners` al WAR y al migrador
mediante una selección física única, verificar migraciones e idempotencia, activar
la capacidad y sus permisos por las pantallas administrativas reales y entregar
una demo visual responsive reproducible.

## Composición de demo

El perfil Maven `with-commercial-catalog-demo` se declara únicamente en
`distribution/logixone-plugin-set` e incluye:

- `business-partners` y `business-partners-api`;
- `commercial-catalog` y `commercial-catalog-api`;
- `reference-plugin` como fixture transversal;
- `reference-customization-a` y `reference-customization-b` para las dos empresas
  ficticias.

WAR y migrador consumen el mismo `logixone-plugin-set`. El perfil es sólo de
desarrollo y demostración. Una implementación real debe incluir exclusivamente
los funcionales seleccionados y el plugin de personalización obligatorio y
distinto de cada empresa.

## Criterios de aceptación

1. el perfil físico se declara una vez y lo consumen WAR y migrador;
2. la aplicación descubre cinco definiciones de plugin y las tres unidades JPA;
3. el migrador descubre V1 de `plg_business_partners`, V1 de
   `plg_commercial_catalog` y V1 del fixture de referencia;
4. una construcción limpia sin perfil deja cero plugins en WAR y migrador;
5. ambos Dockerfiles aceptan el perfil en modos `verified` y
   `visual-candidate`;
6. migraciones y fixture de catálogo son repetibles y no duplican datos;
7. la empresa A habilita `commercial_catalog` y concede sus cuatro permisos por
   administración, sin escribir seguridad o activación mediante SQL;
8. el shell fusiona en un único menú participantes, catálogo, listas de precios y
   panel de referencia;
9. Playwright ejecuta alta de artículo, identificador, clasificación, lista y
   precio, además de denegación al desactivar y restauración posterior;
10. 375, 599, 600, 720, 839, 840 y 1280 px no presentan overflow horizontal;
11. Docker, health, persistencia, logs, reactor, PostgreSQL, guía, runbook y
    evidencia quedan verdes.

## Resultado

Los once criterios quedaron satisfechos. La imagen en ejecución descubre
`business_partners`, `commercial_catalog`, el plugin de referencia y las dos
personalizaciones. `live` y `ready` responden HTTP 200 `UP`; PostgreSQL y Keycloak
conservaron sus volúmenes y la aplicación no registró errores durante el recorrido.

La nueva demo visual quedó disponible en
`http://localhost:18080/logixone/faces/app/index.xhtml`. El menú se compone según
presencia física, activación y permiso efectivo; no existe una segunda lista fija
en el shell. Playwright creó datos ficticios mediante los casos de uso reales y
validó los siete anchos del proyecto. El plugin quedó habilitado al finalizar.

La ejecución detectó además que PowerShell podía degradar acentos al canalizar el
fixture hacia `psql`. El fixture ahora usa literales Unicode ASCII-safe de
PostgreSQL y actualizaciones condicionadas: repara sus nombres controlados y una
ejecución posterior produce cero cambios.

Sprint 7 continúa abierto. `J11-S7-07` debe volver a ejecutar la matriz integral,
presentar la demo oficial de cierre, registrar retrospectiva y regenerar/verificar
el PDF obligatorio.

Evidencia: [J11-S7-06](../../evidence/J11-S7-06-integracion-composicion-commercial-catalog.md).

Guion: [demo reproducible J11-S7-06](../../runbooks/demo-commercial-catalog-j11-s7-06.md).
