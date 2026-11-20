import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { describe, expect, it, vi } from 'vitest';
import { AuthService } from './auth.service';
import { authGuard } from './auth.guard';

function ejecutar(auth: Partial<AuthService>, url = '/tickets/abc?vista=mios') {
  TestBed.configureTestingModule({
    providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
  });
  return TestBed.runInInjectionContext(() =>
    authGuard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot),
  ) as Promise<boolean | UrlTree>;
}

describe('authGuard', () => {
  it('con token deja pasar sin pedir refresh', async () => {
    const restaurar = vi.fn();

    expect(await ejecutar({ token: () => 'x', restaurar })).toBe(true);
    expect(restaurar).not.toHaveBeenCalled();
  });

  it('sin token restaura la sesión y deja pasar', async () => {
    const restaurar = vi.fn().mockResolvedValue(true);

    expect(await ejecutar({ token: () => null, restaurar })).toBe(true);
    expect(restaurar).toHaveBeenCalledTimes(1);
  });

  it('si no puede restaurar redirige a /login con la ruta pedida', async () => {
    const resultado = await ejecutar({ token: () => null, restaurar: () => Promise.resolve(false) });
    const router = TestBed.inject(Router);

    expect(router.serializeUrl(resultado as UrlTree)).toBe('/login?volver=%2Ftickets%2Fabc%3Fvista%3Dmios');
  });
});
