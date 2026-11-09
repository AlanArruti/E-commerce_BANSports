import { Component, OnInit, signal, computed, ChangeDetectionStrategy } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { AdminService } from '../../services/admin.service';
import { ResumenAdmin } from '../../models/resumen-admin';
import { DiasMetricas, MetricasAdmin } from '../../models/metricas-admin';
import { ETIQUETAS_ESTADO_PEDIDO } from '../../models/pedido';
import { PrecioPipe } from '../../pipes/precio.pipe';
import { GraficoVentas } from '../../componente/grafico-ventas/grafico-ventas';
import { ErrorTraducido, traducirErrorResumen } from '../../validaciones/errores-api';

const FORMATO_VARIACION = new Intl.NumberFormat('es-AR', {
  style: 'percent',
  maximumFractionDigits: 0,
  signDisplay: 'exceptZero'
});

/** Cómo cambió un número contra el período anterior. */
interface Variacion {
  texto: string;
  tendencia: 'sube' | 'baja' | 'igual';
}

function variacion(actual: number, anterior: number, dias: number): Variacion | null {
  if (anterior === 0) {
    return null;
  }
  const cambio = (actual - anterior) / anterior;
  const redondeado = Math.round(cambio * 100);
  return {
    texto: `${FORMATO_VARIACION.format(redondeado / 100)} vs. los ${dias} días anteriores`,
    tendencia: redondeado > 0 ? 'sube' : redondeado < 0 ? 'baja' : 'igual'
  };
}

/**
 * Inicio del panel de administración: métricas de ventas del período elegido
 * (gráfico por día, pedidos por estado, más vendidos) y el estado del inventario,
 * con enlaces a las pantallas filtradas correspondientes.
 */
@Component({
  selector: 'app-admin-inicio',
  imports: [RouterLink, PrecioPipe, GraficoVentas],
  templateUrl: './admin-inicio.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './admin-inicio.css'
})
export class AdminInicio implements OnInit {
  protected readonly periodos: DiasMetricas[] = [7, 30, 90];

  protected readonly resumen = signal<ResumenAdmin | null>(null);
  protected readonly metricas = signal<MetricasAdmin | null>(null);
  protected readonly dias = signal<DiasMetricas>(30);
  protected readonly cargando = signal(true);
  /** Cambio de período: se dejan los datos anteriores a la vista mientras llegan los nuevos. */
  protected readonly actualizando = signal(false);
  protected readonly errorCarga = signal<ErrorTraducido | null>(null);

  protected readonly kpis = computed(() => {
    const m = this.metricas();
    if (!m) {
      return [];
    }
    const ticket = m.pedidosPeriodo > 0 ? m.ventasPeriodo / m.pedidosPeriodo : 0;
    const ticketAnterior = m.pedidosPeriodoAnterior > 0 ? m.ventasPeriodoAnterior / m.pedidosPeriodoAnterior : 0;
    return [
      {
        titulo: 'Ventas',
        valor: m.ventasPeriodo,
        esPrecio: true,
        detalle: 'Pedidos cobrados, envío incluido',
        variacion: variacion(m.ventasPeriodo, m.ventasPeriodoAnterior, m.dias)
      },
      {
        titulo: 'Pedidos cobrados',
        valor: m.pedidosPeriodo,
        esPrecio: false,
        detalle: 'Pagados, despachados o entregados',
        variacion: variacion(m.pedidosPeriodo, m.pedidosPeriodoAnterior, m.dias)
      },
      {
        titulo: 'Ticket promedio',
        valor: ticket,
        esPrecio: true,
        detalle: 'Lo que se cobra por pedido',
        variacion: variacion(ticket, ticketAnterior, m.dias)
      }
    ];
  });

  protected readonly estados = computed(() => {
    const lista = this.metricas()?.pedidosPorEstado ?? [];
    const maximo = Math.max(1, ...lista.map((e) => e.cantidad));
    return lista.map((e) => ({
      ...e,
      etiqueta: e.estado === 'PAGADO' ? 'Pagado, para despachar' : ETIQUETAS_ESTADO_PEDIDO[e.estado],
      porcentaje: (e.cantidad / maximo) * 100
    }));
  });

  protected readonly totalPedidos = computed(() => this.estados().reduce((suma, e) => suma + e.cantidad, 0));

  protected readonly masVendidos = computed(() => {
    const lista = this.metricas()?.masVendidos ?? [];
    const maximo = Math.max(1, ...lista.map((p) => p.unidades));
    return lista.map((p) => ({ ...p, porcentaje: (p.unidades / maximo) * 100 }));
  });

  constructor(
    protected authService: AuthService,
    private adminService: AdminService
  ) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.errorCarga.set(null);
    forkJoin({
      resumen: this.adminService.resumen(),
      metricas: this.adminService.metricas(this.dias())
    }).subscribe({
      next: ({ resumen, metricas }) => {
        this.resumen.set(resumen);
        this.metricas.set(metricas);
        this.cargando.set(false);
      },
      error: (e: unknown) => this.mostrarError(e)
    });
  }

  cambiarPeriodo(dias: DiasMetricas): void {
    if (dias === this.dias() || this.actualizando()) {
      return;
    }
    this.dias.set(dias);
    this.actualizando.set(true);
    this.adminService.metricas(dias).subscribe({
      next: (metricas) => {
        this.metricas.set(metricas);
        this.actualizando.set(false);
      },
      error: (e: unknown) => {
        this.actualizando.set(false);
        this.mostrarError(e);
      }
    });
  }

  private mostrarError(e: unknown): void {
    this.cargando.set(false);
    this.errorCarga.set(
      e instanceof HttpErrorResponse
        ? traducirErrorResumen(e)
        : { titulo: 'No pudimos cargar el resumen', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
    );
  }
}
