import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Clasificacion } from './clasificacion.model';
import { ClasificacionesService } from './clasificaciones.service';

const clasificacion: Clasificacion = {
  id: 'c1',
  ticketId: 't1',
  categoriaId: 'cat1',
  prioridad: 'URGENTE',
  motivo: 'El servicio esta caido',
  estado: 'PENDIENTE',
  modelo: 'm',
  creadaEn: '2026-11-16T10:00:00Z',
};

describe('ClasificacionesService', () => {
  let servicio: ClasificacionesService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(ClasificacionesService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sugiere con un POST al ticket', async () => {
    const promesa = servicio.sugerir('t1');
    const peticion = http.expectOne('/api/tickets/t1/clasificaciones');
    expect(peticion.request.method).toBe('POST');
    peticion.flush(clasificacion);
    expect(await promesa).toEqual(clasificacion);
  });

  it('devuelve la clasificacion actual', async () => {
    const promesa = servicio.actual('t1');
    http.expectOne('/api/tickets/t1/clasificaciones/actual').flush(clasificacion);
    expect(await promesa).toEqual(clasificacion);
  });

  it('devuelve null si no hay clasificacion pendiente (404)', async () => {
    const promesa = servicio.actual('t1');
    http
      .expectOne('/api/tickets/t1/clasificaciones/actual')
      .flush({ error: 'no-encontrado' }, { status: 404, statusText: 'Not Found' });
    expect(await promesa).toBeNull();
  });

  it('propaga los errores que no son 404', async () => {
    const promesa = servicio.actual('t1');
    http
      .expectOne('/api/tickets/t1/clasificaciones/actual')
      .flush({ error: 'prohibido' }, { status: 403, statusText: 'Forbidden' });
    await expect(promesa).rejects.toMatchObject({ status: 403 });
  });

  it('aplica enviando la categoria y la prioridad elegidas', async () => {
    const promesa = servicio.aplicar('c1', 'cat2', 'BAJA');
    const peticion = http.expectOne('/api/clasificaciones/c1/aplicacion');
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({ categoriaId: 'cat2', prioridad: 'BAJA' });
    peticion.flush({ id: 't1' });
    expect(await promesa).toEqual({ id: 't1' });
  });

  it('descarta con un POST', async () => {
    const promesa = servicio.descartar('c1');
    const peticion = http.expectOne('/api/clasificaciones/c1/descarte');
    expect(peticion.request.method).toBe('POST');
    peticion.flush({ ...clasificacion, estado: 'DESCARTADA' });
    expect((await promesa).estado).toBe('DESCARTADA');
  });
});
