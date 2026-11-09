import { Component, computed, signal, ChangeDetectionStrategy } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { IsActiveMatchOptions, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { CarritoService } from '../../services/carrito.service';
import { CategoriaService } from '../../services/categoria.service';
import { FavoritoService } from '../../services/favorito.service';
import { BuscadorProductos } from '../buscador-productos/buscador-productos';
import { PerfilModal } from '../perfil-modal/perfil-modal';

/** Cuántas categorías entran en la barra del header. El resto se ve en el catálogo. */
const CATEGORIAS_EN_HEADER = 4;

@Component({
  selector: 'app-header',
  imports: [RouterLink, RouterLinkActive, BuscadorProductos, PerfilModal],
  templateUrl: './header.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './header.css'
})
export class Header {
  protected readonly categorias;
  protected readonly perfilAbierto = signal(false);

  /** La categoría se marca activa aunque haya otros filtros en la URL (búsqueda, precio). */
  protected readonly opcionesActiva: IsActiveMatchOptions = {
    paths: 'exact',
    queryParams: 'subset',
    matrixParams: 'ignored',
    fragment: 'ignored'
  };

  /** Nombre a mostrar: el que confirmó el back o, mientras tanto, el email del token. */
  protected readonly nombreUsuario = computed(
    () => this.authService.cliente()?.nombre ?? this.authService.usuario()?.sub ?? ''
  );

  constructor(
    protected authService: AuthService,
    protected carritoService: CarritoService,
    protected favoritoService: FavoritoService,
    private categoriaService: CategoriaService,
    private router: Router
  ) {
    this.categorias = toSignal(
      this.categoriaService.listar().pipe(map((categorias) => categorias.slice(0, CATEGORIAS_EN_HEADER))),
      { initialValue: [] }
    );
  }

  abrirPerfil(): void {
    this.perfilAbierto.set(true);
  }

  cerrarPerfil(): void {
    this.perfilAbierto.set(false);
  }

  cerrarSesion(): void {
    this.authService.logout();
    this.router.navigate(['/']);
  }
}
