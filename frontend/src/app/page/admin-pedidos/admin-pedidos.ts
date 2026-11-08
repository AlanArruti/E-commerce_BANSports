import { Component, computed, linkedSignal, ChangeDetectionStrategy } from '@angular/core';
import { DatePipe, NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { BehaviorSubject, catchError, combineLatest, map, of, startWith, switchMap } from 'rxjs';
import { EstadoPedidoEtiqueta } from '../../componente/estado-pedido/estado-pedido';
import { Pagina } from '../../models/pagina';
import { ETIQUETAS_ESTADO_PEDIDO, EstadoPedido, Pedido } from '../../models/pedido';
import { PrecioPipe } from '../../pipes/precio.pipe';
import { PedidoService } from '../../services/pedido.service';
import { ErrorTraducido, traducirErrorGestionPedido } from '../../validaciones/errores-api';

const TAMANIO_PAGINA = 10;
const PAGINAS_VISIBLES = 5;

/** Orden del filtro: el mismo en que avanza un pedido, con Cancelado al final. */
const ESTADOS_FILTRO: EstadoPedido[] = ['PENDIENTE_PAGO', 'EN_PREPARACION', 'PAGADO', 'DESPACHADO', 'ENTREGADO', 'CANCELADO'];

interface EstadoListado {
  cargando: boolean;
  pagina: Pagina<Pedido> | null;
  error: ErrorTraducido | null;
}

const CARGANDO: EstadoListado = { cargando: true, pagina: null, error: null };

/** Listado de pedidos del panel (SCRUM-138): filtro por estado y paginado. */
@Component({
  selector: 'app-admin-pedidos',
  imports: [DatePipe, NgClass, ReactiveFormsModule, RouterLink, EstadoPedidoEtiqueta, PrecioPipe],
  templateUrl: './admin-pedidos.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  // La tabla, el paginado y las migas son los mismos del listado de productos.
  styleUrls: ['../admin-productos/admin-productos.css', './admin-pedidos.css']
})
export class AdminPedidos {
  protected readonly estados = ESTADOS_FILTRO.map((valor) => ({ valor, etiqueta: ETIQUETAS_ESTADO_PEDIDO[valor] }));
  protected readonly estado;
  protected readonly paginaActual;
  protected readonly paginas;
  protected readonly resultado;
  protected readonly filtroEstado;
  protected readonly etiquetaFiltro;

  filtro = new FormControl<EstadoPedido | null>(null);

  /** Emite para volver a pedir la página ("Reintentar"). */
  private readonly recargar$ = new BehaviorSubject<void>(undefined);

  constructor(
    private pedidoService: PedidoService,
    private router: Router
  ) {
    this.filtroEstado = toSignal(this.filtro.valueChanges.pipe(startWith(this.filtro.value)), { requireSync: true });
    this.etiquetaFiltro = computed(() => {
      const estado = this.filtroEstado();
      return estado ? ETIQUETAS_ESTADO_PEDIDO[estado].toLowerCase() : '';
    });

    // Cambiar el filtro vuelve a la primera página.
    this.paginaActual = linkedSignal({ source: this.filtroEstado, computation: () => 0 });
    const consulta = computed(() => ({ estado: this.filtroEstado(), pagina: this.paginaActual() }));

    this.estado = toSignal(
      combineLatest([toObservable(consulta), this.recargar$]).pipe(
        switchMap(([{ estado, pagina }]) =>
          this.pedidoService.listar(estado, pagina, TAMANIO_PAGINA).pipe(
            map((resultado): EstadoListado => ({ cargando: false, pagina: resultado, error: null })),
            catchError((e: unknown) =>
              of<EstadoListado>({
                cargando: false,
                pagina: null,
                error:
                  e instanceof HttpErrorResponse
                    ? traducirErrorGestionPedido(e)
                    : { titulo: 'No pudimos cargar los pedidos', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
              })
            ),
            startWith(CARGANDO)
          )
        )
      ),
      { initialValue: CARGANDO }
    );

    this.resultado = computed(() => {
      const pagina = this.estado().pagina;
      if (!pagina || pagina.content.length === 0) {
        return null;
      }
      const desde = pagina.page * pagina.size + 1;
      return { desde, hasta: desde + pagina.content.length - 1, total: pagina.totalElements };
    });

    // Ventana de números alrededor de la página actual: 1 2 [3] 4 5.
    this.paginas = computed(() => {
      const pagina = this.estado().pagina;
      if (!pagina || pagina.totalPages <= 1) {
        return [];
      }
      const inicio = Math.max(0, Math.min(pagina.page - 2, pagina.totalPages - PAGINAS_VISIBLES));
      const fin = Math.min(pagina.totalPages, inicio + PAGINAS_VISIBLES);
      return Array.from({ length: fin - inicio }, (_, i) => inicio + i);
    });
  }

  irAPagina(pagina: number): void {
    this.paginaActual.set(pagina);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  /** Toda la fila lleva al detalle; el número del pedido es el link accesible con teclado. */
  abrir(pedido: Pedido): void {
    this.router.navigate(['/admin/pedidos', pedido.id]);
  }

  verTodos(): void {
    this.filtro.setValue(null);
  }

  reintentar(): void {
    this.recargar$.next();
  }
}
