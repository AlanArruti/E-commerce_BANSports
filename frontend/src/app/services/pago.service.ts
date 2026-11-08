import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { environment } from '../../environments/environment';
import { EstadoPedido } from '../models/pedido';

/** Tipo de factura que se emite al confirmarse el pago. */
export type TipoFactura = 'A' | 'B';

/** Estado de un intento de pago, igual para todas las pasarelas. */
export type EstadoPago = 'CREADO' | 'PENDIENTE' | 'APROBADO' | 'RECHAZADO';

/** Una pasarela que el cliente puede elegir (GET /pago/pasarelas). */
export interface Pasarela {
  codigo: string;
  nombre: string;
}

/** Un intento de pago (PagoResponse). */
export interface Pago {
  id: number;
  pedidoId: number;
  pasarela: string;
  estado: EstadoPago;
  tipoFactura: TipoFactura;
  monto: number;
  montoPagado: number | null;
  estadoPedido: EstadoPedido;
  fechaCreacion: string;
}

interface InicioPago {
  pagoId: number;
  urlPago: string;
}

/**
 * Pago con pasarelas (SCRUM-122). El front no sabe nada de cada pasarela:
 * 1. iniciarPago → el back registra el intento y devuelve la URL de la pasarela elegida.
 * 2. El cliente paga allá y vuelve a /pago/resultado?pago={id}.
 * 3. confirmarPago → el back consulta a la pasarela y, si está aprobado, el pedido queda PAGADO.
 */
@Injectable({ providedIn: 'root' })
export class PagoService {
  private readonly API_URL = `${environment.apiUrl}/pago`;

  constructor(private http: HttpClient) {}

  /** Las pasarelas con credenciales cargadas. Si el back no responde, ninguna. */
  pasarelas(): Observable<Pasarela[]> {
    return this.http.get<Pasarela[]>(`${this.API_URL}/pasarelas`).pipe(catchError(() => of([])));
  }

  iniciarPago(idPedido: number, pasarela: string, tipoFactura: TipoFactura): Observable<InicioPago> {
    return this.http.post<InicioPago>(`${this.API_URL}/pedido/${idPedido}`, { pasarela, tipoFactura });
  }

  confirmarPago(idPago: number): Observable<Pago> {
    return this.http.post<Pago>(`${this.API_URL}/${idPago}/confirmar`, {});
  }

  /** Intentos de pago de un pedido, del más nuevo al más viejo (sirve para Mis pedidos). */
  historial(idPedido: number): Observable<Pago[]> {
    return this.http.get<Pago[]>(`${this.API_URL}/pedido/${idPedido}`);
  }
}
