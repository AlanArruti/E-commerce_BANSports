import { Component, input, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * Bloque "404" para todo lo que no existe: rutas inexistentes, productos dados de baja
 * y, más adelante, pedidos que no son del cliente o no existen. Siempre ofrece volver.
 */
@Component({
  selector: 'app-no-encontrado',
  imports: [RouterLink],
  templateUrl: './no-encontrado.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './no-encontrado.css'
})
export class NoEncontrado {
  readonly titulo = input('No encontramos esta página');
  readonly texto = input('Puede que el link esté mal escrito o que la página ya no exista.');
}
