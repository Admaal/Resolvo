import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { MetricasIa } from './metricas.model';
import { MetricasService } from './metricas.service';

const metricas: MetricasIa = {
  desde: '2026-11-19T12:00:00Z',
  hasta: '2026-11-26T12:00:00Z',
  llamadas: 0,
  fallos: 0,
  tokensEntrada: 0,
  tokensSalida: 0,
  costeUsd: 0,
  nota: '',
  filas: [],
};

describe('MetricasService', () => {
  let servicio: MetricasService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(MetricasService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('pide las métricas de IA desde la fecha dada, en ISO', async () => {
    const promesa = servicio.ia(new Date('2026-11-19T12:00:00Z'));
    const peticion = http.expectOne((p) => p.url === '/api/metricas/ia');

    expect(peticion.request.method).toBe('GET');
    expect(peticion.request.params.get('desde')).toBe('2026-11-19T12:00:00.000Z');
    peticion.flush(metricas);
    expect(await promesa).toEqual(metricas);
  });
});
