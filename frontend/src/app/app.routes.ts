import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/sesion/login.page').then((m) => m.LoginPage),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./core/shell').then((m) => m.Shell),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'tickets' },
      {
        path: 'tickets',
        loadComponent: () => import('./features/tickets/bandeja.page').then((m) => m.BandejaPage),
      },
      {
        path: 'tickets/nuevo',
        loadComponent: () => import('./features/tickets/alta.page').then((m) => m.AltaPage),
      },
      {
        path: 'tickets/:id',
        loadComponent: () => import('./features/tickets/detalle.page').then((m) => m.DetallePage),
      },
    ],
  },
];
