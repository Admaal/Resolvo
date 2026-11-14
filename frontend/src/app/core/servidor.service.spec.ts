import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, vi } from 'vitest';
import { ServidorService } from './servidor.service';

describe('ServidorService', () => {
  const fetchOriginal = globalThis.fetch;

  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
    globalThis.fetch = fetchOriginal;
  });

  it('marca despertando si /api/salud tarda más de 2 s y lo quita al responder', async () => {
    let responder!: (r: Response) => void;
    const llamadas: string[] = [];
    globalThis.fetch = ((url: string) => {
      llamadas.push(url);
      return new Promise<Response>((resolve) => (responder = resolve));
    }) as typeof fetch;

    const servicio = TestBed.inject(ServidorService);
    const comprobacion = servicio.comprobar();

    await vi.advanceTimersByTimeAsync(1900);
    expect(servicio.despertando()).toBe(false);
    await vi.advanceTimersByTimeAsync(200);
    expect(servicio.despertando()).toBe(true);

    responder(new Response('{"estado":"ok"}', { status: 200 }));
    await comprobacion;

    expect(servicio.despertando()).toBe(false);
    expect(llamadas).toEqual(['/api/salud']);
  });

  it('no avisa si el servidor responde a tiempo', async () => {
    globalThis.fetch = (() =>
      Promise.resolve(new Response('{"estado":"ok"}', { status: 200 }))) as typeof fetch;

    const servicio = TestBed.inject(ServidorService);
    await servicio.comprobar();
    await vi.advanceTimersByTimeAsync(5000);

    expect(servicio.despertando()).toBe(false);
  });

  it('reintenta mientras la API no responde con éxito y termina cuando responde', async () => {
    let intentos = 0;
    globalThis.fetch = (() => {
      intentos += 1;
      return intentos < 3
        ? Promise.resolve(new Response('', { status: 503 }))
        : Promise.resolve(new Response('{"estado":"ok"}', { status: 200 }));
    }) as typeof fetch;

    const servicio = TestBed.inject(ServidorService);
    const comprobacion = servicio.comprobar();
    await vi.advanceTimersByTimeAsync(10000);
    await comprobacion;

    expect(intentos).toBe(3);
    expect(servicio.despertando()).toBe(false);
  });
});
