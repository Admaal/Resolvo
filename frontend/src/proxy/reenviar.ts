/**
 * Reenvía una petición de /api del origen de Pages a la API (Render).
 * Así la SPA y la API comparten origen: no hay CORS y la cookie de refresh
 * sigue siendo SameSite=Lax y Path=/api/sesion.
 */
export async function reenviar(
  entrada: Request,
  apiOrigin: string | undefined,
  fetchImpl: typeof fetch = fetch,
): Promise<Response> {
  if (!apiOrigin) {
    return Response.json({ error: 'api-origin-no-configurado' }, { status: 500 });
  }

  const origen = apiOrigin.replace(/\/+$/, '');
  const url = new URL(entrada.url);
  const destino = `${origen}${url.pathname}${url.search}`;

  const headers = new Headers(entrada.headers);
  headers.delete('host');

  const conCuerpo = entrada.method !== 'GET' && entrada.method !== 'HEAD';
  const peticion = new Request(destino, {
    method: entrada.method,
    headers,
    body: conCuerpo ? entrada.body : null,
    redirect: 'manual',
    ...(conCuerpo ? { duplex: 'half' } : {}),
  } as RequestInit);

  return fetchImpl(peticion);
}
