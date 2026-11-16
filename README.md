# Resolvo

Helpdesk B2B multi-tenant para equipos de soporte. Proyecto de portfolio: la demo pública usa datos ficticios. Producto en [docs/producto.md](docs/producto.md), decisiones en [docs/adr/](docs/adr/).

**Demo:** https://resolvo-6lc.pages.dev

## Entrar a la demo

Organización Acme. Contraseña de los tres usuarios: `demo-resolvo`.

| Usuario | Rol |
| --- | --- |
| `cliente@acme.test` | CLIENTE |
| `agente@acme.test` | AGENTE |
| `admin@acme.test` | ADMIN |

## Aviso: el servidor se duerme

La API corre en el plan gratuito de Render, que la apaga tras 15 minutos sin tráfico. Un único ping cada 5 minutos la mantiene despierta, pero si falla, la primera visita espera mientras la JVM arranca (hasta un minuto). La interfaz lo avisa con «Despertando servidor…» en vez de esconderlo. La base de datos (Neon) se apaga sola y despierta con la siguiente consulta, sin ping.

## Arquitectura de despliegue

```
Navegador ──► Cloudflare Pages (Angular estático)
                 └─ /api/*  ──► Pages Function (proxy, mismo origen) ──► Render (API Spring Boot, Docker) ──► Neon (Postgres)
```

La SPA y la API comparten origen gracias al proxy: no hay CORS y la cookie de refresh sigue siendo `HttpOnly`, `SameSite=Lax` y `Path=/api/sesion`.

## Local

Requisitos: Java 21, Node, Docker (Postgres y los tests `full`).

```bash
docker run -d --name resolvo-pg -p 5432:5432 -e POSTGRES_DB=resolvo -e POSTGRES_USER=resolvo -e POSTGRES_PASSWORD=resolvo pgvector/pgvector:pg16
RESOLVO_JWT_SECRET=un-secreto-de-al-menos-32-bytes-xxxxxxxx backend/gradlew -p backend bootRun
cd frontend && npm install && npm start
```

Tests: `powershell -NoProfile -File scripts/gates.ps1 full`. Más comandos en [AGENTS.md](AGENTS.md).

### IA (fase 3, opcional)

Sin clave la app funciona igual: las sugerencias y el reindexado responden `503` con `ia-no-configurada`. Para encenderla, una clave de Gemini (free tier) y el perfil `gemini`. El perfil exige la clave: sin ella el arranque falla diciendo qué variable falta; si quieres la IA apagada, no actives el perfil.

```bash
SPRING_PROFILES_ACTIVE=gemini RESOLVO_GEMINI_API_KEY=<tu-clave> RESOLVO_JWT_SECRET=... backend/gradlew -p backend bootRun
```

Al arrancar se indexan en segundo plano los artículos de ayuda y los tickets resueltos; un ADMIN puede forzarlo con `POST /api/conocimiento/reindexar`. La clave solo se lee de la variable de entorno.

## Despliegue (Neon + Render + Cloudflare Pages)

1. **Neon:** proyecto y base de datos. Guarda dos cadenas: la _pooled_ (para la app) y la _directa_ (para migrar).
2. **Render:** Web Service con Docker desde este repo, directorio raíz `backend`, health check `/api/salud`. Variables:

   | Variable | Valor |
   | --- | --- |
   | `RESOLVO_DB_URL` | JDBC del endpoint pooled de Neon, con `sslmode=require` |
   | `RESOLVO_DB_USER` / `RESOLVO_DB_PASSWORD` | credenciales de Neon |
   | `RESOLVO_DB_MIGRATION_URL` / `_USER` / `_PASSWORD` | lo mismo con la conexión directa (Flyway) |
   | `RESOLVO_JWT_SECRET` | secreto generado, al menos 32 bytes |
   | `RESOLVO_COOKIE_SECURE` | `true` |
   | `SPRING_PROFILES_ACTIVE` / `RESOLVO_GEMINI_API_KEY` | `gemini` y la clave; sin ellas la IA queda apagada |

3. **Cloudflare Pages:** proyecto `resolvo` de subida directa (no está conectado a Git). Se despliega a mano desde `frontend/`: `npm run build && npx wrangler pages deploy --project-name resolvo --branch main` (con `--branch main`, o sale como preview). Las Pages Functions de `frontend/functions` van en el mismo despliegue. Variable `API_ORIGIN` = URL del servicio de Render (sin barra final), ya definida en `wrangler.toml`.
4. **Ping:** un único cron externo (por ejemplo cron-job.org) cada 5 minutos a `GET <URL de Render>/api/salud`. Ningún otro servicio en Render: 744 h/mes caben en las 750 h del plan.

Los secretos viven solo en las variables de Render y Pages, nunca en el repositorio.
