import { Component, ChangeDetectionStrategy } from '@angular/core';
import { CarritoService } from '../../services/carrito.service';

/**
 * Avisa qué pasó al sumar el carrito sin sesión al de la cuenta (lo que se sumó,
 * lo que se ajustó por stock y lo que no se pudo sumar). Va en el carrito y el checkout.
 */
@Component({
  selector: 'app-aviso-fusion',
  templateUrl: './aviso-fusion.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './aviso-fusion.css'
})
export class AvisoFusion {
  constructor(protected carritoService: CarritoService) {}

  protected cerrar(): void {
    this.carritoService.avisoFusion.set(null);
  }
}
