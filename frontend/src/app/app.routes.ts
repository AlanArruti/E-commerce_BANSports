import { Routes } from '@angular/router';
import { Home } from './page/home/home';
import { Login } from './page/login/login';
import { Registro } from './page/registro/registro';
import { guestGuard } from './guards/guest.guard';

export const routes: Routes = [
  { path: '', component: Home, title: 'BAN Sports | Suplementos deportivos' },
  { path: 'login', component: Login, canActivate: [guestGuard], title: 'Iniciar sesión | BAN Sports' },
  { path: 'registro', component: Registro, canActivate: [guestGuard], title: 'Crear cuenta | BAN Sports' },

  // Ejemplos para las próximas pantallas (los guards ya están listos):
  // { path: 'mis-pedidos', component: MisPedidos, canActivate: [rolGuard('CLIENTE')] },
  // { path: 'mis-pedidos/:id', component: DetallePedido, canActivate: [rolGuard('CLIENTE')] },
  // { path: 'panel', component: Panel, canActivate: [rolGuard('ADMIN')] },

  { path: '**', redirectTo: '' }
];
