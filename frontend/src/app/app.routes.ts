import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { soloAdmin } from './core/rol.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./features/inicio/inicio.page').then((m) => m.InicioPage),
  },
  {
    path: 'login',
    loadComponent: () => import('./features/sesion/login.page').then((m) => m.LoginPage),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./core/shell').then((m) => m.Shell),
    children: [
      {
        path: 'metricas',
        canActivate: [soloAdmin],
        loadComponent: () => import('./features/metricas/metricas.page').then((m) => m.MetricasPage),
      },
      {
        path: 'tickets',
        loadComponent: () => import('./features/tickets/bandeja.page').then((m) => m.BandejaPage),
        children: [
          {
            path: '',
            pathMatch: 'full',
            loadComponent: () => import('./features/tickets/sin-seleccion').then((m) => m.SinSeleccion),
          },
          {
            path: 'nuevo',
            loadComponent: () => import('./features/tickets/alta.page').then((m) => m.AltaPage),
          },
          {
            path: ':id',
            loadComponent: () => import('./features/tickets/detalle.page').then((m) => m.DetallePage),
          },
        ],
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
