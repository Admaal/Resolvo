import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MetricasIa } from './metricas.model';
import { MetricasPage } from './metricas.page';
import { MetricasService } from './metricas.service';

const AHORA = new Date('2026-11-26T12:00:00Z');
const DIA = 86_400_000;

const metricas: MetricasIa = {
  desde: '2026-11-19T12:00:00Z',
  hasta: '2026-11-26T12:00:00Z',
  llamadas: 63,
  fallos: 3,
  tokensEntrada: 26391,
  tokensSalida: 4925,
  costeUsd: 0.0123,
  nota: 'Coste en USD a precio de lista; nulo si algun modelo no tiene tarifa. El nivel gratuito cuesta 0.',
  filas: [
    { operacion: 'sugerir', modelo: 'gemini-3.1-flash-lite', llamadas: 40, fallos: 2, tokensEntrada: 20000, tokensSalida: 4000, tokensMedios: 600, p50Ms: 850, p95Ms: 8343, costeUsd: 0.01 },
    { operacion: 'clasificar', modelo: 'gemini-flash-lite-latest', llamadas: 23, fallos: 1, tokensEntrada: 6391, tokensSalida: 925, tokensMedios: 318, p50Ms: 600, p95Ms: 1200, costeUsd: null },
  ],
};

describe('MetricasPage', () => {
  let ia: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(AHORA);
    ia = vi.fn().mockResolvedValue(metricas);
  });

  afterEach(() => vi.useRealTimers());

  async function montar() {
    TestBed.configureTestingModule({
      imports: [MetricasPage],
      providers: [{ provide: MetricasService, useValue: { ia } }],
    });
    const fixture = TestBed.createComponent(MetricasPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return { fixture, raiz: fixture.nativeElement as HTMLElement };
  }

  const texto = (el: Element | null | undefined) => el?.textContent?.replace(/\s+/g, ' ').trim() ?? '';
  const botonDeRango = (raiz: HTMLElement, nombre: string) =>
    Array.from(raiz.querySelectorAll<HTMLButtonElement>('.rango button')).find((b) => texto(b) === nombre);

  it('pide por defecto los últimos 7 días', async () => {
    await montar();

    expect(ia).toHaveBeenCalledWith(new Date(AHORA.getTime() - 7 * DIA));
  });

  it('pinta el resumen del rango', async () => {
    const { raiz } = await montar();

    expect(texto(raiz.querySelector('.metricas-total'))).toContain('63');
    const resumen = texto(raiz.querySelector('.metricas-resumen'));
    expect(resumen).toContain('3 (4,8 %)');
    expect(resumen).toContain('26.391');
    expect(resumen).toContain('4925');
    expect(resumen).toContain('0,0123');
  });

  it('con una sola llamada habla en singular', async () => {
    ia.mockResolvedValue({ ...metricas, llamadas: 1, fallos: 0 });
    const { raiz } = await montar();

    expect(texto(raiz.querySelector('.metricas-total'))).toMatch(/^1 llamada a la IA/);
  });

  it('pinta una fila por operación y modelo, con nombres legibles', async () => {
    const { raiz } = await montar();
    const filas = Array.from(raiz.querySelectorAll('tbody tr')).map(texto);

    expect(filas).toHaveLength(2);
    expect(filas[0]).toContain('Propuesta de respuesta');
    expect(filas[0]).toContain('gemini-3.1-flash-lite');
    expect(filas[0]).toContain('850 ms');
    expect(filas[0]).toContain('8,3 s');
    expect(filas[1]).toContain('Clasificación');
  });

  it('un coste sin tarifa dice «Sin tarifa» con icono, nunca un 0', async () => {
    ia.mockResolvedValue({ ...metricas, costeUsd: null });
    const { raiz } = await montar();

    const coste = raiz.querySelector('.metricas-resumen .sin-tarifa');
    expect(texto(coste)).toBe('Sin tarifa');
    expect(coste?.querySelector('rs-icono')).not.toBeNull();
    expect(texto(raiz.querySelectorAll('tbody tr')[1]?.querySelector('.sin-tarifa'))).toBe('Sin tarifa');
    expect(texto(raiz.querySelector('.metricas-nota'))).toContain('precio de lista');
  });

  it('cambiar a 30 días vuelve a pedir con ese rango y marca el botón', async () => {
    const { fixture, raiz } = await montar();
    const boton = botonDeRango(raiz, '30 días');
    boton?.click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(ia).toHaveBeenLastCalledWith(new Date(AHORA.getTime() - 30 * DIA));
    expect(boton?.getAttribute('aria-pressed')).toBe('true');
    expect(botonDeRango(raiz, '7 días')?.getAttribute('aria-pressed')).toBe('false');
  });

  it('«Hoy» pide desde la medianoche local', async () => {
    const { fixture, raiz } = await montar();
    botonDeRango(raiz, 'Hoy')?.click();
    await fixture.whenStable();
    const medianoche = new Date(AHORA);
    medianoche.setHours(0, 0, 0, 0);

    expect(ia).toHaveBeenLastCalledWith(medianoche);
  });

  it('sin llamadas en el rango lo dice, sin tabla', async () => {
    ia.mockResolvedValue({ ...metricas, llamadas: 0, fallos: 0, tokensEntrada: 0, tokensSalida: 0, costeUsd: 0, filas: [] });
    const { raiz } = await montar();

    expect(texto(raiz.querySelector('.estado-vacio'))).toContain('No hay llamadas a la IA en este rango');
    expect(raiz.querySelector('table')).toBeNull();
  });

  it('si falla dice qué pasó y deja reintentar', async () => {
    ia.mockRejectedValueOnce(new HttpErrorResponse({ status: 500 }));
    const { fixture, raiz } = await montar();

    expect(texto(raiz.querySelector('[role="alert"]'))).toContain('No se pudieron cargar las métricas');
    raiz.querySelector<HTMLButtonElement>('[role="alert"] button')?.click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(ia).toHaveBeenCalledTimes(2);
    expect(raiz.querySelector('[role="alert"]')).toBeNull();
    expect(texto(raiz.querySelector('.metricas-total'))).toContain('63');
  });
});
