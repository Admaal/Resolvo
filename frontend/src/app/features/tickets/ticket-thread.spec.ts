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

  it('envía el borrador recortado y lo limpia', () => {
    TestBed.configureTestingModule({ imports: [TicketThread] });
    const fixture = TestBed.createComponent(TicketThread);
    const enviados: unknown[] = [];
    fixture.componentInstance.enviado.subscribe((evento) => enviados.push(evento));
    fixture.componentInstance.borrador = ' hola ';
    fixture.componentInstance.enviar(true);

    expect(enviados).toEqual([{ cuerpo: 'hola', interno: true }]);
    expect(fixture.componentInstance.borrador).toBe('');
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
    expect(nodo.querySelector('.comentario.interno .chip.nota-interna')?.textContent).toContain('Nota interna');
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
});
