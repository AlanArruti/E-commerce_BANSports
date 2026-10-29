import { Pipe, PipeTransform } from '@angular/core';

/** Formato de precio argentino: 28900 → "$ 28.900", 999.99 → "$ 999,99" (centavos solo si hay). */
export function formatearPrecio(valor: number): string {
  return '$ ' + valor.toLocaleString('es-AR', { minimumFractionDigits: valor % 1 === 0 ? 0 : 2, maximumFractionDigits: 2 });
}

@Pipe({ name: 'precio' })
export class PrecioPipe implements PipeTransform {
  transform(valor: number | null | undefined): string {
    return valor === null || valor === undefined ? '' : formatearPrecio(valor);
  }
}
