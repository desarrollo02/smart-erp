# Manual para presentar la demo visual de Logixone

- Version: 1.0
- Fecha: 2026-07-28
- Baseline: candidata tecnica de Sprint 3
- Duracion recomendada: 12 a 15 minutos
- Audiencia: responsables de empresa, producto, implementacion y equipo tecnico
- PDF verificado: [manual-demo-visual-sprint-03.pdf](../output/pdf/manual-demo-visual-sprint-03.pdf)

## 1. Objetivo de la presentacion

La demo debe demostrar que la base arquitectonica del ERP ya puede:

1. autenticar usuarios mediante Keycloak y OIDC;
2. reconocer a que empresas puede acceder cada identidad;
3. seleccionar y cambiar el contexto empresarial de forma controlada;
4. construir menus desde plugins habilitados y permisos efectivos;
5. presentar una misma pantalla funcional con una personalizacion obligatoria y
   distinta para cada empresa;
6. mantener una interfaz responsive con Material Design 3 sobre Jakarta Faces;
7. cerrar la sesion local y la sesion OIDC de forma coordinada.

La candidata no contiene aun facturacion, ventas, inventario ni otro dominio ERP
productivo. La demostracion valida la plataforma sobre la cual se implementaran
esas capacidades.

## 2. Mensaje central

Usar esta frase al abrir la presentacion:

> Hoy no vamos a mostrar un modulo de facturacion terminado. Vamos a mostrar que
> la plataforma ya resuelve identidad, empresas, permisos, plugins,
> personalizacion por empresa y una interfaz responsive. Esta es la base segura
> sobre la que creceran los modulos del ERP.

La idea que debe recordar la audiencia es:

> El plugin funcional aporta la capacidad comun y el ultimo plugin aporta la
> personalizacion exclusiva de cada empresa, sin copiar ni reemplazar toda la
> pantalla.

## 3. Preparacion privada antes de compartir la pantalla

Realizar estos pasos entre 15 y 30 minutos antes de la reunion.

### 3.1. Verificar el entorno

1. Abrir Docker Desktop y esperar a que el motor este disponible.
2. Desde la raiz `C:\cosme\LogixoneJakarta11`, ejecutar:

   ```powershell
   docker compose --env-file infra/compose/compose.env.local `
     -f infra/compose/compose.yaml ps
   ```

3. Confirmar:
   - `postgres`, `keycloak` y `app` estan activos y saludables;
   - `migrator` termino con codigo `0`;
   - no hay un servicio reiniciandose repetidamente.
4. Si la composicion no esta levantada, ejecutar:

   ```powershell
   docker compose --env-file infra/compose/compose.env.local `
     -f infra/compose/compose.yaml up --wait --wait-timeout 240
   ```

5. No usar `down --volumes`. Esa opcion elimina los datos persistidos de
   PostgreSQL y Keycloak.

### 3.2. Verificar salud

Abrir estas direcciones en pestanas que no se mostraran inicialmente:

- liveness: `http://127.0.0.1:18080/logixone/health/live`;
- readiness: `http://127.0.0.1:18080/logixone/health/ready`.

Ambas deben responder HTTP `200` y estado `UP`. Liveness confirma que el proceso
esta vivo. Readiness confirma que la aplicacion esta preparada y que sus
dependencias locales obligatorias son validas.

### 3.3. Preparar las credenciales sin exponer secretos

La demo usa tres identidades ficticias:

| Usuario | Comportamiento esperado |
|---|---|
| `demo.empresas.ab` | muestra selector con dos empresas y permite comparar A/B |
| `demo.empresa.a` | entra directamente a su unica empresa y muestra A |
| `demo.sin.empresa` | muestra una denegacion controlada sin menu empresarial |

La contrasena comun se obtiene de
`.tools/secrets/demo-user-password.txt`. Copiarla de forma privada antes de
compartir pantalla. No abrir ese archivo, no pegar su contenido en el chat y no
mostrarlo durante la reunion.

### 3.4. Preparar el navegador y la reunion

