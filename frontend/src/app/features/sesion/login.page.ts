import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AccesosDemo, CONTRASENA_DEMO } from '../../core/accesos-demo';
import { AuthService } from '../../core/auth.service';
import { textoDeLogin } from '../../core/texto-login';
import { Icono } from '../../shared/icono';

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink, AccesosDemo, Icono],
  template: `
    <div class="acceso">
      <aside class="acceso-lateral">
        <a class="marca" routerLink="/" aria-label="Resolvo, inicio">
          <span class="marca-logo" aria-hidden="true"><rs-icono nombre="check-circle" [tam]="14" /></span>
          Resolvo
        </a>
        <div class="acceso-pitch">
          <p class="acceso-titular">Responde antes de que venza el plazo, con la fuente a la vista.</p>
          <ul>
            <li><rs-icono nombre="file-text" /> Borradores de IA con citas a la base de conocimiento</li>
            <li><rs-icono nombre="clock" /> Bandeja agrupada por urgencia y SLA</li>
            <li><rs-icono nombre="shield-check" /> Nada se publica sin aprobación humana</li>
          </ul>
        </div>
        <p class="ayuda">Proyecto de portfolio · datos de demostración</p>
      </aside>
      <main class="acceso-principal">
        <a class="btn fantasma volver" routerLink="/"><rs-icono nombre="arrow-left" /> Volver al inicio</a>
        <form class="acceso-form" (ngSubmit)="entrar()">
          <h1>Inicia sesión</h1>
          <p class="ayuda">Entra con tu cuenta o prueba la demo con un clic.</p>
          <label>Email <input name="email" type="email" [(ngModel)]="email" autocomplete="username" required /></label>
          <label>Contraseña <input name="contrasena" type="password" [(ngModel)]="contrasena" autocomplete="current-password" required /></label>
          @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
          <button class="btn primario grande" type="submit" [disabled]="entrando()">{{ entrando() ? 'Entrando…' : 'Entrar' }}</button>
        </form>
        <div class="acceso-demo">
          <p class="separador"><span>o usa la demostración</span></p>
          <app-accesos-demo [destino]="destino()" />
          <p class="ayuda">La contraseña de las cuentas demo es {{ contrasenaDemo }}.</p>
        </div>
      </main>
    </div>
  `,
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  protected readonly contrasenaDemo = CONTRASENA_DEMO;
  email = 'cliente@acme.test';
  contrasena = CONTRASENA_DEMO;
  readonly error = signal('');
  readonly entrando = signal(false);

  destino(): string {
    return rutaDeVuelta(this.route.snapshot.queryParamMap.get('volver'));
  }

  async entrar(): Promise<void> {
    if (this.entrando()) return;
    this.error.set('');
    this.entrando.set(true);
    try {
      await this.auth.entrar(this.email, this.contrasena);
      await this.router.navigateByUrl(this.destino());
    } catch (fallo) {
      this.error.set(textoDeLogin(fallo));
    } finally {
      this.entrando.set(false);
    }
  }
}

/** Solo se vuelve a rutas internas absolutas: nunca a otro sitio. */
export function rutaDeVuelta(volver: string | null): string {
  return volver && volver.startsWith('/') && !volver.startsWith('//') ? volver : '/tickets';
}
