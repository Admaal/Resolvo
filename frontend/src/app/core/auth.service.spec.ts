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
});
