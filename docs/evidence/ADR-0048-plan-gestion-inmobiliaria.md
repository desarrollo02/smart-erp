# Evidencia — ADR-0048 planificación de gestión inmobiliaria

- Fecha: 2026-08-15
- Tipo: decisión y planificación documental
- Estado: incorporado al plan; implementación no autorizada
- Decisión: [ADR-0048](../adr/0048-plugin-gestion-inmobiliaria.md)
- Épica: [Gestión inmobiliaria](../backlog/epica-gestion-inmobiliaria.md)

## Confirmación de producto

Producto solicitó agregar un plugin para un módulo inmobiliario si no existía y
definió como fuente legado la rama `miaterra_master`. La búsqueda del catálogo,
ADR y backlog no encontró un plugin inmobiliario previo.

## Fuente verificada en solo lectura

| Dato | Resultado |
|---|---|
| repositorio | `C:\cosme\mega\miaterra` |
| rama | `miaterra_master` |
| raíz de código | `fuente/tag` |
| commit observado | `7dd043230efcb2d6b0a9855855acad7d9aaf5faa` |
| fecha del commit | 2026-08-06 12:58:17 -03:00 |
| modo | comandos Git de lectura; sin checkout ni modificación |

El inventario estático localizó el paquete `py/com/ping/inmuebles`, el menú y las
vistas `webapp/inmuebles`, con referencias a proyectos, fracciones, lotes,
edificios/alquiler, catastro, mejoras, etapas, documentos, costos y presupuestos.
También encontró cruces nominales con ventas, contratos, cuentas por cobrar,
tesorería y obras. Esta observación sólo justifica la caracterización futura; no
aprueba sus límites ni traslada código.

## Resultado documental

- se planifica `real_estate` como plugin funcional vertical y opcional;
- el catálogo global planificado pasa de treinta y tres a treinta y cuatro
  reutilizables;
- ERP 1–19 y el trabajo activo de Sprint 10 permanecen sin cambios;
- `RE-00` debe congelar el commit que realmente analice y resolver RE-D01 a
  RE-D12;
- la Guía 00 del roadmap se regeneró con 34 reutilizables y una página específica
  de Inmobiliaria;
- no se agregaron módulos, POM, descriptores, migraciones, pantallas ni código
  ejecutable del ERP; sólo se actualizó el generador documental del roadmap.

## Verificación aplicable

| Comprobación | Resultado |
|---|---|
| inexistencia previa del plugin | revisada en ADR, backlog y roadmap |
| rama y commit legado | verificados con Git en solo lectura |
| coherencia de propietarios | revisada contra ADR-0011 y límites de plugins |
| conteo del catálogo | 33 → 34; ERP 1–19 sin cambios |
| pruebas Maven/ArchUnit/PostgreSQL | no aplican: no hubo código ejecutable del ERP ni composición |
| pruebas funcionales/Playwright | pendientes de historias RE futuras |
| PDF del roadmap | 9 páginas, revisión visual 9/9 y SHA-256 `CF2D64A93954A9A0D0B45C618932003B0D7BEBF4BD004B71B21004E010E0819F` |

La revisión documental no equivale a caracterización completa, implementación o
gate técnico verde del futuro plugin.
