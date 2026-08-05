# Levantar Smart ERP con IntelliJ IDEA Ultimate

- Edición: 1.4
- Fecha de verificación: 2026-08-05
- IDE verificado: IntelliJ IDEA Ultimate 2026.2
- Sistema de ejemplo: Windows 11 con PowerShell
- Baseline: Java 21, Maven Wrapper 3.9.16, Docker/Compose, PostgreSQL, Keycloak 26.7.0 y WildFly 41
- Distribución de demo: perfil Maven `with-inventory-demo`, baseline J11-S8-07
- Instalador: `0.8.0-internal.1` disponible sólo para evaluación interna; no
  sustituye la preparación del entorno de desarrollo
- PDF del baseline anterior: [guia-levantar-logixone-intellij-idea-ultimate.pdf](../output/pdf/guia-levantar-logixone-intellij-idea-ultimate.pdf); conserva la marca Logixone y se regenerará en el gate documental de cierre

## 1. Objetivo y resultado esperado

Esta guía ayuda a una persona que recibe el repositorio por primera vez a:

1. abrir correctamente el reactor Maven en IntelliJ IDEA Ultimate;
2. configurar el JDK 21 y el Maven Wrapper del proyecto;
3. preparar la configuración y los secretos locales sin versionarlos;
4. construir las imágenes verificadas de aplicación y migrador;
5. levantar PostgreSQL, migraciones, Keycloak y WildFly mediante Docker Compose;
6. comprobar la salud del sistema y abrir la interfaz;
7. detener y volver a levantar el ambiente sin perder los datos.

Al terminar, deben responder:

- aplicación: `http://localhost:18080/logixone/faces/app/index.xhtml`;
- administración: `http://localhost:18080/logixone/faces/admin/index.xhtml`;
- liveness: `http://localhost:18080/logixone/health/live`;
- readiness: `http://localhost:18080/logixone/health/ready`;
- Keycloak de desarrollo/demo: `http://keycloak.localhost:8180`.

> **Decisión operativa:** IntelliJ es el entorno de edición, navegación, ejecución de
> pruebas y control de Docker. La forma oficial de ejecutar Smart ERP completo sigue
> siendo la imagen reproducible y `infra/compose/compose.yaml`. Instalar WildFly
> directamente en IntelliJ no es necesario para este primer arranque.

> **Relación con el instalador:** el EXE de J11-S8-08 diagnostica y monta una demo
> local, pero no configura IntelliJ, el JDK del proyecto ni las cachés Maven. Está
> sin Authenticode; esta guía continúa siendo el recorrido de desarrollo soportado.

## 2. Por qué usamos Docker aunque IntelliJ Ultimate soporte WildFly

IntelliJ Ultimate puede registrar un servidor JBoss/WildFly local. Sin embargo,
Smart ERP también necesita PostgreSQL, el migrador one-shot, Keycloak, secretos
montados, configuración OIDC y la configuración reproducible de WildFly.

El recorrido oficial conserva estas piezas juntas:

```text
IntelliJ IDEA
    │
    ├── Maven Wrapper ──> compila y prueba el reactor
    │
    └── Docker Compose
            ├── PostgreSQL + volumen persistente
            ├── migrator (termina antes de la aplicación)
            ├── Keycloak + volumen persistente
            └── WildFly 41 + logixone.war
```

Esto evita que una configuración manual del IDE funcione solamente en una
computadora y difiera de la imagen que después se prueba o promueve.

## 3. Prerrequisitos

Antes de abrir el proyecto, comprobar:

- IntelliJ IDEA Ultimate 2026.2 instalado y con licencia activa;
- Docker Desktop iniciado y configurado para contenedores Linux;
- Docker Engine, Buildx y Docker Compose disponibles;
- repositorio ubicado en `C:\cosme\smart-erp`;
- puertos `18080` y `8180` disponibles;
- acceso a Internet para el primer build si las dependencias o imágenes todavía no
  existen en las cachés locales;
