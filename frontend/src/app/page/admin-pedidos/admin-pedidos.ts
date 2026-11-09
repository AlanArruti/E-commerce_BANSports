import { Component, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { NgClass, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { EstadoPedido, ETIQUETAS_ESTADO_PEDIDO, Pedido } from '../../models/pedido';
import { PedidoService } from '../../services/pedido.service';
import { PrecioPipe } from '../../pipes/precio.pipe';
import { ErrorTraducido, traducirErrorPedidos } from '../../validaciones/errores-api';

const TAMANIO_PAGINA = 10;

const ESTADOS: EstadoPedido[] = [
  'PENDIENTE_PAGO',
  'EN_PREPARACION',
  'PAGADO',
  'DESPACHADO',
  'ENTREGADO',
  'CANCELADO'
];

function estadoDeParametro(valor: string | null): EstadoPedido | null {
  if (!valor) {
    return null;
  }
  return (ESTADOS as string[]).includes(valor) ? (valor as EstadoPedido) : null;
}

/** Lista de pedidos del panel, filtrable por estado (?estado=...). */
@Component({
  selector: 'app-admin-pedidos',
  imports: [NgClass, DatePipe, RouterLink, PrecioPipe],
  templateUrl: './admin-pedidos.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './admin-pedidos.css'
})
export class AdminPedidos implements OnInit {
  protected readonly etiquetasEstado = ETIQUETAS_ESTADO_PEDIDO;
  protected readonly estados = ESTADOS;

  protected readonly estado = signal<EstadoPedido | null>(null);
  protected readonly pedidos = signal<Pedido[]>([]);
  protected readonly pagina = signal(0);
  protected readonly totalPaginas = signal(0);
  protected readonly total = signal(0);
  protected readonly cargando = signal(true);
  protected readonly errorCarga = signal<ErrorTraducido | null>(null);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private pedidoService: PedidoService
  ) {}

  ngOnInit(): void {
    this.estado.set(estadoDeParametro(this.route.snapshot.queryParamMap.get('estado')));
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.errorCarga.set(null);
    this.pedidoService.listar({ estado: this.estado(), page: this.pagina(), size: TAMANIO_PAGINA }).subscribe({
      next: (pagina) => {
        this.pedidos.set(pagina.content);
        this.total.set(pagina.totalElements);
        this.totalPaginas.set(pagina.totalPages);
        this.cargando.set(false);
      },
      error: (e: unknown) => {
        this.cargando.set(false);
        this.errorCarga.set(
          e instanceof HttpErrorResponse
            ? traducirErrorPedidos(e)
            : { titulo: 'No pudimos cargar los pedidos', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
        );
      }
    });
  }

  cambiarEstado(valor: string): void {
    this.estado.set(estadoDeParametro(valor));
    this.pagina.set(0);
    this.router.navigate([], { queryParams: valor ? { estado: valor } : {}, replaceUrl: true });
    this.cargar();
  }

  irAPagina(pagina: number): void {
    this.pagina.set(pagina);
    this.cargar();
    window.scrollTo({ top: 0 });
  }

  protected desde(): number {
    return this.total() === 0 ? 0 : this.pagina() * TAMANIO_PAGINA + 1;
  }

  protected hasta(): number {
    return Math.min(this.pagina() * TAMANIO_PAGINA + this.pedidos().length, this.total());
  }

  protected nombreCliente(pedido: Pedido): string {
    const cliente = pedido.cliente;
    return cliente ? `${cliente.nombre} ${cliente.apellido}` : '—';
  }
}
