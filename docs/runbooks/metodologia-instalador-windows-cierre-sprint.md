# Metodología del instalador Windows al cerrar cada Sprint

- Estado: Implementada para canal interno; ejecución condicionada a confirmación
  de producto en cada cierre
- Fecha: 2026-08-01
- Plataforma inicial: Windows de 64 bits
- Fuente funcional: [épica del instalador](../backlog/epica-instalador-windows-reproducible.md)
- Decisión vigente: [ADR-0029](../adr/0029-confirmacion-instalador-por-cierre-sprint.md)

## Propósito

Definir la decisión y el orden reproducible para generar el instalador una vez que
el incremento del Sprint está terminado y su baseline candidato está listo. La
primera implementación está en `installer/windows/`; la evidencia del ejecutable
vigente se registra por Sprint y no se deduce solamente de este procedimiento.

La edición inicial `0.8.0-internal.1` es `INTERNAL_UNSIGNED`: sirve para desarrollo
y evaluación interna, pero no para entrega a una empresa. La distribución externa
exige Authenticode y la matriz de VM completa.

Las reglas de selección y resolución de plugins incluidas más abajo pertenecen a
[WIN-I09](../backlog/WIN-I09-seleccion-plugins-dependencias.md) y están
planificadas, no implementadas en esa edición. Mientras WIN-I09 no esté verde, el
procedimiento vigente conserva un perfil físico fijo y no puede anunciarse como
instalación configurable.

## Decisión obligatoria antes del gate

Cuando los demás gates del cierre estén completos, preguntar al responsable de
producto:

> ¿Crearemos un nuevo instalador Windows para este Sprint?

Registrar `SÍ` o `NO`, fecha, responsable y razón.

- Con `SÍ`, congelar el baseline y ejecutar todo este runbook. El instalador se
  convierte en el último gate técnico.
- Con `NO`, no ejecutar las fases destructivas o de reemplazo, no tocar `current` y
  registrar que el artefacto anterior no representa el Sprint nuevo. El cierre
  continúa con los demás gates ya verdes.
- Sin respuesta, el cierre permanece pendiente de decisión.

## Prerrequisitos cuando la respuesta es SÍ

Antes de regenerar el instalador deben estar verdes:

- reactor y matriz específica del Sprint;
- arquitectura, PostgreSQL/JTA y composición aplicables;
- imágenes, migrador, Compose, health y persistencia;
- seguridad negativa y Playwright;
- demo visual oficial, retrospectiva y siguiente trabajo;
- fotografía de plugins, guías/manuales y PDF obligatorio.
- catálogo instalable generado desde POM/descriptores reales, con versiones,
  dependencias y perfiles verificables.

Si cambia el baseline después, el instalador y sus pruebas quedan invalidados y
deben repetirse.

## Procedimiento cuando la respuesta es SÍ

### 1. Congelar identidad del baseline

Registrar Sprint, versión, commit o identidad equivalente, digest de imagen,
perfil físico de plugins, migraciones y fecha. No usar `latest` como identidad.

### 2. Construir el plan de instalación

Generar desde manifiestos mantenidos:

- requisitos mínimos y recomendados;
- componentes y versiones;
- catálogo de plugins del baseline y estado obligatorio/opcional;
- selección directa solicitada y cierre transitivo de dependencias `REQUIRED`;
- motivo de cada plugin incorporado automáticamente, consumidores y rangos de
  versión resueltos;
- huella de la composición y pareja exacta de aplicación/migrador;
- URLs aprobadas, hashes, firmas y licencias;
- tamaños estimados y espacio requerido;
- acciones sin elevación y con elevación;
- puertos, rutas, reinicios y comprobaciones finales.

### 3. Reemplazar el artefacto `current`

Resolver y verificar el directorio exclusivo de salida. Registrar el SHA-256 del
artefacto anterior y eliminar únicamente los archivos derivados enumerados. Las
fuentes y evidencias permanecen. Construir en temporal y mover a `current` solo si
compilación, firma aplicable y verificaciones son verdes.

### 4. Probar preflight sin cambios

Ejecutar al menos en:

- Windows soportado limpio y compatible;
- Windows con RAM o disco insuficientes;
- virtualización/WSL no disponibles;
- puerto ocupado;
- Docker o Java compatible ya presente;
- versión incompatible de un requisito;
- instalación previa de Logixone con datos.
- selección sin dependencias, cadena transitiva y dependencia compartida;
- dependencia ausente, ciclo, duplicado o rango de versión incompatible;
- intento de deseleccionar una dependencia todavía requerida.

Confirmar estados `COMPATIBLE`, `COMPATIBLE_CON_ADVERTENCIAS` y `BLOQUEADA`.
Una máquina bloqueada no debe mostrar UAC ni modificar el sistema.
Una composición inválida también debe quedar bloqueada antes del consentimiento,
sin descargar, construir, migrar ni modificar el equipo.

