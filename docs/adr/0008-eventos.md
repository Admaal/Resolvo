# ADR-0008: Outbox transaccional y cola en Postgres

- Fecha: 2026-10-31
- Estado: aceptada

## Contexto

La creación de un ticket debe disparar trabajo posterior (clasificación, sugerencia) sin perder eventos si el proceso cae, y sin obligar al usuario a esperar a la IA. El plan original quería enseñar mensajería tipo Kafka sin pagar un servicio gestionado.

## Opciones

1. **Outbox transaccional + cola en Postgres online, Redpanda en local** — a favor: el evento se escribe en la misma transacción que el ticket, así que no hay eventos fantasma ni pérdidas; en la demo no hay servicio extra que pagar ni que se duerma; Redpanda en compose permite enseñar la API de Kafka en local con Testcontainers. En contra: dos implementaciones del mismo puerto (`PublicadorEventos`) que hay que mantener.
2. **Kafka gestionado (Confluent, Redpanda Cloud)** — a favor: la misma tecnología en local y en producción. En contra: free tier limitado, otra cuenta y otra tarjeta, y un servicio más que puede dormirse o caducar.
3. **Sin eventos: llamada síncrona al clasificador** — a favor: el código más corto. En contra: el usuario espera al LLM, un fallo de IA rompe la creación del ticket y no queda rastro para reintentar.

## Decisión

Patrón outbox: la misma transacción inserta el ticket y su evento; un poller publica y marca. En local, adapter Kafka contra Redpanda; online, adapter Postgres con `FOR UPDATE SKIP LOCKED`. Los consumidores son idempotentes con clave de idempotencia en `eventos_procesados`.

## Consecuencias

- Los tests de eventos cubren que el mismo evento procesado dos veces no genera dos sugerencias.
- La fase 1 no usa eventos: la columna y la tabla llegan con la migración de la fase 5, cuando hay algo que consumir.
- El puerto `PublicadorEventos` no depende de `spring-kafka`, que solo se activa con el perfil `kafka`.

## Enmienda de la fase 5 (2026-11-19)

Se implementó **solo el adapter Postgres**. El adapter Kafka contra Redpanda queda fuera: la demo no lo usa y mantener dos implementaciones del puerto no enseña nada que no enseñe la primera. Si se retoma, `PublicadorEventos` y `BandejaEventos` son los puertos a implementar.

- Hay dos eventos, los que tienen consumidor: `TICKET_RESUELTO` (se publica al pasar a RESUELTO o CERRADO, para indexar el ticket con sus últimos comentarios) y `TICKET_REABIERTO` (al salir de RESUELTO, para borrar sus fragmentos). La clasificación y la sugerencia siguen siendo bajo demanda (ADR-0011), así que no hay evento de ticket abierto.
- El evento se inserta en `evento_outbox` en la misma transacción que el cambio del ticket. El reclamo usa `FOR UPDATE SKIP LOCKED` y un vencimiento de 2 minutos: un procesador que cae no pierde el evento y dos no se pisan.
- Idempotencia por consumidor: `evento_procesado (consumidor, evento_id)` se anota al terminar, así que un fallo del segundo consumidor no repite el primero. A los 5 intentos el evento queda `FALLIDO` con su último error.
- **No hay poller continuo**: una consulta cada pocos segundos mantendría Neon despierto las 24 horas. El procesador se despierta al confirmarse la transacción que publicó, al arrancar y, cada minuto, solo si el último pase dejó algo pendiente (un reintento con espera de 30 s, o un evento reclamado por un proceso que murió, cuyo reclamo de 2 minutos vence después del arranque). Si el proceso cae antes de reclamar el evento, se atiende al arrancar; si cae con el evento reclamado, cuando vence su reclamo.
- Sin proveedor de IA no hay consumidor de indexado: el evento se da por atendido y el arranque con IA reindexa lo que falte. Desindexar no necesita IA y va siempre.
