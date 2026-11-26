import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/** Solo el Admin entra; el resto vuelve a la bandeja. La API lo exige igual: esto solo evita una página vacía. */
export const soloAdmin: CanActivateFn = () =>
  inject(AuthService).rol() === 'ADMIN' || inject(Router).createUrlTree(['/tickets']);
