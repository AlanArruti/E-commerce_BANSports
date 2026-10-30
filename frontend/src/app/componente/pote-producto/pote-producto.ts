import { Component, input, ChangeDetectionStrategy } from '@angular/core';
import { NgClass } from '@angular/common';

/**
 * Texto corto para el pote cuando no hay foto, a partir de la primera palabra del nombre:
 * "Whey" → WHEY, "Creatina" → CREA, "Pre-entreno" → PRE.
 */
export function abreviarNombre(nombre: string): string {
  const palabra = nombre.split(/[\s-]/)[0].toUpperCase();
  return palabra.length > 5 ? palabra.slice(0, 4) : palabra;
}

/** Dibujo de un pote de suplemento (se usa hasta tener fotos reales de los productos). */
@Component({
  selector: 'app-pote-producto',
  imports: [NgClass],
  templateUrl: './pote-producto.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './pote-producto.css'
})
export class PoteProducto {
  readonly nombre = input<string>('WHEY');
  readonly subtitulo = input<string>('');
  readonly detalle = input<string>('');
  readonly grande = input<boolean>(false);
}