- espacio suficiente para `.tools/`, las imágenes y los volúmenes Docker.

Comprobar Docker desde PowerShell:

```powershell
docker version
docker buildx version
docker compose version
```

Los tres comandos deben terminar con código `0`. Si `docker version` muestra el
cliente pero no el servidor, iniciar Docker Desktop antes de continuar.

## 4. Abrir el proyecto correctamente

1. Abrir IntelliJ IDEA.
2. Elegir **File > Open**.
3. Seleccionar la carpeta raíz
   `C:\cosme\smart-erp`, no un submódulo ni un POM individual.
4. Confirmar **Trust Project** si IntelliJ solicita confianza.
5. Cuando detecte Maven, seleccionar **Load Maven Project**.
6. Esperar a que finalice el primer índice antes de corregir referencias.

No crear un proyecto Jakarta EE nuevo ni importar por separado `kernel`,
`application` o los plugins. El `pom.xml` padre es quien define el reactor completo
y sus versiones centralizadas.

## 5. Habilitar las integraciones del IDE

Abrir **File > Settings > Plugins > Installed** y comprobar que estén habilitadas
las capacidades:

- Maven;
- Jakarta EE Platform;
- Jakarta EE: Application Servers;
- WildFly/JBoss;
- Docker.

En IntelliJ IDEA Ultimate estas integraciones se distribuyen con el IDE. Si una
aparece deshabilitada, habilitarla y reiniciar IntelliJ cuando lo pida. No instalar
una biblioteca visual, servidor o plugin de terceros para sustituir el baseline
del repositorio.

## 6. Configurar Java 21

### 6.1 SDK del proyecto

1. Abrir **File > Project Structure > Project**.
2. En **SDK**, elegir **Add SDK > JDK**.
3. Seleccionar:

   ```text
   C:\cosme\smart-erp\.tools\jdk\jdk-21.0.11+10
   ```

4. Nombrarlo, por ejemplo, `Smart ERP JDK 21.0.11`.
5. Dejar **Language level** en `SDK default` o Java 21.

### 6.2 JDK usado por Maven

Abrir **File > Settings > Build, Execution, Deployment > Build Tools > Maven**:

- **Maven home path:** `Use Maven wrapper`;
- **User settings file:** no forzar uno global;
- **Local repository:** dejar que `.mvn/maven.config` aplique
  `.tools/maven-repository`.

Luego comprobar:

- en **Maven > Importing > JDK for importer**, seleccionar el JDK 21 del proyecto;
- en **Maven > Runner > JRE**, seleccionar `Project SDK`.

El build canónico siempre usa `mvnw.cmd`. No usar un `mvn.exe` global ni el Maven
embebido del IDE para declarar un resultado de validación.

## 7. Preparar una terminal de IntelliJ

Abrir **View > Tool Windows > Terminal**. Debe iniciar en la raíz del repositorio.
Ejecutar:

```powershell
.\mvnw.cmd --version
```

Resultado esperado:

- Apache Maven `3.9.16`;
- Java `21.0.11`;
- Maven home bajo `.tools/maven-wrapper-home`;
- dependencias del proyecto bajo `.tools/maven-repository`.

El Wrapper selecciona esos recursos automáticamente bajo `.tools`, aunque Java 8
sea el valor global de Windows. No cambia Java para otros proyectos ni requiere
redefinir variables en cada terminal.

## 8. Sincronizar y comprobar el reactor

En la ventana **Maven**, pulsar **Reload All Maven Projects**. Deben aparecer el
padre y los módulos del reactor sin dependencias `javax.*`.

Como comprobación pequeña:

```powershell
.\mvnw.cmd -B -pl plugin-api -am test
```

Para validar el baseline candidato con cuatro plugins productivos, incluida la
fundación `reference_data`:

```powershell
.\mvnw.cmd -B -Pwith-inventory-demo clean verify
```

La segunda ejecución es más larga. Debe terminar con `BUILD SUCCESS`. Una prueba
que se ejecute y falle se corrige antes de continuar; no se desactiva ni se omite
para conseguir una compilación verde.

