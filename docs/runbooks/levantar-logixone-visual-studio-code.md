# Levantar Smart ERP con Visual Studio Code

- Edición: 0.9
- Fecha de actualización: 2026-08-14
- Entorno de referencia: Windows 11 y PowerShell
- Baseline: Java 21, Maven Wrapper 3.9.16, Docker/Compose, PostgreSQL, Keycloak
  26.7.0 y WildFly 41
- Perfil funcional actual: `with-purchasing-demo`
- Estado de validación: revisado contra J11-S9-07/J11-S9-08; Maven,
  Docker/Compose, migraciones, health, OIDC, Playwright y construcción del
  instalador verdes; instalación limpia de VS Code, matriz Windows y validación
  independiente pendientes

> **Instalador interno disponible:** `installer/windows/current/` contiene
> `0.9.0-internal.1`. Diagnostica antes del consentimiento, conserva la marca
> Logixone, está sin Authenticode y no puede entregarse a una empresa. Su
> instalación real sigue pendiente en VM compatible. Esta guía manual
> continúa siendo la fuente para preparar un entorno de desarrollo en VS Code.

## 1. Resultado esperado

Esta guía permite recibir el repositorio, abrir correctamente el reactor Maven,
instalar las extensiones necesarias, construir con el Wrapper y ejecutar el sistema
completo mediante Docker Compose.

El instalador y esta guía tienen objetivos distintos: el instalador prepara una
demo/ejecución local; VS Code prepara el repositorio para desarrollar, depurar y
probar. Ejecutar el instalador no instala las extensiones del editor ni sustituye
los gates Maven.

Al terminar deben responder:

- aplicación: `http://localhost:18080/logixone/faces/app/index.xhtml`;
- administración: `http://localhost:18080/logixone/faces/admin/index.xhtml`;
- liveness: `http://localhost:18080/logixone/health/live`;
- readiness: `http://localhost:18080/logixone/health/ready`;
- Keycloak local: `http://keycloak.localhost:8180`.

Visual Studio Code se usa para editar, navegar, ejecutar el Wrapper, consultar
pruebas y observar contenedores. La ejecución oficial sigue siendo
`infra/compose/compose.yaml`; no se instala un WildFly paralelo ni se copian JAR
manualmente desde el editor.

## 2. Prerrequisitos

Antes de abrir el proyecto:

- instalar la versión estable vigente de Visual Studio Code;
- iniciar Docker Desktop con contenedores Linux;
- disponer de PowerShell y Git;
- ubicar el repositorio en `C:\cosme\smart-erp`;
- dejar libres los puertos `18080` y `8180`;
- conservar espacio para `.tools/`, imágenes y volúmenes Docker;
- tener acceso inicial a los registros de dependencias e imágenes si las cachés
  todavía no existen.

Comprobar Docker:

```powershell
docker version
docker buildx version
docker compose version
```

Los tres comandos deben terminar con código `0`; `docker version` debe mostrar
cliente y servidor.

## 3. Extensiones que debe tener el equipo

Instálelas desde **View > Extensions** (`Ctrl+Shift+X`) buscando el identificador
exacto. No instale extensiones Spring, Quarkus o un servidor WildFly para sustituir
el baseline Jakarta EE del repositorio.

| Obligación | Extensión | Identificador | Editor | Uso en Smart ERP |
|---|---|---|---|---|
| requerida | Extension Pack for Java | `vscjava.vscode-java-pack` | Microsoft | lenguaje Java, navegación, depuración, JUnit, Maven y proyectos |
| requerida | Container Tools | `ms-azuretools.vscode-containers` | Microsoft | Dockerfiles, Compose, imágenes, contenedores y logs |
| requerida | XML | `redhat.vscode-xml` | Red Hat | POM, `persistence.xml`, `beans.xml` y XHTML/XML |
| requerida | YAML | `redhat.vscode-yaml` | Red Hat | validación y edición de Compose y configuración YAML |

Instalación equivalente por terminal:

```powershell
code --install-extension vscjava.vscode-java-pack
code --install-extension ms-azuretools.vscode-containers
code --install-extension redhat.vscode-xml
code --install-extension redhat.vscode-yaml
```

El paquete Java ya incluye Language Support for Java, Debugger for Java, Test
Runner for Java, Maven for Java y Project Manager for Java. No hace falta instalar
por separado esos componentes.

## 4. Abrir la raíz correcta

1. Abra Visual Studio Code.
2. Elija **File > Open Folder**.
3. Seleccione `C:\cosme\smart-erp`, no `plugins/`, `web-shell/` ni un
   `pom.xml` aislado.
4. Confirme confianza sólo si reconoce el repositorio.
5. Espere a que el indicador Java termine de importar los proyectos Maven.
6. Abra **Maven** y confirme que aparecen el padre y los módulos del reactor.

Si Java queda en modo liviano, abra la paleta (`Ctrl+Shift+P`) y ejecute
**Java: Switch to Standard Mode**. No genere un proyecto Java nuevo: el reactor ya
está definido por el POM padre.

## 5. Usar el JDK y las cachés del proyecto