### 5. Probar consentimiento, UAC y progreso

Verificar que el resumen previo enumera todo, que cancelar no deja cambios, que
UAC aparece en el último momento seguro y que cada fase informa acción/resultado.
Rechazar UAC debe terminar de forma comprensible y recuperable.

La interfaz debe distinguir selección directa, dependencia automática, componente
obligatorio y opción no seleccionada. Una dependencia automática permanece
bloqueada mientras tenga consumidores; para retirarla se deben deseleccionar de
forma explícita los plugins afectados.

### 6. Probar instalación limpia

En una VM limpia:

1. ejecutar diagnóstico;
2. aceptar el plan;
3. instalar/reutilizar prerrequisitos;
4. preparar proyecto y configuración no secreta;
5. construir o adquirir la pareja exacta de artefactos correspondiente a la
   composición resuelta;
6. ejecutar migrador y Compose;
7. verificar liveness/readiness y ruta visual;
8. detener con `down` sin `--volumes`;
9. recrear y comprobar conservación de datos.

### 7. Probar actualización, reparación y fallo

- actualizar desde el instalador `current` anterior sin perder volúmenes;
- reparar un componente faltante sin reinstalar todo;
- agregar un plugin a una instalación existente y comprobar sus dependencias;
- retirar físicamente un plugin mediante nueva composición, conservando sus
  esquemas, migraciones y datos;
- simular descarga/hash inválido y confirmar fallo cerrado;
- cancelar entre fases seguras y reanudar;
- revisar que rollback no borre software o datos preexistentes.

### 8. Inspeccionar artefactos y logs

Comprobar versión embebida, manifiesto, SHA-256, firma/estado de firma, terceros,
licencias, ausencia de secretos y detecciones de seguridad. Los logs deben poder
adjuntarse al soporte sin exponer credenciales.

### 9. Registrar evidencia

La evidencia de cierre debe incluir:

- ruta y nombre del instalador vigente;
- Sprint, versión, baseline/digest;
- tamaño y SHA-256;
- firma y certificado, o restricción interna explícita;
- versiones instaladas/reutilizadas;
- selección directa, dependencias automáticas, grafo resuelto, orden topológico y
  huella de composición;
- prueba de igualdad entre los plugins físicos del WAR y del migrador;
- matrices de compatibilidad e incompatibilidad;
- resultados de instalación, cancelación, actualización y reparación;
- health, persistencia y ubicación de logs/capturas;
- confirmación de que `current` contiene solo la edición vigente.

### 10. Declarar cierre

El Sprint puede cerrarse solo cuando el instalador se construyó después del último
cambio del baseline y toda la matriz anterior está verde. El instalador no
reemplaza pruebas, demo, PDF, manuales ni validación humana pendiente.

## Registro cuando la respuesta es NO

La evidencia de cierre debe identificar versión, SHA-256 y baseline del último
`current`, declarar expresamente que no corresponde al Sprint nuevo y explicar el
motivo de no regeneración. Debe enlazar el recorrido manual vigente para levantar
el proyecto. No se elimina, mueve, firma nuevamente ni modifica el artefacto.

## Recuperación

Si producto respondió `SÍ` y falla la regeneración:

1. no promover el temporal a `current`;
2. conservar logs y causa sin secretos;
3. corregir fuente, manifiesto o entorno;
4. repetir primero la prueba fallida y luego la matriz completa;
5. mantener el Sprint abierto.

No se restaura un instalador anterior como si representara el Sprint nuevo. Si el
artefacto anterior se conserva temporalmente para diagnosticar una actualización,
debe permanecer fuera de `current` y eliminarse al completar la evidencia.

## Comandos de la implementación vigente

```powershell
powershell -ExecutionPolicy Bypass -File installer\windows\scripts\build-bootstrapper.ps1 -Test
powershell -ExecutionPolicy Bypass -File installer\windows\scripts\build-installer.ps1
.\installer\windows\current\Logixone-Installer.Cli.exe `
  --manifest .\installer\windows\current\installer-manifest.json `
  --preflight --plan --no-network
```

El CLI debe imprimir que el diagnóstico no realizó cambios y una huella completa
del plan. El ejecutable gráfico debe presentar el mismo diagnóstico y mantener la
instalación deshabilitada hasta el consentimiento. Los códigos de compatibilidad
son `0`, `1` y `2`; `1` indica advertencias revisables, no un fallo interno.

## Linux

No se crean scripts o paquetes Linux en esta fase. La futura implementación
necesitará matriz de distribuciones, gestor de paquetes, permisos, systemd,
contenedores y formato de paquete aprobados por separado.
