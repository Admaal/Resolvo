import { reenviar } from '../../src/proxy/reenviar';

interface Entorno {
  API_ORIGIN?: string;
}

// Cloudflare Pages Function: todo lo que cuelga de /api va a la API de Render.
export const onRequest = (contexto: { request: Request; env: Entorno }): Promise<Response> =>
  reenviar(contexto.request, contexto.env.API_ORIGIN);
