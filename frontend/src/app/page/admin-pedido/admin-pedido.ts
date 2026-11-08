import { Component, computed, effect, signal, ChangeDetectionStrategy } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { BehaviorSubject, Observable, catchError, combineLatest, map, of, startWith, switchMap } from 'rxjs';
import { EstadoPedidoEtiqueta } from '../../componente/estado-pedido/estado-pedido';
import { NoEncontrado } from '../../componente/no-encontrado/no-encontrado';
import { PoteProducto, abreviarNombre } from '../../componente/pote-producto/pote-producto';
import { localidadDeDireccion } from '../../models/direccion';
import { ItemPedido, Pedido, cancelarDevuelveStock } from '../../models/pedido';
import { atributosOrdenados } from '../../models/variante';
import { PrecioPipe } from '../../pipes/precio.pipe';
import { PedidoService } from '../../services/pedido.service';
import { ErrorTraducido, traducirErrorGestionPedido } from '../../validaciones/errores-api';
import { mensajeDeError } from '../../validaciones/mensajes';
import { fechaNoPasada, hoyIso, noAntesDeSalida, requerido } from '../../validaciones/validadores';

interface EstadoDetalle {
  cargando: boolean;
  pedido: Pedido | null;
  noEncontrado: boolean;
  error: ErrorTraducido | null;
}

type Accion = 'preparar' | 'entregar' | 'despachar' | 'cancelar';

const CARGANDO: EstadoDetalle = { cargando: true, pedido: null, noEncontrado: false, error: null };
const ERROR_INESPERADO: ErrorTraducido = {
  titulo: 'No pudimos actualizar el pedido',
  detalle: 'Ocurrió un error inesperado. Intentá de nuevo.'
};

/** Días que se proponen entre la salida y la llegada al abrir "Despachar". */
const DIAS_ENVIO_SUGERIDOS = 4;

function sumarDias(fechaIso: string, dias: number): string {
  const [anio, mes, dia] = fechaIso.split('-').map(Number);
  const fecha = new Date(anio, mes - 1, dia + dias);
  return `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}-${String(fecha.getDate()).padStart(2, '0')}`;
}

/**
 * Detalle de un pedido en el panel (SCRUM-138): ítems, cliente, entrega, factura y envío,
 * con los cambios de estado que permite el back. A Pagado solo se llega cobrando.
 */
@Component({
  selector: 'app-admin-pedido',
  imports: [DatePipe, ReactiveFormsModule, RouterLink, EstadoPedidoEtiqueta, NoEncontrado, PoteProducto, PrecioPipe],
  templateUrl: './admin-pedido.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './admin-pedido.css',
  host: { '(document:keydown.escape)': 'cerrarModal()' }
})
export class AdminPedido {
  protected readonly estado;
  protected readonly unidades;
  protected readonly devuelveStock;
  protected readonly localidad = localidadDeDireccion;

  /** Confirmación de la última acción ("El pedido #7 pasó a En preparación."). */
  protected readonly aviso = signal<string | null>(null);
  /** Error de la última acción. Se muestra en la página (no en el modal) junto al estado real del pedido. */
  protected readonly errorAccion = signal<ErrorTraducido | null>(null);
  protected readonly procesando = signal<Accion | null>(null);

  protected readonly modal = signal<'despachar' | 'cancelar' | null>(null);
  protected readonly intentoDespachar = signal(false);
  protected readonly hoy = hoyIso();

  formEnvio = new FormGroup({
    fechaSalida: new FormControl('', { nonNullable: true, validators: [requerido, fechaNoPasada] }),
    fechaLlegada: new FormControl('', { nonNullable: true, validators: [requerido, noAntesDeSalida] })
  });

  /** Emite para volver a pedirlo (después de una acción o con "Reintentar"). */
  private readonly recargar$ = new BehaviorSubject<void>(undefined);

