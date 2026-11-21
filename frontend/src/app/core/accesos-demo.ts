import { Component, inject, input, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { textoDeLogin } from './texto-login';

export const CONTRASENA_DEMO = 'demo-resolvo';

const ACCESOS = [
  { rol: 'Cliente', email: 'cliente@acme.test', detalle: 'Abre tickets y sigue su conversación' },
  { rol: 'Agente', email: 'agente@acme.test', detalle: 'Gestiona la bandeja y usa la IA' },
  { rol: 'Admin', email: 'admin@acme.test', detalle: 'Todo lo del agente y las métricas de IA' },
];

@Component({
  selector: 'app-accesos-demo',
  template: `
    <div class="accesos-demo">
      @for (acceso of accesos; track acceso.rol) {
        <button class="btn secundario acceso-btn" type="button" [disabled]="entrando()" (click)="entrar(acceso.email)">
          <span>Entrar como {{ acceso.rol }}</span>
          <small>{{ acceso.detalle }}</small>
        </button>
      }
    </div>
    @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
  `,
})
export class AccesosDemo {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly destino = input('/tickets');
  protected readonly accesos = ACCESOS;
  readonly entrando = signal(false);
  readonly error = signal('');

  async entrar(email: string): Promise<void> {
    if (this.entrando()) return;
    this.error.set('');
    this.entrando.set(true);
    try {
      await this.auth.entrar(email, CONTRASENA_DEMO);
      await this.router.navigateByUrl(this.destino());
    } catch (fallo) {
      this.error.set(textoDeLogin(fallo));
    } finally {
      this.entrando.set(false);
    }
  }
}
