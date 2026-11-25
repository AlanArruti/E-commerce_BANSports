import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Pedido } from '../../models/pedido';
import { PedidoService } from '../../services/pedido.service';
import { ErrorTraducido, traducirErrorRecepcion } from '../../validaciones/errores-api';

/** Largo máximo del comentario (igual que ConfirmarRecepcionRequest en el back). */
const MAXIMO_COMENTARIO = 500;

/**
 * RF03: en un pedido entregado, el cliente confirma si le llegó ("Lo recibí" / "No lo recibí").
 * "No lo recibí" deja un comentario opcional para la tienda y se puede cambiar después a "Lo recibí".
 * Lo usan Mis pedidos y el detalle del pedido. En pedidos no entregados no muestra nada.
 */
@Component({
  selector: 'app-confirmar-recepcion',
  imports: [DatePipe],
  templateUrl: './confirmar-recepcion.html',
  styleUrl: './confirmar-recepcion.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ConfirmarRecepcion {
  private readonly pedidoService = inject(PedidoService);

  readonly pedido = input.required<Pedido>();
  /** El pedido como quedó después de responder. */
  readonly confirmado = output<Pedido>();

  protected readonly maximo = MAXIMO_COMENTARIO;
  /** Formulario de "No lo recibí" abierto. */
  protected readonly contando = signal(false);
  protected readonly comentario = signal('');
  protected readonly enviando = signal(false);
  protected readonly error = signal<ErrorTraducido | null>(null);

  protected abrirNoRecibido(): void {
    this.error.set(null);
    this.contando.set(true);
  }

  protected volver(): void {
    this.contando.set(false);
    this.comentario.set('');
  }

  protected escribir(evento: Event): void {
    this.comentario.set((evento.target as HTMLTextAreaElement).value);
  }

  protected responder(recibido: boolean): void {
    this.enviando.set(true);
    this.error.set(null);
    const comentario = recibido ? undefined : this.comentario().trim();
    this.pedidoService.confirmarRecepcion(this.pedido().id, recibido, comentario).subscribe({
      next: (pedido) => {
        this.enviando.set(false);
        this.contando.set(false);
        this.comentario.set('');
        this.confirmado.emit(pedido);
      },
      error: (e: unknown) => {
        this.enviando.set(false);
        this.error.set(
          e instanceof HttpErrorResponse
            ? traducirErrorRecepcion(e)
            : { titulo: 'No pudimos guardar tu respuesta', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
        );
      }
    });
  }
}
