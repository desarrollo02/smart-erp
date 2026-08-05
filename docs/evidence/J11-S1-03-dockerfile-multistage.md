# J11-S1-03 — Dockerfile multi-stage

- Fecha: 2026-07-23
- Estado: Completado para el incremento Dockerfile
- Ambiente: Windows 11, Docker Engine 29.6.2, BuildKit 0.31.2, `linux/amd64`
- Alcance: builder Maven y runtime WildFly; Compose, migrador y PostgreSQL siguen pendientes

## Archivos de infraestructura

- `infra/docker/Dockerfile`: etapas `builder` y `runtime`.
- `infra/docker/unzip-with-jar`: extracción reproducible del ZIP de Maven usando el JDK.
- `.dockerignore`: excluye `.tools/`, `target/`, VCS, documentación, configuración local y secretos.

El POM padre también cambió para desactivar el descriptor Maven generado automáticamente dentro de JAR y WAR. Esta metadata contenía finales de línea dependientes del sistema operativo y era la única causa observada de divergencia binaria entre Windows y Linux.

## Diseño verificado

### Builder

- base Temurin 21.0.11+10 Noble fijada por digest `linux/amd64`;
- Maven Wrapper 3.3.4 y Maven 3.9.16;
- `mvnw -B verify` como parte obligatoria del build;
- caché BuildKit versionada `logixone-maven-tools-3.9.16` montada en `/workspace/.tools`;
- comprobación explícita de que `distribution/logixone-war/target/logixone.war` existe y no está vacío.

La caché es un montaje BuildKit: su contenido no se incorpora a la capa builder ni al runtime. La imagen builder inspeccionada tenía cero entradas bajo `/workspace/.tools`.

### Runtime

- base WildFly 41.0.0.Final con JDK 21 fijada por digest `linux/amd64`;
- solo recibe `logixone.war` desde el builder;
- WAR propiedad de `jboss:root`, modo `0644`;
- usuario configurado `jboss`, UID efectivo 1000;
- puerto 8080 expuesto;
- sin `/workspace`, `.tools`, Maven ni código fuente del builder;
- etiquetas OCI de título, descripción y versión.

## Comandos principales

```powershell
docker buildx build --check --platform linux/amd64 `
  --file infra/docker/Dockerfile .

docker buildx build --pull --no-cache --platform linux/amd64 `
  --target builder --load `
  --tag logixone/builder:j11-s1-03 `
  --file infra/docker/Dockerfile .

docker buildx build --pull --platform linux/amd64 `
  --target runtime --load `
  --tag logixone/app:j11-s1-03 `
  --file infra/docker/Dockerfile .