## 9. Preparar el archivo local de Compose

El archivo local no se versiona. Crearlo únicamente si todavía no existe:

```powershell
$localEnv = 'infra\compose\compose.env.local'
if (-not (Test-Path -LiteralPath $localEnv)) {
    Copy-Item -LiteralPath 'infra\compose\compose.env.example' -Destination $localEnv
} else {
    Write-Host 'Se conserva compose.env.local existente.'
}
```

Abrir `infra/compose/compose.env.local` en IntelliJ y ajustar:

```properties
LOGIXONE_HTTP_PORT=18080
LOGIXONE_OIDC_REDIRECT_URI=http://localhost:18080/logixone/*
LOGIXONE_OIDC_WEB_ORIGIN=http://localhost:18080
LOGIXONE_OIDC_POST_LOGOUT_REDIRECT_URI=http://localhost:18080/logixone/faces/app/index.xhtml
LOGIXONE_APP_IMAGE=logixone/app:j11-s6-07-closing
LOGIXONE_MIGRATOR_IMAGE=logixone/migrator:j11-s6-07-closing
```

Las cuatro direcciones públicas de aplicación deben cambiar juntas. No colocar
contraseñas, tokens ni secretos dentro de este archivo.

Si ya existe un volumen de Keycloak operativo, conservar sus URL actuales. Cambiar
las URI de un cliente OIDC existente exige actualizar de forma coherente su
configuración; no borrar el volumen para evitar ese trabajo.

## 10. Crear los secretos locales sin pisar los existentes

Smart ERP necesita cuatro archivos externos. El siguiente bloque crea solamente los
que falten y conserva cualquier valor existente:

```powershell
$secretDir = Join-Path (Get-Location).Path '.tools\secrets'
New-Item -ItemType Directory -Force -Path $secretDir | Out-Null

$secretNames = @(
    'postgres-password.txt',
    'keycloak-admin-password.txt',
    'oidc-client-secret.txt',
    'demo-user-password.txt'
)

foreach ($secretName in $secretNames) {
    $secretPath = Join-Path $secretDir $secretName
    if (-not (Test-Path -LiteralPath $secretPath)) {
        $bytes = New-Object byte[] 32
        $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
        try {
            $generator.GetBytes($bytes)
        } finally {
            $generator.Dispose()
        }
        [IO.File]::WriteAllText(
            $secretPath,
            [Convert]::ToBase64String($bytes),
            [Text.Encoding]::ASCII
        )
        Write-Host "Creado: $secretName"
    } else {
        Write-Host "Conservado: $secretName"
    }
}
```

No abrir, imprimir, copiar al portapapeles ni registrar el contenido en una
captura. El usuario de demo se llama `demo.empresas.ab`; su contraseña se consulta
privadamente en `demo-user-password.txt` solo al iniciar sesión.

## 11. Validar la configuración antes de crear recursos

```powershell
docker compose `
  --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml `
  config --quiet
```

Resultado esperado: código `0` y ninguna salida de error. Este comando valida la
estructura sin levantar contenedores ni crear volúmenes.

## 12. Construir las dos imágenes verificadas

Desde la terminal de IntelliJ:

```powershell
docker build --platform linux/amd64 `
  --build-arg LOGIXONE_BUILD_MODE=verified `
  --build-arg LOGIXONE_MAVEN_PROFILE=with-inventory-demo `
  --tag logixone/app:local-intellij `
  --file infra/docker/Dockerfile .
```

Después:

```powershell
docker build --platform linux/amd64 `
  --build-arg LOGIXONE_BUILD_MODE=verified `
  --build-arg LOGIXONE_MAVEN_PROFILE=with-inventory-demo `
  --tag logixone/migrator:local-intellij `
  --file infra/docker/Dockerfile.migrator .
```

La primera imagen contiene WildFly 41 y el WAR. La segunda contiene el ejecutable
de migraciones con el mismo conjunto físico de plugins. Usar el mismo perfil evita
que la aplicación y el migrador discrepen sobre qué plugins existen.

