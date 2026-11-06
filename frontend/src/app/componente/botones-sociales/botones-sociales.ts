import { ChangeDetectionStrategy, Component, OnInit, input, signal } from '@angular/core';
import { AuthService, ProveedorSocial } from '../../services/auth.service';

/** A dónde volver después del login social. Lo lee la página /ingreso-social. */
export const CLAVE_DESTINO_SOCIAL = 'ban_destino_social';

/**
 * Botones "Continuar con GitHub / Google" (SCRUM-102, SCRUM-135).
 * Solo muestra los proveedores que el back tiene configurados; si no hay ninguno, no muestra nada
 * (ni el separador).
 */
@Component({
  selector: 'app-botones-sociales',
  templateUrl: './botones-sociales.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './botones-sociales.css'
})
export class BotonesSociales implements OnInit {
  /** Dónde va el separador respecto de los botones: en el login van arriba del formulario, en el registro abajo. */
  readonly separador = input<'antes' | 'despues'>('despues');
  readonly textoSeparador = input('o');
  /** Ruta interna a la que volver al terminar (por ejemplo, el producto que se estaba viendo). */
  readonly destino = input<string | null>(null);

  protected readonly proveedores = signal<ProveedorSocial[]>([]);

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    this.authService.proveedoresSociales().subscribe((lista) => this.proveedores.set(lista));
  }

  protected url(proveedor: ProveedorSocial): string {
    return this.authService.urlLoginSocial(proveedor);
  }

  /** Se guarda antes de salir hacia GitHub/Google, porque la vuelta llega a otra página. */
  protected recordarDestino(): void {
    const destino = this.destino();
    try {
      if (destino && destino.startsWith('/') && !destino.startsWith('//')) {
        sessionStorage.setItem(CLAVE_DESTINO_SOCIAL, destino);
      } else {
        sessionStorage.removeItem(CLAVE_DESTINO_SOCIAL);
      }
    } catch {
      // Sin almacenamiento disponible se vuelve al inicio.
    }
  }
}
