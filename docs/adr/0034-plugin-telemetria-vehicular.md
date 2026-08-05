# ADR-0034 — Plugin de telemetría vehicular y seguimiento GPS

- Estado: Aceptado
- Fecha: 2026-08-03
- Decisión de producto: incorporar telemetría y seguimiento GPS para vehículos de
  cualquier categoría como plugin funcional independiente
- Modifica: cantidad y orden vigente del roadmap; amplía ADR-0011 después de
  `logistics`
- Fuente: caracterización controlada del módulo de flota del legado Multienvíos

## Contexto

El roadmap asigna a `logistics` la identidad de vehículos, transportistas, rutas,
viajes y despachos. El legado contiene posición actual, recorridos, velocidad,
rumbo, odómetro virtual, temperatura, combustible y tiempos de movimiento, pero
mezcla esas capacidades con SQL del proveedor, DTO externos, mapas, credenciales y
la entidad de vehículo.

La telemetría tiene volumen, temporalidad, seguridad, retención, integración con
dispositivos y fallos propios. Incluirla dentro de `logistics` haría que la ficha de
vehículo y los procesos de despacho poseyeran protocolos GPS y series de tiempo.
Ubicarla en `inventory`, `fuel_station`, mantenimiento o el kernel trasladaría la
responsabilidad a dominios que solo consumen algunos de sus resultados.

Producto también solicita poder parar el seguimiento. Esa operación debe
distinguirse de ocultar una posición en la UI, desasignar un dispositivo o enviar
un comando físico al vehículo.

## Decisión

### 1. Plugin y orden

Se agrega `vehicle_telemetry` como plugin funcional reutilizable número **7**,
después de `logistics` y antes de `commercial_documents`. El roadmap pasa a
diecinueve plugins reutilizables:

1. `business_partners`;
2. `commercial_catalog`;
3. `inventory`;
4. `purchasing`;
5. `sales`;
6. `logistics`;
7. `vehicle_telemetry`;
8. `commercial_documents`;
9. `recurring_billing`;
10. `sifen`;
11. `treasury`;
12. `point_of_sale`;
13. `fuel_station`;
14. `accounts_receivable`;
15. `accounts_payable`;
16. `accounting`;
17. `human_resources`;
18. `payroll`;
19. `payroll_paraguay`.

Una distribución completa para `N` empresas podrá contener `19 + N` plugins
productivos. La presencia física no activa telemetría para una empresa ni obliga a
que todos sus vehículos sean seguidos.

El orden es de construcción. `commercial_documents` no depende funcionalmente de
telemetría y puede operar cuando `vehicle_telemetry` esté ausente o desactivado.

### 2. Propiedad funcional

`vehicle_telemetry` será dueño de:

- dispositivos de seguimiento y sus identidades neutrales;
- asignaciones dispositivo–vehículo con vigencia e historia;
- conexiones y cursores de ingestión sin exponer secretos;
- observaciones normalizadas de posición y sensores;
- proyección de última posición conocida;
- recorridos derivados, detenciones, geocercas y alertas;
- calidad, procedencia, instante observado e instante recibido;
- política y estado auditado del seguimiento;
- retención, cuarentena, inbox/outbox e idempotencia propias.

No será dueño del maestro de vehículo, su clasificación logística, conductor,
transportista, pedido, despacho, remisión, mantenimiento, compra de combustible,
factura, usuario ni empresa.

### 3. Vehículos de cualquier categoría

`logistics` publicará un `VehicleId` opaco y una referencia mínima. Tipo,
clasificación y capacidades del vehículo pertenecen a logística; telemetría no
creará entidades separadas para automóvil, camión, motocicleta, tractor,
montacargas, maquinaria, embarcación u otra categoría.

Un dispositivo puede asociarse a cualquier `VehicleId` cuya referencia pública
declare que admite seguimiento. Las particularidades de sensores se expresan como
capacidades versionadas y mediciones tipadas, no como columnas o DTO específicos de
un fabricante.

### 4. Dependencias

- requerida: `logistics-api`, únicamente para `VehicleId`, referencia y estado
  público del vehículo;
