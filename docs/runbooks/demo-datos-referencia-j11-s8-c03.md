# Demo de datos de referencia normativos — J11-S8-C03

- Estado: ejecutada y validada; recongelación formal de Sprint 8 pendiente
- Fecha: 2026-08-04
- Perfil físico: `with-inventory-demo`
- Usuario ficticio: `demo.empresas.ab`
- Permiso adicional: `reference_data.view`

## Objetivo

Demostrar que `reference_data` es el propietario compartido de países y monedas,
que el primer corte identifica honestamente su completitud `BOOTSTRAP_SUBSET` y
que Socios Comerciales y Catálogo Comercial consumen sus contratos públicos. La
demo no presenta el subconjunto `PY/PYG/USD` como publicación normativa completa.

## Preparación

1. Construya aplicación y migrador con `LOGIXONE_BUILD_MODE=verified` y el perfil
   `with-inventory-demo`.
2. Ejecute el migrador dos veces. La primera ejecución debe aplicar
   `plg_reference_data` V1 y la segunda no debe aplicar cambios.
3. Levante Compose sin borrar volúmenes y verifique liveness/readiness en `UP`.
4. En la empresa ficticia, active en este orden `reference_data`,
   `business_partners`, `commercial_catalog` e `inventory`.
5. Conceda `reference_data.view`, `business_partners.view` y los permisos de
   catálogo requeridos por el recorrido. Cierre e inicie sesión para renovar el
   snapshot de autoridad.

## Recorrido

1. Abra **Datos de referencia** y compruebe la ruta `/reference-data`.
2. Confirme que la vista es de sólo lectura y muestra:
   - país `PY` y código alfa-3 `PRY`;
   - monedas `PYG` y `USD`;
   - autoridad, fecha observada, SHA-256 y completitud `BOOTSTRAP_SUBSET`.
3. Abra **Socios comerciales**, agregue una identificación y seleccione `PY` en
   **País emisor**. No debe existir entrada de texto libre para ese dato.
4. Abra **Listas de precios** y seleccione `PYG` en **Moneda**. El servidor debe
   revalidar el código dentro de la transacción del alta.
5. Repita la vista de datos de referencia en 375, 720 y 1280 px; compruebe foco,
   labels, legibilidad, ausencia de altas arbitrarias y ausencia de overflow
   horizontal normal.

## Ejecución automatizada

```powershell
$projectRoot = (Resolve-Path '.').Path
.\mvnw.cmd -Pvisual-e2e -pl tests/e2e-tests `
  "-Dit.test=BusinessPartnersVisualIT" `
  "-Dlogixone.business-partners.e2e=true" `
  "-Dlogixone.app-url=http://localhost:18080/logixone/faces/app/index.xhtml" `
  "-Dlogixone.admin-url=http://localhost:18080/logixone/faces/admin/index.xhtml" `
  "-Dlogixone.demo-user-password-file=$projectRoot/.tools/secrets/demo-user-password.txt" `
  "-Dlogixone.evidence-dir=$projectRoot/docs/evidence/screenshots/J11-S8-C03/e2e" `
  "-Dlogixone.playwright.executable=$projectRoot/.tools/playwright/chromium-1228/chrome-win64/chrome.exe" `
  verify
```

Resultado esperado: una prueba verde y capturas
`reference-data-expanded-1280.png`, `reference-data-medium-720.png` y
`reference-data-compact-375.png`, además de las capturas del recorrido de Socios
Comerciales.

## Resultado observado 2026-08-04

- migración V1 aplicada una vez y segunda ejecución sin cambios;
- aplicación, PostgreSQL y Keycloak sanos en Compose, con volúmenes conservados;
- health REST 2/2 y componentes de readiness, migraciones y OIDC en `UP`;
- `BusinessPartnersVisualIT` 1/1 verde, sin fallos, errores u omitidas;
- capturas de 1280, 720 y 375 px revisadas visualmente y sin overflow horizontal.

La primera ejecución en 375 px detectó que el SHA-256 extendía el documento a
503 px. Se corrigió el contenedor para admitir encogimiento y corte seguro de
palabras largas; la repetición automatizada y la revisión visual quedaron verdes.

## Restauración segura

Los datos son ficticios y pueden conservarse. No ejecutar `DELETE`, `TRUNCATE`,
`DROP` ni `docker compose down --volumes`. Para detener el ambiente use
`docker compose ... down`, que conserva los volúmenes nombrados.

## Límites

El corte no contiene el catálogo mundial completo, importación en runtime, tasas
de cambio ni edición de códigos normativos. Publicación completa, reconciliación,
retiros e historial operativo permanecen en RD-04 a RD-06.
