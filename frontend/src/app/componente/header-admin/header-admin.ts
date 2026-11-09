import { Component, ChangeDetectionStrategy } from '@angular/core';
import { IsActiveMatchOptions, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../services/auth.service';

interface SeccionPanel {
  ruta: string;
  etiqueta: string;
}

/**
 * Header del admin. El admin no navega la tienda: solo ve las secciones del panel.
 * Reemplaza al header de la tienda (ver app.html).
 */
@Component({
  selector: 'app-header-admin',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './header-admin.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './header-admin.css'
})
export class HeaderAdmin {
  protected readonly secciones: SeccionPanel[] = [
    { ruta: '/admin', etiqueta: 'Inicio' },
    { ruta: '/admin/pedidos', etiqueta: 'Pedidos' },
    { ruta: '/admin/envios', etiqueta: 'Envíos' },
    { ruta: '/admin/productos', etiqueta: 'Productos' },
    { ruta: '/admin/categorias', etiqueta: 'Categorías' },
    { ruta: '/admin/cupones', etiqueta: 'Cupones' },
    { ruta: '/admin/stock-bajo', etiqueta: 'Stock' },
    { ruta: '/admin/clientes', etiqueta: 'Clientes' },
    { ruta: '/admin/administradores', etiqueta: 'Admins' }
  ];

  /** "Inicio" solo se marca en /admin; el resto también en sus subpantallas (/admin/pedidos/12). */
  protected readonly exacta: IsActiveMatchOptions = {
    paths: 'exact',
    queryParams: 'ignored',
    matrixParams: 'ignored',
    fragment: 'ignored'
  };
  protected readonly parcial: IsActiveMatchOptions = { ...this.exacta, paths: 'subset' };

  constructor(
    protected authService: AuthService,
    private router: Router
  ) {}

  cerrarSesion(): void {
    this.authService.logout();
    this.router.navigate(['/admin/login']);
  }
}
