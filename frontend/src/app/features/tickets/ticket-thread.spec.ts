import { Component, input } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';
import { TicketThread } from './ticket-thread';
import { Comentario } from './ticket.model';

@Component({
  imports: [TicketThread],
  template: `<app-ticket-thread [comentarios]="comentarios()" yoId="yo" [autores]="{ ag: 'agente@acme.test' }" />`,
})
class Host {
  readonly comentarios = input<Comentario[]>([]);
}

describe('TicketThread', () => {
  it('pinta el cuerpo como texto', async () => {
    TestBed.configureTestingModule({ imports: [Host] });
    const fixture = TestBed.createComponent(Host);
    const marca = '<script>alert(1)</script>';
    fixture.componentRef.setInput('comentarios', [
      { id: '1', autorId: 'a', cuerpo: marca, interno: false, creadoEn: '2026-11-04T10:00:00Z' },
    ]);
    fixture.detectChanges();
    await fixture.whenStable();
    const nodo = fixture.nativeElement as HTMLElement;
    expect(nodo.querySelector('script')).toBeNull();
    expect(nodo.textContent).toContain(marca);
  });

  it('no envía un borrador vacío', () => {
    TestBed.configureTestingModule({ imports: [TicketThread] });
    const fixture = TestBed.createComponent(TicketThread);
    const enviados: unknown[] = [];
    fixture.componentInstance.enviado.subscribe((evento) => enviados.push(evento));
    fixture.componentInstance.borrador = '   ';
    fixture.componentInstance.enviar(false);

    expect(enviados).toEqual([]);
  });

  it('envía el borrador recortado y lo conserva hasta que se confirme', () => {
    TestBed.configureTestingModule({ imports: [TicketThread] });
    const fixture = TestBed.createComponent(TicketThread);
    const enviados: unknown[] = [];
    fixture.componentInstance.enviado.subscribe((evento) => enviados.push(evento));
    fixture.componentInstance.borrador = ' hola ';
    fixture.componentInstance.enviar(true);

    expect(enviados).toEqual([{ cuerpo: 'hola', interno: true }]);
    expect(fixture.componentInstance.borrador).toBe(' hola ');
  });

  it('limpiar vacía el borrador que se acaba de enviar', () => {
    TestBed.configureTestingModule({ imports: [TicketThread] });
    const fixture = TestBed.createComponent(TicketThread);
    fixture.componentInstance.borrador = ' hola ';
    fixture.componentInstance.enviar(false);
    fixture.componentInstance.limpiar();

    expect(fixture.componentInstance.borrador).toBe('');
  });

  it('limpiar no borra un texto distinto del que se envió', () => {
    TestBed.configureTestingModule({ imports: [TicketThread] });
    const fixture = TestBed.createComponent(TicketThread);
    fixture.componentInstance.borrador = 'hola';
    fixture.componentInstance.enviar(false);
    fixture.componentInstance.borrador = 'hola, y además esto';
    fixture.componentInstance.limpiar();

    expect(fixture.componentInstance.borrador).toBe('hola, y además esto');
  });

  it('distingue la nota interna del resto', async () => {
    TestBed.configureTestingModule({ imports: [Host] });
    const fixture = TestBed.createComponent(Host);
    fixture.componentRef.setInput('comentarios', [
      { id: '1', autorId: 'a', cuerpo: 'pública', interno: false, creadoEn: '2026-11-04T10:00:00Z' },
      { id: '2', autorId: 'a', cuerpo: 'privada', interno: true, creadoEn: '2026-11-04T10:05:00Z' },
    ]);
    fixture.detectChanges();
    await fixture.whenStable();
    const nodo = fixture.nativeElement as HTMLElement;

    expect(nodo.querySelectorAll('.comentario.interno').length).toBe(1);
    const etiqueta = nodo.querySelector('.comentario.interno .nota-interna');
    expect(etiqueta?.textContent).toContain('Nota interna');
    expect(etiqueta?.classList.contains('chip')).toBe(false);
    expect(etiqueta?.querySelector('rs-icono')).not.toBeNull();
  });

  it('enseña el cuerpo del ticket como primer mensaje', () => {
    TestBed.configureTestingModule({ imports: [TicketThread] });
    const fixture = TestBed.createComponent(TicketThread);
    fixture.componentRef.setInput('cuerpo', 'No puedo entrar');
    fixture.componentRef.setInput('autorEtiqueta', 'Cliente');
    fixture.componentRef.setInput('abiertoEn', '2026-11-04T09:00:00Z');
    fixture.detectChanges();
    const primero = (fixture.nativeElement as HTMLElement).querySelector('.mensaje');

    expect(primero?.textContent).toContain('No puedo entrar');
    expect(primero?.querySelector('.autor')?.textContent).toContain('Cliente');
  });

  it('Ctrl+Enter envía la respuesta', () => {
    TestBed.configureTestingModule({ imports: [TicketThread] });
    const fixture = TestBed.createComponent(TicketThread);
    const enviados: unknown[] = [];
    fixture.componentInstance.enviado.subscribe((evento) => enviados.push(evento));
    fixture.detectChanges();
    const area = (fixture.nativeElement as HTMLElement).querySelector('textarea') as HTMLTextAreaElement;
    fixture.componentInstance.borrador = 'hola';
    area.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', ctrlKey: true, bubbles: true }));

    expect(enviados).toEqual([{ cuerpo: 'hola', interno: false }]);
  });

  it('con la pestaña Nota interna el envío va como nota', () => {
    TestBed.configureTestingModule({ imports: [TicketThread] });
    const fixture = TestBed.createComponent(TicketThread);
    fixture.componentRef.setInput('puedeNota', true);
    const enviados: unknown[] = [];
    fixture.componentInstance.enviado.subscribe((evento) => enviados.push(evento));
    fixture.detectChanges();
    const pestanas = (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('[role="tab"]');
    pestanas[1].click();
    fixture.detectChanges();
    fixture.componentInstance.borrador = 'privado';
    fixture.componentInstance.enviar();

    expect(Array.from(pestanas).map((p) => p.textContent?.trim())).toEqual(['Responder', 'Nota interna']);
    expect(enviados).toEqual([{ cuerpo: 'privado', interno: true }]);
  });

  it('un cliente no tiene pestaña de nota interna', () => {
    TestBed.configureTestingModule({ imports: [TicketThread] });
    const fixture = TestBed.createComponent(TicketThread);
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelectorAll('[role="tab"]')).toHaveLength(0);
  });

  it('pone el nombre de cada autor y Tú para la persona actual', async () => {
    TestBed.configureTestingModule({ imports: [Host] });
    const fixture = TestBed.createComponent(Host);
    fixture.componentRef.setInput('comentarios', [
      { id: '1', autorId: 'yo', cuerpo: 'a', interno: false, creadoEn: '2026-11-04T10:00:00Z' },
      { id: '2', autorId: 'ag', cuerpo: 'b', interno: false, creadoEn: '2026-11-04T10:05:00Z' },
      { id: '3', autorId: 'otro', cuerpo: 'c', interno: false, creadoEn: '2026-11-04T10:06:00Z' },
    ]);
    fixture.detectChanges();
    await fixture.whenStable();
    const autores = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('.comentario .autor')).map((a) => a.textContent?.trim());

    expect(autores).toEqual(['Tú', 'agente@acme.test', 'Cliente']);
  });

  describe('moderación', () => {
    const base = { autorId: 'a', creadoEn: '2026-11-04T10:00:00Z', interno: false };
    const spam = { ...base, id: '1', cuerpo: 'compra ya', retirado: false, oculto: null };
    const retirado = {
      ...base,
      id: '2',
      cuerpo: 'texto-oculto',
      retirado: true,
      oculto: { motivo: 'SPAM', porId: 'ag', en: '2026-11-04T11:00:00Z' },
    };

    async function montar(comentarios: Comentario[], opciones: { moderar?: boolean; restaurar?: boolean } = {}) {
      TestBed.configureTestingModule({ imports: [TicketThread] });
      const fixture = TestBed.createComponent(TicketThread);
      fixture.componentRef.setInput('comentarios', comentarios);
      fixture.componentRef.setInput('puedeModerar', opciones.moderar ?? false);
      fixture.componentRef.setInput('puedeRestaurar', opciones.restaurar ?? false);
      fixture.componentRef.setInput('autores', { ag: 'agente@acme.test' });
      fixture.detectChanges();
      await fixture.whenStable();
      return fixture;
    }

    const nodo = (fixture: { nativeElement: unknown }) => fixture.nativeElement as HTMLElement;
    const pulsar = (fixture: { nativeElement: unknown; detectChanges(): void }, selector: string) => {
      (nodo(fixture).querySelector(selector) as HTMLButtonElement).click();
      fixture.detectChanges();
    };

    it('un agente elige el motivo y oculta el comentario', async () => {
      const fixture = await montar([spam], { moderar: true });
      const ocultados: unknown[] = [];
      fixture.componentInstance.ocultado.subscribe((evento) => ocultados.push(evento));

      pulsar(fixture, 'button.ocultar');
      const motivo = nodo(fixture).querySelector('select[name="motivo"]') as HTMLSelectElement;
      motivo.value = 'OFENSIVO';
      motivo.dispatchEvent(new Event('change'));
      pulsar(fixture, 'button.confirmar-ocultar');

      expect(ocultados).toEqual([{ id: '1', motivo: 'OFENSIVO' }]);
      expect(nodo(fixture).querySelector('select[name="motivo"]')).toBeNull();
    });

    it('el motivo por defecto es spam y se puede cancelar', async () => {
      const fixture = await montar([spam], { moderar: true });
      const ocultados: unknown[] = [];
      fixture.componentInstance.ocultado.subscribe((evento) => ocultados.push(evento));

      pulsar(fixture, 'button.ocultar');
      pulsar(fixture, 'button.cancelar-ocultar');
      pulsar(fixture, 'button.ocultar');
      pulsar(fixture, 'button.confirmar-ocultar');

      expect(ocultados).toEqual([{ id: '1', motivo: 'SPAM' }]);
    });

    it('quien no modera no ve el botón de ocultar', async () => {
      const fixture = await montar([spam], { moderar: false });

      expect(nodo(fixture).querySelector('button.ocultar')).toBeNull();
    });

    it('un comentario retirado se pliega con quién lo ocultó y por qué, sin enseñar el texto', async () => {
      const fixture = await montar([retirado], { moderar: true });
      const linea = nodo(fixture).querySelector('.comentario.retirado') as HTMLElement;

      expect(linea.textContent).toContain('Oculto por agente@acme.test');
      expect(linea.textContent).toContain('Spam');
      expect(linea.querySelector('rs-icono')).not.toBeNull();
      expect(linea.textContent).not.toContain('texto-oculto');
      expect(nodo(fixture).querySelector('button.ocultar')).toBeNull();
    });

    it('el equipo puede ver el texto de lo oculto', async () => {
      const fixture = await montar([retirado], { moderar: true });

      pulsar(fixture, 'button.ver-texto');

      expect(nodo(fixture).querySelector('.comentario.retirado')?.textContent).toContain('texto-oculto');
    });

    it('el cliente solo ve que se ha retirado', async () => {
      const paraElCliente = {
        ...retirado,
        cuerpo: null,
        oculto: { motivo: null, porId: null, en: null },
      };
      const fixture = await montar([paraElCliente], { moderar: false });
      const linea = nodo(fixture).querySelector('.comentario.retirado') as HTMLElement;

      expect(linea.textContent).toContain('Este comentario se ha retirado.');
      expect(linea.querySelector('button')).toBeNull();
      expect(linea.querySelector('rs-icono')).not.toBeNull();
    });

    it('solo el admin ve Restaurar y emite el id', async () => {
      const comoAgente = await montar([retirado], { moderar: true, restaurar: false });
      expect(nodo(comoAgente).querySelector('button.restaurar')).toBeNull();
      TestBed.resetTestingModule();

      const comoAdmin = await montar([retirado], { moderar: true, restaurar: true });
      const restaurados: string[] = [];
      comoAdmin.componentInstance.restaurado.subscribe((id) => restaurados.push(id));
      pulsar(comoAdmin, 'button.restaurar');

      expect(restaurados).toEqual(['2']);
    });
  });
});