1. Usar Chrome o Edge con zoom al `100%`.
2. Abrir una ventana InPrivate o Incognito limpia.
3. Cerrar marcadores, extensiones, consolas, correo y otras pestanas no necesarias.
4. Silenciar notificaciones del sistema.
5. Dejar la ventana en aproximadamente `1280 x 900` para la primera parte.
6. Abrir como primera pagina visible:
   `http://localhost:18080/logixone/faces/app/index.xhtml`.
7. Recordar que la aplicacion usa `localhost`, mientras Keycloak se presenta como
   `keycloak.localhost:8180`. No cambiar esos hosts durante la demo.

## 4. Recorrido recomendado de 12 a 15 minutos

| Tiempo | Accion | Que demuestra |
|---:|---|---|
| 0:00-1:00 | explicar alcance | expectativa correcta: plataforma, no ERP productivo |
| 1:00-2:00 | entrar por Keycloak | identidad externa y acceso protegido |
| 2:00-3:00 | elegir una empresa | membresias autorizadas y contexto empresarial |
| 3:00-4:00 | mostrar el workspace | menus derivados de plugins y permisos |
| 4:00-7:00 | abrir y explicar la variante A | contrato neutral mas personalizacion exclusiva |
| 7:00-10:00 | cambiar de empresa y mostrar B | recomposicion aislada en la misma sesion |
| 10:00-12:00 | reducir la ventana | Material Design 3 responsive sobre JSF |
| 12:00-13:00 | cerrar sesion | logout local y OIDC coordinado |
| 13:00-15:00 | escenario de seguridad o preguntas | denegacion segura y limites actuales |

## 5. Paso a paso principal

### Paso 1. Mostrar la entrada protegida

1. Compartir solamente la ventana del navegador.
2. Navegar a
   `http://localhost:18080/logixone/faces/app/index.xhtml`.
3. Mostrar que el navegador es redirigido a Keycloak.
4. Ingresar `demo.empresas.ab` y la contrasena copiada de forma privada.
5. Pulsar `Sign In`.

Decir:

> La aplicacion no presenta un formulario propio de contrasena. Delega la
> identidad a Keycloak mediante OIDC. Despues del login, Logixone decide empresas,
> roles y permisos usando su propio modelo de autorizacion.

No mostrar tokens, cookies, herramientas de desarrollo ni la consola de
administracion de Keycloak.

### Paso 2. Mostrar la seleccion empresarial

1. Esperar el selector `Empresa autorizada`.
2. Senalar que aparecen exactamente dos opciones empresariales autorizadas.
3. Elegir la primera opcion disponible y pulsar `Continuar`.
4. No describir los identificadores tecnicos como nombres definitivos de empresa.
   Esos identificadores pueden variar al recrear una demo desde cero.

Decir:

> Esta identidad tiene dos membresias. El navegador solo recibe las empresas que
> el servidor autorizo. Elegir una empresa establece el contexto de trabajo; no
> concede permisos nuevos.

La primera opcion puede corresponder a A o B. Confirmar la variante mas adelante
por el distintivo visible `Personalizacion A aplicada` o
`Personalizacion B aplicada`.

### Paso 3. Mostrar el workspace y el menu

1. Esperar el titulo `Funciones disponibles`.
2. Senalar en el encabezado:
   - el usuario de demostracion;
   - la empresa seleccionada;
   - el boton `Cerrar sesion`.
3. Senalar el selector `Cambiar empresa`, pero no cambiar todavia.
4. Mostrar el enlace `Panel de demostracion`.

Decir:

> El menu no esta escrito de forma fija para este usuario. Se calcula con la
> empresa actual, sus plugins activos y los permisos efectivos. Ocultar un enlace
> no es la seguridad: el servidor vuelve a autorizar cuando se abre la pantalla.

### Paso 4. Abrir la pantalla compuesta

1. Pulsar `Panel de demostracion`.
2. Esperar el titulo `Panel de composicion empresarial`.
3. Mostrar, de arriba hacia abajo:
   - el texto `Contrato neutral renderizado`;
   - el ID publico `reference_plugin:dashboard` y su version;
   - el distintivo de personalizacion A o B;
   - las tarjetas construidas por el shell Jakarta Faces;
   - el aviso inferior que declara el alcance tecnico.

Decir:

> El plugin funcional no entrega una pagina XHTML propia. Publica un contrato
> neutral. El shell JSF autorizado decide como representarlo y luego aplica la
> personalizacion exclusiva de la empresa mediante operaciones tipadas.

