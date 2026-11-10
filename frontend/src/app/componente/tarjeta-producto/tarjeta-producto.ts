import { Component, DestroyRef, computed, input, signal, ChangeDetectionStrategy } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { NgClass } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Estrellas } from '../estrellas/estrellas';
import { PoteProducto, abreviarNombre } from '../pote-producto/pote-producto';
import { Producto } from '../../models/producto';
import { STOCK_BAJO, Variante, atributosOrdenados, precioFinalDe, rangoDePrecios } from '../../models/variante';
import { PrecioPipe } from '../../pipes/precio.pipe';
import { PromedioPipe } from '../../pipes/promedio.pipe';
import { AuthService } from '../../services/auth.service';
import { FavoritoService } from '../../services/favorito.service';
import { CarritoService } from '../../services/carrito.service';
import { traducirErrorCarrito } from '../../validaciones/errores-api';

/**
 * Tarjeta de un producto del catálogo: imagen, categoría, variantes, stock y precio. Toda la tarjeta lleva al
 * detalle, salvo el corazón y el carrito. Con una sola opción disponible el carrito la agrega directo; con
 * varias (sabores, pesos) abre un selector dentro de la tarjeta.
 */
@Component({
  selector: 'app-tarjeta-producto',
  imports: [NgClass, RouterLink, PoteProducto, Estrellas, PrecioPipe, PromedioPipe],
  templateUrl: './tarjeta-producto.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './tarjeta-producto.css',
  host: { '(keydown.escape)': 'cerrarSelector()' }
})
export class TarjetaProducto {
  readonly producto = input.required<Producto>();

  constructor(
    protected authService: AuthService,
    private favoritoService: FavoritoService,
    private carritoService: CarritoService,
    destroyRef: DestroyRef
  ) {
    destroyRef.onDestroy(() => clearTimeout(this.temporizadorAviso));
  }

  // ── Agregar al carrito desde la tarjeta ─────────────────

  protected readonly eligiendo = signal(false);
  protected readonly agregando = signal<number | null>(null);
  /** Aviso corto después de agregar: qué se agregó, o por qué no se pudo. */
  protected readonly aviso = signal<{ ok: boolean; texto: string } | null>(null);
  private temporizadorAviso?: ReturnType<typeof setTimeout>;

  /** Variantes que todavía se pueden sumar: con stock y sin haber llevado ya todo al carrito. */
  protected readonly opciones = computed(() =>
    this.variantesActivas().filter((v) => v.stock - this.carritoService.cantidadDeVariante(v.id) > 0)
  );

  protected readonly puedeAgregar = computed(() => !this.authService.esAdmin() && this.opciones().length > 0);

  /** Texto del botón para lectores de pantalla y tooltip; explica por qué está apagado. */
  protected readonly motivoCarrito = computed(() => {
    if (this.puedeAgregar()) return 'Agregar al carrito';
    return (this.stock() ?? 0) > 0 ? 'Ya tenés en el carrito todo el stock' : 'Sin stock';
  });

  protected descripcion(variante: Variante): string {
    return atributosOrdenados(variante).map(([, valor]) => valor).join(' · ');
  }

  protected precioDe(variante: Variante): number {
    return precioFinalDe(this.producto(), variante);
  }

  protected clickCarrito(): void {
    if (!this.puedeAgregar() || this.agregando() !== null) return;
    const opciones = this.opciones();
    if (opciones.length === 1) {
      this.agregar(opciones[0]);
    } else {
      this.aviso.set(null);
      this.eligiendo.set(true);
    }
  }

  protected cerrarSelector(): void {
    this.eligiendo.set(false);
  }

  protected agregar(variante: Variante): void {
    this.agregando.set(variante.id);
    this.carritoService.agregar({ idProducto: this.producto().id, idVariante: variante.id, cantidad: 1 }).subscribe({
      next: () => {
        this.agregando.set(null);
        this.eligiendo.set(false);
        const detalle = this.descripcion(variante);
        this.mostrarAviso(true, detalle ? `Agregaste ${detalle}` : 'Agregado al carrito');
      },
      error: (e: unknown) => {
        this.agregando.set(null);
        this.eligiendo.set(false);
        // El stock o el carrito pudieron cambiar: se trae de nuevo para que las opciones queden al día.
        this.carritoService.cargar().subscribe({ error: () => {} });
        this.mostrarAviso(false, e instanceof HttpErrorResponse ? traducirErrorCarrito(e).titulo : 'No pudimos agregarlo');
      }
    });
  }

  private mostrarAviso(ok: boolean, texto: string): void {
    clearTimeout(this.temporizadorAviso);
    this.aviso.set({ ok, texto });
    this.temporizadorAviso = setTimeout(() => this.aviso.set(null), ok ? 3500 : 5000);
  }

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