Comprobar los tags sin imprimir configuración sensible:

```powershell
docker image inspect logixone/app:local-intellij `
  --format '{{.Id}}'
docker image inspect logixone/migrator:local-intellij `
  --format '{{.Id}}'
```

## 13. Levantar el proyecto

```powershell
docker compose `
  --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml `
  up -d --wait --wait-timeout 180
```

Compose realiza la secuencia:

1. crea las redes y los volúmenes nombrados si no existen;
2. inicia PostgreSQL;
3. ejecuta el migrador y espera que termine correctamente;
4. inicia Keycloak y espera su health check;
5. inicia la aplicación WildFly;
6. espera que readiness indique `UP`.

El migrador puede terminar con estado `Exited (0)`. Eso es correcto: es un proceso
one-shot, no un servidor permanente.

## 14. Comprobar el estado

### 14.1 Contenedores

```powershell
docker compose `
  --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml `
  ps
```

PostgreSQL, Keycloak y app deben estar saludables. Migrator debe haber terminado
con código `0`.

### 14.2 Salud semántica

```powershell
$live = Invoke-RestMethod `
  'http://localhost:18080/logixone/health/live'
$ready = Invoke-RestMethod `
  'http://localhost:18080/logixone/health/ready'

$live.status
$ready.status
```

Ambos deben mostrar `UP`. Liveness indica que el proceso puede responder;
readiness comprueba además catálogo, configuración, base, migraciones y coherencia
OIDC local.

### 14.3 Interfaz

Abrir en el navegador:

```text
http://localhost:18080/logixone/faces/app/index.xhtml
```

Keycloak debe solicitar autenticación. Usar el usuario ficticio
`demo.empresas.ab` y leer localmente la contraseña ya creada, sin compartirla.

## 15. Administrar Docker desde IntelliJ

### 15.1 Conexión Docker

1. Abrir **File > Settings > Build, Execution, Deployment > Docker**.
2. Agregar una conexión **Docker for Windows**.
3. Esperar el mensaje de conexión satisfactoria.
4. Abrir **View > Tool Windows > Services** para ver imágenes, contenedores, logs y
   estado.

### 15.2 Configuración Docker Compose

Se puede agregar una configuración:

1. **Run > Edit Configurations > + > Docker Compose**.
2. Nombre: `Smart ERP - Compose`.
3. Compose file: `infra/compose/compose.yaml`.
4. Environment file: `infra/compose/compose.env.local`.
5. Servicios: todos.
6. Comando: `up`.

La terminal documentada sigue siendo la referencia para una evidencia reproducible,
porque muestra de forma explícita archivo, variables y timeout. La configuración
visual es un atajo para el trabajo diario.

### 15.3 Configuración Maven útil

Crear una configuración **Maven**:

- nombre: `Smart ERP - Verify demo`;
- directorio: raíz del repositorio;
- comando: `-B -Pwith-inventory-demo clean verify`;
- Maven: Wrapper;
- JRE: Project SDK 21.

Para depurar una prueba unitaria, abrir la clase y usar el icono de ejecución del
margen. Los breakpoints funcionan sin exponer puertos de depuración del contenedor.

## 16. Ciclo diario de desarrollo

Después de cambiar un único módulo:

```powershell
.\mvnw.cmd -B -pl <modulo> -am test
```

Al completar un corte coherente:

```powershell
.\mvnw.cmd -B -Pwith-inventory-demo clean verify
```

Si el cambio debe observarse en la aplicación:

1. reconstruir las imágenes afectadas con el mismo perfil;
2. validar Compose;
3. recrear la composición;
4. verificar health y luego la pantalla.

```powershell
docker compose `
  --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml `
  up -d --force-recreate --wait --wait-timeout 180
```

No copiar manualmente un JAR dentro del contenedor: agregar o retirar físicamente un
plugin requiere reconstruir y redesplegar la distribución.

## 17. Menú y permisos al agregar plugins

