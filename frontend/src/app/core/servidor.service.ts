import { Injectable, signal } from '@angular/core';

const ESPERA_AVISO_MS = 2000;
const ESPERA_REINTENTO_MS = 2000;
const MAX_INTENTOS = 60;

/** Detecta que la API (Render Free) está arrancando en frío y deja un aviso a la vista. */
@Injectable({ providedIn: 'root' })
export class ServidorService {
  readonly despertando = signal(false);

  async comprobar(): Promise<void> {
    const aviso = setTimeout(() => this.despertando.set(true), ESPERA_AVISO_MS);
    try {
      for (let intento = 0; intento < MAX_INTENTOS; intento++) {
        if (await this.responde()) return;
        await new Promise((resolver) => setTimeout(resolver, ESPERA_REINTENTO_MS));
      }
    } finally {
      clearTimeout(aviso);
      this.despertando.set(false);
    }
  }

  private async responde(): Promise<boolean> {
    try {
      return (await fetch('/api/salud')).ok;
    } catch {
      return false;
    }
  }
}
