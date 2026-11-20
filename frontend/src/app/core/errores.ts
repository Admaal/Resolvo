import { HttpErrorResponse } from '@angular/common/http';

const GENERICO = 'No se pudo completar la acción.';

/** Texto legible para un fallo de la API; nunca el código crudo del backend. */
export function mensajeDeError(fallo: unknown): string {
  const codigo = fallo instanceof HttpErrorResponse ? (fallo.error as { error?: string } | null)?.error : undefined;
  switch (codigo) {
    case 'conflicto':
      return 'El ticket ha cambiado o la acción ya no es válida. Recarga e inténtalo de nuevo.';
    case 'invalido':
      return 'Revisa los datos del formulario.';
    case 'prohibido':
      return 'No tienes permiso para hacer esto.';
    default:
      return GENERICO;
  }
}
