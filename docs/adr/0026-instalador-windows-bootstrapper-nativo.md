# ADR-0026 - Bootstrapper Windows nativo, manifiesto y entrega interna

> ADR-0029 modificó la obligatoriedad de regenerarlo en cada Sprint. Esta decisión
> técnica continúa vigente cuando producto responde `SÍ` en el cierre.

- Estado: Aceptado
- Fecha: 2026-08-01
- Decisores: responsable de producto y arquitectura de Logixone
- Alcance: J11-S8-08 y regeneraciones posteriores del instalador Windows

## Contexto

Desde Sprint 8, el cierre exige un instalador Windows reproducible que diagnostique
la máquina antes de modificarla, explique el plan, solicite consentimiento y eleve
solo cuando una acción lo necesite. La ejecución oficial de Logixone usa
Docker/Compose; no corresponde instalar otro WildFly, PostgreSQL o Keycloak en el
host.

El artefacto de Sprint 8 debe permanecer ligado a los digests congelados en
J11-S8-07. La primera edición es para desarrollo y demostración interna. No existe
todavía certificado Authenticode ni un repositorio público de releases.

## Decisión

### Tecnología y empaquetado

El bootstrapper será una aplicación Windows Forms compilada contra .NET Framework
4.8, presente como baseline de Windows 11. Se construirá con el compilador de
.NET Framework disponible en Windows y no agregará un runtime privado. La lógica
de diagnóstico y planificación quedará separada de la interfaz y ofrecerá una
salida JSON para pruebas y soporte.

La edición se compone de un ejecutable, manifiesto, payload, licencias, hash y
manual declarados. Se construye primero en un directorio temporal y se promueve al
directorio exclusivo `installer/windows/current` solo después de verificar todos
los archivos. `current` contiene una sola edición; fuentes, pruebas y evidencias
permanecen fuera de él.

### Compatibilidad inicial

- Windows 11 x64, versión 24H2/build 26100 o superior y todavía soportada por
  Microsoft;
- CPU x64 con virtualización y SLAT;
- 8 GiB de RAM como mínimo y 16 GiB recomendados;
- 30 GiB libres como mínimo y 60 GiB recomendados en la unidad de instalación;
- WSL 2.1.5 o superior;
- Docker Desktop 4.84.0 para una instalación nueva, backend WSL2 y contenedores
  Linux;
- Docker Engine 29.6 y Compose 5.3 como mínimos funcionales del baseline;
- puertos loopback 18080 y 8180 libres o pertenecientes a la instalación Logixone
  detectada.

Windows 10, Windows Server, ARM64 y Linux quedan fuera del soporte inicial. La
matriz versionada, no la interfaz, es la fuente de estos valores.

### Perfiles y adquisición

La primera edición ofrece un único perfil `demo-local`:

1. reutiliza Docker/WSL compatibles;
2. si Docker falta, descarga Docker Desktop 4.84.0 desde el origen oficial,
   verifica SHA-256 y presenta sus términos antes de ejecutarlo en modo por
   usuario;
3. si WSL o una característica de Windows necesita habilitarse, solicita UAC
   justo antes de esa operación y admite un reinicio seguro;
4. instala el payload versionado de Logixone en el perfil del usuario;
5. construye o carga las imágenes exactas, verifica los digests de aplicación y
   migrador, ejecuta Compose y comprueba migración, liveness y readiness.

Visual Studio Code es opcional y solo se enlaza/documenta en esta edición.
IntelliJ IDEA Ultimate no se instala ni se licencia desde Logixone.

### Estados y códigos

El preflight es de solo lectura y devuelve `COMPATIBLE`,
`COMPATIBLE_CON_ADVERTENCIAS` o `BLOQUEADA`. Una ausencia instalable de Docker o
WSL produce advertencia; sistema operativo, arquitectura, hardware o manifiesto
incompatibles producen bloqueo. Los códigos públicos son 0, 1 y 2
respectivamente; 64 identifica uso inválido y 70 un fallo interno cerrado.

No se crea log, configuración, directorio ni descarga antes del consentimiento.
Después del consentimiento, cada fase registra instalado, reutilizado, omitido o
fallido sin valores secretos.

### Actualización, reparación y datos

Una instalación existente se identifica por un marcador de edición y por el
proyecto Compose. Actualizar o reparar puede reemplazar binarios y configuración
no sensible administrada, pero nunca elimina volúmenes. Detener utiliza
`docker compose down` sin `--volumes`. Desinstalar Docker, purgar datos o ejecutar
factory reset queda fuera del flujo normal.

### Firma y distribución

La edición inicial se marca `INTERNAL_UNSIGNED`. Puede probarse dentro del equipo,
pero no entregarse a una empresa. Una entrega externa exige Authenticode válido,
verificación de cadena y sello de tiempo, además de repetir la matriz contra el
artefacto firmado.

## Alternativas consideradas

### MSI/WiX como primer artefacto

Se descartó inicialmente porque agrega toolchain y complejidad de instalación
antes de validar el flujo de producto. Puede reconsiderarse cuando exista firma,
canal corporativo y requisitos de administración centralizada.

### Inno Setup o NSIS

Son opciones maduras, pero introducen una dependencia de empaquetado adicional.
El bootstrapper nativo cubre el preflight previo a UAC y permite probar el núcleo
sin descargar otra herramienta.

### PowerShell como único artefacto

Se conserva para operaciones auxiliares, pero no como experiencia principal: la
aplicación gráfica ofrece identidad, estados, consentimiento y progreso
consistentes. Ningún script debe eludir las políticas de ejecución.

### Instalar servicios nativos de PostgreSQL, Keycloak y WildFly

Se rechaza porque duplicaría el runtime oficial, ampliaría privilegios y haría
divergir desarrollo, demo y producción.

## Consecuencias

- el preflight y el plan son deterministas y probables sin UAC;
- Docker Desktop conserva su licencia independiente y debe aceptarse de forma
  explícita;
- la instalación limpia necesita red en Sprint 8; un paquete totalmente offline
  queda para una decisión posterior;
- la compilación del bootstrapper solo se soporta en Windows;
- no puede declararse cierre productivo mientras la edición esté sin firmar o la
  matriz de VM no esté verde.

## Fuentes verificadas

- [Docker Desktop para Windows](https://docs.docker.com/desktop/setup/install/windows-install/)
- [Notas de Docker Desktop 4.84.0](https://docs.docker.com/desktop/release-notes/#4840)
- [Versiones y dependencias de .NET Framework](https://learn.microsoft.com/en-us/dotnet/framework/install/versions-and-dependencies)
- [Ciclo de vida de Windows 11](https://learn.microsoft.com/en-us/lifecycle/products/windows-11-home-and-pro)