Abra **Terminal > New Terminal** desde la raíz y ejecute:

```powershell
.\mvnw.cmd --version
```

El Wrapper selecciona automáticamente el JDK y su caché Maven bajo `.tools`, aun
si Windows tiene Java 8 global. No es necesario redefinir variables en cada
terminal.

Resultado esperado:

- Maven 3.9.16;
- Java 21.0.11;
- distribución del Wrapper bajo `.tools/maven-wrapper-home`;
- repositorio Maven bajo `.tools/maven-repository` por `.mvn/maven.config`.

Para que el lenguaje Java use el mismo JDK:

1. abra la paleta con `Ctrl+Shift+P`;
2. elija **Java: Configure Java Runtime**;
3. asigne al workspace el JDK de `.tools\jdk\jdk-21.0.11+10`;
4. ejecute **Java: Clean Java Language Server Workspace** sólo si el índice quedó
   inconsistente después de cambiar el JDK.

No versione una ruta absoluta personal dentro de `.vscode/settings.json` y no use
un `mvn.exe` global para declarar una validación oficial.

## 6. Verificar el reactor antes de levantar servicios

Prueba pequeña:

```powershell
.\mvnw.cmd -B -pl plugin-api -am test
```

Baseline candidato con Compras y sus dependencias:

```powershell
.\mvnw.cmd -B -Pwith-purchasing-demo clean verify
```

La vista **Testing** permite lanzar o depurar casos JUnit individuales, pero el
resultado canónico del corte se obtiene con el Wrapper. Si una prueba ejecutada
falla, se corrige antes de continuar.

## 7. Preparar configuración local sin sobrescribirla

Crear `compose.env.local` sólo si falta:

```powershell
$localEnv = 'infra\compose\compose.env.local'
if (-not (Test-Path -LiteralPath $localEnv)) {
    Copy-Item -LiteralPath 'infra\compose\compose.env.example' -Destination $localEnv
} else {
    Write-Host 'Se conserva compose.env.local existente.'
}
```

Edite únicamente valores no sensibles. Para este recorrido:

```properties
LOGIXONE_HTTP_PORT=18080
LOGIXONE_OIDC_REDIRECT_URI=http://localhost:18080/logixone/*
LOGIXONE_OIDC_WEB_ORIGIN=http://localhost:18080
LOGIXONE_OIDC_POST_LOGOUT_REDIRECT_URI=http://localhost:18080/logixone/faces/app/index.xhtml
LOGIXONE_APP_IMAGE=logixone/app:local-vscode
LOGIXONE_MIGRATOR_IMAGE=logixone/migrator:local-vscode
```

Las cuatro URL públicas de aplicación cambian juntas. Si un volumen existente de
Keycloak ya fue creado con otro puerto, primero siga el runbook OIDC; no borre el
volumen para evitar una corrección de configuración.

## 8. Crear secretos sólo cuando falten

Smart ERP monta cuatro archivos externos. Este bloque conserva los existentes y
crea valores aleatorios únicamente para nombres ausentes:

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
        try { $generator.GetBytes($bytes) } finally { $generator.Dispose() }
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

No abra, imprima, capture ni copie el contenido. Los secretos y el archivo local
están excluidos del control de versiones.

## 9. Validar Compose antes de crear recursos

```powershell
docker compose `
  --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml `
  config --quiet
```

Debe terminar con código `0` y sin errores. Esto todavía no crea contenedores ni
volúmenes.

## 10. Construir aplicación y migrador con la misma selección

```powershell
docker build --platform linux/amd64 `
  --build-arg LOGIXONE_BUILD_MODE=verified `
  --build-arg LOGIXONE_MAVEN_PROFILE=with-purchasing-demo `
  --tag logixone/app:local-vscode `
  --file infra/docker/Dockerfile .
```

```powershell
docker build --platform linux/amd64 `
  --build-arg LOGIXONE_BUILD_MODE=verified `
  --build-arg LOGIXONE_MAVEN_PROFILE=with-purchasing-demo `
  --tag logixone/migrator:local-vscode `
  --file infra/docker/Dockerfile.migrator .
```

No mezcle perfiles: el WAR y el migrador deben descubrir los mismos plugins y
migraciones.

## 11. Levantar el sistema

```powershell
docker compose `
  --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml `
  up -d --wait --wait-timeout 240
```

Compose crea los volúmenes nombrados sólo si no existen, inicia PostgreSQL, ejecuta
el migrador one-shot, inicia Keycloak y luego WildFly. `migrator` en
`Exited (0)` es normal.

Compruebe:

```powershell
docker compose `
  --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml `
  ps

$live = Invoke-RestMethod 'http://localhost:18080/logixone/health/live'
$ready = Invoke-RestMethod 'http://localhost:18080/logixone/health/ready'
$live.status
$ready.status
```

Ambos estados deben ser `UP`. Después abra la URL de aplicación. En un ambiente
nuevo, el aprovisionamiento de empresas, autoridad, activaciones y permisos se
realiza con la [guía de implementación](../implementation-guide/README.md); que el
plugin esté dentro de la imagen no lo activa para una empresa.

