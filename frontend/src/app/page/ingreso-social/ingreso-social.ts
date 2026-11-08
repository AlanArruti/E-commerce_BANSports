import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CLAVE_DESTINO_SOCIAL } from '../../componente/botones-sociales/botones-sociales';
import { AuthService } from '../../services/auth.service';

/**
 * Vuelta del login con GitHub o Google. El back redirige acá con ?token=...;
 * se guarda la sesión y se sigue a donde estaba yendo el cliente (o al inicio).
 */
@Component({
  selector: 'app-ingreso-social',
  template: `
    <div class="ingreso" role="status">
      <span class="spinner" aria-hidden="true"></span>
      Ingresando…
    </div>
  `,
  styles: `
    .ingreso {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 12px;
      color: var(--color-texto-secundario);
    }
    .spinner {
      width: 20px;
      height: 20px;
      border: 2px solid var(--color-borde);
      border-top-color: var(--color-verde);
      border-radius: 50%;
      animation: girar 0.8s linear infinite;
    }
    @keyframes girar {
      to {
        transform: rotate(360deg);
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class IngresoSocial implements OnInit {
  constructor(
    private authService: AuthService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.irAlLogin();
      return;
    }

    this.authService.iniciarSesionSocial(token).subscribe((ok) => {
      if (!ok) {
        this.authService.logout();
        this.irAlLogin();
        return;
      }
      // replaceUrl: el token no queda en el historial del navegador.
      this.router.navigateByUrl(this.leerDestino(), { replaceUrl: true });
    });
  }

  private irAlLogin(): void {
    this.router.navigate(['/login'], { queryParams: { errorSocial: 'fallo' }, replaceUrl: true });
  }

  private leerDestino(): string {
    try {
      const destino = sessionStorage.getItem(CLAVE_DESTINO_SOCIAL);
      sessionStorage.removeItem(CLAVE_DESTINO_SOCIAL);
      if (destino && destino.startsWith('/') && !destino.startsWith('//')) {
        return destino;
      }
    } catch {
      // Sin almacenamiento: al inicio.
    }
    return '/';
  }
}
