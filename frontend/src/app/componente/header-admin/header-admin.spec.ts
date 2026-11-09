import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { JwtPayload } from '../../models/jwt-payload';
import { AuthService } from '../../services/auth.service';
import { HeaderAdmin } from './header-admin';

describe('HeaderAdmin (header del panel)', () => {
  let fixture: ComponentFixture<HeaderAdmin>;
  let pantalla: HTMLElement;
  let logout: jasmine.Spy;

  beforeEach(() => {
    logout = jasmine.createSpy('logout');
    TestBed.configureTestingModule({
      imports: [HeaderAdmin],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: { usuario: signal({ sub: 'admin', rol: 'ADMIN', id: 1, iat: 0, exp: 0 } as JwtPayload), logout }
        }
      ]
    });
    fixture = TestBed.createComponent(HeaderAdmin);
    pantalla = fixture.nativeElement;
    fixture.detectChanges();
  });

  it('solo muestra secciones del panel, nada de la tienda', () => {
    const rutas = Array.from(pantalla.querySelectorAll('a')).map((a) => a.getAttribute('href'));
    expect(rutas.length).toBeGreaterThan(5);
    expect(rutas.every((ruta) => ruta?.startsWith('/admin'))).toBeTrue();
    expect(pantalla.textContent).not.toContain('Ofertas');
    expect(pantalla.querySelector('[aria-label^="Carrito"]')).toBeNull();
  });

  it('muestra el usuario de la sesión', () => {
    expect(pantalla.querySelector('.usuario')?.textContent).toContain('admin');
  });

  it('al cerrar sesión vuelve al login del panel', () => {
    const navegar = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    const boton = Array.from(pantalla.querySelectorAll('button')).find((b) => b.textContent?.includes('Cerrar sesión'));

    boton!.click();

    expect(logout).toHaveBeenCalled();
    expect(navegar).toHaveBeenCalledWith(['/admin/login']);
  });
});
