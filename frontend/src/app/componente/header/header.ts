import { Component, computed, signal, ChangeDetectionStrategy, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { IsActiveMatchOptions, NavigationStart, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { CarritoService } from '../../services/carrito.service';
import { CategoriaService } from '../../services/categoria.service';
import { FavoritoService } from '../../services/favorito.service';
import { ArrastrarScroll } from '../../directivas/arrastrar-scroll';
import { BuscadorProductos } from '../buscador-productos/buscador-productos';
import { ModoPerfil, PerfilModal } from '../perfil-modal/perfil-modal';
import { APELLIDO_PENDIENTE, faltanDatos } from '../../models/cliente';
import { CampanaNotificaciones } from '../campana-notificaciones/campana-notificaciones';

/** Cuántas categorías entran en la barra del header. El resto se ve en el catálogo y en el menú del celular. */
const CATEGORIAS_EN_HEADER = 4;

@Component({
  selector: 'app-header',
  imports: [RouterLink, RouterLinkActive, BuscadorProductos, PerfilModal, ArrastrarScroll, CampanaNotificaciones],
  templateUrl: './header.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './header.css',
  host: {
    '(document:keydown.escape)': 'cerrarMenu()'
  }
})
export class Header {
  protected readonly todasLasCategorias;
  protected readonly categorias = computed(() => this.todasLasCategorias().slice(0, CATEGORIAS_EN_HEADER));
  protected readonly perfilAbierto = signal(false);
  protected readonly modoPerfil = signal<ModoPerfil>('ver');
  /** Cuenta creada con Google o GitHub a la que le falta el DNI o el apellido. */
  protected readonly faltanDatos = computed(() => faltanDatos(this.authService.cliente()));
  /** "DNI", "apellido" o "DNI y apellido". */
  protected readonly datosFaltantes = computed(() => {
    const cliente = this.authService.cliente();
    const faltan = [
      !cliente?.dni ? 'DNI' : null,
      !cliente?.apellido || cliente.apellido === APELLIDO_PENDIENTE ? 'apellido' : null
    ].filter((dato) => dato !== null);
    return faltan.join(' y ');
  });
  /** Menú lateral del celular (botón hamburguesa). */
  protected readonly menuAbierto = signal(false);

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
    this.todasLasCategorias = toSignal(this.categoriaService.listar(), { initialValue: [] });

    // Cualquier navegación (un link del menú, el buscador, atrás) cierra el menú.
    this.router.events
      .pipe(
        filter((evento) => evento instanceof NavigationStart),
        takeUntilDestroyed(inject(DestroyRef))
      )
      .subscribe(() => this.cerrarMenu());
  }

  abrirMenu(): void {
    this.menuAbierto.set(true);
  }

  cerrarMenu(): void {
    this.menuAbierto.set(false);
  }

  abrirPerfil(): void {
    this.cerrarMenu();
    this.modoPerfil.set('ver');
    this.perfilAbierto.set(true);
  }

  /** Desde el aviso de datos pendientes: abre el perfil directo en el formulario. */
  completarDatos(): void {
    this.cerrarMenu();
    this.modoPerfil.set('editar');
    this.perfilAbierto.set(true);
  }

  cerrarPerfil(): void {
    this.perfilAbierto.set(false);
  }

  cerrarSesion(): void {
    this.cerrarMenu();
    this.authService.logout();
    this.router.navigate(['/']);
  }
}
