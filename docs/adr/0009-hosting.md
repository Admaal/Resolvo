# ADR-0009: Hosting de la demo a coste 0 €

- Fecha: 2026-10-31
- Estado: aceptada

## Contexto

La demo pública tiene que mantenerse en 0 €, sin tarjeta y sin tareas de mantenimiento que dependan de que alguien recuerde hacer ping. La API es una JVM Spring Boot, lo que descarta plataformas que solo ejecutan JavaScript y hace del arranque en frío un problema de producto. La base de datos necesita `pgvector` desde la fase 3.

## Opciones

1. **Neon Free + Render Free + Cloudflare Pages** — a favor: Neon apaga el cómputo a los 5 minutos sin uso y lo reactiva en menos de 1 segundo con la siguiente consulta, así que la base no necesita ping y no pide tarjeta; Render ejecuta la JVM en Docker con el mismo cron de ping que ya se usa en Solvo; Pages sirve el Angular estático sin cómputo que mantener. En contra: Render Free duerme tras 15 minutos sin tráfico y el arranque de la JVM se nota; el ping consume horas del plan.
2. **Supabase Free para la base** — a favor: Postgres con pgvector y panel cómodo. En contra: pausa el proyecto tras una semana sin actividad y obliga a un ping extra para no tener que reactivarlo a mano.
3. **Cloud Run** — a favor: arranque mejor y escala a cero con contenedores. En contra: pide tarjeta y la alerta de presupuesto no corta el gasto, lo que rompe la restricción de 0 € sin sobresaltos.
4. **Cloudflare Workers** — a favor: free tier generoso y latencia baja. En contra: no ejecuta la JVM, así que el backend tendría que reescribirse en otro lenguaje.

## Decisión

Tres servicios: **Neon Free** para Postgres + pgvector, **Render Free** para la API en Docker y **Cloudflare Pages** para el Angular.

- Neon no lleva ping: el cómputo se apaga solo y despierta en la siguiente consulta.
- Render lleva **un solo** ping, cada 5 minutos, para no superar las 750 h/mes del plan gratuito. Ningún otro servicio en Render.
- Vercel, que era la opción del plan técnico inicial, no se usa.

## Consecuencias

- La interfaz muestra un aviso de «despertando servidor…» mientras la API arranca; el README explica el cold start en vez de esconderlo.
- La API se empaqueta con CDS/AOT para que Spring arranque antes y el aviso dure menos.
- La conexión a Neon usa el endpoint pooled; las migraciones Flyway se ejecutan contra la cadena de conexión directa o con el pooler en modo sesión, según lo que admita el plan.
- Si el ping se cae, la API se duerme igual y despierta con la siguiente visita: no hay pérdida de datos, solo latencia.
- Las cuotas reales de Neon y Render se anotan con fecha antes de la fase 2.
- Cerrado el 2026-11-14: la API se empaqueta con CDS, no con AOT. En JVM, AOT solo entra en el jar con el plugin nativo de GraalVM. CDS se entrena en el build sin base de datos (`ddl-auto=none`, sin Flyway). Medido en local con 512 MB: 3,26 s sin CDS y 3,09 s con CDS, una mejora pequeña; en Render Free (menos CPU) se espera algo más, sin medir. La imagen arranca con 199 MiB.
- Cerrado el 2026-11-14: Flyway migra por `RESOLVO_DB_MIGRATION_URL` (conexión directa) y la app sirve por `RESOLVO_DB_URL` (pooled). Sin esas variables, ambas usan la misma conexión.
