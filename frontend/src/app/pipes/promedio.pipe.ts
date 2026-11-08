import { Pipe, PipeTransform } from '@angular/core';

/** Promedio de estrellas con un decimal y coma: 4.666 → "4,7", 5 → "5,0". */
@Pipe({ name: 'promedio' })
export class PromedioPipe implements PipeTransform {
  transform(valor: number | null | undefined): string {
    return (valor ?? 0).toLocaleString('es-AR', { minimumFractionDigits: 1, maximumFractionDigits: 1 });
  }
}