```

## Resultados

| Control | Resultado |
|---|---|
| Análisis nativo de Dockerfile | Sin advertencias |
| Contexto builder observado | 10,22 KB; sin `.tools` ni `target` del host |
| Maven en Linux | 3.9.16 |
| Java en Linux | Temurin 21.0.11+10 |
| Reactor en builder limpio | `BUILD SUCCESS`, 14/14 |
| Imagen builder | `sha256:7cdc8e91064e823b6c83a2b90094be1dab3a351dcc215096b701cf790f0c22dc` |
| Tamaño builder | 210.866.787 bytes |
| Entradas `.tools` en builder | 0 |
| Maven host después del cambio | `BUILD SUCCESS`, 14/14 |
| WAR Windows y Linux | SHA-256 idéntico |
| WAR canónico actual | `23C935CEB30AB75CCDD72A9CF96658E6A0B63B50B4A552AE908D7B7F1488BD39` |
| Tamaño runtime | 498.441.704 bytes |
| Usuario runtime | `jboss`, UID 1000 |
| Historial con secretos | 0 coincidencias |
| Despliegue WildFly | `logixone.war` desplegado |
| Arranque WildFly | 41.0.0.Final, 6.813 ms |
| Smoke HTTP | 403, respuesta esperada para WAR todavía vacío |
| Errores de servidor | 0 |
| Contenedor smoke restante | 0; fue eliminado |

## Reproducibilidad del WAR

Antes de la corrección:

- WAR Windows: `85C9BC9F5E2D0926C59E0362A0E88AB37D7BC3D7D71B8DF73D653860D4E86200`;
- WAR Linux: `64BCFC8DA41FE4F52569F23CF822337BB66C727C974B9DE2CE8B981DD6C32E8C`.

Ambos contenían las mismas 13 rutas. La diferencia provenía de `META-INF/maven/pom.properties` y, por transitividad, de los JAR internos. Se configuró `addMavenDescriptor=false` en Maven JAR Plugin y Maven WAR Plugin siguiendo la [guía oficial de configuración de archivos Maven](https://maven.apache.org/guides/mini/guide-archive-configuration.html).

Después de la corrección, Windows y Linux produjeron exactamente `23C935CEB30AB75CCDD72A9CF96658E6A0B63B50B4A552AE908D7B7F1488BD39`.

Consecuencia aceptada: los archivos generados ya no incorporan `META-INF/maven/pom.xml` ni `pom.properties`. Las coordenadas continúan declaradas en los POM del reactor y los nombres de artefactos.

## Incidencias y correcciones

1. Un primer comando fue cortado por un timeout de 5 segundos antes de producir una imagen. Se repitió con una ventana suficiente.
2. Temurin no incluía `unzip`; el Wrapper cambió a `tar.gz` y rechazó correctamente el checksum porque estaba fijado para el ZIP. Se añadió `unzip-with-jar`, sin instalar paquetes ni relajar el checksum.
3. `jar` no restauró el bit ejecutable de Maven. El adaptador pasó a aplicar `0755` únicamente a `bin/mvn`, `bin/mvnDebug` y `bin/mvnyjp`.
4. La extracción fallida dejó una caché BuildKit no reutilizable. Se aisló mediante el identificador versionado `logixone-maven-tools-3.9.16`; no se ejecutó una purga global.
5. La primera comparación ZIP usó por error la variable reservada `$Host`. El verificador se corrigió y se repitió antes de extraer conclusiones.
6. La primera inspección runtime tuvo una expresión de shell mal escapada. La inspección simplificada pasó completamente.
7. El primer smoke esperaba el texto antiguo `WildFly Full`; los logs reales usan `WildFly 41.0.0.Final`. El gate corregido pasó y ambos contenedores temporales fueron eliminados.

Ninguna prueba se desactivó. Cada causa se corrigió y el gate afectado se repitió.

## Identidad OCI y procedencia

Dos exportaciones sin cambios conservaron:

- manifiesto runtime: `sha256:2e4a3810d2cd9d3633b4c6e0e48c63a6c1001c7cd8147af86aec5174de1befd1`;
- configuración runtime: `sha256:e781873b11559e4f5bb40ce8be434a8006b668fa4a8c74aa7eed254d7e91fb53`.

El índice local superior cambió de `sha256:4d96...` a `sha256:c812...` porque BuildKit adjuntó una nueva atestación de procedencia específica de cada ejecución. Esto refuerza la política aprobada: la imagen que se publique se probará y promoverá por un único digest de registro, sin reconstruirla por ambiente.

## Pendientes deliberados

- No existe todavía Compose.
- No se construyó aún la imagen del migrador.
- No se descargó ni arrancó PostgreSQL.
- El WAR todavía no tiene endpoints funcionales; por eso el smoke HTTP esperado es 403.
- Health checks, migración, configuración y persistencia pertenecen a los siguientes incrementos de `J11-S1-03` y `J11-S1-06` según su alcance.

## Siguiente gate

Declarar Compose con `postgres`, `migrator` y `app`, configuración externa sin secretos y volumen persistente; su primer control será `docker compose config --quiet` antes de arrancar servicios.

