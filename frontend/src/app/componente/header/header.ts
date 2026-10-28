import { Component, computed, ChangeDetectionStrategy } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-header',
  imports: [RouterLink],
  templateUrl: './header.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './header.css'
})
export class Header {
  protected readonly categorias = ['Proteínas', 'Creatinas', 'Pre-entrenos', 'Vitaminas'];

  /** Nombre a mostrar: el que confirmó el back o, mientras tanto, el email del token. */
  protected readonly nombreUsuario = computed(
    () => this.authService.cliente()?.nombre ?? this.authService.usuario()?.sub ?? ''
  );

  constructor(
    protected authService: AuthService,
    private router: Router
  ) {}

  cerrarSesion(): void {
    this.authService.logout();
    this.router.navigate(['/']);
  }
}
