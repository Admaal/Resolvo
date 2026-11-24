import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

const conToken = (req: HttpRequest<unknown>, token: string) => req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });

/**
 * Pone el access token y, si la API contesta 401 porque ha caducado (dura 15 minutos), renueva la
 * sesión con la cookie de refresco y reintenta la petición una vez. Reintentar es seguro: el 401 lo da
 * el filtro de seguridad antes de llegar al controlador, así que la primera no tuvo efectos. Si la
 * renovación falla, se vuelve al login con la ruta actual y la petición falla con su 401.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const usado = auth.token();
  if (!usado || req.url.includes('/api/sesion')) {
    return next(req);
  }
  return next(conToken(req, usado)).pipe(
    catchError((fallo: unknown) => {
      if (!(fallo instanceof HttpErrorResponse) || fallo.status !== 401) {
        return throwError(() => fallo);
      }
      return from(tokenRenovado(auth, usado)).pipe(
        switchMap((nuevo) => {
          if (nuevo) return next(conToken(req, nuevo));
          auth.expirar();
          void router.navigateByUrl(router.createUrlTree(['/login'], { queryParams: { volver: router.url } }));
          return throwError(() => fallo);
        }),
      );
    }),
  );
};

/** El token vigente si otra petición ya renovó mientras esta volaba; si no, renueva (una sola vez a la vez). */
async function tokenRenovado(auth: AuthService, usado: string): Promise<string | null> {
  const actual = auth.token();
  if (actual && actual !== usado) return actual;
  return (await auth.restaurar()) ? auth.token() : null;
}
