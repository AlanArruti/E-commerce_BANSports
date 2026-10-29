import { Component, computed, ChangeDetectionStrategy } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { IsActiveMatchOptions, NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { filter, map } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { CategoriaService } from '../../services/categoria.service';

/** Cuántas categorías entran en la barra del header. El resto se ve en el catálogo. */
const CATEGORIAS_EN_HEADER = 4;

@Component({
  selector: 'app-header',
  imports: [ReactiveFormsModule, RouterLink, RouterLinkActive],
  templateUrl: './header.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './header.css'
})
export class Header {
  protected readonly categorias;
  protected readonly busqueda = new FormControl('', { nonNullable: true });

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
    private categoriaService: CategoriaService,
    private router: Router
  ) {
    this.categorias = toSignal(
      this.categoriaService.listar().pipe(map((categorias) => categorias.slice(0, CATEGORIAS_EN_HEADER))),
      { initialValue: [] }
    );

    // El buscador muestra lo que se está buscando en el catálogo, y se vacía al salir de él.
    this.router.events
      .pipe(
        filter((evento) => evento instanceof NavigationEnd),
        takeUntilDestroyed()
      )
      .subscribe((evento) => {
        const url = this.router.parseUrl(evento.urlAfterRedirects);
        const enCatalogo = evento.urlAfterRedirects.split('?')[0] === '/productos';
        this.busqueda.setValue(enCatalogo ? (url.queryParamMap.get('search') ?? '') : '');
      });
  }

  /** Busca en todo el catálogo (sin los filtros que hubiera antes). */
  buscar(evento: Event): void {
    evento.preventDefault();
    const texto = this.busqueda.value.trim();
    this.router.navigate(['/productos'], { queryParams: texto ? { search: texto } : {} });
  }

  cerrarSesion(): void {
    this.authService.logout();
    this.router.navigate(['/']);
  }
}