- opcionales: contratos públicos logísticos para viaje/conductor vigente y eventos
  de consumidores autorizados;
- sin dependencia directa de `inventory`, `commercial_documents`, `fuel_station`,
  recursos humanos, tesorería o contabilidad;
- ningún consumidor accede a tablas, entidades o payloads privados de telemetría.

Logística tampoco depende de `vehicle_telemetry`: puede registrar vehículos,
planificar rutas y ejecutar despachos sin GPS. La disponibilidad de seguimiento se
resuelve mediante una capacidad pública opcional.

### 5. Ingestión y adaptadores

El dominio y `vehicle-telemetry-api` no contendrán SDK, tablas, acciones, nombres de
campos ni DTO de un proveedor. Cada integración será un adaptador técnico
versionado que convierta API, webhook, archivo o polling al contrato neutral.

La ingestión deberá:

- identificar empresa, conexión, dispositivo y vehículo vigente;
- deduplicar por proveedor, dispositivo y evento/clave de origen;
- aceptar de forma controlada eventos tardíos o fuera de orden;
- conservar instante del dispositivo e instante de recepción;
- validar coordenadas, precisión, unidad, calidad y rango de sensores;
- aislar payload inválido en cuarentena sin bloquear otros dispositivos;
- reanudar desde un cursor confirmado sin perder ni duplicar observaciones.

Las credenciales y claves de mapas se inyectan externamente y nunca se registran en
código, URL, log, auditoría o tabla de negocio.

### 6. Pausa y finalización del seguimiento

El ciclo inicial es cerrado y auditado:

- `ACTIVE`: se admite y publica seguimiento según la política vigente;
- `PAUSED`: pausa temporal autorizada con motivo, inicio y posible vencimiento;
- `STOPPED`: seguimiento finalizado o asignación cerrada.

Pausar o detener no borra observaciones ni reescribe recorridos históricos. Cada
cambio revalida empresa, vehículo, dispositivo, permiso, versión y motivo. La UI
distingue claramente:

1. ocultar una capa o vehículo para el usuario actual;
2. pausar la aceptación/publicación de seguimiento;
3. cerrar la asignación del dispositivo;
4. enviar un comando físico al equipo o al vehículo.

El punto 4, incluida inmovilización o apagado remoto, queda fuera del primer
alcance. Solo podrá proponerse mediante otro ADR con análisis de seguridad,
responsabilidad, confirmación reforzada, recuperación y pruebas con hardware
autorizado.

### 7. Persistencia y tiempo

El esquema privado será `plg_vehicle_telemetry`. PostgreSQL es la tecnología
inicial; particionado, índices espaciales o extensiones especializadas se decidirán
con volumen medido y ADR si alteran el baseline.

Posiciones, mediciones y eventos operativos se almacenan relacionalmente con
unidades, precisión y timestamps explícitos. Un payload crudo opcional puede
conservarse separado, cifrado o referenciado, con versión y checksum; nunca será la
única fuente operativa. Retención, agregación, anonimización y borrado controlado se
congelarán antes de cargar datos reales.

### 8. Seguridad, permisos y privacidad

La ubicación actual e histórica se trata como información operativa sensible. Se
aplican aislamiento empresarial, mínimo privilegio, auditoría de lectura sensible,
retención declarada y exportación controlada.

Permisos iniciales previstos:

- `vehicle_telemetry.current.view`;
- `vehicle_telemetry.history.view`;
- `vehicle_telemetry.sensitive.view`;
- `vehicle_telemetry.devices.manage`;
- `vehicle_telemetry.tracking.manage`;
- `vehicle_telemetry.geofences.manage`;
- `vehicle_telemetry.alerts.manage`;
- `vehicle_telemetry.integrations.manage`;
- `vehicle_telemetry.export`.

Logs y auditoría general usarán IDs, conteos, estado, código de resultado y
correlación. No contendrán coordenadas completas, recorridos, identidad personal,
tokens, credenciales ni payloads crudos.

### 9. UI y mapas

