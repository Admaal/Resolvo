import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './auth.service';
import { CLAVE_TEMA, Tema } from './tema';

const EXACTO = { paths: 'exact', queryParams: 'exact', fragment: 'ignored', matrixParams: 'ignored' } as const;

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="shell">
      <aside class="sidebar">
        <a class="marca" routerLink="/tickets">Resolvo</a>
        <nav class="nav" aria-label="Principal">
          <a routerLink="/tickets" [queryParams]="{ vista: 'mios' }" routerLinkActive="activo" ariaCurrentWhenActive="page" [routerLinkActiveOptions]="exacto">Mis tickets</a>
          <a routerLink="/tickets" [queryParams]="{ vista: 'sin' }" routerLinkActive="activo" ariaCurrentWhenActive="page" [routerLinkActiveOptions]="exacto">Sin asignar</a>
          <a routerLink="/tickets" routerLinkActive="activo" ariaCurrentWhenActive="page" [routerLinkActiveOptions]="exacto">Todos</a>
          <a routerLink="/tickets/nuevo" routerLinkActive="activo" ariaCurrentWhenActive="page" [routerLinkActiveOptions]="exacto">Nuevo ticket</a>
        </nav>
        <div class="sidebar-pie">
          <button class="btn fantasma tema" type="button" (click)="tema()">Tema {{ oscuro() ? 'claro' : 'oscuro' }}</button>
          <button class="btn fantasma" type="button" (click)="salir()">Salir</button>
        </div>
      </aside>
      <main class="contenido">
        <router-outlet />
      </main>
    </div>
  `,
})
export class Shell {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly exacto = EXACTO;
  readonly oscuro = signal(document.documentElement.dataset['theme'] !== 'light');

  tema(): void {
    const siguiente: Tema = this.oscuro() ? 'light' : 'dark';
    document.documentElement.dataset['theme'] = siguiente;
    try {
      localStorage.setItem(CLAVE_TEMA, siguiente);
    } catch {
      // sin almacenamiento: el cambio vale solo para esta sesión
    }
    this.oscuro.set(siguiente === 'dark');
  }

  async salir(): Promise<void> {
    await this.auth.salir();
    await this.router.navigateByUrl('/login');
  }
}
