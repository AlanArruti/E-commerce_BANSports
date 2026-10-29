import { Component, computed, input, ChangeDetectionStrategy } from '@angular/core';
import { NgClass } from '@angular/common';
import { PoteProducto } from '../pote-producto/pote-producto';
import { Producto } from '../../models/producto';
import { PrecioPipe } from '../../pipes/precio.pipe';

/** A partir de cuántas unidades se avisa que queda poco stock. */
const STOCK_BAJO = 5;

/** Tarjeta de un producto del catálogo: imagen, categoría, variantes, stock y precio. */
@Component({
  selector: 'app-tarjeta-producto',
  imports: [NgClass, PoteProducto, PrecioPipe],
  templateUrl: './tarjeta-producto.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './tarjeta-producto.css'
})
export class TarjetaProducto {
  readonly producto = input.required<Producto>();

  private readonly variantesActivas = computed(() =>
    this.producto().variantes.filter((variante) => variante.activo)
  );

  /** Suma del stock de las variantes. null si el producto no tiene variantes cargadas. */
  protected readonly stock = computed<number | null>(() => {
    const variantes = this.variantesActivas();
    return variantes.length ? variantes.reduce((total, variante) => total + variante.stock, 0) : null;
  });

  protected readonly sinStock = computed(() => this.stock() === 0);
  protected readonly stockBajo = computed(() => {
    const stock = this.stock();
    return stock !== null && stock > 0 && stock <= STOCK_BAJO;
  });

  /**
   * Valores de las variantes agrupados por atributo: "1 kg · Chocolate, Vainilla".
   * Solo se listan las variantes con stock; si el producto está agotado, se muestran todas.
   */
  protected readonly resumenVariantes = computed(() => {
    const activas = this.variantesActivas();
    const conStock = activas.filter((variante) => variante.stock > 0);
    const valoresPorAtributo = new Map<string, Set<string>>();
    for (const variante of conStock.length ? conStock : activas) {
      // El back guarda los atributos en jsonb y no respeta el orden: la presentación va siempre primero.
      const atributos = Object.entries(variante.atributos).sort(
        ([a], [b]) => Number(b === 'presentacion') - Number(a === 'presentacion')
      );
      for (const [atributo, valor] of atributos) {
        if (!valoresPorAtributo.has(atributo)) {
          valoresPorAtributo.set(atributo, new Set());
        }
        valoresPorAtributo.get(atributo)!.add(valor);
      }
    }
    return [...valoresPorAtributo.values()].map((valores) => [...valores].join(', ')).join(' · ');
  });

  /**
   * Texto corto para el dibujo del pote cuando no hay foto, a partir de la primera palabra:
   * "Whey" → WHEY, "Creatina" → CREA, "Pre-entreno" → PRE.
   */
  protected readonly abreviatura = computed(() => {
    const palabra = this.producto().nombre.split(/[\s-]/)[0].toUpperCase();
    return palabra.length > 5 ? palabra.slice(0, 4) : palabra;
  });
}
