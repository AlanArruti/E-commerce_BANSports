import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

/**
 * Inicio del panel de administración (ruta protegida por rol ADMIN).
 * Es la base: el layout y la navegación del panel llegan con SCRUM-111.
 */
@Component({
  selector: 'app-admin-inicio',
  imports: [RouterLink],
  templateUrl: './admin-inicio.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './admin-inicio.css'
})
export class AdminInicio {
  constructor(protected authService: AuthService) {}
}