Cuando una nueva imagen contiene otro plugin, el kernel descubre y valida sus
contribuciones. El shell compone un único menú con las entradas de todos los
plugins que simultáneamente:

- estén físicamente presentes;
- sean compatibles;
- estén activos para la empresa seleccionada;
- tengan sus dependencias activas;
- publiquen una entrada de menú;
- y cuyo permiso requerido tenga el usuario.

Por eso no se fusionan archivos XHTML ni menús a mano. Un plugin inactivo o un
usuario sin permiso no debe ver ni poder invocar su funcionalidad.

Los permisos se administran en:

```text
http://localhost:18080/logixone/faces/admin/security.xhtml
```

Esa pantalla permite administrar usuarios, membresías, roles empresariales y
permisos. La autoridad global de administración se gestiona por separado en:

```text
http://localhost:18080/logixone/faces/admin/system-authority.xhtml
```

Ocultar una opción no reemplaza la seguridad: cada operación vuelve a comprobar en
el servidor la empresa, el plugin y el permiso efectivo.

## 18. Detener sin perder datos

```powershell
docker compose `
  --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml `
  down
```

`down` elimina contenedores y redes recreables, pero conserva los volúmenes
nombrados de PostgreSQL y Keycloak. Al ejecutar nuevamente `up`, Compose reutiliza
esos volúmenes; el migrador reconoce las versiones y checksums ya aplicados y no
recrea ni pisa los datos existentes.

> **No agregar `--volumes`.** Esa opción elimina los datos de PostgreSQL y el
> estado local de Keycloak. Solo corresponde a un ambiente explícitamente efímero,
> identificado y autorizado, después de verificar el respaldo.

## 19. Diagnóstico

### Docker no está disponible

- comprobar que Docker Desktop esté iniciado;
- revisar la conexión en **Settings > Docker**;
- ejecutar `docker version` y verificar que muestre cliente y servidor.

### IntelliJ usa Java incorrecto

- revisar Project SDK, Maven Importer y Maven Runner;
- confirmar con `.\mvnw.cmd --version` que el Wrapper usa Java 21 desde `.tools`;
- si el JDK local existe pero el Wrapper usa otro, tratarlo como una regresión del
  bootstrap, no como una preparación manual pendiente de la terminal.

### Maven descarga fuera de `.tools`

- ejecutar siempre el Wrapper del repositorio;
- confirmar que `mvnw.cmd --version` informa Maven home bajo
  `.tools/maven-wrapper-home`;
- confirmar que `.mvn/maven.config` conserva
  `-Dmaven.repo.local=.tools/maven-repository`.

### Compose informa variables o secretos ausentes

- comprobar que existe `infra/compose/compose.env.local`;
- comprobar solo los nombres de archivo bajo `.tools/secrets/`;
- no imprimir sus contenidos ni copiarlos al archivo `.env`.

### Migrator falla

```powershell
docker compose `
  --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml `
  logs --no-color postgres migrator
```

Corregir imagen, configuración o migración. No editar a mano las tablas de historial
ni cambiar una migración ya aplicada.

### Liveness está UP pero readiness está DOWN

```powershell
docker compose `
  --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml `
  logs --no-color postgres migrator keycloak app
```

Revisar el componente marcado `DOWN` en readiness. Es normal que liveness siga
verde cuando una dependencia impide atender tráfico.

### La aplicación redirige mal después de cambiar el puerto

Comprobar juntas `LOGIXONE_HTTP_PORT`, `LOGIXONE_OIDC_REDIRECT_URI`,
`LOGIXONE_OIDC_WEB_ORIGIN` y
`LOGIXONE_OIDC_POST_LOGOUT_REDIRECT_URI`. Un realm ya creado conserva su cliente;
no eliminar su volumen como primera solución.

### Falta una opción del menú

Comprobar, en este orden:

1. que la imagen contenga el plugin;
2. que el catálogo sea válido;
3. que el plugin esté activo para la empresa;
4. que sus dependencias estén activas;
5. que el usuario tenga membresía y permiso;
6. que se haya seleccionado la empresa correcta.

