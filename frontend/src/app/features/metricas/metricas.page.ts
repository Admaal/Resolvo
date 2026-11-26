import { Component, inject, signal } from '@angular/core';
import { Icono } from '../../shared/icono';
import { MetricasIa } from './metricas.model';
import { MetricasService } from './metricas.service';

type Rango = 'hoy' | '7' | '30';

const RANGOS: { valor: Rango; nombre: string }[] = [
  { valor: 'hoy', nombre: 'Hoy' },
  { valor: '7', nombre: '7 días' },
  { valor: '30', nombre: '30 días' },
];

const DIA = 86_400_000;

const OPERACIONES: Record<string, string> = {
  sugerir: 'Propuesta de respuesta',
  clasificar: 'Clasificación',
};

const ENTERO = new Intl.NumberFormat('es-ES');
const PORCENTAJE = new Intl.NumberFormat('es-ES', { maximumFractionDigits: 1 });
const SEGUNDOS = new Intl.NumberFormat('es-ES', { minimumFractionDigits: 1, maximumFractionDigits: 1 });
const USD = new Intl.NumberFormat('es-ES', { minimumFractionDigits: 2, maximumFractionDigits: 4 });
const FECHA = new Intl.DateTimeFormat('es-ES', { day: 'numeric', month: 'short' });

/** Desde cuándo pide cada rango: «Hoy» empieza a medianoche local; los demás cuentan días hacia atrás. */
export function inicioDe(rango: Rango, ahora: Date): Date {
  if (rango === 'hoy') {
    const medianoche = new Date(ahora);
    medianoche.setHours(0, 0, 0, 0);
    return medianoche;
  }
  return new Date(ahora.getTime() - Number(rango) * DIA);
}

