# Demo visual oficial de cierre técnico de Sprint 9

- Estado: ejecutada y reproducible
- Fecha: 2026-08-13
- URL inicial: `http://localhost:18080/logixone/`
- Perfil: `with-purchasing-demo`
- Aplicación: `logixone/app:j11-s9-07-closing`
- Migrador: `logixone/migrator:j11-s9-07-closing`
- Datos: exclusivamente ficticios

## Qué demuestra

La demo ejecuta un WAR sobre WildFly 41 con `reference_data`,
`business_partners`, `commercial_catalog`, `inventory` y `purchasing` como
plugins productivos separados. Compras crea una solicitud, exige un aprobador
distinto, emite una orden, confirma una recepción y una devolución, y consulta el
cumplimiento usando únicamente contratos públicos.

No demuestra factura del proveedor, deuda, pago, retención, contabilidad, costos,
SIFEN, migración Oracle ni BPM. Los tres plugins de referencia/personalización
son fixtures técnicos y no constituyen una personalización empresarial entregable.

## Preparación segura

1. Inicie Docker Desktop y compruebe que los puertos 18080 y 8180 estén libres.
2. Prepare `infra/compose/compose.env.local` y los secretos externos descritos en
   la guía de implementación.
3. Use Java 21 y el Maven Wrapper canónico de la raíz.
4. Construya las dos imágenes desde el mismo corte:

```powershell
docker build --file infra/docker/Dockerfile `
  --build-arg LOGIXONE_BUILD_MODE=verified `
  --build-arg LOGIXONE_MAVEN_PROFILE=with-purchasing-demo `
  --tag logixone/app:j11-s9-07-closing .

docker build --file infra/docker/Dockerfile.migrator `
  --build-arg LOGIXONE_BUILD_MODE=verified `
  --build-arg LOGIXONE_MAVEN_PROFILE=with-purchasing-demo `
  --tag logixone/migrator:j11-s9-07-closing .
```

5. Ejecute el migrador dos veces y levante los servicios sin eliminar volúmenes:

```powershell
$env:LOGIXONE_APP_IMAGE='logixone/app:j11-s9-07-closing'
$env:LOGIXONE_MIGRATOR_IMAGE='logixone/migrator:j11-s9-07-closing'

docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml run --rm migrator
docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml run --rm migrator
docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml up -d --wait --wait-timeout 240
```

6. Verifique `/logixone/health/live` y `/logixone/health/ready`: ambos deben
   responder HTTP 200 y estado `UP`.

No use `down --volumes`, no edite migraciones aplicadas y no copie secretos a
comandos, capturas ni documentación.

## Guion de presentación

1. Inicie sesión con el usuario ficticio multiempresa y seleccione empresa A.
2. Muestre el inicio y las funciones aportadas por Referencias, Socios, Catálogo,
   Inventario y Compras.
3. Abra **Administración > Plugins por empresa** y explique las dependencias
   requeridas de Compras.
4. Abra **Solicitudes**, cree o consulte una solicitud con artículo y unidad, y
   muestre su envío.
5. Cambie al actor aprobador ficticio y demuestre la separación de funciones.
6. Abra **Órdenes**, muestre la orden emitida con snapshots históricos.
7. Abra **Recepciones**, confirme cantidades contra un depósito público de
   Inventario y observe el movimiento idempotente.
8. Abra **Devoluciones**, confirme una cantidad y explique por qué vuelve a abrir
   el pendiente del proveedor.
9. Abra **Seguimiento** y recorra referencias y cantidades pedida, recibida,
   devuelta, cerrada y pendiente.
10. Cambie el ancho a 375, 720 y 1280 px; confirme acciones reordenadas y ausencia
    de desplazamiento horizontal normal.
11. Desactive Compras, pruebe su ruta directa y muestre la denegación genérica;
    reactive Compras antes de terminar.
12. Confirme que la autoridad temporal de sistema queda deshabilitada.

## Resultados esperados

- empresa activa y actor visibles sin exponer información sensible;
- cinco menús de Compras fusionados mediante contribuciones neutrales;
- el solicitante no puede aprobar su propia solicitud;
- recepciones y devoluciones conservan coherencia con Inventario;
- ruta inactiva denegada sin filtrar detalles internos;
- datos y activaciones sobreviven a recrear sólo `app`;
- 375, 720 y 1280 px no presentan overflow horizontal normal.

## Evidencia y restauración

Las capturas oficiales están en
`docs/evidence/screenshots/J11-S9-07-closing/e2e/`. La suite restaura todos los
plugins productivos habilitados y la autoridad de sistema en `false`. Los datos
ficticios permanecen como evidencia y no se eliminan con SQL.

Para detener conservando los volúmenes:

```powershell
docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml down
```

La demo completa el gate automatizado y visual J11-S9-07. Sprint 9 continúa
abierto por G7 independiente y por la matriz externa/Authenticode de J11-S9-08.
