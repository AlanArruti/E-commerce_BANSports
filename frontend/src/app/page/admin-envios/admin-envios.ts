import { Component, computed, signal, ChangeDetectionStrategy } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { BehaviorSubject, catchError, map, of, startWith, switchMap } from 'rxjs';
import { localidadDeDireccion } from '../../models/direccion';
import { ETIQUETAS_ESTADO_ENVIO, Envio, EstadoEnvio, siguienteEstadoEnvio } from '../../models/pedido';
import { PedidoService } from '../../services/pedido.service';
import { ErrorTraducido, traducirErrorGestionPedido } from '../../validaciones/errores-api';

type Filtro = 'ACTIVOS' | EstadoEnvio | 'TODOS';

const FILTROS: { valor: Filtro; etiqueta: string }[] = [
  { valor: 'ACTIVOS', etiqueta: 'Activos' },
  { valor: 'DESPACHADO', etiqueta: 'Despachados' },
  { valor: 'EN_CAMINO', etiqueta: 'En camino' },
  { valor: 'ENTREGADO', etiqueta: 'Entregados' },
  { valor: 'TODOS', etiqueta: 'Todos' }
];

interface EstadoListado {
  cargando: boolean;
  envios: Envio[];
  error: ErrorTraducido | null;
}

const CARGANDO: EstadoListado = { cargando: true, envios: [], error: null };

/**
 * Envíos del panel (SCRUM-139). El back no pagina este listado, así que se pide una vez
 * y se filtra acá ("Activos" = despachados y en camino).
 */
@Component({
  selector: 'app-admin-envios',
  imports: [DatePipe, RouterLink],
  templateUrl: './admin-envios.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  // La tabla y las migas son las mismas del listado de productos.
  styleUrls: ['../admin-productos/admin-productos.css', './admin-envios.css']
})
export class AdminEnvios {
  protected readonly filtros = FILTROS;
  protected readonly etiquetas = ETIQUETAS_ESTADO_ENVIO;
  protected readonly localidad = localidadDeDireccion;
  protected readonly siguiente = siguienteEstadoEnvio;

  protected readonly filtro = signal<Filtro>('ACTIVOS');
  protected readonly estado;
  protected readonly visibles;

  protected readonly aviso = signal<string | null>(null);
  protected readonly errorAccion = signal<ErrorTraducido | null>(null);
  /** Pedido cuyo envío se está avanzando. */
  protected readonly avanzando = signal<number | null>(null);

  private readonly recargar$ = new BehaviorSubject<void>(undefined);

  constructor(private pedidoService: PedidoService) {
    // Al recargar después de avanzar uno no se vuelve al esqueleto.
    let anterior: EstadoListado = CARGANDO;
    this.estado = toSignal(
      this.recargar$.pipe(
        switchMap(() =>
          this.pedidoService.listarEnvios(null).pipe(
            map((envios): EstadoListado => ({ cargando: false, envios, error: null })),
            catchError((e: unknown) =>
              of<EstadoListado>({
                cargando: false,
                envios: [],
                error:
                  e instanceof HttpErrorResponse
                    ? traducirErrorGestionPedido(e)
                    : { titulo: 'No pudimos cargar los envíos', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
              })
            ),
            startWith(anterior.cargando ? CARGANDO : anterior)
          )
        ),
        map((estado) => (anterior = estado))
      ),
      { initialValue: CARGANDO }
    );

    this.visibles = computed(() => {
      const filtro = this.filtro();
      return this.estado().envios.filter((envio) =>
        filtro === 'TODOS' ? true : filtro === 'ACTIVOS' ? envio.estado !== 'ENTREGADO' : envio.estado === filtro
      );
    });
  }

  elegirFiltro(filtro: Filtro): void {
    this.filtro.set(filtro);
  }

  reintentar(): void {
    this.recargar$.next();
  }

  avanzar(envio: Envio): void {
    const siguiente = siguienteEstadoEnvio(envio.estado);
    if (!siguiente) {
      return;
    }
    this.avanzando.set(envio.idPedido);
    this.aviso.set(null);
    this.errorAccion.set(null);
    this.pedidoService.avanzarEnvio(envio.idPedido, siguiente).subscribe({
      next: () => {
        this.avanzando.set(null);
        this.aviso.set(
          siguiente === 'ENTREGADO'
            ? `El pedido #${envio.idPedido} quedó entregado.`
            : `El envío del pedido #${envio.idPedido} está en camino.`
        );
        this.recargar$.next();
      },
      error: (e: unknown) => {
        this.avanzando.set(null);
        this.errorAccion.set(
          e instanceof HttpErrorResponse
            ? traducirErrorGestionPedido(e)
            : { titulo: 'No pudimos actualizar el envío', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
        );
        this.recargar$.next();
      }
    });
  }

  /** "1 envío activo", "3 envíos en camino". */
  protected cuenta(): string {
    const total = this.visibles().length;
    const sustantivo = total === 1 ? 'envío' : 'envíos';
    switch (this.filtro()) {
      case 'ACTIVOS':
        return `${total} ${sustantivo} ${total === 1 ? 'activo' : 'activos'}`;
      case 'DESPACHADO':
        return `${total} ${sustantivo} ${total === 1 ? 'despachado' : 'despachados'}`;
      case 'EN_CAMINO':
        return `${total} ${sustantivo} en camino`;
      case 'ENTREGADO':
        return `${total} ${sustantivo} ${total === 1 ? 'entregado' : 'entregados'}`;
      default:
        return `${total} ${sustantivo}`;
    }
  }
}
