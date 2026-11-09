import { Routes } from '@angular/router';
import { Home } from './page/home/home';
import { Login } from './page/login/login';
import { Registro } from './page/registro/registro';
import { Perfil } from './page/perfil/perfil';
import { AdminLogin } from './page/admin-login/admin-login';
import { guestGuard } from './guards/guest.guard';
import { rolGuard, sinAdminGuard } from './guards/rol.guard';

export const routes: Routes = [
  // ── Públicas ───────────────────────────────────────────
  // La tienda es de invitados y clientes: el admin que entra acá va a su panel (sinAdminGuard).
  { path: '', component: Home, canActivate: [sinAdminGuard], title: 'BAN Sports | Suplementos deportivos' },

  // Catálogo, detalle y carrito se cargan recién al entrar, para que la primera carga sea liviana.
  {
    path: 'productos',
    loadComponent: () => import('./page/catalogo/catalogo').then((m) => m.Catalogo),
    canActivate: [sinAdminGuard],
    title: 'Catálogo | BAN Sports'
  },
  {
    path: 'ofertas',
    loadComponent: () => import('./page/catalogo/catalogo').then((m) => m.Catalogo),
    data: { ofertas: true },
    canActivate: [sinAdminGuard],
    title: 'Ofertas | BAN Sports'
  },
  {
    // El título final lo pone la página con el nombre del producto.
    path: 'productos/:id',
    loadComponent: () => import('./page/detalle-producto/detalle-producto').then((m) => m.DetalleProducto),
    canActivate: [sinAdminGuard],
    title: 'Producto | BAN Sports'
  },
  {
    path: 'carrito',
    loadComponent: () => import('./page/carrito/carrito').then((m) => m.Carrito),
    // Funciona sin sesión: el carrito se guarda en el navegador hasta que el cliente ingresa.
    canActivate: [sinAdminGuard],
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
  {
    // Vuelta de MercadoPago (?pedido=...&payment_id=...&status=...). Ver MercadoPagoService.
    path: 'pago/resultado',
    loadComponent: () => import('./page/resultado-pago/resultado-pago').then((m) => m.ResultadoPago),
    canActivate: [rolGuard('CLIENTE')],
    title: 'Resultado del pago | BAN Sports'
  },
  {
    // Vuelta del login con GitHub o Google (?token=...). Ver OAUTH2_REDIRECT_URI en compose.yaml.
    path: 'ingreso-social',
    loadComponent: () => import('./page/ingreso-social/ingreso-social').then((m) => m.IngresoSocial),
    title: 'Ingresando | BAN Sports'
  },
  {
    path: 'recuperar-contrasena',
    loadComponent: () =>
      import('./page/recuperar-contrasena/recuperar-contrasena').then((m) => m.RecuperarContrasena),
    canActivate: [guestGuard],
    title: 'Recuperar contraseña | BAN Sports'
  },
  {
    // Es la página del enlace que llega por mail (?token=...).
    path: 'restablecer-contrasena',
    loadComponent: () =>
      import('./page/restablecer-contrasena/restablecer-contrasena').then((m) => m.RestablecerContrasena),
    canActivate: [guestGuard],
    title: 'Contraseña nueva | BAN Sports'
  },
  // Va antes de 'admin' para que no la tape el guard del panel.
  { path: 'admin/login', component: AdminLogin, canActivate: [guestGuard], title: 'Acceso administradores | BAN Sports' },

  // ── Cliente: todo lo que cuelga de /perfil exige rol CLIENTE ──
  // Las próximas pantallas (direcciones, mis pedidos) se agregan como children.
  {
    path: 'perfil',
    // canActivateChild vuelve a validar al pasar de una pantalla hija a otra (canActivate solo corre al entrar).
    canActivate: [rolGuard('CLIENTE')],
    canActivateChild: [rolGuard('CLIENTE')],
    children: [
      { path: '', component: Perfil, title: 'Mi perfil | BAN Sports' },
      {
        path: 'favoritos',
        loadComponent: () => import('./page/favoritos/favoritos').then((m) => m.Favoritos),
        title: 'Mis favoritos | BAN Sports'
      },
      {
        path: 'pedidos',
        loadComponent: () => import('./page/mis-pedidos/mis-pedidos').then((m) => m.MisPedidos),
        title: 'Mis pedidos | BAN Sports'
      },
      {
        // El título final lo pone la página con el número de pedido.
        path: 'pedidos/:id',
        loadComponent: () => import('./page/detalle-pedido/detalle-pedido').then((m) => m.DetallePedido),
        title: 'Pedido | BAN Sports'
      },
      {
        // El título final lo pone la página con el tipo y número de factura.
        path: 'pedidos/:id/factura',
        loadComponent: () => import('./page/factura-pedido/factura-pedido').then((m) => m.FacturaPedido),
        title: 'Factura | BAN Sports'
      }
    ]
  },

  // ── Admin: todo lo que cuelga de /admin exige rol ADMIN ──
  // Categorías, productos y stock se agregan como children.
  {
    path: 'admin',
    canActivate: [rolGuard('ADMIN')],
    canActivateChild: [rolGuard('ADMIN')],
    children: [
      {
        path: '',
        loadComponent: () => import('./page/admin-inicio/admin-inicio').then((m) => m.AdminInicio),
        title: 'Panel | BAN Sports'
      },
      {
        path: 'pedidos',
        loadComponent: () => import('./page/admin-pedidos/admin-pedidos').then((m) => m.AdminPedidos),
        title: 'Pedidos | Panel BAN Sports'
      },
      {
        path: 'clientes',
        loadComponent: () => import('./page/admin-clientes/admin-clientes').then((m) => m.AdminClientes),
        title: 'Clientes | Panel BAN Sports'
      },
      {
        path: 'administradores',
        loadComponent: () =>
          import('./page/admin-administradores/admin-administradores').then((m) => m.AdminAdministradores),
        title: 'Administradores | Panel BAN Sports'
      },
      {
        path: 'cupones',
        loadComponent: () => import('./page/admin-cupones/admin-cupones').then((m) => m.AdminCupones),
        title: 'Cupones | Panel BAN Sports'
      },
      {
        path: 'stock-bajo',
        loadComponent: () => import('./page/admin-stock-bajo/admin-stock-bajo').then((m) => m.AdminStockBajo),
        title: 'Stock bajo | Panel BAN Sports'
      },
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
      },
      {
        // El título final lo pone la página con el nombre del producto.
        path: 'productos/:id/variantes',
        loadComponent: () => import('./page/admin-variantes/admin-variantes').then((m) => m.AdminVariantes),
        title: 'Variantes y stock | Panel BAN Sports'
      },
      {
        // El título final lo pone la página con el número de pedido.
        path: 'pedidos/:id',
        loadComponent: () => import('./page/admin-pedido/admin-pedido').then((m) => m.AdminPedido),
        title: 'Pedido | Panel BAN Sports'
      },
      {
        path: 'envios',
        loadComponent: () => import('./page/admin-envios/admin-envios').then((m) => m.AdminEnvios),
        title: 'Envíos | Panel BAN Sports'
      }
    ]
  },

  // Cualquier otra ruta: página 404 (antes volvía al inicio sin avisar).
  {
    path: '**',
    loadComponent: () => import('./page/no-encontrada/no-encontrada').then((m) => m.NoEncontrada),
    title: 'Página no encontrada | BAN Sports'
  }
];