No pulsar `Actualizar vista`; no agrega valor al recorrido. En B el boton se usa
precisamente para mostrar que la personalizacion puede deshabilitar una accion.

### Paso 5. Explicar la variante A

Si el distintivo dice `Personalizacion A aplicada`, mostrar:

1. el campo `Referencia tributaria de ejemplo`;
2. la marca `Obligatorio`;
3. el texto de ayuda agregado por A;
4. la tarjeta `Validacion tributaria destacada`;
5. el boton `Actualizar vista` habilitado.

Decir:

> La empresa A conserva la pantalla funcional comun, pero su plugin exclusivo
> cambia la etiqueta y el orden del campo, agrega ayuda, lo vuelve obligatorio e
> inserta una tarjeta tributaria. La regla de autorizacion del servidor no cambia.

Si la primera empresa era B, explicar primero el Paso 6 y presentar A despues del
cambio. No intentar adivinar A/B por el identificador empresarial.

### Paso 6. Cambiar de empresa y explicar la variante B

1. Pulsar `Volver al espacio de trabajo`.
2. En `Cambiar empresa`, elegir la otra opcion.
3. Pulsar `Cambiar`.
4. Comprobar que el encabezado muestra otra empresa.
5. Abrir nuevamente `Panel de demostracion`.
6. Confirmar que ahora aparece la personalizacion que faltaba.

Cuando se muestre B, senalar:

1. el distintivo `Personalizacion B aplicada`;
2. que el campo de resumen ya no aparece;
3. la tarjeta `Operacion simplificada`;
4. el aviso de que un elemento base fue ocultado;
5. el boton `Actualizar vista` deshabilitado.

Decir:

> Seguimos en la misma aplicacion y en la misma pantalla funcional. Al cambiar la
> empresa, el servidor recompuso las contribuciones. B oculta un campo, deshabilita
> una accion e inserta su propio aviso. No se mezclan A y B, y cada empresa tiene
> obligatoriamente una sola personalizacion efectiva.

La comparacion A/B es el momento central de la demo. Dar tiempo a la audiencia para
observar la diferencia.

### Paso 7. Mostrar responsive sobre Jakarta Faces

Mantener abierta la variante B y reducir progresivamente el ancho del navegador:

1. escritorio, alrededor de `1280 px`;
2. tableta, alrededor de `720 px`;
3. movil, alrededor de `375 px`.

En cada ancho, mostrar que:

- las tarjetas se reorganizan y apilan;
- los botones siguen visibles y alcanzables;
- el encabezado se adapta;
- el texto no se corta;
- no aparece desplazamiento horizontal.

Decir:

> La interfaz sigue siendo Jakarta Faces. Material Design 3 aporta el sistema de
> color, forma, elevacion y estados mediante tokens propios. No dependemos de un
> CDN ni de una pagina separada para movil.

Volver a maximizar la ventana antes del cierre.

### Paso 8. Cerrar la sesion

1. Pulsar `Cerrar sesion`.
2. Esperar el retorno al login de Keycloak.
3. Explicar que la sesion local y la sesion del proveedor quedaron coordinadas.
4. No volver atras con el navegador para reutilizar una pagina cacheada.

Decir:

> El cierre no se limita a ocultar la pantalla. La aplicacion invalida su sesion y
> coordina la salida OIDC. Una nueva visita protegida vuelve a exigir login.

## 6. Escenarios adicionales

Ejecutarlos solo si hay tiempo o si la audiencia pregunta por autorizacion.

### 6.1. Usuario con una sola empresa

1. Abrir una nueva ventana InPrivate o Incognito.
2. Entrar con `demo.empresa.a`.
3. Mostrar que no aparece el selector inicial.
4. Mostrar que se abre directamente `Funciones disponibles`.
5. Abrir el panel y confirmar la variante A.
6. Cerrar sesion.

Mensaje:

> Cuando existe una sola membresia valida, el contexto se selecciona
> automaticamente. No agregamos un paso innecesario al usuario.

### 6.2. Usuario sin empresa

