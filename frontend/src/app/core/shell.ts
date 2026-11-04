import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButton } from '@angular/material/button';
import { AuthService } from './auth.service';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatButton],
  template: `
    <div class="shell">
      <aside class="sidebar">
        <div class="marca">Resolvo</div>
        <nav class="nav">
          <a routerLink="/tickets" [queryParams]="{ vista: 'mios' }" routerLinkActive="activo">Mis tickets</a>
          <a routerLink="/tickets" [queryParams]="{ vista: 'sin' }" routerLinkActive="activo">Sin asignar</a>
          <a routerLink="/tickets" [queryParams]="{ vista: 'todos' }" routerLinkActive="activo">Todos</a>
          <a routerLink="/tickets/nuevo">Nuevo ticket</a>
        </nav>
        <button class="secundario" type="button" (click)="tema()">Tema {{ oscuro() ? 'claro' : 'oscuro' }}</button>
        <button matButton type="button" (click)="salir()">Salir</button>
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
  readonly oscuro = signal(document.documentElement.dataset['theme'] !== 'light');

  tema(): void {
    const siguiente = this.oscuro() ? 'light' : 'dark';
    document.documentElement.dataset['theme'] = siguiente;
    this.oscuro.set(siguiente === 'dark');
  }

  async salir(): Promise<void> {
    await this.auth.salir();
    await this.router.navigateByUrl('/login');
  }
}
