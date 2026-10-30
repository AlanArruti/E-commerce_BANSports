import { Routes } from '@angular/router';
import { Home } from './page/home/home';
import { Login } from './page/login/login';
import { Registro } from './page/registro/registro';
import { Perfil } from './page/perfil/perfil';
import { AdminLogin } from './page/admin-login/admin-login';
import { AdminInicio } from './page/admin-inicio/admin-inicio';
import { guestGuard } from './guards/guest.guard';
import { rolGuard } from './guards/rol.guard';

export const routes: Routes = [
  // ── Públicas ───────────────────────────────────────────
  { path: '', component: Home, title: 'BAN Sports | Suplementos deportivos' },

  // Catálogo, detalle y carrito se cargan recién al entrar, para que la primera carga sea liviana.
  {
    path: 'productos',
    loadComponent: () => import('./page/catalogo/catalogo').then((m) => m.Catalogo),
    title: 'Catálogo | BAN Sports'
  },
  {
    // El título final lo pone la página con el nombre del producto.
    path: 'productos/:id',
    loadComponent: () => import('./page/detalle-producto/detalle-producto').then((m) => m.DetalleProducto),
    title: 'Producto | BAN Sports'
  },
  {
    path: 'carrito',
    loadComponent: () => import('./page/carrito/carrito').then((m) => m.Carrito),
    canActivate: [rolGuard('CLIENTE')],
    title: 'Carrito | BAN Sports'
  },

  {
    path: 'checkout',
    loadComponent: () => import('./page/checkout/checkout').then((m) => m.Checkout),
    canActivate: [rolGuard('CLIENTE')],
    title: 'Confirmar pedido | BAN Sports'
  },

  { path: 'login', component: Login, canActivate: [guestGuard], title: 'Iniciar sesión | BAN Sports' },
  { path: 'registro', component: Registro, canActivate: [guestGuard], title: 'Crear cuenta | BAN Sports' },
  // Va antes de 'admin' para que no la tape el guard del panel.
  { path: 'admin/login', component: AdminLogin, canActivate: [guestGuard], title: 'Acceso administradores | BAN Sports' },

  // ── Cliente: todo lo que cuelga de /perfil exige rol CLIENTE ──
  // Las próximas pantallas (direcciones, mis pedidos) se agregan como children.
  {
    path: 'perfil',
    canActivate: [rolGuard('CLIENTE')],
    children: [{ path: '', component: Perfil, title: 'Mi perfil | BAN Sports' }]
  },

  // ── Admin: todo lo que cuelga de /admin exige rol ADMIN ──
  // Categorías, productos y stock se agregan como children.
  {
    path: 'admin',
    canActivate: [rolGuard('ADMIN')],
    children: [
      { path: '', component: AdminInicio, title: 'Panel | BAN Sports' },
      {
        path: 'categorias',
        loadComponent: () => import('./page/admin-categorias/admin-categorias').then((m) => m.AdminCategorias),
        title: 'Categorías | Panel BAN Sports'
      },
      {
        path: 'productos',
        loadComponent: () => import('./page/admin-productos/admin-productos').then((m) => m.AdminProductos),
        title: 'Productos | Panel BAN Sports'
      },
      {
        path: 'productos/nuevo',
        loadComponent: () => import('./page/admin-producto/admin-producto').then((m) => m.AdminProducto),
        title: 'Nuevo producto | Panel BAN Sports'
      },
      {
        // El título final lo pone la página con el nombre del producto.
        path: 'productos/:id/editar',
        loadComponent: () => import('./page/admin-producto/admin-producto').then((m) => m.AdminProducto),
        title: 'Editar producto | Panel BAN Sports'
      },
      {
        // El título final lo pone la página con el nombre del producto.
        path: 'productos/:id/imagen',
        loadComponent: () =>
          import('./page/admin-imagen-producto/admin-imagen-producto').then((m) => m.AdminImagenProducto),
        title: 'Imagen del producto | Panel BAN Sports'
      }
    ]
  },

  { path: '**', redirectTo: '' }
];