1. Abrir otra ventana InPrivate o Incognito limpia.
2. Entrar con `demo.sin.empresa`.
3. Mostrar el titulo
   `No podemos abrir un espacio de trabajo para esta sesion`.
4. Confirmar que no se muestra `Funciones disponibles`, menu ni listado de
   empresas.
5. Cerrar la sesion o cerrar la ventana.

Mensaje:

> La identidad es valida, pero no tiene una membresia empresarial. El sistema
> falla cerrado y no enumera empresas ni funciones que el usuario no posee.

### 6.3. Health para una audiencia tecnica

Mostrar solo si es relevante:

- `/health/live`: indica si el proceso esta vivo;
- `/health/ready`: indica si puede recibir trabajo con seguridad.

No presentar esos endpoints como una API funcional del ERP. La candidata no tiene
por ahora una pagina Swagger/OpenAPI de producto.

## 7. Preguntas frecuentes y respuestas sugeridas

### Es realmente un sistema de plugins?

Si. Cada plugin es un modulo Maven empaquetado como JAR dentro del WAR. Agregar o
retirar fisicamente un plugin requiere reconstruir y redesplegar. Un plugin ya
presente puede activarse o desactivarse por empresa en tiempo de ejecucion.

### La personalizacion puede cambiar pantallas de otro plugin?

Si. Ese es su objetivo principal. La pantalla funcional publica un contrato y el
plugin exclusivo de la empresa aplica cambios tipados: renombrar, reordenar,
ocultar, requerir o deshabilitar componentes e insertar fragmentos permitidos. No
inyecta XHTML, JavaScript o CSS arbitrario y no puede relajar autorizaciones del
servidor.

### Por que existen A y B?

Permiten demostrar dos empresas sobre el mismo plugin funcional. A y B son
personalizaciones distintas, exclusivas y aisladas. En una implementacion real se
creara un plugin de personalizacion propio para cada empresa.

### Es responsive aunque usa JSF?

Si. La candidata usa Jakarta Faces y CSS propio basado en tokens de Material Design
3. Se valido sin overflow horizontal en anchos de `375`, `720` y `1280 px`.

### Los datos sobreviven si se reinician los contenedores?

Si. PostgreSQL y Keycloak usan volumenes explicitos. `docker compose down` conserva
los volumenes y el siguiente arranque reutiliza los datos. `down --volumes` los
elimina y no debe ejecutarse como parte de la demo.

### Ya se puede facturar o vender?

No. La candidata valida infraestructura, identidad, autorizacion, empresas,
plugins, personalizacion y UI. Los dominios ERP se implementaran sobre esta base.

### Ya se puede instalar un plugin sin redesplegar?

No. No existe carga dinamica de JAR. La composicion fisica requiere una nueva
construccion y despliegue; la activacion de plugins ya incluidos si es por empresa
y en tiempo de ejecucion.

### Esta lista para produccion?

No. Es una candidata tecnica de demostracion local. El baseline de Sprint 3 tiene
sus gates tecnicos G2-G6 verdes, pero el cierre formal conserva trabajo documental
independiente pendiente y no autoriza promocion a produccion.

## 8. Lo que no debe mostrarse ni afirmarse

No mostrar:

- contrasenas ni el contenido de `.tools/secrets/`;
- tokens, cookies, claims OIDC o herramientas de desarrollo;
- variables de entorno, archivos `compose.env.local` o secretos montados;
- la consola administrativa de Keycloak;
- consultas directas a PostgreSQL;
- logs completos durante la presentacion;
- UUID internos como si fueran nombres empresariales definitivos.

No afirmar:

- que facturacion, ventas, inventario u otro dominio ya esta implementado;
- que la candidata esta aprobada para produccion;
- que se pueden instalar JAR en caliente;
- que ocultar un componente reemplaza la autorizacion del servidor;
- que la topologia local de Keycloak es la topologia final de produccion;
- que existe Swagger/OpenAPI para APIs funcionales que aun no fueron creadas.

## 9. Recuperacion rapida ante problemas

### La URL no abre

1. Dejar de compartir pantalla.
2. Ejecutar `docker compose ... ps` con el comando completo de la seccion 3.1.
3. Si falta un servicio, ejecutar `up --wait --wait-timeout 240`.
4. Reintentar primero liveness y readiness.
5. Volver a compartir solo cuando ambos esten en `200 UP`.

