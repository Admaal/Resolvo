import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';

const cuerpo = btoa(JSON.stringify({ rol: 'CLIENTE', sub: 'usuario-1' }));
const token = (sufijo: string) => `aaa.${cuerpo}.${sufijo}`;
const sinSesion = { status: 401, statusText: 'Unauthorized' };

describe('authInterceptor: sesión caducada', () => {
  const fetchOriginal = globalThis.fetch;
  let refrescos = 0;
  let refrescoValido = true;

  beforeEach(() => {
    refrescos = 0;
    refrescoValido = true;
    globalThis.fetch = async (url: RequestInfo | URL) => {
      if (String(url) === '/api/sesion/refresh') {
        refrescos++;
        await new Promise((resolver) => setTimeout(resolver));
        return refrescoValido
          ? new Response(JSON.stringify({ access: token(`r${refrescos}`) }), { status: 200 })
          : new Response(null, { status: 401 });
      }
      return new Response(JSON.stringify({ access: token('inicial') }), { status: 200 });
    };
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: '**', children: [] }]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
  });

  afterEach(() => {
    globalThis.fetch = fetchOriginal;
  });

  const conSesion = async () => {
    const auth = TestBed.inject(AuthService);
    await auth.entrar('cliente@acme.test', 'demo-resolvo');
    return { auth, http: TestBed.inject(HttpClient), ctrl: TestBed.inject(HttpTestingController) };
  };
  const cabecera = (peticion: { request: { headers: { get(n: string): string | null } } }) =>
    peticion.request.headers.get('Authorization');

  it('un 401 renueva la sesión y reintenta con el token nuevo', async () => {
    const { http, ctrl } = await conSesion();

    const resultado = firstValueFrom(http.get('/api/tickets'));
    const primera = ctrl.expectOne('/api/tickets');
    expect(cabecera(primera)).toBe(`Bearer ${token('inicial')}`);
    primera.flush(null, sinSesion);
    const reintento = await vi.waitFor(() => ctrl.expectOne('/api/tickets'));
    expect(cabecera(reintento)).toBe(`Bearer ${token('r1')}`);
    reintento.flush({ ok: true });

    expect(await resultado).toEqual({ ok: true });
    expect(refrescos).toBe(1);
  });

  it('dos peticiones con 401 a la vez piden un solo refresco y ambas se reintentan', async () => {
    const { http, ctrl } = await conSesion();

    const uno = firstValueFrom(http.get('/api/uno'));
    const dos = firstValueFrom(http.get('/api/dos'));
    ctrl.expectOne('/api/uno').flush(null, sinSesion);
    ctrl.expectOne('/api/dos').flush(null, sinSesion);
    const reintentoUno = await vi.waitFor(() => ctrl.expectOne('/api/uno'));
    const reintentoDos = await vi.waitFor(() => ctrl.expectOne('/api/dos'));
    reintentoUno.flush('uno');
    reintentoDos.flush('dos');

    expect(await Promise.all([uno, dos])).toEqual(['uno', 'dos']);
    expect(refrescos).toBe(1);
    expect(cabecera(reintentoUno)).toBe(`Bearer ${token('r1')}`);
    expect(cabecera(reintentoDos)).toBe(`Bearer ${token('r1')}`);
  });

  it('si el token ya cambió reintenta con el actual sin pedir otro refresco', async () => {
    const { auth, http, ctrl } = await conSesion();

    const resultado = firstValueFrom(http.get('/api/tickets'));
    const primera = ctrl.expectOne('/api/tickets');
    await auth.restaurar();
    expect(refrescos).toBe(1);
    primera.flush(null, sinSesion);
    const reintento = await vi.waitFor(() => ctrl.expectOne('/api/tickets'));
    reintento.flush('ok');

    expect(await resultado).toBe('ok');
    expect(cabecera(reintento)).toBe(`Bearer ${token('r1')}`);
    expect(refrescos).toBe(1);
  });

  it('si la renovación falla va a /login con la ruta actual y propaga el 401', async () => {
    const { auth, http, ctrl } = await conSesion();
    const router = TestBed.inject(Router);
    await router.navigateByUrl('/tickets/abc?vista=mios');
    const navegar = vi.spyOn(router, 'navigateByUrl');
    refrescoValido = false;

    const resultado = firstValueFrom(http.get('/api/tickets'));
    ctrl.expectOne('/api/tickets').flush(null, sinSesion);

    const fallo = (await resultado.catch((error: unknown) => error)) as HttpErrorResponse;
    expect(fallo.status).toBe(401);
    expect(auth.token()).toBeNull();
    expect(auth.rol()).toBeNull();
    expect(navegar).toHaveBeenCalledTimes(1);
    expect(router.serializeUrl(navegar.mock.calls[0][0] as never)).toBe('/login?volver=%2Ftickets%2Fabc%3Fvista%3Dmios');
  });

  it('un segundo 401 tras renovar se propaga sin bucle', async () => {
    const { http, ctrl } = await conSesion();

    const resultado = firstValueFrom(http.get('/api/tickets'));
    ctrl.expectOne('/api/tickets').flush(null, sinSesion);
    const reintento = await vi.waitFor(() => ctrl.expectOne('/api/tickets'));
    reintento.flush(null, sinSesion);

    const fallo = (await resultado.catch((error: unknown) => error)) as HttpErrorResponse;
    expect(fallo.status).toBe(401);
    expect(refrescos).toBe(1);
    ctrl.verify();
  });

  it('el 401 de /api/sesion (login) no intenta renovar', async () => {
    const { http, ctrl } = await conSesion();

    const resultado = firstValueFrom(http.post('/api/sesion', {}));
    ctrl.expectOne('/api/sesion').flush({ error: 'no-autorizado' }, sinSesion);

    const fallo = (await resultado.catch((error: unknown) => error)) as HttpErrorResponse;
    expect(fallo.status).toBe(401);
    expect(refrescos).toBe(0);
  });

  it('un error distinto de 401 pasa tal cual', async () => {
    const { http, ctrl } = await conSesion();

    const resultado = firstValueFrom(http.post('/api/tickets/t1/comentarios', {}));
    ctrl.expectOne('/api/tickets/t1/comentarios').flush({ error: 'demasiados-enlaces' }, { status: 422, statusText: 'Unprocessable' });

    const fallo = (await resultado.catch((error: unknown) => error)) as HttpErrorResponse;
    expect(fallo.status).toBe(422);
    expect(refrescos).toBe(0);
  });

  it('sin token no intenta renovar', async () => {
    const http = TestBed.inject(HttpClient);
    const ctrl = TestBed.inject(HttpTestingController);

    const resultado = firstValueFrom(http.get('/api/publico'));
    ctrl.expectOne('/api/publico').flush(null, sinSesion);

    const fallo = (await resultado.catch((error: unknown) => error)) as HttpErrorResponse;
    expect(fallo.status).toBe(401);
    expect(refrescos).toBe(0);
  });
});
