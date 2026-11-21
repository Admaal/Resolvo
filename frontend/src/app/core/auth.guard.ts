import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = async (_ruta, estado) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.token() || (await auth.restaurar())) {
    return true;
  }
  return router.createUrlTree(['/login'], { queryParams: { volver: estado.url } });
};
