import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { AuthService } from '../../core/auth.service';
import { Sugerencia } from './sugerencia.model';
import { SugerenciaPanel } from './sugerencia-panel';
import { SugerenciasService } from './sugerencias.service';

const borrador: Sugerencia = {
  id: 's1',
  ticketId: 't1',
  borrador: 'Pulsa el boton reset.',
  estado: 'PENDIENTE',
  modelo: 'm',
  creadaEn: '2026-11-16T10:00:00Z',
  citas: [
    { orden: 1, fuente: 'ARTICULO', fuenteId: 'a1', titulo: 'Reiniciar router' },
    { orden: 2, fuente: 'TICKET', fuenteId: 't9', titulo: 'RS-4 Cambio de factura' },
  ],
};

function fallo(status: number, error: string): HttpErrorResponse {
  return new HttpErrorResponse({ status, error: { error } });
}

@Component({
  imports: [SugerenciaPanel],
  template: `<app-sugerencia-panel ticketId="t1" (aprobada)="aprobadas = aprobadas + 1" />`,
})
class Host {
  aprobadas = 0;
}

describe('SugerenciaPanel', () => {
  let servicio: {
    actual: ReturnType<typeof vi.fn>;
    sugerir: ReturnType<typeof vi.fn>;
    aprobar: ReturnType<typeof vi.fn>;
    descartar: ReturnType<typeof vi.fn>;
    articulo: ReturnType<typeof vi.fn>;
  };

  function montar(rol: string): ComponentFixture<Host> {
    TestBed.configureTestingModule({
      imports: [Host],
      providers: [provideRouter([]), { provide: SugerenciasService, useValue: servicio }],
    });
    TestBed.inject(AuthService).rol.set(rol);
    const fixture = TestBed.createComponent(Host);
    fixture.detectChanges();
    return fixture;
  }

  async function asentar(fixture: ComponentFixture<Host>): Promise<void> {
    for (let vuelta = 0; vuelta < 2; vuelta++) {
      await new Promise((resolver) => setTimeout(resolver, 0));
      fixture.detectChanges();
    }
  }

  function el(fixture: ComponentFixture<Host>, selector: string): HTMLElement | null {
    return (fixture.nativeElement as HTMLElement).querySelector(selector);
  }

  function pulsar(fixture: ComponentFixture<Host>, selector: string): void {
    (el(fixture, selector) as HTMLButtonElement).click();
    fixture.detectChanges();
  }

  beforeEach(() => {
    servicio = {
      actual: vi.fn().mockResolvedValue(null),
      sugerir: vi.fn().mockResolvedValue(borrador),
      aprobar: vi.fn().mockResolvedValue({}),
      descartar: vi.fn().mockResolvedValue({ ...borrador, estado: 'DESCARTADA' }),
      articulo: vi.fn().mockResolvedValue({ id: 'a1', titulo: 'Reiniciar router', cuerpo: 'Pulsa reset 10 s.' }),
    };
  });

  it('noSeMuestraAlCliente ni consulta la API', async () => {
    const fixture = montar('CLIENTE');
    await asentar(fixture);

    expect(el(fixture, 'app-sugerencia-panel')?.textContent?.trim()).toBe('');
    expect(el(fixture, '.sugerir')).toBeNull();
    expect(servicio.actual).not.toHaveBeenCalled();
  });

  it('muestra el boton al personal y carga la sugerencia pendiente', async () => {
    servicio.actual.mockResolvedValue(borrador);
    const fixture = montar('AGENTE');
    await asentar(fixture);

    expect(servicio.actual).toHaveBeenCalledWith('t1');
    expect((el(fixture, 'textarea.borrador') as HTMLTextAreaElement).value).toBe('Pulsa el boton reset.');
  });

  it('sugerir muestra el borrador editable marcado como IA y sus citas', async () => {
    const fixture = montar('AGENTE');
    await asentar(fixture);

    pulsar(fixture, '.sugerir');
    await asentar(fixture);

    expect(servicio.sugerir).toHaveBeenCalledWith('t1');
    expect(el(fixture, '.marca-ia')?.textContent).toContain('Borrador generado por IA');
    expect((el(fixture, 'textarea.borrador') as HTMLTextAreaElement).value).toBe('Pulsa el boton reset.');
    const citas = Array.from(fixture.nativeElement.querySelectorAll('.cita')) as HTMLElement[];
    expect(citas.map((cita) => cita.textContent)).toEqual([
      expect.stringContaining('Reiniciar router'),
      expect.stringContaining('RS-4 Cambio de factura'),
    ]);
  });

  it('la cita de un ticket enlaza al ticket y la de un articulo muestra su contenido', async () => {
    servicio.actual.mockResolvedValue(borrador);
    const fixture = montar('AGENTE');
    await asentar(fixture);

    expect((el(fixture, 'a.cita-ticket') as HTMLAnchorElement).getAttribute('href')).toBe('/tickets/t9');

    pulsar(fixture, 'button.cita-articulo');
    await asentar(fixture);

    expect(servicio.articulo).toHaveBeenCalledWith('a1');
    expect(el(fixture, '.articulo-citado')?.textContent).toContain('Pulsa reset 10 s.');
  });

  it('aprobarEnviaElTextoEditado y avisa al detalle', async () => {
    servicio.actual.mockResolvedValue(borrador);
    const fixture = montar('AGENTE');
    await asentar(fixture);
    const area = el(fixture, 'textarea.borrador') as HTMLTextAreaElement;
    area.value = 'Texto revisado por mi';
    area.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    pulsar(fixture, '.aprobar');
    await asentar(fixture);

    expect(servicio.aprobar).toHaveBeenCalledWith('s1', 'Texto revisado por mi');
    expect(fixture.componentInstance.aprobadas).toBe(1);
    expect(el(fixture, 'textarea.borrador')).toBeNull();
  });

  it('botonDeshabilitadoMientrasEnvia: un doble clic aprueba una sola vez', async () => {
    servicio.actual.mockResolvedValue(borrador);
    let resolver!: (valor: unknown) => void;
    servicio.aprobar.mockReturnValue(new Promise((r) => (resolver = r)));
    const fixture = montar('AGENTE');
    await asentar(fixture);

    pulsar(fixture, '.aprobar');
    expect((el(fixture, '.aprobar') as HTMLButtonElement).disabled).toBe(true);
    (el(fixture, '.aprobar') as HTMLButtonElement).click();
    resolver({});
    await asentar(fixture);

    expect(servicio.aprobar).toHaveBeenCalledTimes(1);
  });

  it('el boton sugerir tambien se deshabilita mientras espera al proveedor', async () => {
    let resolver!: (valor: Sugerencia) => void;
    servicio.sugerir.mockReturnValue(new Promise((r) => (resolver = r)));
    const fixture = montar('AGENTE');
    await asentar(fixture);

    pulsar(fixture, '.sugerir');
    expect((el(fixture, '.sugerir') as HTMLButtonElement).disabled).toBe(true);
    (el(fixture, '.sugerir') as HTMLButtonElement).click();
    resolver(borrador);
    await asentar(fixture);

    expect(servicio.sugerir).toHaveBeenCalledTimes(1);
  });

  it('muestraAvisoSinFuentes', async () => {
    servicio.sugerir.mockRejectedValue(fallo(422, 'sin-fuentes'));
    const fixture = montar('AGENTE');
    await asentar(fixture);

    pulsar(fixture, '.sugerir');
    await asentar(fixture);

    expect(el(fixture, '.aviso')?.textContent).toContain('No hay fuentes');
    expect(el(fixture, 'textarea.borrador')).toBeNull();
  });

  it('muestra los avisos de IA no disponible segun el motivo', async () => {
    const casos: [string, string][] = [
      ['ia-no-configurada', 'no está disponible en este entorno'],
      ['ia-cuota-agotada', 'Cuota de IA agotada'],
      ['ia-no-disponible', 'no responde'],
    ];
    for (const [motivo, texto] of casos) {
      TestBed.resetTestingModule();
      servicio.sugerir.mockRejectedValue(fallo(503, motivo));
      const fixture = montar('AGENTE');
      await asentar(fixture);

      pulsar(fixture, '.sugerir');
      await asentar(fixture);

      expect(el(fixture, '.aviso')?.textContent).toContain(texto);
    }
  });

  it('con429MuestraElLimiteDeUsoSegunElMotivo', async () => {
    const casos: [string, string][] = [
      ['demasiadas-peticiones', 'Demasiadas peticiones'],
      ['ia-presupuesto-agotado', 'límite de IA de hoy'],
    ];
    for (const [motivo, texto] of casos) {
      TestBed.resetTestingModule();
      servicio.sugerir.mockRejectedValue(fallo(429, motivo));
      const fixture = montar('AGENTE');
      await asentar(fixture);

      pulsar(fixture, '.sugerir');
      await asentar(fixture);

      expect(el(fixture, '.aviso')?.textContent).toContain(texto);
    }
  });

  it('un conflicto al aprobar avisa y recarga el estado', async () => {
    servicio.actual.mockResolvedValueOnce(borrador).mockResolvedValueOnce(null);
    servicio.aprobar.mockRejectedValue(fallo(409, 'conflicto'));
    const fixture = montar('AGENTE');
    await asentar(fixture);

    pulsar(fixture, '.aprobar');
    await asentar(fixture);

    expect(el(fixture, '.aviso')?.textContent).toContain('ya fue resuelta');
    expect(servicio.actual).toHaveBeenCalledTimes(2);
    expect(fixture.componentInstance.aprobadas).toBe(0);
  });

  it('no deja aprobar un texto vacio', async () => {
    servicio.actual.mockResolvedValue(borrador);
    const fixture = montar('AGENTE');
    await asentar(fixture);
    const area = el(fixture, 'textarea.borrador') as HTMLTextAreaElement;
    area.value = '   ';
    area.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect((el(fixture, '.aprobar') as HTMLButtonElement).disabled).toBe(true);
  });

  it('descartar limpia el panel y permite pedir otra sugerencia', async () => {
    servicio.actual.mockResolvedValue(borrador);
    const fixture = montar('AGENTE');
    await asentar(fixture);

    pulsar(fixture, '.descartar');
    await asentar(fixture);

    expect(servicio.descartar).toHaveBeenCalledWith('s1');
    expect(el(fixture, 'textarea.borrador')).toBeNull();
    expect(el(fixture, '.sugerir')).not.toBeNull();
  });

  it('pinta el borrador como texto y no como HTML', async () => {
    servicio.actual.mockResolvedValue({ ...borrador, borrador: '<img src=x onerror=alert(1)>' });
    const fixture = montar('AGENTE');
    await asentar(fixture);

    expect(el(fixture, 'img')).toBeNull();
    expect((el(fixture, 'textarea.borrador') as HTMLTextAreaElement).value).toBe('<img src=x onerror=alert(1)>');
  });
});
