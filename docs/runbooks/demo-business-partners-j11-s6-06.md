# Demo reproducible de `business_partners` - J11-S6-06

- Estado: Disponible con floorplan ERP corregido sobre composición física verificada
- Fecha: 2026-07-29
- URL inicial: `http://localhost:18080/logixone/faces/app/index.xhtml`
- URL directa después de seleccionar empresa:
  `http://localhost:18080/logixone/faces/app/view.xhtml?route=%2Fbusiness-partners&mode=directory`
- Perfil: `with-business-partners-demo`

## Qué demuestra

Esta demo ejecuta una imagen real de WildFly 41 con Jakarta Faces, PostgreSQL,
Keycloak y el plugin productivo `business_partners`. El mismo perfil construye WAR
y migrador. Los plugins de referencia y personalización A/B son fixtures del
ambiente de dos empresas; no deben promoverse como personalización de una empresa
real.

## Preparar las imágenes

Desde la raíz del repositorio:

```powershell
docker build --check --file infra/docker/Dockerfile .
docker build --check --file infra/docker/Dockerfile.migrator .

docker build --file infra/docker/Dockerfile.migrator `
  --build-arg LOGIXONE_BUILD_MODE=verified `
  --build-arg LOGIXONE_MAVEN_PROFILE=with-business-partners-demo `
  --tag logixone/migrator:j11-s6-06-business-partners-demo .

docker build --file infra/docker/Dockerfile `
  --build-arg LOGIXONE_BUILD_MODE=verified `
  --build-arg LOGIXONE_MAVEN_PROFILE=with-business-partners-demo `
  --tag logixone/app:j11-s6-06-business-partners-demo .
```

No colocar secretos en argumentos de build. Para un cliente real, declarar un
perfil controlado que incluya sus plugins funcionales y su personalización
exclusiva; aplicación y migrador deben usar exactamente ese mismo perfil.

## Migrar y arrancar sin pisar datos

Definir las etiquetas sólo para la sesión actual:

```powershell
$env:LOGIXONE_APP_IMAGE='logixone/app:j11-s6-06-business-partners-demo'
$env:LOGIXONE_MIGRATOR_IMAGE='logixone/migrator:j11-s6-06-business-partners-demo'

docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml run --rm migrator
docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml run --rm migrator

docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml up -d --no-deps --force-recreate app
docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml up -d --no-deps --wait --wait-timeout 180 app
```

La segunda migración debe aplicar cero cambios. `--force-recreate app` sustituye
el contenedor, no los volúmenes. No usar `down --volumes`; esa opción borra estado
y requiere autorización destructiva y respaldo probado.

## Verificación previa

1. confirmar que `postgres`, `keycloak` y `app` estén saludables;
2. abrir `/logixone/health/live` y `/logixone/health/ready` y esperar HTTP 200;
3. usar únicamente el usuario ficticio de demo y su contraseña externa;
4. confirmar que la empresa A posee plugin activo y permisos `view`, `manage`,
   `roles.manage` y `lifecycle.manage`;
5. no mostrar secretos, tokens, variables completas ni datos reales.

## Guion para presentar

1. Iniciar sesión con el usuario multiempresa ficticio.
2. Seleccionar la primera empresa autorizada.
3. En “Funciones disponibles”, explicar que el menú aparece por composición física,
   activación empresarial y permiso efectivo; luego abrir “Socios comerciales”.
4. En el directorio, mostrar la navegación estable, el filtro compacto y los
   resultados. Explicar únicamente el valor de negocio; los IDs de pantalla, slots y
   versiones técnicas ya no se exponen al usuario operativo.
5. Buscar `BP-DEMO-001`, abrir “Cliente Demo S.A.” y mostrar el resumen de lectura.
6. Recorrer “Datos generales”, “Identificaciones”, “Direcciones”, “Contacto” y
   “Roles y estado”. Señalar que cada pestaña contiene una tarea concreta y evita la
   antigua página vertical con todos los formularios simultáneos.
7. Volver al directorio, pulsar “Nuevo socio” y registrar un participante ficticio
   con un código único. La alta sólo solicita datos principales y abre la ficha
   resultante.
8. Regresar al directorio, buscar el código, abrir la ficha, entrar en “Roles y
   estado” y asignar cliente. Volver a “Resumen” y confirmar `Cliente · Activo`.
9. Explicar verbalmente que cada acción revalida empresa, plugin y permiso en el
   servidor y que las mutaciones usan JTA, versión optimista y auditoría; no mostrar
   esos detalles como ruido dentro de la pantalla.
10. Mostrar 375, 720 y 1280 px: compacto y medio usan lista adaptable; expandido usa
    tabla y navegación lateral. Ningún rango debe tener overflow horizontal de
    página ni acciones cortadas.

## Mensajes importantes

- es funcional sobre datos reales de demo, no un mock;
- retirar/desactivar un plugin no borra automáticamente su esquema ni sus datos;
- el kernel no contiene la lógica de participantes;
- las personalizaciones futuras usan contratos y slots, nunca XHTML/CSS/JavaScript
  arbitrario;
- cada empresa real debe tener su plugin de personalización propio y obligatorio;
- esta demo intermedia no sustituye el gate final `J11-S6-07` ni autoriza producción.

## Evidencia y estado posterior

Las capturas revisadas del floorplan corregido están en
`docs/evidence/screenshots/J11-S6-06-redesign/e2e/`. El escenario automático crea datos
ficticios con prefijo `E2E-`; no borrarlos con SQL directo. Para restaurar un estado
de presentación idéntico se debe usar un backup controlado o casos de uso públicos
cuando exista una política de eliminación/archivo aprobada.

Al terminar se puede conservar la demo encendida. Para detener contenedores sin
eliminar datos:

```powershell
docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml down
```

No añadir `--volumes`.
