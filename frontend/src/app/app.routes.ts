import { Routes } from '@angular/router';
import { Home } from './page/home/home';
import { Login } from './page/login/login';
import { Registro } from './page/registro/registro';
import { guestGuard } from './guards/guest.guard';
import { rolGuard } from './guards/rol.guard';

export const routes: Routes = [
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

  { path: 'login', component: Login, canActivate: [guestGuard], title: 'Iniciar sesión | BAN Sports' },
  { path: 'registro', component: Registro, canActivate: [guestGuard], title: 'Crear cuenta | BAN Sports' },

  // Ejemplos para las próximas pantallas (los guards ya están listos):
  // { path: 'mis-pedidos', component: MisPedidos, canActivate: [rolGuard('CLIENTE')] },
  // { path: 'mis-pedidos/:id', component: DetallePedido, canActivate: [rolGuard('CLIENTE')] },
  // { path: 'panel', component: Panel, canActivate: [rolGuard('ADMIN')] },

  { path: '**', redirectTo: '' }
];
