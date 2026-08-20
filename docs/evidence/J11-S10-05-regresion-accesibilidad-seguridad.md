# Evidencia J11-S10-05 — Regresión, accesibilidad y seguridad

- Fecha: 2026-08-20
- Estado: Implementada y validada automáticamente; validación independiente pendiente
- Validación independiente: pendiente dentro del calendario autorizado
- Materialización de código: `.tools/tmp/validation/J11-S10-05-compact/`
- Perfil físico: `with-purchasing-demo`
- Entorno aislado: proyecto Compose `logixone-j11-s10-05`

## Evidencia estática

- `floorplan.js` registra únicamente la intención de restaurar foco; no conserva
  valores funcionales ni datos personales en el navegador;
- el destino de foco se decide por orden: control inválido, aviso visible y
  título principal enfocable;
- el `h1` del workspace admite foco programático sin entrar en el orden normal de
  tabulación;
- los tests de recursos cubren foco, movimiento reducido y la grilla compacta;
- Playwright solicita `ReducedMotion.REDUCE`, verifica la media query, transición
  nula y foco visible por teclado;
- los recorridos de Inventario y Compras prueban 375/599/600/720/839/840/1280 y
  denegación segura al desactivar el plugin;
- la corrección responsive pertenece al shell y no introduce aportes visuales
  arbitrarios desde plugins.

## Gates ejecutados

```powershell
.\mvnw.cmd -f .tools\tmp\validation\J11-S10-05-compact\pom.xml -pl web-shell -am '-Dtest=SmartErpBrandingResourceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
.\mvnw.cmd -f .tools\tmp\validation\J11-S10-05-compact\pom.xml -Pwith-purchasing-demo verify
docker build --build-arg LOGIXONE_MAVEN_PROFILE=with-purchasing-demo -t logixone/app:j11-s10-05-regression .tools/tmp/validation/J11-S10-05-compact
```

| Gate | Resultado |
|---|---|
| regresión focal del shell | 4 pruebas, 0 fallos/errores/omitidas |
| reactor Surefire | 28 módulos; 565 pruebas, 0 fallos/errores/omitidas |
| ArchUnit | 34 pruebas, 0 fallos/errores/omitidas |
| integración health/OIDC | 6 pruebas, 0 fallos/errores/omitidas |
| `InventoryVisualIT` | 1 recorrido integral, 0 fallos/errores/omitidas |
| `PurchasingVisualIT` | 1 recorrido integral, 0 fallos/errores/omitidas |
| total automatizado | 155 reportes; 573 pruebas, 0 fallos/errores/omitidas |
| responsive | 375, 599, 600, 720, 839, 840 y 1280 px sin overflow normal |
| evidencia visual | 42 PNG; 6.674.853 bytes; revisión visual conforme |
| migraciones | 7 esquemas válidos; 0 migraciones ejecutadas en la repetición |
| Compose | aplicación, PostgreSQL y Keycloak saludables |
| catálogo runtime | 8 plugins esperados y versiones verificadas |
| health | `/health/live` 200 y `/health/ready` 200 |

La imagen final validada es:

- etiqueta: `logixone/app:j11-s10-05-regression`;
- manifiesto: `sha256:ebdcbed6cb391bf7eb5df608fc00bf7e1522955e07ba40de0005d16ab2d4477b`;
- migrador: `logixone/migrator:j11-s10-05-regression`, manifiesto
  `sha256:b73d1b605900c08cd35bc3a4df2cf23fa38de3f3f09b53b332e7a67df0c76cb6`.

El catálogo runtime confirmó `reference_data` 1.1.0,
`business_partners` 1.1.0, `commercial_catalog` 1.1.0, `inventory` 1.2.0,
`purchasing` 1.2.0 y los tres plugins de referencia/personalización 1.0.0.
Las capturas están en
[`screenshots/J11-S10-05`](screenshots/J11-S10-05/).

## Revisión visual

Se revisaron ejemplos representativos de Inventario y Compras en los tres rangos
principales. La primera revisión detectó superposición en la barra empresarial a
375 px. Después de corregir la grilla compacta se reconstruyó la imagen, se
repitieron ambos recorridos completos y se revisó nuevamente la captura compacta
final. Selector, acción, fuente y enlace quedaron separados y legibles.

## Incidencias y correcciones durante la ejecución

- la escritura de evidencia desde el sandbox fue denegada; Playwright escribió
  primero dentro de la materialización y los archivos se copiaron después al
  directorio versionado;
- el navegador no pudo iniciar de forma estable dentro del sandbox; se ejecutó
  fuera de él conservando el JDK y navegador gobernados por el proyecto;
- el usuario ficticio no tenía autoridad administrativa para la prueba negativa;
  el bootstrap de un solo uso rechazó dos nombres no exactos y sólo aceptó el
  nombre persistido correcto; luego se desactivó inmediatamente;
- una recreación Compose tomó inicialmente la etiqueta antigua declarada en el
  entorno local; se fijaron explícitamente ambas imágenes y se verificaron el
  identificador de la imagen activa y los ocho plugins del catálogo;
- la inspección visual encontró el solapamiento compacto; se corrigió, se agregó
  una regresión focal y se repitieron Docker y Playwright;
- dos intentos de prueba focal usaron un alcance/quoting incorrecto; el comando se
  corrigió sin relajar ni omitir pruebas.

No se registraron secretos ni identidades reales. El bootstrap global terminó
desactivado. El proyecto Compose fue exclusivo de esta historia; al terminar se
eliminaron sus cuatro contenedores, tres redes y dos volúmenes efímeros. No se
tocó ningún servicio, volumen ni dato del usuario o del IDE.

La validación independiente permanece pendiente. No se promueve la imagen, no se
declara producción y Sprint 10 continúa abierto.
