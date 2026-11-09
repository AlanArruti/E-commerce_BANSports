import { Component, ChangeDetectionStrategy } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { AvisoConexion } from './componente/aviso-conexion/aviso-conexion';
import { Header } from './componente/header/header';
import { HeaderAdmin } from './componente/header-admin/header-admin';
import { Footer } from './componente/footer/footer';
import { AuthService } from './services/auth.service';

// Rutas que se muestran a pantalla completa, sin header ni footer.
const RUTAS_SIN_LAYOUT = [
  '/login',
  '/registro',
  '/ingreso-social',
  '/recuperar-contrasena',
  '/restablecer-contrasena',
  '/admin/login'
];

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, AvisoConexion, Header, HeaderAdmin, Footer],
  templateUrl: './app.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './app.css'
})
export class App {
  protected readonly mostrarLayout;

  constructor(
    private router: Router,
    protected authService: AuthService
  ) {
    this.mostrarLayout = toSignal(
      this.router.events.pipe(
        filter((evento) => evento instanceof NavigationEnd),
        map((evento) => !RUTAS_SIN_LAYOUT.includes(evento.urlAfterRedirects.split('?')[0]))
      ),
      { initialValue: true }
    );
  }
}
