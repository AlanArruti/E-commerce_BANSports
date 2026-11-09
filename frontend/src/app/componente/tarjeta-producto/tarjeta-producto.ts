import { Component, computed, input, ChangeDetectionStrategy } from '@angular/core';
import { NgClass } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Estrellas } from '../estrellas/estrellas';
import { PoteProducto, abreviarNombre } from '../pote-producto/pote-producto';
import { Producto } from '../../models/producto';
import { STOCK_BAJO, atributosOrdenados, rangoDePrecios } from '../../models/variante';
import { PrecioPipe } from '../../pipes/precio.pipe';
import { PromedioPipe } from '../../pipes/promedio.pipe';
import { AuthService } from '../../services/auth.service';
import { FavoritoService } from '../../services/favorito.service';

/** Tarjeta de un producto del catálogo: imagen, categoría, variantes, stock y precio. Toda la tarjeta lleva al detalle. */
@Component({
  selector: 'app-tarjeta-producto',
  imports: [NgClass, RouterLink, PoteProducto, Estrellas, PrecioPipe, PromedioPipe],
  templateUrl: './tarjeta-producto.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './tarjeta-producto.css'
})
export class TarjetaProducto {
  readonly producto = input.required<Producto>();

  constructor(
    protected authService: AuthService,
    private favoritoService: FavoritoService
  ) {}

  protected readonly esFavorito = computed(() => this.favoritoService.ids().has(this.producto().id));

  protected alternarFavorito(): void {
    this.favoritoService.alternar(this.producto());
  }

  private readonly variantesActivas = computed(() =>
    this.producto().variantes.filter((variante) => variante.activo)
  );

  /** Suma del stock de las variantes. null si el producto no tiene variantes cargadas. */
  protected readonly stock = computed<number | null>(() => {
    const variantes = this.variantesActivas();
    return variantes.length ? variantes.reduce((total, variante) => total + variante.stock, 0) : null;
  });

  protected readonly sinStock = computed(() => this.stock() === 0);
  protected readonly tieneOferta = computed(() => (this.producto().descuento ?? 0) > 0);
  protected readonly descuento = computed(() => Math.round(this.producto().descuento ?? 0));
  /** Si las variantes tienen precios distintos se muestra el más barato con "Desde" (SCRUM-154). */
  protected readonly rango = computed(() => rangoDePrecios(this.producto()));
  protected readonly precioVigente = computed(() => this.rango().minimo);
  protected readonly esPrecioDesde = computed(() => this.rango().minimo < this.rango().maximo);
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
      for (const [atributo, valor] of atributosOrdenados(variante)) {
        if (!valoresPorAtributo.has(atributo)) {
          valoresPorAtributo.set(atributo, new Set());
        }
        valoresPorAtributo.get(atributo)!.add(valor);
      }
    }
    return [...valoresPorAtributo.values()].map((valores) => [...valores].join(', ')).join(' · ');
  });

  protected readonly abreviatura = computed(() => abreviarNombre(this.producto().nombre));
}
