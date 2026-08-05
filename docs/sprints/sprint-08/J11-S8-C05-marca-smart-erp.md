# J11-S8-C05 — Cambio de marca seguro a Smart ERP

- Estado: Implementada; Maven y documentación verdes; validación visual pendiente por Docker no disponible
- Fecha de decisión: 2026-08-05
- Responsable: responsable de producto
- ADR: [ADR-0039](../../adr/0039-marca-smart-erp-identificadores-compatibles.md)

## Objetivo

Adoptar **Smart ERP** como nombre visible sin transformar el rebranding en una
migración de paquetes, despliegue, autenticación o datos.

## Alcance

- shell JSF: títulos, cabeceras, marca, pie y accesibilidad;
- respuesta HTML de denegación y nombre de usuario de respaldo;
- nombres descriptivos de los módulos Maven;
- nombre mostrado por el realm y cliente Keycloak;
- arquitectura y manuales vigentes;
- prueba focal de marca y compatibilidad.

## Fuera de alcance

- paquetes `py.com.logixone`, clases, coordenadas y módulos Maven;
- WAR, contexto `/logixone`, URLs, JNDI, realm/cliente por identificador,
  variables, imágenes, bases, redes, volúmenes y scripts;
- evidencia histórica y nombres de documentos existentes;
- regeneración o modificación del instalador interno anterior.
- regeneración de PDF derivados, que corresponde al gate documental de cierre.

## Criterios de aceptación

- **CA-01:** las ocho páginas JSF muestran Smart ERP en título y marca.
- **CA-02:** el logotipo textual usa `S`, conserva texto alternativo y no altera
  la estructura responsive.
- **CA-03:** la denegación segura y el usuario de respaldo muestran Smart ERP.
- **CA-04:** Maven y Keycloak usan Smart ERP sólo en metadatos visibles.
- **CA-05:** `py.com.logixone`, `logixone.war`, `/logixone`, realm, cliente,
  variables y persistencia permanecen iguales.
- **CA-06:** manuales vigentes explican la diferencia entre marca e identificador.
- **CA-07:** la prueba focal, el reactor y la validación documental quedan verdes
  desde una materialización bajo `.tools/tmp/validation/`.
- **CA-08:** no se modifica `installer/windows/current` ni se representa el
  instalador anterior como artefacto Smart ERP.

## Validación prevista

1. prueba focal `SmartErpBrandingResourceTest`;
2. `mvn verify` completo en materialización aislada;
3. validación de UTF-8, enlaces, mojibake y secretos;
4. inventario negativo de marca anterior en superficies JSF;
5. comprobación positiva de identificadores técnicos preservados;
6. Playwright responsive en 375, 720 y 1280 px antes de cerrar la historia visual.

La evidencia se registra en
`docs/evidence/J11-S8-C05-marca-smart-erp.md`.
