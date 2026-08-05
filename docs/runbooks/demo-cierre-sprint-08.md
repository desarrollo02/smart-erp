# Demo visual oficial de cierre técnico de Sprint 8

- Estado: ejecutada y reproducible
- Fecha: 2026-08-01
- URL inicial: `http://localhost:18080/logixone/faces/app/index.xhtml`
- Perfil: `with-inventory-demo`
- Aplicación: `logixone/app:j11-s8-07-closing`
- Migrador: `logixone/migrator:j11-s8-07-closing`
- Datos: exclusivamente ficticios

## Qué demuestra

La demo ejecuta un WAR WildFly 41 con `business_partners`,
`commercial_catalog` e `inventory` como plugins productivos separados. También
incluye el plugin funcional de referencia y dos personalizaciones técnicas. El
shell fusiona siete opciones de menú mediante contratos y revalida empresa, plugin
y permiso en el servidor.

No demuestra compras, ventas, costos, valoración contable, documentos fiscales ni
SIFEN. Los plugins de referencia son fixtures y no constituyen una personalización
empresarial entregable.

## Preparación segura

1. Inicie Docker Desktop y compruebe que los puertos 18080 y 8180 estén libres.
2. Prepare `infra/compose/compose.env.local` y los secretos externos descritos en
   la guía de implementación.
3. Desde la raíz, configure JDK 21 y use siempre Maven Wrapper.
4. Construya aplicación y migrador con el mismo perfil:

```powershell
docker build --file infra/docker/Dockerfile `
  --build-arg LOGIXONE_BUILD_MODE=verified `
  --build-arg LOGIXONE_MAVEN_PROFILE=with-inventory-demo `
  --tag logixone/app:j11-s8-07-closing .

docker build --file infra/docker/Dockerfile.migrator `
  --build-arg LOGIXONE_BUILD_MODE=verified `
  --build-arg LOGIXONE_MAVEN_PROFILE=with-inventory-demo `
  --tag logixone/migrator:j11-s8-07-closing .
```

5. Migre dos veces y levante sin eliminar volúmenes:

```powershell
$env:LOGIXONE_APP_IMAGE='logixone/app:j11-s8-07-closing'
$env:LOGIXONE_MIGRATOR_IMAGE='logixone/migrator:j11-s8-07-closing'

docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml run --rm migrator
docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml run --rm migrator
docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml up -d --wait --wait-timeout 240
```

6. Verifique `GET /logixone/health/live` y `GET /logixone/health/ready`: ambos
   deben responder HTTP 200 y estado `UP`.

No use `down --volumes`, no edite migraciones aplicadas y no copie secretos a
comandos, capturas ni documentación.

## Guion de presentación

1. Inicie sesión con el usuario ficticio multiempresa y seleccione empresa A.
2. Muestre el inicio: aparecen las funciones autorizadas fusionadas. Explique qué
   plugin aporta Datos de referencia, Socios, Catálogo, Inventario y el panel fixture.
3. Abra **Administración > Plugins por empresa**. Muestre siete plugins físicos y
   las dependencias `business_partners/commercial_catalog -> reference_data` e
   `inventory -> commercial_catalog`, todas `REQUIRED` 1.x.
4. Intente desactivar catálogo con inventario activo. Explique que el servidor
   rechaza la composición inválida y conserva ambos activos.
5. Vuelva al espacio de trabajo y abra **Artículos y servicios** para mostrar la
   referencia pública usada por inventario.
6. Abra **Depósitos** y muestre un depósito ficticio con su ubicación `GENERAL`.
7. Abra **Existencias**, seleccione un producto y muestre físico, reservado y
   disponible; después enseñe el libro de movimientos y reservas.
8. Abra **Conteos físicos**, muestre un conteo contabilizado y explique el flujo
   preparar, iniciar, capturar, revisar y contabilizar.
9. Cambie el ancho a 375, 720 y 1280 px. En compacto abra el menú lateral y confirme
   que las acciones se reordenan sin desplazamiento horizontal normal.
10. Como control negativo, desactive inventario, abra su ruta directa y muestre la
    denegación genérica; reactive inventario antes de terminar.
11. Regrese a inicio y confirme nuevamente las funciones disponibles.

## Resultados esperados

- empresa activa visible y consistente;
- siete menús autorizados, sin enlaces escritos manualmente por el shell;
- catálogo no puede desactivarse mientras inventario dependa de él;
- movimientos, reservas y conteos conservan trazabilidad y auditoría;
- ruta de un plugin inactivo queda denegada sin revelar información interna;
- datos y activaciones sobreviven a recrear únicamente `app`;
- no existe overflow horizontal normal en 375, 720 o 1280 px.

## Evidencia y restauración

Las capturas oficiales están en
`docs/evidence/screenshots/J11-S8-07-closing/e2e/`. La suite restaura catálogo e
inventario habilitados. Los registros ficticios permanecen como evidencia; no se
eliminan mediante SQL.

Para detener de forma segura:

```powershell
docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml down
```

Esto conserva los volúmenes. La demo completa J11-S8-07. El instalador interno se
presenta por separado mediante el
[guion J11-S8-08](demo-instalador-windows-sprint-08.md). Sprint 8 permanece abierto
hasta completar su matriz Windows, Authenticode y la validación independiente.
