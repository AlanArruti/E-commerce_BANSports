import { Component, OnInit, computed, signal, ChangeDetectionStrategy } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { EstadoPedidoEtiqueta } from '../../componente/estado-pedido/estado-pedido';
import { ETIQUETAS_ESTADO_PEDIDO, EstadoPedido, Pedido } from '../../models/pedido';
import { PrecioPipe } from '../../pipes/precio.pipe';
import { ClienteService } from '../../services/cliente.service';
import { ErrorTraducido, traducirErrorPedidos } from '../../validaciones/errores-api';

interface FiltroEstado {
  /** null = todos. */
  estado: EstadoPedido | null;
  etiqueta: string;
  cantidad: number;
}

/** Historial de pedidos del cliente, filtrable por estado (SCRUM-121). */
@Component({
  selector: 'app-mis-pedidos',
  imports: [DatePipe, RouterLink, EstadoPedidoEtiqueta, PrecioPipe],
  templateUrl: './mis-pedidos.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './mis-pedidos.css'
})
export class MisPedidos implements OnInit {
  protected readonly cargando = signal(true);
  protected readonly error = signal<ErrorTraducido | null>(null);
  protected readonly pedidos = signal<Pedido[]>([]);
  protected readonly estadoElegido = signal<EstadoPedido | null>(null);

  /** Un botón por estado que el cliente tiene, con cuántos pedidos hay en cada uno. */
  protected readonly filtros = computed<FiltroEstado[]>(() => {
    const pedidos = this.pedidos();
    const estados = (Object.keys(ETIQUETAS_ESTADO_PEDIDO) as EstadoPedido[]).filter((estado) =>
      pedidos.some((pedido) => pedido.estado === estado)
    );
    return [
      { estado: null, etiqueta: 'Todos', cantidad: pedidos.length },
      ...estados.map((estado) => ({
        estado,
        etiqueta: ETIQUETAS_ESTADO_PEDIDO[estado],
        cantidad: pedidos.filter((pedido) => pedido.estado === estado).length
      }))
    ];
  });

  protected readonly pedidosVisibles = computed(() => {
    const estado = this.estadoElegido();
    return estado ? this.pedidos().filter((pedido) => pedido.estado === estado) : this.pedidos();
  });

  constructor(private clienteService: ClienteService) {}

  ngOnInit(): void {
    this.cargar();
  }

  /** Se traen todos de una vez: así el filtro es instantáneo y se pueden contar por estado. */
  cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.clienteService.listarPedidos().subscribe({
      next: (pedidos) => {
        this.cargando.set(false);
        this.pedidos.set(pedidos);
      },
      error: (e: unknown) => {
        this.cargando.set(false);
        this.error.set(
          e instanceof HttpErrorResponse
            ? traducirErrorPedidos(e)
            : { titulo: 'No pudimos cargar tus pedidos', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
        );
      }
    });
  }

  elegirEstado(estado: EstadoPedido | null): void {
    this.estadoElegido.set(estado);
  }

  protected unidades(pedido: Pedido): number {
    return pedido.items.reduce((total, item) => total + item.cantidad, 0);
  }
}