  constructor(
    private pedidoService: PedidoService,
    route: ActivatedRoute,
    title: Title
  ) {
    const id$ = route.paramMap.pipe(map((params) => Number(params.get('id'))));

    // Al recargar después de una acción no se vuelve al esqueleto: se queda lo que había hasta que llega lo nuevo.
    let anterior: EstadoDetalle = CARGANDO;
    this.estado = toSignal(
      combineLatest([id$, this.recargar$]).pipe(
        switchMap(([id]) => {
          if (!Number.isInteger(id) || id <= 0) {
            return of<EstadoDetalle>({ ...CARGANDO, cargando: false, noEncontrado: true });
          }
          return this.pedidoService.obtener(id).pipe(
            map((pedido): EstadoDetalle => ({ ...CARGANDO, cargando: false, pedido })),
            catchError((e: unknown) => {
              if (e instanceof HttpErrorResponse && e.status === 404) {
                return of<EstadoDetalle>({ ...CARGANDO, cargando: false, noEncontrado: true });
              }
              return of<EstadoDetalle>({
                ...CARGANDO,
                cargando: false,
                error: e instanceof HttpErrorResponse ? traducirErrorGestionPedido(e) : ERROR_INESPERADO
              });
            }),
            startWith(anterior.pedido?.id === id ? anterior : CARGANDO)
          );
        }),
        map((estado) => (anterior = estado))
      ),
      { initialValue: CARGANDO }
    );

    this.unidades = computed(
      () => this.estado().pedido?.items.reduce((total, item) => total + item.cantidad, 0) ?? 0
    );
    this.devuelveStock = computed(() => {
      const pedido = this.estado().pedido;
      return !!pedido && cancelarDevuelveStock(pedido.estado);
    });

    // Cambiar la salida puede arreglar (o romper) la llegada.
    this.formEnvio.controls.fechaSalida.valueChanges.subscribe(() =>
      this.formEnvio.controls.fechaLlegada.updateValueAndValidity()
    );

    effect(() => {
      const { pedido, noEncontrado } = this.estado();
      if (pedido) {
        title.setTitle(`Pedido #${pedido.id} | Panel BAN Sports`);
      } else if (noEncontrado) {
        title.setTitle('Pedido no encontrado | Panel BAN Sports');
      }
    });
  }

  reintentar(): void {
    this.recargar$.next();
  }

  // ── Acciones directas ──────────────────────────────────

  pasarAPreparacion(pedido: Pedido): void {
    this.ejecutar('preparar', this.pedidoService.cambiarEstado(pedido.id, 'EN_PREPARACION'), `El pedido #${pedido.id} pasó a En preparación.`);
  }

  marcarEntregado(pedido: Pedido): void {
    this.ejecutar('entregar', this.pedidoService.cambiarEstado(pedido.id, 'ENTREGADO'), `El pedido #${pedido.id} quedó entregado.`);
  }

  // ── Modales ────────────────────────────────────────────

  abrirDespacho(): void {
    this.formEnvio.reset({ fechaSalida: this.hoy, fechaLlegada: sumarDias(this.hoy, DIAS_ENVIO_SUGERIDOS) });
    this.intentoDespachar.set(false);
    this.abrirModal('despachar');
  }

  abrirCancelacion(): void {
    this.abrirModal('cancelar');
  }

  cerrarModal(): void {
    if (!this.procesando()) {
      this.modal.set(null);
    }
  }

  confirmarDespacho(pedido: Pedido): void {
    this.intentoDespachar.set(true);
    if (this.formEnvio.invalid) {
      return;
    }
    const fechas = this.formEnvio.getRawValue();
    this.ejecutar('despachar', this.pedidoService.despachar(pedido.id, fechas), `Despachaste el pedido #${pedido.id}.`);
  }

  confirmarCancelacion(pedido: Pedido): void {
    const texto = cancelarDevuelveStock(pedido.estado)
      ? `Cancelaste el pedido #${pedido.id}. El stock de sus productos volvió al catálogo.`
      : `Cancelaste el pedido #${pedido.id}.`;
    this.ejecutar('cancelar', this.pedidoService.cancelar(pedido.id), texto);
  }

  errorFecha(campo: 'fechaSalida' | 'fechaLlegada'): string | null {
    const control = this.formEnvio.controls[campo];
    return this.intentoDespachar() || control.touched ? mensajeDeError(control, campo) : null;
  }

  // ── Presentación ───────────────────────────────────────

  /** "1 kg · Chocolate". Vacío si el producto no tiene variante. */
  protected descripcionVariante(item: ItemPedido): string {
    return item.variante
      ? atributosOrdenados(item.variante)
          .map(([, valor]) => valor)
          .join(' · ')
      : '';
  }

  /** El back guarda el total de la línea; el unitario es el precio al momento de la compra. */
  protected precioUnitario(item: ItemPedido): number {
    return item.precioTotalProducto / item.cantidad;
  }

  protected abreviatura(item: ItemPedido): string {
    return abreviarNombre(item.producto.nombre);
  }

  private abrirModal(cual: 'despachar' | 'cancelar'): void {
    this.aviso.set(null);
    this.errorAccion.set(null);
    this.modal.set(cual);
  }

  /**
   * Corre la acción y siempre vuelve a pedir el pedido: si salió bien, para mostrar el estado nuevo;
   * si falló porque otro lo cambió antes, para que se vea el estado real.
   */
  private ejecutar(accion: Accion, pedido$: Observable<unknown>, textoOk: string): void {
    this.procesando.set(accion);
    this.aviso.set(null);
    this.errorAccion.set(null);
    pedido$.subscribe({
      next: () => {
        this.procesando.set(null);
        this.modal.set(null);
        this.aviso.set(textoOk);
        this.recargar$.next();
      },
      error: (e: unknown) => {
        this.procesando.set(null);
        this.modal.set(null);
        this.errorAccion.set(e instanceof HttpErrorResponse ? traducirErrorGestionPedido(e) : ERROR_INESPERADO);
        this.recargar$.next();
      }
    });
  }
}
