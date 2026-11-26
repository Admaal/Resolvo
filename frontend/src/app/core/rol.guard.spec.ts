import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { describe, expect, it } from 'vitest';
import { AuthService } from './auth.service';
import { soloAdmin } from './rol.guard';

function ejecutar(rol: string | null): boolean | UrlTree {
  TestBed.configureTestingModule({
    providers: [provideRouter([]), { provide: AuthService, useValue: { rol: () => rol } }],
  });
  return TestBed.runInInjectionContext(() =>
    soloAdmin({} as ActivatedRouteSnapshot, { url: '/metricas' } as RouterStateSnapshot),
  ) as boolean | UrlTree;
}

describe('soloAdmin', () => {
  it('deja pasar al Admin', () => {
    expect(ejecutar('ADMIN')).toBe(true);
  });

  it.each(['AGENTE', 'CLIENTE', null])('manda a la bandeja a %s', (rol) => {
    const resultado = ejecutar(rol);

    expect(resultado).not.toBe(true);
    expect(TestBed.inject(Router).serializeUrl(resultado as UrlTree)).toBe('/tickets');
  });
});
