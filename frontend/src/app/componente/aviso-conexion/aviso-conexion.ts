import { Component, ChangeDetectionStrategy } from '@angular/core';
import { ConexionService } from '../../services/conexion.service';

/** Franja única debajo del header cuando el back no responde, y aviso breve cuando vuelve. */
@Component({
  selector: 'app-aviso-conexion',
  templateUrl: './aviso-conexion.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './aviso-conexion.css'
})
export class AvisoConexion {
  constructor(protected conexion: ConexionService) {}
}