### Readiness responde 503

No continuar la demo. Readiness esta indicando que una dependencia obligatoria no
esta preparada. Revisar el estado de Compose fuera de pantalla. No intentar ocultar
el fallo mostrando solamente liveness.

### El login entra en un bucle o no regresa

1. Confirmar que la aplicacion se abrio con `localhost:18080`.
2. Confirmar que `keycloak.localhost:8180` responde.
3. Cerrar la ventana privada completa y abrir otra.
4. No modificar redirects ni hosts durante la reunion.

### La contrasena es rechazada

1. Dejar de compartir pantalla.
2. Volver a copiar el secreto desde el archivo local sin mostrarlo.
3. Confirmar el usuario exacto y que no se copio un salto de linea.
4. No escribir ni enviar la contrasena por mensajeria.

### Se muestra la variante opuesta a la esperada

No es un error. Identificar la variante por su distintivo, explicarla y luego usar
`Volver al espacio de trabajo` para cambiar a la otra empresa. El orden de los IDs
empresariales no define A o B.

### La sesion anterior sigue visible

Cerrar la sesion desde la aplicacion. Si la navegacion quedo en un estado previo,
cerrar toda la ventana InPrivate o Incognito y comenzar con un contexto nuevo.

### Plan de contingencia sin runtime

Si el entorno no puede recuperarse en pocos minutos, no improvisar cambios de
infraestructura. Usar las capturas verificadas ubicadas localmente en
`.tools/evidence/J11-S3-08-visual/` y explicar que son evidencia del ultimo gate
Playwright verde. Aclarar que se esta mostrando un respaldo visual, no una sesion
en vivo.

## 10. Version corta de 5 minutos

1. Dar el mensaje de alcance en 30 segundos.
2. Entrar con `demo.empresas.ab`.
3. Elegir una empresa y abrir `Panel de demostracion`.
4. Mostrar dos diferencias de la primera variante.
5. Volver, cambiar de empresa y mostrar dos diferencias de la segunda variante.
6. Reducir la ventana rapidamente hasta ancho movil.
7. Cerrar sesion.
8. Terminar con el mensaje de cierre.

Omitir los escenarios de una y cero empresas, health y preguntas tecnicas. No
omitir la comparacion A/B, porque es el objetivo principal de esta candidata.

## 11. Mensaje de cierre

Usar esta frase:

> La demo confirma que podemos construir un ERP modular, multiempresa y seguro,
> donde cada empresa conserva los modulos comunes y recibe obligatoriamente su
> propia capa de personalizacion. El siguiente avance ya puede concentrarse en
> capacidades reales del negocio sin romper esta separacion.

## 12. Lista final del presentador

Antes de comenzar:

- [ ] Docker y cuatro servicios en estado esperado.
- [ ] Liveness y readiness en `200 UP`.
- [ ] Contrasena copiada de forma privada.
- [ ] Ventana InPrivate o Incognito limpia.
- [ ] Notificaciones y pestanas ajenas cerradas.
- [ ] URL de aplicacion comprobada.

Durante la demo:

- [ ] Aclarar que es plataforma tecnica, no modulo ERP terminado.
- [ ] Mostrar Keycloak, selector empresarial y workspace.
- [ ] Comparar A y B sin mezclarlas.
- [ ] Mostrar al menos un ancho movil.
- [ ] Cerrar sesion.

Al terminar:

- [ ] No dejar una sesion abierta.
- [ ] Responder con los limites reales del baseline.
- [ ] Ejecutar `docker compose down` solo si se desea detener el entorno.
- [ ] No usar `down --volumes`.

## 13. Referencias mantenidas

- [Keycloak y OIDC para desarrollo y demo](keycloak-oidc.md)
- [Shell Jakarta Faces y navegacion autorizada](shell-ui.md)
- [Evidencia tecnica de la candidata](../evidence/J11-S3-08-validacion-demo-cierre.md)
- [ADR-0005: personalizacion obligatoria por empresa](../adr/0005-contexto-empresarial-activacion-personalizacion.md)
- [ADR-0007: Material Design 3 responsive sobre JSF](../adr/0007-material-design-responsive-sobre-jsf.md)