## 12. Trabajar desde las vistas de VS Code

- **Maven:** explore módulos y objetivos, pero ejecute los gates oficiales desde
  la terminal con `mvnw.cmd`.
- **Testing:** ejecute o depure un JUnit focalizado mediante los iconos de la clase
  o del método.
- **Run and Debug:** use breakpoints en pruebas unitarias. El contenedor oficial no
  publica actualmente un puerto de depuración remota.
- **Container Explorer:** consulte estado y logs. No use **Prune System** ni borre
  volúmenes del proyecto como acción de diagnóstico.
- **Problems:** revise Java, XML y YAML antes de construir.

## 13. Ciclo diario

Después de un cambio coherente en un módulo:

```powershell
.\mvnw.cmd -B -pl <modulo> -am test
```

Al completar el corte:

```powershell
.\mvnw.cmd -B -Pwith-purchasing-demo clean verify
```

Si el cambio afecta la distribución, reconstruya ambas imágenes con el mismo
perfil, ejecute dos veces el migrador cuando corresponda y recree `app`. Nunca
copie un JAR manualmente dentro de WildFly.

## 14. Detener sin perder datos

```powershell
docker compose `
  --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml `
  down
```

No agregue `--volumes`. `down` elimina contenedores y redes recreables, pero
conserva PostgreSQL y Keycloak. El siguiente `up` reutiliza los volúmenes y Flyway
reconoce versión y checksum sin volver a aplicar migraciones ni pisar datos.

## 15. Diagnóstico rápido

### Java o Maven incorrectos

Confirme `.\mvnw.cmd --version`: debe usar Java 21 y Maven home bajo `.tools`. Si
no ocurre y el JDK local existe, es una regresión del Wrapper y no debe corregirse
redefiniendo variables en cada terminal. Configure además el runtime Java del
workspace para que el lenguaje y el build coincidan.

### El reactor no termina de importar

Confirme que abrió la raíz, espere la importación Maven y ejecute
**Java: Clean Java Language Server Workspace**. No abra cada plugin como workspace
separado.

### Docker no aparece

Confirme `docker version`, que Docker Desktop esté iniciado y que Container Tools
use Docker como runtime. La extensión no reemplaza el Docker Engine.

### Migrator o readiness falla

```powershell
docker compose `
  --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml `
  logs --no-color postgres migrator keycloak app
```

Revise el componente `DOWN`. No edite la tabla de historial de Flyway, una
migración aplicada ni datos privados para forzar el arranque.

### Falta un menú

Compruebe en orden: presencia física, compatibilidad, dependencia, activación para
la empresa, membresía, permiso y empresa seleccionada. El shell fusiona
contribuciones autorizadas; no fusiona XHTML manualmente.

## 16. Checklist de montaje

- [ ] Abrí la raíz del reactor.
- [ ] Instalé las cuatro extensiones por su identificador exacto.
- [ ] VS Code y el Wrapper usan Java 21.
- [ ] Maven usa las cachés de `.tools/`.
- [ ] `compose.env.local` existe y no contiene secretos.
- [ ] Los cuatro secretos existen y ninguno fue sobrescrito.
- [ ] Compose valida con código `0`.
- [ ] Aplicación y migrador usan `with-purchasing-demo`.
- [ ] Migrator terminó con código `0`.
- [ ] PostgreSQL, Keycloak y app están saludables.
- [ ] Liveness y readiness responden `UP`.
- [ ] Sé detener con `down` sin eliminar volúmenes.

## 17. Fuentes oficiales consultadas

Consultadas el 2026-07-31:

- [Java en Visual Studio Code](https://code.visualstudio.com/docs/java/java-tutorial)
- [Gestión de proyectos Java](https://code.visualstudio.com/docs/java/java-project)
- [Maven y herramientas de build Java](https://code.visualstudio.com/docs/java/java-build)
- [Pruebas Java](https://code.visualstudio.com/docs/java/java-testing)
- [Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack)
- [Containers en Visual Studio Code](https://code.visualstudio.com/docs/containers/overview)
- [Container Tools](https://marketplace.visualstudio.com/items?itemName=ms-azuretools.vscode-containers)
- [XML de Red Hat](https://marketplace.visualstudio.com/items?itemName=redhat.vscode-xml)
- [YAML de Red Hat](https://marketplace.visualstudio.com/items?itemName=redhat.vscode-yaml)

## 18. Documentación relacionada

- [Guía para IntelliJ IDEA Ultimate](levantar-logixone-intellij-idea-ultimate.md)
- [Construcción local](build-local.md)
- [Construcción Docker](docker-build.md)
- [Compose](compose.md)
- [Migrador](migrator.md)
- [Keycloak y OIDC](keycloak-oidc.md)
- [Manual técnico para desarrolladores](../developer-guide/README.md)
- [Guía de implementación por empresa](../implementation-guide/README.md)
- [Metodología del instalador Windows](metodologia-instalador-windows-cierre-sprint.md)
- [Demo del instalador interno de Sprint 8](demo-instalador-windows-sprint-08.md)
