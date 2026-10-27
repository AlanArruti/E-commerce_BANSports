import { Component } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { Header } from './componente/header/header';
import { Footer } from './componente/footer/footer';

// Rutas que se muestran a pantalla completa, sin header ni footer.
const RUTAS_SIN_LAYOUT = ['/login', '/registro'];

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Header, Footer],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly mostrarLayout;

  constructor(private router: Router) {
    this.mostrarLayout = toSignal(
      this.router.events.pipe(
        filter((evento) => evento instanceof NavigationEnd),
        map((evento) => !RUTAS_SIN_LAYOUT.includes(evento.urlAfterRedirects.split('?')[0]))
      ),
      { initialValue: true }
    );
  }
}
