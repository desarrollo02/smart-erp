# J11-S1-02 — Relocalización de descargas dentro del proyecto

- Fecha: 2026-07-23
- Estado: Completado
- Ambiente: Windows 11, PowerShell

## Traslado

Se trasladaron seis elementos desde `C:\tmp` hacia `C:\cosme\LogixoneJakarta11\.tools`:

| Origen anterior | Destino dentro del proyecto |
|---|---|
| `OpenJDK21U-jdk_x64_windows_hotspot_21.0.11_10.zip` | `.tools/downloads/` |
| `apache-maven-3.9.16-bin.zip` | `.tools/downloads/` |
| `logixone-jdk21` | `.tools/jdk/` |
| `logixone-maven-wrapper-verified` | `.tools/maven-wrapper-home/` |
| `logixone-maven-wrapper-home` | `.tools/cache/maven-wrapper-bootstrap/` |
| `logixone-maven-repository` | `.tools/maven-repository/` |

Resultado del traslado:

- elementos movidos: 6;
- rutas originales restantes: 0;
- destinos ausentes: 0;
- bytes conservados dentro de `.tools/`: 613.651.216.

## Integridad

- JDK ZIP SHA-256: `D3625E7CADF23787EA540229544B6E2AB494B3B54DA1801879E583E1DFEE0A64`.
- Maven ZIP SHA-256: `5AF3B743DD8B876B5C45DA33B676251E5F1687712644ABB4EE519CA56E1D89CE`.

Ambos valores coinciden con los verificados antes del traslado.

## Build desde herramientas locales

Variables de proceso utilizadas:

```powershell
$projectRoot = 'C:\cosme\LogixoneJakarta11'
$env:JAVA_HOME = Join-Path $projectRoot '.tools\jdk\jdk-21.0.11+10'
$env:MAVEN_USER_HOME = Join-Path $projectRoot '.tools\maven-wrapper-home'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd -B verify
```

Resultados:

- Maven 3.9.16: correcto;
- Java 21.0.11: correcto;
- reactor: `BUILD SUCCESS`, 14/14 proyectos;
- repositorio Maven efectivo: `C:\cosme\LogixoneJakarta11\.tools\maven-repository`;
- WAR SHA-256: `85C9BC9F5E2D0926C59E0362A0E88AB37D7BC3D7D71B8DF73D653860D4E86200`.

## Protección

- `.tools/` está ignorado por Git.
- `.dockerignore` excluye `.tools/` del contexto de construcción de Docker.
- `.mvn/maven.config` dirige dependencias Maven a `.tools/maven-repository`.
- `AGENTS.md` prohíbe dejar descargas del proyecto fuera de `.tools/`.
- `.tools/` no debe incluirse en Docker ni en artefactos.
