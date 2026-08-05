# Demo reproducible de plugins y `commercial_catalog` - J11-S7-06

- Estado: Disponible como candidata visual
- Fecha: 2026-07-30
- URL inicial: `http://localhost:18080/logixone/faces/app/index.xhtml`
- Perfil: `with-commercial-catalog-demo`
- Imagen de aplicación:
  `logixone/app:j11-s7-06-commercial-catalog-demo`
- Imagen de migrador:
  `logixone/migrator:j11-s7-06-commercial-catalog-demo`

## Qué demuestra

La demo ejecuta un único WAR WildFly 41 con dos plugins productivos separados:
`business_partners` y `commercial_catalog`. El shell Jakarta Faces compone sus
menús sin importar sus clases internas. Cada pantalla vuelve a comprobar empresa,
plugin y permiso en el servidor antes de consultar o mutar datos.

No demuestra inventario, ventas, documentos comerciales, facturación ni
cumplimiento SIFEN. Los plugins de referencia y personalización A/B son fixtures y
no deben promoverse a una empresa real.

## Prerrequisitos

1. Java 21 y Maven Wrapper disponibles según el runbook de construcción;
2. Docker Engine operativo;
3. `infra/compose/compose.env.local` y secretos externos preparados;
4. puertos locales 18080 y 8180 disponibles;
5. usar sólo usuarios y datos ficticios.

No ejecutar `down --volumes`, no editar migraciones aplicadas y no colocar
contraseñas en comandos, argumentos de build o documentación.

## Construir el par de imágenes

```powershell
docker build --check --file infra/docker/Dockerfile .
docker build --check --file infra/docker/Dockerfile.migrator .

docker build --file infra/docker/Dockerfile.migrator `
  --build-arg LOGIXONE_BUILD_MODE=verified `
  --build-arg LOGIXONE_MAVEN_PROFILE=with-commercial-catalog-demo `
  --tag logixone/migrator:j11-s7-06-commercial-catalog-demo .

docker build --file infra/docker/Dockerfile `
  --build-arg LOGIXONE_BUILD_MODE=verified `
  --build-arg LOGIXONE_MAVEN_PROFILE=with-commercial-catalog-demo `
  --tag logixone/app:j11-s7-06-commercial-catalog-demo .
```

Aplicación y migrador deben construirse con el mismo perfil. Cambiar uno sin el
otro invalida la distribución.

## Migrar y arrancar sin borrar volúmenes

```powershell
$env:LOGIXONE_APP_IMAGE='logixone/app:j11-s7-06-commercial-catalog-demo'
$env:LOGIXONE_MIGRATOR_IMAGE='logixone/migrator:j11-s7-06-commercial-catalog-demo'

docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml run --rm migrator
docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml run --rm migrator

docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml up -d --wait --wait-timeout 240
```

La segunda migración debe informar cero cambios. `up` conserva los volúmenes
declarados; no añadir `--volumes` al detener el ambiente.

## Preparar únicamente los datos ficticios del catálogo

El fixture está en recursos de prueba y sólo escribe definiciones privadas de
`plg_commercial_catalog`. No concede permisos, no activa plugins, no toca `core` y
no es un mecanismo de carga productiva.

Obtener desde la administración el UUID de la empresa ficticia A y definirlo:

```powershell
$companyId='<uuid-de-la-empresa-ficticia-a>'
Get-Content `
  plugins/commercial-catalog/src/test/resources/demo/seed-commercial-catalog-demo.sql `
  -Encoding UTF8 | docker compose `
    --env-file infra/compose/compose.env.local `
    -f infra/compose/compose.yaml exec -T postgres `
    psql -U logixone -d logixone -v company_id=$companyId
```

Desde J11-S8-C01, repetir el comando debe conservar exactamente dos unidades, una
categoría, una marca y tres perfiles tributarios ficticios —general, reducido y
exento—, sin duplicados ni cambios.

## Activar y conceder permisos por la UI

1. iniciar sesión con el usuario ficticio multiempresa;
2. abrir `Administración > Plugins por empresa` y seleccionar empresa A;
3. localizar `commercial_catalog` y pulsar `Habilitar`;
4. abrir `Administración > Seguridad empresarial` para la misma empresa;
5. en “Conceder permiso funcional”, seleccionar el rol `demo_operator`;
6. conceder, uno por vez:
   `commercial_catalog.view`, `commercial_catalog.items.manage`,
   `commercial_catalog.prices.manage` y
   `commercial_catalog.definitions.manage`;
7. volver al inicio de la aplicación y seleccionar empresa A.

No crear activaciones o permisos con SQL. La UI reautoriza cada comando y registra
auditoría.

## Guion para presentar

1. Mostrar “Funciones disponibles” y explicar que “Socios comerciales”,
   “Artículos y servicios”, “Listas de precios”, “Perfiles tributarios” y
   “Panel de demostración” se
   fusionan desde aportes independientes.
2. Abrir “Perfiles tributarios”, registrar un perfil ficticio y aclarar que no es
   una tasa ni regla SIFEN certificada.
3. Abrir “Artículos y servicios”; mostrar el filtro y los resultados compactos.
4. Pulsar “Nuevo artículo o servicio” y registrar un servicio ficticio con código
   único, alcance compra/venta, unidad `EA` y perfil tributario demo.
5. En la ficha, abrir “Identificadores” y agregar un código alternativo ficticio.
6. Abrir “Clasificación” y asignar categoría y marca de demostración.
7. Abrir “Listas de precios”, crear una lista `PYG`, impuestos incluidos, escala
   cero y redondeo mitad hacia arriba.
8. En “Precios”, seleccionar el artículo creado, unidad `EA`, cantidad mínima `1`,
   importe ficticio y una vigencia ISO-8601; pulsar “Agregar precio”.
9. Explicar que precio, moneda, unidad y vigencia deben congelarse como snapshot en
   futuros documentos; el catálogo actual no reescribirá historia.
10. Mostrar el recorrido en 375, 720 y 1280 px. En compacto/medio el menú se
   colapsa y los formularios se apilan; en expandido aparece navegación lateral.
11. Como control negativo, deshabilitar el catálogo, abrir directamente `/catalog`
    y mostrar “Esta función no está disponible para tu contexto actual”; luego
    volver a habilitarlo.

## Verificación automática del recorrido

```powershell
$projectRoot=(Resolve-Path '.').Path

.\mvnw.cmd -Pvisual-e2e -pl tests/e2e-tests `
  "-Dit.test=CommercialCatalogVisualIT" `
  "-Dlogixone.commercial-catalog.e2e=true" `
  "-Dlogixone.app-url=http://localhost:18080/logixone/faces/app/index.xhtml" `
  "-Dlogixone.admin-url=http://localhost:18080/logixone/faces/admin/index.xhtml" `
  "-Dlogixone.demo-user-password-file=$projectRoot/.tools/secrets/demo-user-password.txt" `
  "-Dlogixone.evidence-dir=$projectRoot/docs/evidence/screenshots/J11-S7-06/e2e" `
  "-Dlogixone.playwright.executable=$projectRoot/.tools/playwright/chromium-1228/chrome-win64/chrome.exe" `
  verify
```

## Estado posterior y detención segura

El escenario crea artículos y listas con sufijos aleatorios y deja
`commercial_catalog` habilitado. No borrar esos datos con SQL directo. Para volver
a un estado idéntico usar un backup controlado o futuros casos de uso de archivo.

```powershell
docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml down
```

El comando detiene contenedores y conserva datos. No añadir `--volumes`.

Esta candidata no cierra Sprint 7 ni autoriza producción; falta `J11-S7-07`.
