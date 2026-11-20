import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  it('no escribe el token en localStorage ni sessionStorage', async () => {
    const escrituras: string[] = [];
    const original = Storage.prototype.setItem;
    Storage.prototype.setItem = function (clave: string, valor: string) {
      escrituras.push(clave);
      return original.call(this, clave, valor);
    };
    const cuerpo = btoa(JSON.stringify({ rol: 'CLIENTE', sub: 'usuario-1' }));
    const fetchOriginal = globalThis.fetch;
    globalThis.fetch = () =>
      Promise.resolve(new Response(JSON.stringify({ access: `aaa.${cuerpo}.zzz` }), { status: 200 }));

    try {
      const servicio = TestBed.inject(AuthService);
      await servicio.entrar('cliente@acme.test', 'demo-resolvo');
      expect(servicio.token()).toBe(`aaa.${cuerpo}.zzz`);
      expect(localStorage.getItem('access')).toBeNull();
      expect(sessionStorage.getItem('access')).toBeNull();
      expect(escrituras).toEqual([]);
    } finally {
      Storage.prototype.setItem = original;
      globalThis.fetch = fetchOriginal;
    }
  });

  describe('restaurar', () => {
    const cuerpo = btoa(JSON.stringify({ rol: 'AGENTE', sub: 'usuario-2' }));

    it('guarda token, rol y usuario si el refresh responde 200', async () => {
      const fetchOriginal = globalThis.fetch;
      const llamadas: { url: string; init?: RequestInit }[] = [];
      globalThis.fetch = (url: RequestInfo | URL, init?: RequestInit) => {
        llamadas.push({ url: String(url), init });
        return Promise.resolve(new Response(JSON.stringify({ access: `aaa.${cuerpo}.zzz` }), { status: 200 }));
      };
      try {
        const servicio = TestBed.inject(AuthService);

        expect(await servicio.restaurar()).toBe(true);
        expect(servicio.token()).toBe(`aaa.${cuerpo}.zzz`);
        expect(servicio.rol()).toBe('AGENTE');
        expect(servicio.usuarioId()).toBe('usuario-2');
        expect(llamadas[0].url).toBe('/api/sesion/refresh');
        expect(llamadas[0].init?.method).toBe('POST');
        expect(llamadas[0].init?.credentials).toBe('include');
      } finally {
        globalThis.fetch = fetchOriginal;
      }
    });

    it('devuelve false sin cookie de sesión y no guarda nada', async () => {
      const fetchOriginal = globalThis.fetch;
      globalThis.fetch = () => Promise.resolve(new Response(null, { status: 401 }));
      try {
        const servicio = TestBed.inject(AuthService);

        expect(await servicio.restaurar()).toBe(false);
        expect(servicio.token()).toBeNull();
        expect(servicio.rol()).toBeNull();
      } finally {
        globalThis.fetch = fetchOriginal;
      }
    });

    it('devuelve false si la red falla', async () => {
      const fetchOriginal = globalThis.fetch;
      globalThis.fetch = () => Promise.reject(new TypeError('red'));
      try {
        expect(await TestBed.inject(AuthService).restaurar()).toBe(false);
      } finally {
        globalThis.fetch = fetchOriginal;
      }
    });
  });
});
