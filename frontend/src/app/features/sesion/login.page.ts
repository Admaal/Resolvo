import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  template: `
    <form class="formulario" (ngSubmit)="entrar()">
      <h1>Resolvo</h1>
      <label>Email <input name="email" type="email" [(ngModel)]="email" autocomplete="username" required /></label>
      <label>Contraseña <input name="contrasena" type="password" [(ngModel)]="contrasena" autocomplete="current-password" required /></label>
      @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
      <button class="btn primario" type="submit" [disabled]="entrando()">{{ entrando() ? 'Entrando…' : 'Entrar' }}</button>
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
  readonly entrando = signal(false);

  async entrar(): Promise<void> {
    if (this.entrando()) return;
    this.error.set('');
    this.entrando.set(true);
    try {
      await this.auth.entrar(this.email, this.contrasena);
      await this.router.navigateByUrl('/tickets');
    } catch (fallo) {
      this.error.set(textoDeError(fallo));
    } finally {
      this.entrando.set(false);
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
