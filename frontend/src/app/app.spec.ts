import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { afterEach } from 'vitest';
import { App } from './app';

describe('App', () => {
  const fetchOriginal = globalThis.fetch;

  beforeEach(async () => {
    globalThis.fetch = (() =>
      Promise.resolve(new Response('{"estado":"ok"}', { status: 200 }))) as typeof fetch;
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  afterEach(() => {
    globalThis.fetch = fetchOriginal;
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('no muestra el aviso de servidor si la API responde', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.aviso-servidor')).toBeNull();
  });
});
