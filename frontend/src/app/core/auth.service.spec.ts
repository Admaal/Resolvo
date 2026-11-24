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

    /** El refresco rota el token y es de un solo uso: dos a la vez cerrarían la sesión entera. */
    it('dos restaurar concurrentes hacen un solo refresh', async () => {
      const fetchOriginal = globalThis.fetch;
      let llamadas = 0;
      globalThis.fetch = async () => {
        llamadas++;
        await new Promise((resolver) => setTimeout(resolver));
        return new Response(JSON.stringify({ access: `aaa.${cuerpo}.zzz` }), { status: 200 });
      };
      try {
        const servicio = TestBed.inject(AuthService);
        const [primera, segunda] = await Promise.all([servicio.restaurar(), servicio.restaurar()]);

        expect(llamadas).toBe(1);
        expect([primera, segunda]).toEqual([true, true]);
      } finally {
        globalThis.fetch = fetchOriginal;
      }
    });

    it('tras terminar, un restaurar nuevo vuelve a pedir refresh', async () => {
      const fetchOriginal = globalThis.fetch;
      let llamadas = 0;
      globalThis.fetch = () => {
        llamadas++;
        return Promise.resolve(new Response(JSON.stringify({ access: `aaa.${cuerpo}.zzz` }), { status: 200 }));
      };
      try {
        const servicio = TestBed.inject(AuthService);
        await servicio.restaurar();
        await servicio.restaurar();

        expect(llamadas).toBe(2);
      } finally {
        globalThis.fetch = fetchOriginal;
      }
    });
  });

  it('expirar limpia token, rol y usuario sin llamar a la API', async () => {
    const cuerpo = btoa(JSON.stringify({ rol: 'CLIENTE', sub: 'usuario-3' }));
    const fetchOriginal = globalThis.fetch;
    let llamadas = 0;
    globalThis.fetch = () => {
      llamadas++;
      return Promise.resolve(new Response(JSON.stringify({ access: `aaa.${cuerpo}.zzz` }), { status: 200 }));
    };
    try {
      const servicio = TestBed.inject(AuthService);
      await servicio.entrar('cliente@acme.test', 'demo-resolvo');
      const antes = llamadas;
      servicio.expirar();

      expect(servicio.token()).toBeNull();
      expect(servicio.rol()).toBeNull();
      expect(servicio.usuarioId()).toBeNull();
      expect(llamadas).toBe(antes);
    } finally {
      globalThis.fetch = fetchOriginal;
    }
  });
});