@Component({
  selector: 'app-metricas',
  imports: [Icono],
  template: `
    <header class="panel-cabecera">
      <h1>Métricas de IA</h1>
      <span class="espacio"></span>
      <div class="rango" role="group" aria-label="Rango de fechas">
        @for (opcion of rangos; track opcion.valor) {
          <button type="button" [attr.aria-pressed]="rango() === opcion.valor" (click)="elegir(opcion.valor)">
            {{ opcion.nombre }}
          </button>
        }
      </div>
    </header>
    <div class="panel-cuerpo">
      <div class="metricas">
        @if (carga() === 'error') {
          <div class="aviso error" role="alert">
            <rs-icono nombre="alert" />
            <p>No se pudieron cargar las métricas. Comprueba la conexión y vuelve a intentarlo.</p>
            <button class="btn secundario" type="button" (click)="cargar()">Reintentar</button>
          </div>
        } @else if (carga() === 'cargando' && !datos()) {
          <p class="estado-carga">Cargando las métricas…</p>
        } @else if (datos(); as m) {
          @if (m.llamadas === 0) {
            <div class="estado-vacio">
              <rs-icono nombre="sparkles" [tam]="40" />
              <p>No hay llamadas a la IA en este rango.</p>
              <p class="ayuda">Aparecen al pedir una propuesta de respuesta o al clasificar un ticket.</p>
            </div>
          } @else {
            <section class="metricas-cabeza" aria-label="Resumen">
              <p class="metricas-total">
                <span class="cifra">{{ entero(m.llamadas) }}</span>
                <span class="texto">{{ m.llamadas === 1 ? 'llamada' : 'llamadas' }} a la IA {{ periodo(m) }}</span>
              </p>
              <dl class="metricas-resumen">
                <div>
                  <dt>Fallos</dt>
                  <dd>{{ entero(m.fallos) }} ({{ porcentaje(m.fallos, m.llamadas) }} %)</dd>
                </div>
                <div>
                  <dt>Tokens de entrada</dt>
                  <dd>{{ entero(m.tokensEntrada) }}</dd>
                </div>
                <div>
                  <dt>Tokens de salida</dt>
                  <dd>{{ entero(m.tokensSalida) }}</dd>
                </div>
                <div>
                  <dt>Coste</dt>
                  <dd>
                    @if (m.costeUsd === null) {
                      <span class="sin-tarifa"><rs-icono nombre="alert" [tam]="14" />Sin tarifa</span>
                    } @else {
                      {{ usd(m.costeUsd) }} USD
                    }
                  </dd>
                </div>
              </dl>
            </section>

            <section class="metricas-detalle" aria-labelledby="titulo-detalle">
              <h2 id="titulo-detalle">Por operación y modelo</h2>
              <div class="tabla-scroll" tabindex="0" role="region" aria-labelledby="titulo-detalle">
                <table class="tabla">
                  <thead>
                    <tr>
                      <th scope="col">Operación</th>
                      <th scope="col">Modelo</th>
                      <th scope="col" class="num">Llamadas</th>
                      <th scope="col" class="num">Fallos</th>
                      <th scope="col" class="num">Tokens medios</th>
                      <th scope="col" class="num">Mediana</th>
                      <th scope="col" class="num">p95</th>
                      <th scope="col" class="num">Coste (USD)</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (fila of m.filas; track fila.operacion + fila.modelo) {
                      <tr>
                        <th scope="row">{{ operacion(fila.operacion) }}</th>
                        <td class="modelo">{{ fila.modelo }}</td>
                        <td class="num">{{ entero(fila.llamadas) }}</td>
                        <td class="num">{{ entero(fila.fallos) }}</td>
                        <td class="num">{{ entero(fila.tokensMedios) }}</td>
                        <td class="num">{{ latencia(fila.p50Ms) }}</td>
                        <td class="num">{{ latencia(fila.p95Ms) }}</td>
                        <td class="num">
                          @if (fila.costeUsd === null) {
                            <span class="sin-tarifa"><rs-icono nombre="alert" [tam]="14" />Sin tarifa</span>
                          } @else {
                            {{ usd(fila.costeUsd) }}
                          }
                        </td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
              <p class="metricas-nota ayuda">{{ m.nota }}</p>
            </section>
          }
        }
      </div>
    </div>
  `,
})
export class MetricasPage {
  private readonly servicio = inject(MetricasService);
  protected readonly rangos = RANGOS;
  readonly rango = signal<Rango>('7');
  readonly datos = signal<MetricasIa | null>(null);
  readonly carga = signal<'cargando' | 'listo' | 'error'>('cargando');
  private peticion = 0;

  constructor() {
    void this.cargar();
  }

  elegir(rango: Rango): void {
    if (rango === this.rango()) return;
    this.rango.set(rango);
    void this.cargar();
  }

  async cargar(): Promise<void> {
    const esta = ++this.peticion;
    this.carga.set('cargando');
    try {
      const datos = await this.servicio.ia(inicioDe(this.rango(), new Date()));
      if (esta !== this.peticion) return;
      this.datos.set(datos);
      this.carga.set('listo');
    } catch {
      if (esta === this.peticion) this.carga.set('error');
    }
  }

  protected entero(valor: number): string {
    return ENTERO.format(valor);
  }

  protected porcentaje(parte: number, total: number): string {
    return PORCENTAJE.format(total === 0 ? 0 : (parte / total) * 100);
  }

  protected usd(valor: number): string {
    return USD.format(valor);
  }

  protected latencia(ms: number): string {
    return ms < 1000 ? `${ENTERO.format(ms)} ms` : `${SEGUNDOS.format(ms / 1000)} s`;
  }

  protected operacion(clave: string): string {
    return OPERACIONES[clave] ?? clave;
  }

  protected periodo(m: MetricasIa): string {
    if (this.rango() === 'hoy') return 'hoy';
    return `del ${FECHA.format(new Date(m.desde))} al ${FECHA.format(new Date(m.hasta))}`;
  }
}