### Logs completos

```powershell
docker compose `
  --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml `
  logs --no-color postgres migrator keycloak app
```

No adjuntar volcados de variables, tokens ni contenidos de `/run/secrets`.

## 20. Uso opcional de un WildFly local

IntelliJ Ultimate 2026.2 permite crear una configuración **JBoss/WildFly Server**.
Esa integración sirve para despliegue o depuración local, pero el repositorio no
entrega actualmente una configuración local equivalente a la imagen oficial:
datasource, driver, CLI, OIDC, estabilidad preview, secretos y migraciones deben
coincidir exactamente.

Por lo tanto:

- no es necesaria para levantar el proyecto;
- no sustituye Docker/Compose en pruebas, demo o evidencia;
- no debe configurarse improvisando credenciales o habilitando actualización
  automática de Hibernate;
- solo debe adoptarse cuando exista un runbook versionado que reproduzca el
  baseline completo.

## 21. Checklist final

- [ ] Abrí la raíz del reactor Maven.
- [ ] Project SDK, Maven Importer y Maven Runner usan Java 21.
- [ ] Maven usa el Wrapper y las cachés del proyecto bajo `.tools`.
- [ ] Docker Desktop responde desde IntelliJ y PowerShell.
- [ ] `compose.env.local` existe y no contiene secretos.
- [ ] Los cuatro secretos existen y no fueron sobrescritos.
- [ ] Compose valida con código `0`.
- [ ] Las imágenes app y migrator usan `with-inventory-demo`.
- [ ] Migrator terminó con código `0`.
- [ ] PostgreSQL, Keycloak y app están saludables.
- [ ] Liveness y readiness responden `UP`.
- [ ] La interfaz abre y redirige a Keycloak.
- [ ] Sé detener con `down` sin eliminar volúmenes.

## 22. Reversión segura

Si el arranque no debe continuar:

1. guardar logs sin secretos;
2. ejecutar `docker compose ... down` sin `--volumes`;
3. conservar `compose.env.local`, los cuatro secretos y ambos volúmenes;
4. corregir la causa;
5. reconstruir una imagen identificada y repetir validación, migrador y health.

No usar `git reset --hard`, no borrar `.tools`, no eliminar volúmenes y no modificar
la base manualmente para ocultar el fallo.

## 23. Fuentes oficiales de IntelliJ IDEA

Consultadas el 2026-07-30:

- [IntelliJ IDEA 2026.2 — publicación y correcciones](https://blog.jetbrains.com/idea/2026/07/whats-fixed-intellij-idea-2026-2/)
- [Importar un proyecto o módulo](https://www.jetbrains.com/help/idea/import-project-or-module-wizard.html)
- [Configuración de importación Maven](https://www.jetbrains.com/help/idea/maven-importing.html)
- [Soporte Maven](https://www.jetbrains.com/help/idea/maven-support.html)
- [Integración con servidores de aplicación](https://www.jetbrains.com/help/idea/configuring-and-managing-application-server-integration.html)
- [Configuración JBoss/WildFly](https://www.jetbrains.com/help/idea/run-debug-configuration-jboss-server.html)
- [Configuraciones Docker y Docker Compose](https://www.jetbrains.com/help/idea/docker-run-configurations.html)

## 24. Documentación relacionada del repositorio

- [Construcción local con Maven Wrapper](build-local.md)
- [Construcción de imágenes](docker-build.md)
- [Operación de Compose](compose.md)
- [Migrador](migrator.md)
- [Keycloak y OIDC](keycloak-oidc.md)
- [Demo visual de cierre de Sprint 6](demo-cierre-sprint-06.md)
- [Guía de implementación por empresa](../implementation-guide/README.md)
- [Metodología del instalador Windows](metodologia-instalador-windows-cierre-sprint.md)
- [Demo del instalador interno de Sprint 8](demo-instalador-windows-sprint-08.md)
