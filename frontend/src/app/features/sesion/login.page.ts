import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButton } from '@angular/material/button';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule, MatButton],
  template: `
    <form class="login" (ngSubmit)="entrar()">
      <h1>Resolvo</h1>
      <label>Email <input name="email" [(ngModel)]="email" autocomplete="username" /></label>
      <label>Contraseña <input name="contrasena" type="password" [(ngModel)]="contrasena" autocomplete="current-password" /></label>
      @if (error()) { <p class="error">{{ error() }}</p> }
      <button matButton class="primario" type="submit">Entrar</button>
      <p class="ayuda">Demostración: cliente@acme.test, agente@acme.test o admin@acme.test. Contraseña demo-resolvo.</p>
    </form>
  `,
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  email = 'cliente@acme.test';
  contrasena = 'demo-resolvo';
  readonly error = signal('');

  async entrar(): Promise<void> {
    this.error.set('');
    try {
      await this.auth.entrar(this.email, this.contrasena);
      await this.router.navigateByUrl('/tickets');
    } catch (fallo) {
      this.error.set(textoDeError(fallo));
    }
  }
}

function textoDeError(fallo: unknown): string {
  switch (fallo instanceof Error ? fallo.message : '') {
    case 'demasiados-intentos':
      return 'Demasiados intentos fallidos. Espera unos minutos e inténtalo de nuevo.';
    case 'no-autorizado':
      return 'Email o contraseña incorrectos.';
    default:
      return 'No se pudo entrar, inténtalo de nuevo.';
  }
}
