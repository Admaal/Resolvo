import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Sugerencia } from './sugerencia.model';
import { SugerenciasService } from './sugerencias.service';

const sugerencia: Sugerencia = {
  id: 's1',
  ticketId: 't1',
  borrador: 'Hola',
  estado: 'PENDIENTE',
  modelo: 'm',
  creadaEn: '2026-11-16T10:00:00Z',
  citas: [{ orden: 1, fuente: 'ARTICULO', fuenteId: 'a1', titulo: 'Guia' }],
};

describe('SugerenciasService', () => {
  let servicio: SugerenciasService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(SugerenciasService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sugiere con un POST al ticket', async () => {
    const promesa = servicio.sugerir('t1');
    const peticion = http.expectOne('/api/tickets/t1/sugerencias');
    expect(peticion.request.method).toBe('POST');
    peticion.flush(sugerencia);
    expect(await promesa).toEqual(sugerencia);
  });

  it('devuelve la sugerencia actual', async () => {
    const promesa = servicio.actual('t1');
    http.expectOne('/api/tickets/t1/sugerencias/actual').flush(sugerencia);
    expect(await promesa).toEqual(sugerencia);
  });

  it('devuelve null si no hay sugerencia pendiente (404)', async () => {
    const promesa = servicio.actual('t1');
    http
      .expectOne('/api/tickets/t1/sugerencias/actual')
      .flush({ error: 'no-encontrado' }, { status: 404, statusText: 'Not Found' });
    expect(await promesa).toBeNull();
  });

  it('propaga los errores que no son 404', async () => {
    const promesa = servicio.actual('t1');
    http
      .expectOne('/api/tickets/t1/sugerencias/actual')
      .flush({ error: 'prohibido' }, { status: 403, statusText: 'Forbidden' });
    await expect(promesa).rejects.toMatchObject({ status: 403 });
  });

  it('aprueba enviando el texto final', async () => {
    const promesa = servicio.aprobar('s1', 'Texto final');
    const peticion = http.expectOne('/api/sugerencias/s1/aprobar');
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({ texto: 'Texto final' });
    peticion.flush({ id: 'c1' });
    await promesa;
  });

  it('descarta con un POST', async () => {
    const promesa = servicio.descartar('s1');
    const peticion = http.expectOne('/api/sugerencias/s1/descartar');
    expect(peticion.request.method).toBe('POST');
    peticion.flush({ ...sugerencia, estado: 'DESCARTADA' });
    expect((await promesa).estado).toBe('DESCARTADA');
  });

  it('lee un articulo citado', async () => {
    const promesa = servicio.articulo('a1');
    http.expectOne('/api/articulos/a1').flush({ id: 'a1', titulo: 'Guia', cuerpo: 'Contenido' });
    expect(await promesa).toEqual({ id: 'a1', titulo: 'Guia', cuerpo: 'Contenido' });
  });
});
