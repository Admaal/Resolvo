import { reenviar } from './reenviar';

type Captura = { peticion?: Request };

function falsoFetch(captura: Captura, respuesta: Response): typeof fetch {
  return ((entrada: Request) => {
    captura.peticion = entrada;
    return Promise.resolve(respuesta);
  }) as unknown as typeof fetch;
}

describe('reenviar (proxy de /api hacia la API)', () => {
  it('reenvía método, ruta, query, cuerpo y Cookie a la API', async () => {
    const captura: Captura = {};
    const entrada = new Request('https://resolvo.pages.dev/api/sesion/refresh?x=1', {
      method: 'POST',
      headers: { cookie: 'refresh=abc', 'content-type': 'application/json' },
      body: '{"a":1}',
    });

    await reenviar(entrada, 'https://api.onrender.com', falsoFetch(captura, new Response('{}')));

    const enviada = captura.peticion!;
    expect(enviada.url).toBe('https://api.onrender.com/api/sesion/refresh?x=1');
    expect(enviada.method).toBe('POST');
    expect(enviada.headers.get('cookie')).toBe('refresh=abc');
    expect(await enviada.text()).toBe('{"a":1}');
  });

  it('devuelve estado, cuerpo y Set-Cookie tal cual los da la API', async () => {
    const respuesta = new Response('{"access":"t"}', {
      status: 201,
      headers: { 'set-cookie': 'refresh=zzz; Path=/api/sesion; HttpOnly; Secure; SameSite=Lax' },
    });

    const salida = await reenviar(
      new Request('https://resolvo.pages.dev/api/sesion', { method: 'POST', body: '{}' }),
      'https://api.onrender.com',
      falsoFetch({}, respuesta),
    );

    expect(salida.status).toBe(201);
    expect(salida.headers.get('set-cookie')).toBe(
      'refresh=zzz; Path=/api/sesion; HttpOnly; Secure; SameSite=Lax',
    );
    expect(await salida.json()).toEqual({ access: 't' });
  });

  it('no sigue redirecciones: las devuelve al navegador', async () => {
    const captura: Captura = {};
    await reenviar(
      new Request('https://resolvo.pages.dev/api/tickets'),
      'https://api.onrender.com',
      falsoFetch(captura, new Response(null, { status: 302, headers: { location: '/otra' } })),
    );

    expect(captura.peticion!.redirect).toBe('manual');
  });

  it('tolera una barra final en API_ORIGIN', async () => {
    const captura: Captura = {};
    await reenviar(
      new Request('https://resolvo.pages.dev/api/salud'),
      'https://api.onrender.com/',
      falsoFetch(captura, new Response('{}')),
    );

    expect(captura.peticion!.url).toBe('https://api.onrender.com/api/salud');
  });

  it('responde 500 con un error claro si API_ORIGIN no está configurado', async () => {
    const salida = await reenviar(
      new Request('https://resolvo.pages.dev/api/salud'),
      undefined,
      falsoFetch({}, new Response('{}')),
    );

    expect(salida.status).toBe(500);
    expect(await salida.json()).toEqual({ error: 'api-origin-no-configurado' });
  });

  describe('IP del cliente firmada', () => {
    const entradaDesde = (cabeceras: Record<string, string>) =>
      new Request('https://resolvo.pages.dev/api/sesion', { method: 'POST', body: '{}', headers: cabeceras });

    it('con secreto envia la IP real en X-Cliente-Ip y el secreto en X-Resolvo-Proxy', async () => {
      const captura: Captura = {};

      await reenviar(
        entradaDesde({ 'cf-connecting-ip': '203.0.113.9' }),
        'https://api.onrender.com',
        falsoFetch(captura, new Response('{}')),
        'secreto-de-prueba',
      );

      expect(captura.peticion!.headers.get('x-cliente-ip')).toBe('203.0.113.9');
      expect(captura.peticion!.headers.get('x-resolvo-proxy')).toBe('secreto-de-prueba');
    });

    it('descarta lo que el navegador mande en X-Cliente-Ip y X-Resolvo-Proxy', async () => {
      const captura: Captura = {};

      await reenviar(
        entradaDesde({
          'cf-connecting-ip': '203.0.113.9',
          'x-cliente-ip': '1.2.3.4',
          'x-resolvo-proxy': 'adivinado',
        }),
        'https://api.onrender.com',
        falsoFetch(captura, new Response('{}')),
        'secreto-de-prueba',
      );

      expect(captura.peticion!.headers.get('x-cliente-ip')).toBe('203.0.113.9');
      expect(captura.peticion!.headers.get('x-resolvo-proxy')).toBe('secreto-de-prueba');
    });

    it('sin secreto configurado no firma nada y borra las cabeceras que vengan de fuera', async () => {
      const captura: Captura = {};

      await reenviar(
        entradaDesde({
          'cf-connecting-ip': '203.0.113.9',
          'x-cliente-ip': '1.2.3.4',
          'x-resolvo-proxy': 'adivinado',
        }),
        'https://api.onrender.com',
        falsoFetch(captura, new Response('{}')),
      );

      expect(captura.peticion!.headers.has('x-cliente-ip')).toBe(false);
      expect(captura.peticion!.headers.has('x-resolvo-proxy')).toBe(false);
    });

    it('sin CF-Connecting-IP no envia X-Cliente-Ip ni el secreto', async () => {
      const captura: Captura = {};

      await reenviar(
        entradaDesde({ 'x-cliente-ip': '1.2.3.4' }),
        'https://api.onrender.com',
        falsoFetch(captura, new Response('{}')),
        'secreto-de-prueba',
      );

      expect(captura.peticion!.headers.has('x-cliente-ip')).toBe(false);
      expect(captura.peticion!.headers.has('x-resolvo-proxy')).toBe(false);
    });
  });
});
