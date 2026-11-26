import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { MetricasIa } from './metricas.model';

@Injectable({ providedIn: 'root' })
export class MetricasService {
  private readonly http = inject(HttpClient);

  /** Las llamadas a la IA de la organización desde la fecha dada hasta ahora. Solo ADMIN. */
  ia(desde: Date): Promise<MetricasIa> {
    return firstValueFrom(this.http.get<MetricasIa>('/api/metricas/ia', { params: { desde: desde.toISOString() } }));
  }
}
