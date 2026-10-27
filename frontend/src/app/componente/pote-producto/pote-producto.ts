import { Component, input } from '@angular/core';
import { NgClass } from '@angular/common';

/** Dibujo de un pote de suplemento (se usa hasta tener fotos reales de los productos). */
@Component({
  selector: 'app-pote-producto',
  imports: [NgClass],
  templateUrl: './pote-producto.html',
  styleUrl: './pote-producto.css'
})
export class PoteProducto {
  readonly nombre = input<string>('WHEY');
  readonly subtitulo = input<string>('');
  readonly detalle = input<string>('');
  readonly grande = input<boolean>(false);
}
