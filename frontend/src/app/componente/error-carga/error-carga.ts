import { Component, input, output, ChangeDetectionStrategy } from '@angular/core';
import { ConexionService } from '../../services/conexion.service';
import { ErrorTraducido } from '../../validaciones/errores-api';

/**
 * Error al cargar una pantalla, con "Reintentar".
 * Si es un problema de conexión y la franja general ya lo está avisando, no se repite:
 * queda solo una línea tranquila hasta que vuelva la conexión.
 */
@Component({
  selector: 'app-error-carga',
  templateUrl: './error-carga.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './error-carga.css'
})
export class ErrorCarga {
  readonly error = input.required<ErrorTraducido>();
  readonly reintentar = output<void>();

  constructor(protected conexion: ConexionService) {}
}
