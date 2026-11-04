import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private access: string | null = null;
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
    const cuerpo = (await respuesta.json()) as { access: string };
    this.access = cuerpo.access;
    const datos = datosDelToken(cuerpo.access);
    this.rol.set(datos.rol);
    this.usuarioId.set(datos.sub);
  }

  async salir(): Promise<void> {
    this.access = null;
    this.rol.set(null);
    this.usuarioId.set(null);
    await fetch('/api/sesion', { method: 'DELETE', credentials: 'include' });
  }
}

export function datosDelToken(token: string): { rol: string; sub: string } {
  const parte = token.split('.')[1] ?? '';
  const json = atob(parte.replace(/-/g, '+').replace(/_/g, '/'));
  return JSON.parse(json) as { rol: string; sub: string };
}
