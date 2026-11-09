import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  computed,
  effect,
  linkedSignal,
  signal,
  viewChild
} from '@angular/core';
import { DatePipe, NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { BehaviorSubject, catchError, combineLatest, debounceTime, distinctUntilChanged, map, of, startWith, switchMap } from 'rxjs';
import { Cliente } from '../../models/cliente';
import { Pagina } from '../../models/pagina';
import { ETIQUETAS_ESTADO_PEDIDO, EstadoPedido, Pedido } from '../../models/pedido';
import { PrecioPipe } from '../../pipes/precio.pipe';
import { AdminService } from '../../services/admin.service';
import {
  ErrorTraducido,
  traducirErrorBajaCliente,
  traducirErrorClientes,
  traducirErrorPedidosCliente
} from '../../validaciones/errores-api';

const TAMANIO_PAGINA = 10;
/** Espera después de la última tecla antes de buscar. */
const DEMORA_BUSQUEDA_MS = 350;
const PAGINAS_VISIBLES = 5;
/** Estados en los que el pedido ya se cobró (los mismos que usa el back para las ventas). */
const ESTADOS_COBRADOS: EstadoPedido[] = ['PAGADO', 'DESPACHADO', 'ENTREGADO'];

interface EstadoListado {
  cargando: boolean;
  pagina: Pagina<Cliente> | null;
  error: ErrorTraducido | null;
}

interface PanelPedidos {
  cliente: Cliente;
  cargando: boolean;
  pedidos: Pedido[];
  error: ErrorTraducido | null;
}

/**
 * Clientes registrados (activos): búsqueda por nombre o email, sus pedidos en un
 * panel lateral y baja lógica con confirmación.
 */
@Component({
  selector: 'app-admin-clientes',
  imports: [DatePipe, NgClass, ReactiveFormsModule, RouterLink, PrecioPipe],
  templateUrl: './admin-clientes.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './admin-clientes.css',
  host: { '(document:keydown.escape)': 'cerrarTodo()' }
})
export class AdminClientes {
  protected readonly etiquetasEstado = ETIQUETAS_ESTADO_PEDIDO;

  readonly busqueda = new FormControl('', { nonNullable: true });

  protected readonly textoBuscado;
  protected readonly paginaActual;
  protected readonly estado;
  protected readonly resultado;
  protected readonly paginas;

  protected readonly aviso = signal<string | null>(null);

  protected readonly clienteBaja = signal<Cliente | null>(null);
  protected readonly dandoDeBaja = signal(false);
  protected readonly errorBaja = signal<ErrorTraducido | null>(null);

  protected readonly panel = signal<PanelPedidos | null>(null);
  protected readonly resumenPedidos = computed(() => {
    const pedidos = this.panel()?.pedidos ?? [];
    const cobrados = pedidos.filter((p) => ESTADOS_COBRADOS.includes(p.estado));
    return {
      cantidad: pedidos.length,
      cobrados: cobrados.length,
      totalCobrado: cobrados.reduce((suma, p) => suma + p.total, 0)
    };
  });

  /** Emite para volver a pedir la página (después de una baja o con "Reintentar"). */
  private readonly recargar$ = new BehaviorSubject<void>(undefined);
  /** Botón que abrió el panel o el modal: recibe el foco al cerrarlos. */
  private disparador: HTMLElement | null = null;
  private readonly cerrarPanelBtn = viewChild<ElementRef<HTMLButtonElement>>('cerrarPanelBtn');
  private readonly cancelarBajaBtn = viewChild<ElementRef<HTMLButtonElement>>('cancelarBajaBtn');

  constructor(private adminService: AdminService) {
    this.textoBuscado = toSignal(
      this.busqueda.valueChanges.pipe(
        debounceTime(DEMORA_BUSQUEDA_MS),
        map((texto) => texto.trim()),
        distinctUntilChanged()
      ),
      { initialValue: '' }
    );

    // Cambiar la búsqueda vuelve a la primera página.
    this.paginaActual = linkedSignal({ source: this.textoBuscado, computation: () => 0 });
    const consulta = computed(() => ({ buscar: this.textoBuscado(), page: this.paginaActual(), size: TAMANIO_PAGINA }));

    this.estado = toSignal(
      combineLatest([toObservable(consulta), this.recargar$]).pipe(
        switchMap(([filtros]) =>
          this.adminService.clientes(filtros).pipe(
            map((pagina): EstadoListado => ({ cargando: false, pagina, error: null })),
            catchError((e: unknown) =>
              of<EstadoListado>({
                cargando: false,
                pagina: null,
                error:
                  e instanceof HttpErrorResponse
                    ? traducirErrorClientes(e)
                    : { titulo: 'No pudimos cargar los clientes', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
              })
            ),
            startWith<EstadoListado>({ cargando: true, pagina: null, error: null })
          )
        )
      ),
      { initialValue: { cargando: true, pagina: null, error: null } }
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

    // Al abrir el panel o el modal, el foco va adentro para poder usarlos con teclado.
    effect(() => this.cerrarPanelBtn()?.nativeElement.focus({ preventScroll: true }));
    effect(() => this.cancelarBajaBtn()?.nativeElement.focus({ preventScroll: true }));
  }

  irAPagina(pagina: number): void {
    this.paginaActual.set(pagina);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  limpiarBusqueda(): void {
    this.busqueda.setValue('');
  }

  reintentar(): void {
    this.recargar$.next();
  }

  // ── Pedidos del cliente ───────────────────────────────────

  verPedidos(cliente: Cliente, evento: Event): void {
    this.disparador = evento.currentTarget as HTMLElement;
    this.panel.set({ cliente, cargando: true, pedidos: [], error: null });
    this.adminService.pedidosDeCliente(cliente.id).subscribe({
      next: (pedidos) => {
        const ordenados = [...pedidos].sort((a, b) => b.fechaCreacion.localeCompare(a.fechaCreacion));
        this.panel.update((p) => (p?.cliente.id === cliente.id ? { ...p, cargando: false, pedidos: ordenados } : p));
      },
      error: (e: unknown) => {
        const error =
          e instanceof HttpErrorResponse
            ? traducirErrorPedidosCliente(e)
            : { titulo: 'No pudimos cargar los pedidos del cliente', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' };
        this.panel.update((p) => (p?.cliente.id === cliente.id ? { ...p, cargando: false, error } : p));
      }
    });
  }

  cerrarPanel(): void {
    if (this.panel()) {
      this.panel.set(null);
      this.devolverFoco();
    }
  }

  // ── Baja ──────────────────────────────────────────────────

  pedirBaja(cliente: Cliente, evento: Event): void {
    this.disparador = evento.currentTarget as HTMLElement;
    this.aviso.set(null);
    this.errorBaja.set(null);
    this.clienteBaja.set(cliente);
  }

  cerrarBaja(): void {
    if (this.clienteBaja() && !this.dandoDeBaja()) {
      this.clienteBaja.set(null);
      this.devolverFoco();
    }
  }

  confirmarBaja(): void {
    const cliente = this.clienteBaja();
    if (!cliente) {
      return;
    }
    this.dandoDeBaja.set(true);
    this.errorBaja.set(null);
    this.adminService.bajaCliente(cliente.id).subscribe({
      next: () => {
        this.dandoDeBaja.set(false);
        this.clienteBaja.set(null);
        this.disparador = null;
        this.aviso.set(`Diste de baja a ${this.nombreCompleto(cliente)}. Ya no puede ingresar; sus pedidos se conservan.`);
        this.actualizarDespuesDeBaja();
      },
      error: (e: unknown) => {
        this.dandoDeBaja.set(false);
        this.errorBaja.set(
          e instanceof HttpErrorResponse
            ? traducirErrorBajaCliente(e)
            : { titulo: 'No pudimos dar de baja al cliente', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
        );
        if (e instanceof HttpErrorResponse && e.status === 404) {
          this.actualizarDespuesDeBaja();
        }
      }
    });
  }

  /** Escape cierra lo que esté abierto (primero el modal, que va encima). */
  cerrarTodo(): void {
    if (this.clienteBaja()) {
      this.cerrarBaja();
    } else {
      this.cerrarPanel();
    }
  }

  protected nombreCompleto(cliente: Cliente): string {
    return `${cliente.nombre} ${cliente.apellido}`.trim();
  }

  protected iniciales(cliente: Cliente): string {
    return `${cliente.nombre.charAt(0)}${cliente.apellido.charAt(0)}`.toUpperCase();
  }

  protected cantidadItems(pedido: Pedido): number {
    return pedido.items.reduce((suma, item) => suma + item.cantidad, 0);
  }

  private devolverFoco(): void {
    this.disparador?.focus({ preventScroll: true });
    this.disparador = null;
  }

  /** Si se dio de baja el único cliente de la última página, se vuelve a la anterior. */
  private actualizarDespuesDeBaja(): void {
    const pagina = this.estado().pagina;
    if (pagina && pagina.content.length === 1 && pagina.page > 0) {
      this.paginaActual.set(pagina.page - 1);
    } else {
      this.recargar$.next();
    }
  }
}
