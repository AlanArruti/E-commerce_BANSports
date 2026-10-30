import { Component, computed, signal, ChangeDetectionStrategy } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { PerfilModal } from '../perfil-modal/perfil-modal';

@Component({
  selector: 'app-header',
  imports: [RouterLink, PerfilModal],
  templateUrl: './header.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './header.css'
})
export class Header {
  protected readonly categorias = ['Proteínas', 'Creatinas', 'Pre-entrenos', 'Vitaminas'];
  protected readonly perfilAbierto = signal(false);

  /** Nombre a mostrar: el que confirmó el back o, mientras tanto, el email del token. */
  protected readonly nombreUsuario = computed(
    () => this.authService.cliente()?.nombre ?? this.authService.usuario()?.sub ?? ''
  );

  constructor(
    protected authService: AuthService,
    private router: Router
  ) {}

  abrirPerfil(): void {
    this.perfilAbierto.set(true);
  }

  cerrarPerfil(): void {
    this.perfilAbierto.set(false);
  }

  cerrarSesion(): void {
    this.authService.logout();
    this.router.navigate(['/']);
  }
}
