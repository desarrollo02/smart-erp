# Inventory API

Contrato público Java puro `1.0.0` del plugin `inventory`.

Publica identidades opacas, disponibilidad por clave exacta, snapshot de
conversión, movimientos y reservas. Solo depende de `kernel-api` para `CompanyId`;
no expone Jakarta, JPA, tablas, entidades, repositorios ni clases privadas del
plugin o de `commercial_catalog`.

Las cantidades admiten hasta 6 decimales y los factores de conversión hasta 12.
Cada consumidor debe pasar una empresa obtenida de un contexto confiable; el
contrato no concede autorización por sí mismo.