La UI será Jakarta Faces 4.1, Material Design 3 y responsive en 375, 720 y 1280 px.
El shell conserva los renderers y el tema. El plugin aporta contratos neutrales
para directorio de dispositivos, posición actual, historial, geocercas, alertas y
gestión del seguimiento.

El proveedor de mapas, licencia, uso de claves, límites, residencia de datos y modo
sin mapa externo se decidirán antes de la historia visual. No se inyectará
JavaScript remoto o una clave embebida desde el plugin.

### 10. Alcance inicial

La primera edición cubrirá:

- alta y asignación histórica de dispositivos;
- simulador de proveedor y contrato neutral de ingestión;
- última posición y recorrido por rango acotado;
- velocidad, rumbo, odómetro/horas y sensores disponibles con unidad/calidad;
- pausa, reanudación y finalización del seguimiento;
- geocercas y alertas básicas;
- seguridad, auditoría, retención y recuperación;
- interfaz responsive y demo con datos ficticios.

La integración con el primer proveedor real se realizará solo después de obtener
documentación, ambiente de prueba, límites, licencias y credenciales externas.

## Consecuencias

### Positivas

- vehículos y procesos logísticos permanecen independientes de proveedores GPS;
- cualquier categoría de vehículo usa el mismo contrato de identidad;
- seguimiento actual, historia y sensores tienen propietario claro;
- pausar o detener conserva trazabilidad y no borra el pasado;
- consumidores futuros reciben referencias/eventos sin acceder a series privadas.

### Costes y riesgos

- el roadmap crece a diecinueve plugins y desplaza órdenes posteriores;
- posiciones frecuentes elevan volumen, coste de índices y necesidades de
  retención;
- ubicación y asociación con conductor amplían la superficie de privacidad;
- relojes incorrectos, duplicados, pérdida de señal y eventos tardíos requieren
  pruebas específicas;
- mapas y proveedores externos introducen licencias, cuotas y disponibilidad;
- confundir detener seguimiento con controlar el vehículo puede crear un riesgo
  físico y jurídico.

## Alternativas descartadas

### Incluir telemetría dentro de `logistics`

Se descarta porque obliga al dominio logístico a poseer protocolos, dispositivos,
series temporales y retención aunque una empresa no use GPS.

### Incluirla en `fuel_station` o mantenimiento

Se descarta porque combustible, kilometraje y mantenimiento son consumidores
parciales, no propietarios de posiciones o dispositivos.

### Plugin por fabricante GPS

Se descarta como modelo de dominio. Los fabricantes se aíslan en adaptadores
técnicos y el contrato público continúa neutral.

### Copiar el módulo legado

Se descarta por acceso directo a tablas externas, DTO de proveedor, JSON en la
entidad de vehículo, dependencia de mapas, credenciales embebidas y tecnología
incompatible.

## Verificación futura obligatoria

Antes de implementar se confirmarán proveedor inicial, categorías reales,
frecuencia, volumen, retención, semántica exacta de pausa, visibilidad por rol,
geocercas, alertas y proveedor de mapas. Las historias deberán cubrir JUnit,
PostgreSQL/Testcontainers, JPA/JTA, duplicados y desorden, carga, ArchUnit,
outbox/inbox, seguridad negativa, OIDC, Docker/Compose, health, recuperación,
Playwright responsive, demo, manuales, fotografía de plugins y PDF.

No se inicia código durante Sprint 8 ni antes de estabilizar `logistics` y sus
contratos públicos de vehículo.

## Referencias

- [Caracterización de telemetría vehicular](../knowledge-base/vehicle-telemetry/legacy-characterization.md)
- [Épica de `vehicle_telemetry`](../backlog/epica-telemetria-vehicular.md)
- [ADR-0011 — Roadmap de plugins productivos](0011-roadmap-dependencias-plugins-productivos.md)
- [ADR-0013 — Eventos de integración y outbox](0013-eventos-integracion-outbox-por-plugin.md)
- [ADR-0032 — Estaciones de servicio](0032-plugin-estaciones-servicio-combustible.md)

