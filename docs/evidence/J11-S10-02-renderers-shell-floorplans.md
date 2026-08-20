# Evidencia J11-S10-02 — Renderers cerrados de floorplans

- Fecha: 2026-08-14
- Estado: Implementada y validada automáticamente
- Validación independiente: pendiente dentro del calendario autorizado
- Materialización: `.tools/tmp/validation/J11-S10-02-kernel/`
- Alcance: composición de pantalla y shell; sin migrar plugins productivos

## Evidencia técnica

El kernel conserva ahora `ScreenExperienceDefinition` en `ComposedScreen` y
mantiene el constructor v1. El shell:

- reconoce 1.x como renderer heredado y 2.x como floorplan cerrado;
- rechaza majors desconocidos y contratos 2.x estructuralmente incompletos;
- implementa cinco selecciones internas de propósito;
- ordena regiones semánticas sin interpretar IDs de negocio como layout;
- asigna componentes por `ScreenElementType` y `ScreenSemanticType`;
- asigna énfasis primario, secundario o destructivo desde el contrato;
- copia estados dinámicos a un modelo JSF seguro;
- rechaza IDs dinámicos ajenos y revalida acciones bloqueadas antes del handler.

El XHTML v2 pertenece a `web-shell`, contiene un único formulario por tarea y no
incluye vistas aportadas por plugins. El CSS contiene composición diferenciada
para editor transaccional y operación guiada, además de adaptación a 600–839 y
0–599. La tabla queda contenida en su región con scroll local cuando no puede
refluir; la página no gana overflow horizontal normal.

## Gates ejecutados

```powershell
.\mvnw.cmd -f .tools/tmp/validation/J11-S10-02-kernel/pom.xml -pl kernel-application -am '-Dtest=CompanyScreenComposerTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
.\mvnw.cmd -f .tools/tmp/validation/J11-S10-02-kernel/pom.xml -pl web-shell -am '-Dtest=ShellFloorplanRendererTest,ShellScreenInteractionViewTest,InventoryScreenRendererTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
.\mvnw.cmd -f .tools/tmp/validation/J11-S10-02-kernel/pom.xml -pl web-shell -am '-Dtest=ShellFloorplanResourceTest,SelectorReturnResourceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
.\mvnw.cmd -f .tools/tmp/validation/J11-S10-02-kernel/pom.xml -pl web-shell -am test
.\mvnw.cmd -f .tools/tmp/validation/J11-S10-02-kernel/pom.xml -pl tests/architecture-tests -am test
.\mvnw.cmd -f .tools/tmp/validation/J11-S10-02-kernel/pom.xml verify
.\.tools\python\3.13.14\python.exe tools\validate_docs.py
```

| Gate | Resultado |
|---|---|
| compositor | 8 pruebas, 0 fallos/errores/omitidas |
| renderer/estado/compatibilidad | 9 pruebas, 0 fallos/errores/omitidas |
| recursos Facelets/CSS/selectores | 4 pruebas, 0 fallos/errores/omitidas |
| módulo `web-shell` | 69 pruebas, 0 fallos/errores/omitidas; 16/16 módulos verdes |
| arquitectura/composición | 24/24 módulos; 34 pruebas verdes |
| reactor `verify` | 28/28 módulos; `BUILD SUCCESS` en 2 min 46 s |
| reportes JUnit | 147 reportes; 546 pruebas; 0 fallos, errores u omitidas |
| documentación del índice | 371 Markdown; 0 enlaces rotos, errores UTF-8, mojibake o secretos |

El primer comando Maven intentado usó una propiedad sin comillas; PowerShell la
interpretó como fase y Maven terminó antes de compilar. Se repitió con argumentos
entrecomillados y sólo las ejecuciones completas verdes cuentan como evidencia.

## Regresión v1

`InventoryScreenRendererTest` y la suite completa del shell conservaron directorio,
alta, ficha, selectores, retorno seguro y administración existentes. Ningún
`ScreenDefinition` productivo cambió a 2.x, por lo que esta historia no altera aún
un recorrido visible del usuario.

El manual de usuario fue revisado y no requiere cambios en este corte. La guía de
desarrollador y la de implementación sí describen la nueva frontera y el orden de
migración.

PostgreSQL/Testcontainers, JTA/OIDC, Docker/Compose y Playwright no aplican a esta
historia porque no cambia persistencia, composición física, handlers productivos
ni una ruta navegable v2. Serán obligatorios al migrar los pilotos.

## Siguiente trabajo

J11-S10-03 puede migrar únicamente el piloto de movimiento de Inventario a
`GUIDED_OPERATION`, completar sus estados condicionales y ejecutar sus gates de
handler, seguridad y Playwright. Compras permanece en v1 hasta J11-S10-04.
