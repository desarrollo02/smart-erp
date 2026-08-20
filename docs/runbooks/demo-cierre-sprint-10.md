# Demo visual oficial de cierre técnico de Sprint 10

- Estado: ejecutado y reproducible; evidencia oficial en J11-S10-06
- Fecha: 2026-08-20
- URL inicial: `http://localhost:18080/logixone/`
- Perfil: `with-purchasing-demo`
- Aplicación: `logixone/app:j11-s10-06-closing-v3`
- Migrador: `logixone/migrator:j11-s10-06-closing`
- Datos: exclusivamente ficticios

## Qué demuestra

La demo conserva los cinco plugins productivos del baseline y muestra el valor de
Sprint 10: contratos visuales neutrales v2, cinco floorplans cerrados, operación
guiada de Inventario y bandeja/editor/operaciones/consulta de Compras. También
demuestra teclado, restauración de foco, movimiento reducido, responsive y
denegación de servidor cuando un plugin no está efectivo.

No demuestra Ventas, facturación del proveedor, deuda, pago, retención,
contabilidad, costos, SIFEN, migración Oracle ni BPM. Los tres plugins de
referencia/personalización son fixtures técnicos.

## Preparación segura

1. Use una materialización exacta del índice Git bajo
   `.tools/tmp/validation/J11-S10-06/`.
2. Compruebe que Docker Desktop esté disponible y que 18080/8180 estén libres.
3. Use el JDK, Maven, navegador y secretos gobernados bajo `.tools/`.
4. Construya aplicación y migrador desde el mismo corte y perfil:

```powershell
docker build --file infra/docker/Dockerfile `
  --build-arg LOGIXONE_BUILD_MODE=verified `
  --build-arg LOGIXONE_MAVEN_PROFILE=with-purchasing-demo `
  --tag logixone/app:j11-s10-06-closing-v3 .

docker build --file infra/docker/Dockerfile.migrator `
  --build-arg LOGIXONE_BUILD_MODE=verified `
  --build-arg LOGIXONE_MAVEN_PROFILE=with-purchasing-demo `
  --tag logixone/migrator:j11-s10-06-closing .
```

5. Use un proyecto Compose inequívoco, ejecute el migrador dos veces y espere
   health:

```powershell
$env:LOGIXONE_APP_IMAGE='logixone/app:j11-s10-06-closing-v3'
$env:LOGIXONE_MIGRATOR_IMAGE='logixone/migrator:j11-s10-06-closing'

docker compose --project-name logixone-j11-s10-06 `
  --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml run --rm migrator
docker compose --project-name logixone-j11-s10-06 `
  --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml run --rm migrator
docker compose --project-name logixone-j11-s10-06 `
  --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml up -d --wait --wait-timeout 240
```

6. Verifique `/logixone/health/live` y `/logixone/health/ready` con HTTP 200 y
   estado `UP`.

## Guion de presentación

1. Inicie sesión con el usuario ficticio multiempresa y seleccione empresa A.
2. Muestre inicio, empresa activa y menús fusionados de los cinco plugins.
3. Abra un maestro v1 y confirme que conserva directorio, alta y ficha.
4. Abra **Inventario > Existencias**, seleccione tarea y artículo y use
   **Continuar** para adaptar la captura.
5. Registre una entrada y explique que fuente, identidad, versión e idempotencia
   permanecen ocultas y se revalidan en servidor.
6. Abra **Compras > Solicitudes**, muestre la bandeja y la separación de
   solicitante/aprobador.
7. Abra **Órdenes** y recorra cabecera, líneas, resumen y acciones de estado.
8. Abra **Recepciones** y **Devoluciones**; muestre campos dependientes de la
   línea y trazabilidad de Stock.
9. Abra **Seguimiento** y confirme su carácter de solo lectura.
10. Recorra con Tab; provoque una validación y confirme la restauración de foco.
11. Active movimiento reducido y confirme que desaparecen transiciones no
    esenciales.
12. Revise 375, 720 y 1280 px; en compacto use tarjetas y confirme que la barra
    empresarial no se superpone ni genera overflow.
13. Desactive Inventario y Compras por separado, pruebe sus rutas directas y
    muestre la denegación genérica; restaure dependencias antes de terminar.
14. Confirme que el bootstrap global queda deshabilitado.

## Resultados esperados

- misma ruta y menú para contratos v1 y v2, sin pantallas paralelas;
- foco visible y destino útil después de cada postback;
- acciones válidas según empresa, permiso, actor y estado;
- campos condicionales sin pedir tokens técnicos al operador;
- 375/599/600/720/839/840/1280 sin overflow horizontal normal;
- plugin inactivo o permiso ausente bloqueado también en servidor;
- health y catálogo de ocho plugins saludables.

## Evidencia y restauración

Las capturas oficiales se guardan en
`docs/evidence/screenshots/J11-S10-06/e2e/`. La suite debe restaurar los plugins
productivos y dejar `LOGIXONE_SECURITY_BOOTSTRAP_ENABLED=false`.

Al finalizar, confirme el nombre exacto y elimine únicamente el proyecto
sintético de esta historia:

```powershell
docker compose --project-name logixone-j11-s10-06 `
  --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml down --volumes --remove-orphans
```

No use este comando contra otro proyecto ni contra servicios del usuario o del
IDE. Sprint 10 continúa abierto después de la demo hasta completar la validación
independiente y J11-S10-07.
