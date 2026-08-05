# Telemetría vehicular — caracterización del legado y frontera propuesta

- Fecha: 2026-08-03
- Fuente autorizada de solo lectura: `C:\cosme\multienvios\miaterra`
- Resultado: existe comportamiento útil de flota/GPS, pero no una arquitectura
  portable al nuevo ERP
- Decisión: [ADR-0034](../../adr/0034-plugin-telemetria-vehicular.md)

## Propósito

Identificar capacidades, riesgos e invariantes observables del seguimiento
vehicular legado para planificar `vehicle_telemetry` sin copiar código, tablas,
credenciales ni acoplamientos `javax.*`.

## Elementos revisados

- `ApiMonitorClient.java`;
- `MovilApiMonitorDTO.java`, `ResumenMovilApiMonitorDTO.java` y
  `MovilDatosMonitorDTO.java`;
- `FlwFlotasControlador.java`;
- `Geolocalizacion.java`;
- `StwVehiculos.java`;
- `WEB-INF/menuFlota.xhtml` y recorridos relacionados.

No se modificó la fuente. Esta caracterización registra comportamiento y defectos
arquitectónicos sin reproducir secretos o grandes bloques de código.

## Comportamiento observado

| Capacidad | Evidencia funcional observada | Lectura para Logixone |
|---|---|---|
| posición actual | móvil, matrícula, instante, latitud/longitud, velocidad y rumbo | consulta autorizada de última posición con calidad e instante observado/recibido |
| sensores | temperaturas, odómetro virtual y niveles/consumo de combustible | mediciones tipadas, unidad, precisión y procedencia opcionales |
| mantenimiento | kilometraje y fecha del último mantenimiento | dato publicable a un consumidor futuro; telemetría no posee mantenimiento |
| historial | rango de fechas, distancia, movimiento, detención y peajes | recorrido derivado reproducible, paginado y sujeto a retención |
| puntos | segmentos con timestamps, coordenadas y referencia de conductor | observaciones append-only y asociación por ID público/vigencia |
| mapa | posición de la flota y recorrido histórico | UI neutral con proveedor/licencia decididos y alternativa textual |
| flota | móviles, choferes, ficha, georreferencias, lubricantes/combustible e historial | separar vehículo/logística, telemetría y consumidores sectoriales |

## Casos de uso extraídos

1. consultar la última posición disponible de un vehículo;
2. ver vehículos de una empresa sobre una representación geográfica;
3. consultar un recorrido entre fechas;
4. distinguir tiempo en movimiento y detenido;
5. consultar velocidad, rumbo y sensores disponibles;
6. asociar una observación con el vehículo y conductor vigentes;
7. mostrar señal ausente o dato atrasado sin presentarlo como posición actual;
8. conservar kilometraje/horas para consumidores autorizados;
9. pausar, reanudar o finalizar seguimiento sin borrar historia;
10. integrar proveedores distintos mediante un contrato neutral.

## Problemas que no deben trasladarse

### Acoplamiento al proveedor

El cliente legado consulta directamente tablas del sistema externo y conoce sus
acciones, columnas y DTO. Esto impide cambiar de proveedor y rompe la propiedad de
datos. Logixone usará un adaptador que entregue comandos/eventos neutrales al
plugin.

### Mezcla de responsabilidades

Vehículo, controlador JSF, consulta externa, mapa, credenciales y resumen de viaje
están conectados en el mismo recorrido. El nuevo diseño separa:

- `logistics`: vehículo, conductor, ruta, viaje y despacho;
- `vehicle_telemetry`: dispositivos, observaciones, recorridos y tracking lifecycle;
- adaptador: protocolo y autenticación del proveedor;
- shell: render visual controlado.

### Persistencia opaca

La ficha legado guarda datos del monitor serializados como JSON. No se adoptará
JSON como única fuente operativa. Las columnas normalizadas conservarán identidad,
tiempo, coordenadas, unidad, precisión, calidad y procedencia; un payload crudo
opcional quedará separado y gobernado.

### Seguridad

La fuente contiene una credencial de mapas embebida y transporta credenciales de
integración de forma insegura. No se copia ni registra su valor. Toda credencial se
considera configuración externa; si aún estuviera activa, su propietario deberá
rotarla fuera de este proyecto.

### Tecnología y UI

