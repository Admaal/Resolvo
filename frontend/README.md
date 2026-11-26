# Resolvo: SPA

La interfaz de Resolvo: Angular 22 con componentes standalone y signals, y estilos propios sobre tokens, sin librería de componentes. El proyecto completo está en el [README de la raíz](../README.md) y la dirección visual en [docs/design.md](../docs/design.md).

## Arrancar

```bash
npm install
npm start          # http://localhost:4200, con proxy de /api a http://localhost:8080
npm test           # Vitest, una pasada
npm run build      # salida en dist/
```

La API local necesita Postgres y `RESOLVO_JWT_SECRET`; los pasos están en el README de la raíz. Sin API, la portada se ve igual, pero no se puede entrar.

## Cómo está organizado

- `src/app/core/`: sesión (`AuthService`, el guard y el interceptor que renueva el token una vez ante un 401), tema claro u oscuro, aviso de servidor dormido, traducción de errores de la API y el armazón de la app.
- `src/app/features/inicio/`: la portada pública con los accesos de demo.
- `src/app/features/sesion/`: login.
- `src/app/features/tickets/`: bandeja agrupada por SLA, detalle con hilo y propiedades, alta de tickets y los paneles de IA (sugerencia de respuesta y clasificación), que solo ven AGENTE y ADMIN.
- `src/app/shared/`: iconos y el indicador de estado.
- `src/styles/`: tokens (`tokens.css`), semántica (`semantic.css`), temas (`themes.css`) y fuentes. Los componentes no llevan colores ni medidas sueltas: todo sale de ahí.
- `functions/api/`: la Pages Function que hace de proxy de `/api` a la API en Render, en el mismo origen (sin CORS y con la cookie de refresh `HttpOnly`). Firma la IP real del cliente con `PROXY_CLAVE`.
- `public/`: fuentes autoalojadas, favicon, imagen de vista previa (`og.png`) y `_redirects` para que Pages sirva la SPA en cualquier ruta.

## Despliegue

Cloudflare Pages, proyecto `resolvo`, de subida directa. El workflow `despliegue.yml` lo publica al cambiar `frontend/` en `main`; a mano: `npm run build && npx wrangler pages deploy --project-name resolvo --branch main`.
