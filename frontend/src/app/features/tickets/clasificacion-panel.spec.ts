import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { AuthService } from '../../core/auth.service';
import { Clasificacion } from './clasificacion.model';
import { ClasificacionPanel } from './clasificacion-panel';
import { ClasificacionesService } from './clasificaciones.service';
import { Categoria } from './ticket.model';

const categorias: Categoria[] = [
  { id: 'cat1', nombre: 'Incidencia' },
  { id: 'cat2', nombre: 'Consulta' },
];

const propuesta: Clasificacion = {
  id: 'c1',
  ticketId: 't1',
  categoriaId: 'cat1',
  prioridad: 'URGENTE',
  motivo: 'El servicio esta caido',
  estado: 'PENDIENTE',
  modelo: 'm',
  creadaEn: '2026-11-16T10:00:00Z',
};

function fallo(status: number, error: string): HttpErrorResponse {
  return new HttpErrorResponse({ status, error: { error } });
}

@Component({
  imports: [ClasificacionPanel],
  template: `<app-clasificacion-panel ticketId="t1" [categorias]="categorias" (aplicada)="aplicadas = aplicadas + 1" />`,
})
class Host {
  categorias = categorias;
  aplicadas = 0;
}

describe('ClasificacionPanel', () => {
  let servicio: {
    actual: ReturnType<typeof vi.fn>;
    sugerir: ReturnType<typeof vi.fn>;
    aplicar: ReturnType<typeof vi.fn>;
    descartar: ReturnType<typeof vi.fn>;
  };

  function montar(rol: string): ComponentFixture<Host> {
    TestBed.configureTestingModule({
      imports: [Host],
      providers: [{ provide: ClasificacionesService, useValue: servicio }],
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

  function elegir(fixture: ComponentFixture<Host>, selector: string, valor: string): void {
    const select = el(fixture, selector) as HTMLSelectElement;
    select.value = valor;
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  }

  beforeEach(() => {
    servicio = {
      actual: vi.fn().mockResolvedValue(null),
      sugerir: vi.fn().mockResolvedValue(propuesta),
      aplicar: vi.fn().mockResolvedValue({}),
      descartar: vi.fn().mockResolvedValue({ ...propuesta, estado: 'DESCARTADA' }),
    };
  });

  it('noSePintaAUnCliente ni consulta la API', async () => {
    const fixture = montar('CLIENTE');
    await asentar(fixture);

    expect(el(fixture, 'app-clasificacion-panel')?.textContent?.trim()).toBe('');
    expect(el(fixture, '.sugerir-clasificacion')).toBeNull();
    expect(servicio.actual).not.toHaveBeenCalled();
  });

  it('muestra el boton al personal', async () => {
    const fixture = montar('AGENTE');
    await asentar(fixture);

    expect(servicio.actual).toHaveBeenCalledWith('t1');
    expect(el(fixture, '.sugerir-clasificacion')).not.toBeNull();
    expect(el(fixture, 'select.categoria')).toBeNull();
  });

  it('un admin tambien lo ve', async () => {
    const fixture = montar('ADMIN');
    await asentar(fixture);

    expect(el(fixture, '.sugerir-clasificacion')).not.toBeNull();
  });

  it('carga la propuesta pendiente con la categoria y la prioridad preseleccionadas', async () => {
    servicio.actual.mockResolvedValue(propuesta);
    const fixture = montar('AGENTE');
    await asentar(fixture);

    expect((el(fixture, 'select.categoria') as HTMLSelectElement).value).toBe('cat1');
    expect((el(fixture, 'select.prioridad') as HTMLSelectElement).value).toBe('URGENTE');
    expect(el(fixture, '.marca-ia')?.textContent).toContain('sugerida por IA');
    expect(el(fixture, '.motivo')?.textContent).toContain('El servicio esta caido');
  });

  it('sugerir muestra la propuesta con las categorias de la organizacion', async () => {
    const fixture = montar('AGENTE');
    await asentar(fixture);

    pulsar(fixture, '.sugerir-clasificacion');
    await asentar(fixture);

    expect(servicio.sugerir).toHaveBeenCalledWith('t1');
    const opciones = Array.from((el(fixture, 'select.categoria') as HTMLSelectElement).options).map((o) => o.text);
    expect(opciones).toEqual(['Incidencia', 'Consulta']);
    const prioridades = Array.from((el(fixture, 'select.prioridad') as HTMLSelectElement).options).map((o) => o.value);
    expect(prioridades).toEqual(['BAJA', 'MEDIA', 'ALTA', 'URGENTE']);
  });

  it('aplicarEnviaLoEditadoPorElAgenteYAvisaAlDetalle', async () => {
    servicio.actual.mockResolvedValue(propuesta);
    const fixture = montar('AGENTE');
    await asentar(fixture);
    elegir(fixture, 'select.categoria', 'cat2');
    elegir(fixture, 'select.prioridad', 'BAJA');

    pulsar(fixture, '.aplicar-clasificacion');
    await asentar(fixture);

    expect(servicio.aplicar).toHaveBeenCalledWith('c1', 'cat2', 'BAJA');
    expect(fixture.componentInstance.aplicadas).toBe(1);
    expect(el(fixture, 'select.categoria')).toBeNull();
  });

  it('aplicar sin tocar nada envia lo propuesto', async () => {
    servicio.actual.mockResolvedValue(propuesta);
    const fixture = montar('AGENTE');
    await asentar(fixture);

    pulsar(fixture, '.aplicar-clasificacion');
    await asentar(fixture);

    expect(servicio.aplicar).toHaveBeenCalledWith('c1', 'cat1', 'URGENTE');
  });

  it('botonDeshabilitadoMientrasEnvia: un doble clic aplica una sola vez', async () => {
    servicio.actual.mockResolvedValue(propuesta);
    let resolver!: (valor: unknown) => void;
    servicio.aplicar.mockReturnValue(new Promise((r) => (resolver = r)));
    const fixture = montar('AGENTE');
    await asentar(fixture);

    pulsar(fixture, '.aplicar-clasificacion');
    expect((el(fixture, '.aplicar-clasificacion') as HTMLButtonElement).disabled).toBe(true);
    (el(fixture, '.aplicar-clasificacion') as HTMLButtonElement).click();
    resolver({});
    await asentar(fixture);

    expect(servicio.aplicar).toHaveBeenCalledTimes(1);
  });

  it('el boton sugerir tambien se deshabilita mientras espera al proveedor', async () => {
    let resolver!: (valor: Clasificacion) => void;
    servicio.sugerir.mockReturnValue(new Promise((r) => (resolver = r)));
    const fixture = montar('AGENTE');
    await asentar(fixture);

    pulsar(fixture, '.sugerir-clasificacion');
    expect((el(fixture, '.sugerir-clasificacion') as HTMLButtonElement).disabled).toBe(true);
    (el(fixture, '.sugerir-clasificacion') as HTMLButtonElement).click();
    resolver(propuesta);
    await asentar(fixture);

    expect(servicio.sugerir).toHaveBeenCalledTimes(1);
  });

  it('con503MuestraIaNoDisponible segun el motivo', async () => {
    const casos: [string, string][] = [
      ['ia-no-configurada', 'no está disponible en este entorno'],
      ['ia-cuota-agotada', 'Cuota de IA agotada'],
      ['ia-no-disponible', 'no responde'],
      ['clasificacion-invalida', 'no válida'],
    ];
    for (const [motivo, texto] of casos) {
      TestBed.resetTestingModule();
      servicio.sugerir.mockRejectedValue(fallo(503, motivo));
      const fixture = montar('AGENTE');
      await asentar(fixture);

      pulsar(fixture, '.sugerir-clasificacion');
      await asentar(fixture);

      expect(el(fixture, '.aviso')?.textContent).toContain(texto);
      expect(el(fixture, 'select.categoria')).toBeNull();
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

      pulsar(fixture, '.sugerir-clasificacion');
      await asentar(fixture);

      expect(el(fixture, '.aviso')?.textContent).toContain(texto);
    }
  });

  it('un conflicto al aplicar avisa y recarga el estado', async () => {
    servicio.actual.mockResolvedValueOnce(propuesta).mockResolvedValueOnce(null);
    servicio.aplicar.mockRejectedValue(fallo(409, 'conflicto'));
    const fixture = montar('AGENTE');
    await asentar(fixture);

    pulsar(fixture, '.aplicar-clasificacion');
    await asentar(fixture);

    expect(el(fixture, '.aviso')?.textContent).toContain('ya fue resuelta');
    expect(servicio.actual).toHaveBeenCalledTimes(2);
    expect(fixture.componentInstance.aplicadas).toBe(0);
  });

  it('descartar limpia el panel y permite pedir otra propuesta', async () => {
    servicio.actual.mockResolvedValue(propuesta);
    const fixture = montar('AGENTE');
    await asentar(fixture);

    pulsar(fixture, '.descartar-clasificacion');
    await asentar(fixture);

    expect(servicio.descartar).toHaveBeenCalledWith('c1');
    expect(el(fixture, 'select.categoria')).toBeNull();
    expect(el(fixture, '.sugerir-clasificacion')).not.toBeNull();
    expect(fixture.componentInstance.aplicadas).toBe(0);
  });

  it('pinta el motivo como texto y no como HTML', async () => {
    servicio.actual.mockResolvedValue({ ...propuesta, motivo: '<img src=x onerror=alert(1)>' });
    const fixture = montar('AGENTE');
    await asentar(fixture);

    expect(el(fixture, 'img')).toBeNull();
    expect(el(fixture, '.motivo')?.textContent).toContain('<img src=x onerror=alert(1)>');
  });
});
