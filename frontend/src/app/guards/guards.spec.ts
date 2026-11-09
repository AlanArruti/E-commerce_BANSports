import { computed, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { Observable, firstValueFrom, of } from 'rxjs';
import { JwtPayload } from '../models/jwt-payload';
import { Rol } from '../models/rol';
import { AuthService } from '../services/auth.service';
import { guestGuard } from './guest.guard';
import { routes } from '../app.routes';
import { rolGuard, sinAdminGuard } from './rol.guard';

/** AuthService falso: cada test define si la sesión es válida y con qué rol. */
function authFalso(sesionValida: boolean, rol: Rol | null) {
  const usuario = signal<JwtPayload | null>(rol ? { sub: 'x', rol, id: 1, iat: 0, exp: 0 } as JwtPayload : null);
  return {
    usuario,
    esAdmin: computed(() => usuario()?.rol === 'ADMIN'),
    validarSesion: () => of(sesionValida)
  };
}

function configurar(sesionValida: boolean, rol: Rol | null): void {
  TestBed.configureTestingModule({
    providers: [provideRouter([]), { provide: AuthService, useValue: authFalso(sesionValida, rol) }]
  });
}

/** Corre un guard para la URL dada y devuelve true o la URL a la que redirige, como texto. */
async function correr(guard: (r: ActivatedRouteSnapshot, s: RouterStateSnapshot) => unknown, url: string): Promise<true | string> {
  const resultado = await firstValueFrom(
    TestBed.runInInjectionContext(
      () => guard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot) as Observable<boolean | UrlTree>
    )
  );
  return resultado instanceof UrlTree ? TestBed.inject(Router).serializeUrl(resultado) : (resultado as true);
}

describe('rolGuard', () => {
  it('sin sesión manda al login del admin y recuerda a dónde quería ir', async () => {
    configurar(false, null);
    expect(await correr(rolGuard('ADMIN'), '/admin/pedidos')).toBe('/admin/login?redirect=%2Fadmin%2Fpedidos');
  });

  it('sin sesión, en una pantalla de cliente, manda al login de clientes', async () => {
    configurar(false, null);
    expect(await correr(rolGuard('CLIENTE'), '/checkout')).toBe('/login?redirect=%2Fcheckout');
  });

  it('un CLIENTE no entra a /admin: vuelve a la tienda', async () => {
    configurar(true, 'CLIENTE');
    expect(await correr(rolGuard('ADMIN'), '/admin')).toBe('/');
  });

  it('un ADMIN no entra a pantallas de cliente: vuelve al panel', async () => {
    configurar(true, 'ADMIN');
    expect(await correr(rolGuard('CLIENTE'), '/perfil')).toBe('/admin');
  });

  it('deja pasar al rol correcto', async () => {
    configurar(true, 'ADMIN');
    expect(await correr(rolGuard('ADMIN'), '/admin')).toBeTrue();
  });
});

describe('sinAdminGuard', () => {
  it('deja entrar sin sesión (el carrito funciona sin cuenta)', async () => {
    configurar(false, null);
    expect(await correr(sinAdminGuard, '/carrito')).toBeTrue();
  });

  it('manda al admin a su panel', async () => {
    configurar(true, 'ADMIN');
    expect(await correr(sinAdminGuard, '/carrito')).toBe('/admin');
  });

  it('deja navegar la tienda al cliente', async () => {
    configurar(true, 'CLIENTE');
    expect(await correr(sinAdminGuard, '/productos/2')).toBeTrue();
  });

  it('el admin no navega la tienda: inicio, catálogo, ofertas, producto y carrito lo mandan al panel', () => {
    const tienda = ['', 'productos', 'ofertas', 'productos/:id', 'carrito'];
    for (const path of tienda) {
      const ruta = routes.find((r) => r.path === path);
      expect(ruta?.canActivate).withContext(path || '(inicio)').toContain(sinAdminGuard);
    }
  });
});

describe('guestGuard', () => {
  it('sin sesión deja ver el login', async () => {
    configurar(false, null);
    expect(await correr(guestGuard, '/login')).toBeTrue();
  });

  it('un cliente con sesión vuelve a la tienda', async () => {
    configurar(true, 'CLIENTE');
    expect(await correr(guestGuard, '/login')).toBe('/');
  });

  it('un admin con sesión vuelve al panel', async () => {
    configurar(true, 'ADMIN');
    expect(await correr(guestGuard, '/registro')).toBe('/admin');
  });

  it('con un token que el back rechazó, deja ver el login', async () => {
    configurar(false, 'CLIENTE');
    expect(await correr(guestGuard, '/login')).toBeTrue();
  });
});
