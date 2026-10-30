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
    children: [{ path: '', component: AdminInicio, title: 'Panel | BAN Sports' }]
  },

  { path: '**', redirectTo: '' }
];
