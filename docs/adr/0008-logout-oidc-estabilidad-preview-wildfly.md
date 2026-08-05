# ADR-0008 — Logout OIDC y estabilidad preview de WildFly

- Estado: Aceptado
- Fecha: 2026-07-28
- Historia: `J11-S3-08`
- Reemplaza: ninguna decisión anterior

## Contexto

El shell protegido necesita que la acción de logout cierre tanto la sesión HTTP de
Logixone como la sesión correspondiente en el proveedor OIDC. Invalidar únicamente
la sesión local permite que una navegación posterior reutilice la sesión todavía
abierta en Keycloak y vuelva a autenticar al usuario sin solicitar credenciales.

WildFly 41 incorpora RP-Initiated Logout en `elytron-oidc-client` mediante los
atributos `logout-path` y `post-logout-redirect-uri`. La funcionalidad está clasificada
como `preview`. La distribución estándar arranca en estabilidad `community`; en ese
nivel los atributos no forman parte efectiva del modelo de administración, aunque
una configuración que intente declararlos pueda no producir el comportamiento de
logout esperado.

La candidata de Sprint 3 reprodujo ese fallo: la ruta local terminaba la sesión de la
aplicación, pero una visita posterior reutilizaba la sesión del proveedor. Al arrancar
y configurar WildFly con estabilidad `preview`, ambos atributos quedaron efectivos y
el recorrido completo aplicación → proveedor → aplicación protegida volvió a la
pantalla de login.

## Decisión

1. Se mantiene la imagen estándar y fijada de WildFly 41; no se cambia a una
   distribución diferente para obtener esta capacidad.
2. La configuración embebida se ejecuta con `embed-server --stability=preview` y el
   proceso runtime se inicia con `standalone.sh --stability=preview`.
3. `logout-path=/app/logout` y `post-logout-redirect-uri` se escriben de forma
   explícita sobre el `secure-deployment` después de crearlo. La URI posterior es
   absoluta, externa y está registrada en el cliente OIDC.
4. El redirect posterior apunta a una vista protegida. Por ello, después de cerrar la
   sesión del proveedor, el intento de volver al ERP debe desembocar en el login y no
   en una página que aparente conservar autenticación.
5. Cada actualización de WildFly debe comprobar el nivel real de esos atributos en
   el modelo de administración y repetir el E2E de logout. Cuando la capacidad sea
   promovida a `community` o `default`, un ADR posterior podrá retirar `preview`.
6. Habilitar `preview` no autoriza el uso implícito de otras funciones preview. Toda
   capacidad adicional en ese nivel requiere decisión y prueba propias.

## Alternativas consideradas

### Invalidar únicamente la sesión HTTP local

Se descarta porque no termina la sesión del proveedor. El usuario puede volver a
entrar sin una autenticación explícita y el botón no cumple la expectativa de cerrar
sesión coordinadamente.

### Construir manualmente la URL de logout de Keycloak desde la aplicación

Se descarta en este corte. Introduciría acoplamiento directo al proveedor y obligaría
a la aplicación a manipular detalles del token y del protocolo que ya pertenecen al
adaptador OIDC de WildFly.

### Omitir logout coordinado hasta una versión futura de WildFly

Se descarta para la demo segura. Login, cambio de empresa y logout forman un único
recorrido de sesión y no es aceptable presentar el último paso como funcional si la
sesión externa continúa activa.

### Sustituir la imagen por WildFly Preview

Se descarta por ahora. La imagen estándar ya contiene la función y permite elegir el
nivel al arrancar; cambiar de distribución ampliaría innecesariamente el baseline que
debe evaluarse.

## Consecuencias

### Positivas

- el logout usa el soporte OIDC del contenedor y no código propietario de Keycloak;
- la sesión local y la sesión del proveedor se cierran en un flujo comprobable;
- la decisión queda visible en Docker, CLI, runbooks y pruebas E2E;
- el redirect y los secretos continúan siendo configuración externa.

### Costes y riesgos aceptados

- `preview` tiene menores garantías de compatibilidad que `community` o `default`;
- una actualización de WildFly puede modificar nombres, semántica o disponibilidad;
- el arranque completo del servidor habilita la visibilidad de otras funciones
  preview, aunque el proyecto no las utilizará sin ADR;
- el baseline no se promoverá a producción sin repetir los gates de seguridad y
  compatibilidad sobre la versión exacta elegida.

## Verificación obligatoria

- inspeccionar en runtime que `logout-path` y `post-logout-redirect-uri` tengan los
  valores efectivos esperados;
- ejecutar login, logout y una segunda visita a la ruta protegida;
- comprobar que la segunda visita termina en el login de Keycloak y no reutiliza la
  sesión anterior;
- verificar que la URI posterior esté registrada exactamente y no admita redirects
  abiertos;
- mantener el secreto fuera de fuentes, capas de imagen, respuestas y logs.

## Compatibilidad con decisiones anteriores

Este ADR especializa el logout definido por
[ADR-0006](0006-identidad-oidc-membresia-autorizacion.md). No cambia la identidad
estable, las reglas empresariales, la autorización, el uso de JSF ni la separación
entre kernel y proveedor OIDC.

## Referencias verificadas

- [WildFly Proposal WFLY-19314 — Logout Support for OIDC](https://docs.wildfly.org/wildfly-proposals/elytron/WFLY-19314-oidc-logout-support.html), consultada el 2026-07-28.
- [WildFly Admin Guide — Feature stability levels](https://docs.wildfly.org/40/Admin_Guide.html#Feature_stability_levels), consultada el 2026-07-28.
- [WildFly 41 model reference — `secure-deployment`](https://docs.wildfly.org/41/feature-pack/doc/reference/subsystem/elytron-oidc-client/secure-deployment/index.html), consultada el 2026-07-28.
