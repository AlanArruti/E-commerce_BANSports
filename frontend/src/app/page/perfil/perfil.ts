import { Component, ChangeDetectionStrategy } from '@angular/core';
import { AuthService } from '../../services/auth.service';

/**
 * "Mi perfil" del cliente (ruta protegida por rol CLIENTE).
 * Por ahora es la base de la sección: los datos editables llegan con SCRUM-108.
 */
@Component({
  selector: 'app-perfil',
  templateUrl: './perfil.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './perfil.css'
})
export class Perfil {
  constructor(protected authService: AuthService) {}
}