El módulo usa `javax.*`, PrimeFaces y objetos específicos de mapas. Es incompatible
con Java 21, Jakarta EE 11 y la regla de renderers neutrales del shell.

## Frontera propuesta

### Propiedad de `logistics`

- `VehicleId`, ficha, matrícula e identificadores;
- tipo, clasificación y capacidades del vehículo;
- transportista, conductor, ruta, viaje y despacho;
- estado operativo logístico.

### Propiedad de `vehicle_telemetry`

- dispositivo y conexión lógica;
- asignación vigente/histórica a `VehicleId`;
- observación normalizada y última posición derivada;
- recorrido, detención, geocerca, transición y alerta;
- estado `ACTIVE`, `PAUSED` o `STOPPED`;
- idempotencia, cursor, cuarentena, retención y auditoría específica.

### Consumidores

Combustible, mantenimiento, documentos o analítica podrán consumir referencias o
eventos públicos autorizados. No leerán observaciones privadas ni convertirán la
telemetría en propiedad compartida.

## Modelo conceptual mínimo

- `telemetry_device`: identidad empresarial, proveedor neutral, estado y
  capacidades;
- `vehicle_tracking_assignment`: dispositivo, `VehicleId`, vigencia, política y
  estado;
- `telemetry_observation`: evento de origen, instantes, coordenadas, calidad y
  procedencia;
- `telemetry_measurement`: tipo cerrado/versionado, valor decimal, unidad y calidad;
- `last_known_position`: proyección reconstruible de la observación válida más
  reciente;
- `journey`/`journey_segment`: intervalo, distancia, movimiento/detención y calidad
  del cálculo;
- `geofence`, `geofence_transition` y `telemetry_alert`;
- `tracking_state_change`: transición, motivo, actor, versión e instante;
- `ingestion_cursor`, `inbox`, `outbox` y `quarantined_payload`.

## Invariantes propuestas

1. toda observación pertenece a una empresa, dispositivo y asignación verificadas;
2. un dispositivo no tiene asignaciones activas solapadas;
3. un evento repetido no duplica observaciones, recorridos o alertas;
4. el instante recibido no reemplaza el instante observado;
5. un evento tardío no retrocede silenciosamente la última posición;
6. coordenadas y mediciones inválidas se rechazan o cuarentenan;
7. pausar/detener no elimina historia ni cambia el vehículo maestro;
8. reanudar exige asignación vigente, permiso y versión esperada;
9. un proveedor no aparece en el dominio o API pública;
10. ninguna operación física remota forma parte del tracking lifecycle inicial.

## Decisiones abiertas para VT-01

- categorías reales: carretera, agrícola, industrial, maquinaria, embarcación u
  otras;
- vehículos propios, terceros y contratados;
- proveedor inicial, API/webhook/polling y ambiente de prueba;
- frecuencia, cantidad máxima de vehículos, picos y tolerancia offline;
- retención de puntos crudos, agregados y exportaciones;
- significado operativo exacto de pausa para cada proveedor;
- actores autorizados a ver ubicación actual, histórica y sensible;
- geocercas, alertas y vínculo con conductor/viaje;
- proveedor de mapas, licencia, cuotas y alternativa sin servicio externo;
- requisitos de privacidad, residencia y eliminación aplicables a cada empresa.

## Pruebas de caracterización propuestas

- posición válida, coordenadas fuera de rango y precisión desconocida;
- evento duplicado, tardío, desordenado y con reloj incorrecto;
- pérdida/recuperación de señal y reinicio desde cursor;
- cambio de vehículo sin solapar asignaciones;
- pausa/reanudación/finalización concurrentes con versión optimista;
- consulta histórica acotada por empresa, vehículo, permiso y rango;
- geocerca con puntos limítrofes y datos de baja calidad;
- retención y reconstrucción de última posición;
- adaptador ausente, credencial inválida y payload en cuarentena;
- `logistics` y documentos operando con telemetría ausente/inactiva;
- ausencia de coordenadas y secretos en logs, URL y auditoría general.

## Conclusión

El legado demuestra valor funcional suficiente para planificar telemetría, pero su
implementación no es portable. ADR-0034 y la épica asignan el nuevo plugin al orden
7, después de estabilizar la identidad pública de vehículo en `logistics`. Sprint
8 continúa abierto y no se autoriza código de telemetría.

