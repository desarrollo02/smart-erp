# Evidencia — Análisis del plugin de estaciones de servicio

- Fecha: 2026-08-02
- Tipo de cambio: documental y arquitectónico
- Resultado: `fuel_station` agregado como plugin funcional futuro número 11

> Nota vigente: ADR-0033 insertó posteriormente `recurring_billing` como orden 8;
> `fuel_station` pasó entonces al orden 12. ADR-0034 agregó después
> `vehicle_telemetry`; `fuel_station` ocupa actualmente el orden 13 dentro de
> diecinueve reutilizables.

## Fuentes locales

| Fuente | Commit limpio | Resultado |
|---|---|---|
| `C:\cosme\multienvios\miaterra` | `55a56963f00329edd2da57b53a1a94da129cc819` | consumo de combustible de flota mediante movimientos de stock, vehículo, operador, solicitud, kilometraje y horas |
| `C:\cosme\felsina\ingeniolafelsina` | `412b3cd978757b1b8a389f2007060a90f5c7322b` | catálogo de tipo de combustible y referencia desde vehículos; sin estación completa |

Ambas fuentes conservaron `git status --short` vacío y fueron leídas sin
modificaciones.

## Elementos revisados

- `FlwRegistroCombustibleControlador.java`;
- `FlwTipoCombustibleControlador.java` y `StwTipoCombustible.java`;
- pantallas de registro, detalle y catálogo de combustible;
- asociaciones de combustible, vehículo, compra, remisión, operador, sector,
  kilometraje, horas y movimientos de stock;
- búsquedas específicas de tanque, surtidor, pico, manguera, playero y lecturas.

La búsqueda no encontró un agregado operativo de estación. El resultado se usó
para caracterizar despachos internos y ausencias, no para portar código.

## Fuentes oficiales

Se consultaron el 2026-08-02:

- [habilitación de estaciones del MIC](https://www.mic.gov.py/habilitacion-de-estaciones-de-servicios/);
- [procedimiento INTN MLE-PT-02 Rev.05 de 2026-03-04](https://intn.gov.py/wp-content/uploads/2026/03/MLE-PT-02-Rev.05-2026-03-04-Procedimiento-Tecnico-Verificacion-subs-de-surtidores-de-combustibles.pdf);
- [requisitos ambientales comunicados por MADES](https://www.mades.gov.py/2019/08/16/mades-adopta-normas-de-intn-como-requisitos-obligatorios-para-instalacion-y-operacion-de-estaciones-de-servicios-y-afines/);
- [información visible de tipos y precios MIC/SEDECO](https://www.mic.gov.py/socializan-resolucion-que-obliga-a-las-estaciones-de-servicio-a-dar-informacion-clara-y-real-sobre-combustibles/).

No se descargaron normas ni se afirmó cumplimiento. La historia de implementación
deberá congelar los documentos oficiales aplicables dentro de `.tools/`, validar
checksums y obtener revisión experta legal, ambiental y metrológica.

## Decisión resultante

- plugin reutilizable `fuel_station` en orden 11;
- diecisiete plugins reutilizables y `17 + N` con personalizaciones;
- propiedad de tanques, surtidores/picos, turnos, mediciones, recepción, despacho y
  conciliación húmeda;
- catálogo, stock contable, POS, factura, SIFEN, caja, crédito y RR. HH. conservan
  sus propietarios;
- primera versión manual/importadora, sin control remoto;
- adaptadores de fabricante y posible `fuel_station_paraguay` sujetos a ADR
  posteriores.

## Cambios documentales

- [ADR-0032](../adr/0032-plugin-estaciones-servicio-combustible.md);
- [caracterización](../knowledge-base/fuel-station/legacy-characterization.md);
- [épica](../backlog/epica-estaciones-servicio-combustible.md);
- roadmap, arquitectura, guías e índices actualizados.

## Pruebas y límites

No se modificaron Java, POM, descriptor, migración, Compose ni UI. No corresponde
ejecutar Maven, Docker o Playwright. El cambio se valida mediante G0 documental.
Sprint 8 y J11-S8-C02 continúan como trabajo activo.

G0 se ejecutó con `tmp/validate_docs.py` usando el runtime Python local validado:

- 266 archivos Markdown revisados;
- 1.102 enlaces locales inventariados;
- 0 enlaces rotos;
- 0 errores de codificación UTF-8;
- 0 archivos con mojibake;
- 0 posibles secretos.
