import { Component, input } from '@angular/core';
import { NgClass } from '@angular/common';
import { RouterLink } from '@angular/router';

/**
 * Panel de marca de las pantallas de login y registro.
 * El contenido (título y beneficios) se pasa desde la página con <ng-content>.
 */
@Component({
  selector: 'app-panel-auth',
  imports: [NgClass, RouterLink],
  templateUrl: './panel-auth.html',
  styleUrl: './panel-auth.css'
})
export class PanelAuth {
  /** 'oscuro' = login (fondo negro con franja verde), 'verde' = registro. */
  readonly variante = input<'oscuro' | 'verde'>('oscuro');
  protected readonly anio = new Date().getFullYear();
}
