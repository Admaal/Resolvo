import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private access: string | null = null;
  private enCurso: Promise<boolean> | null = null;
  readonly rol = signal<string | null>(null);
  readonly usuarioId = signal<string | null>(null);

  token(): string | null {
    return this.access;
  }

  async entrar(email: string, contrasena: string): Promise<void> {
    const respuesta = await fetch('/api/sesion', {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, contrasena }),
    });
    if (!respuesta.ok) {
      const cuerpo = await respuesta.json().catch(() => ({ error: 'no-autorizado' }));
      throw new Error(cuerpo.error ?? 'no-autorizado');
    }
    this.guardar(((await respuesta.json()) as { access: string }).access);
  }

  /**
   * Recupera la sesión con la cookie de refresco: al recargar la página o cuando el access token
   * caduca. El refresco rota la cookie y es de un solo uso (reutilizar una ya gastada revoca toda la
   * sesión), así que las llamadas concurrentes comparten una única petición.
   */
  restaurar(): Promise<boolean> {
    this.enCurso ??= this.pedirRefresh().finally(() => (this.enCurso = null));
    return this.enCurso;
  }

  private async pedirRefresh(): Promise<boolean> {
    try {
      const respuesta = await fetch('/api/sesion/refresh', { method: 'POST', credentials: 'include' });
      if (!respuesta.ok) return false;
      this.guardar(((await respuesta.json()) as { access: string }).access);
      return true;
    } catch {
      return false;
    }
  }

  async salir(): Promise<void> {
    this.expirar();
    await fetch('/api/sesion', { method: 'DELETE', credentials: 'include' });
  }

  /** Olvida la sesión en local, sin avisar a la API: sirve cuando la cookie de refresco ya no vale. */
  expirar(): void {
    this.access = null;
    this.rol.set(null);
    this.usuarioId.set(null);
  }

  private guardar(access: string): void {
    this.access = access;
    const datos = datosDelToken(access);
    this.rol.set(datos.rol);
    this.usuarioId.set(datos.sub);
  }
}

export function datosDelToken(token: string): { rol: string; sub: string } {
  const parte = token.split('.')[1] ?? '';
  const json = atob(parte.replace(/-/g, '+').replace(/_/g, '/'));
  return JSON.parse(json) as { rol: string; sub: string };
}
