import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { MSJ_SESION_INVALIDA } from '../validaciones/errores-api';
import { CLAVE_TOKEN, crearToken, guardarToken, limpiarSesion } from '../../testing/sesion-de-prueba';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let router: Router;

  /** Arma el entorno con una sesión guardada (o ninguna) antes de crear AuthService. */
  function configurar(token: string | null): void {
    limpiarSesion();
    if (token) guardarToken(token);
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting()
      ]
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
  }

  afterEach(() => {
    backend.verify();
    limpiarSesion();
  });

  it('agrega el token a los pedidos a nuestra API', () => {
    const token = crearToken();
    configurar(token);

    http.get('/api/v1/carrito').subscribe();

    expect(backend.expectOne('/api/v1/carrito').request.headers.get('Authorization')).toBe(`Bearer ${token}`);
  });

  it('no manda el token al login ni al registro', () => {
    configurar(crearToken());

    http.post('/api/v1/auth/login', {}).subscribe();
    http.post('/api/v1/auth/register', {}).subscribe();

    expect(backend.expectOne('/api/v1/auth/login').request.headers.has('Authorization')).toBeFalse();
    expect(backend.expectOne('/api/v1/auth/register').request.headers.has('Authorization')).toBeFalse();
  });

  it('no manda el token a otros sitios', () => {
    configurar(crearToken());

    http.get('https://apis.datos.gob.ar/georef/api/localidades').subscribe();

    expect(backend.expectOne(() => true).request.headers.has('Authorization')).toBeFalse();
  });

  it('sin sesión, el pedido sale sin token', () => {
    configurar(null);

    http.get('/api/v1/producto').subscribe();

    expect(backend.expectOne('/api/v1/producto').request.headers.has('Authorization')).toBeFalse();
  });

  it('con un 401 cierra la sesión y manda al login de clientes', () => {
    configurar(crearToken({ rol: 'CLIENTE' }));
    const auth = TestBed.inject(AuthService);
    let error: unknown;

    http.get('/api/v1/carrito').subscribe({ error: (e) => (error = e) });
    backend.expectOne('/api/v1/carrito').flush({ message: 'Token inválido' }, { status: 401, statusText: 'Unauthorized' });

    expect(auth.estaLogueado()).toBeFalse();
    expect(localStorage.getItem(CLAVE_TOKEN)).toBeNull();
    expect(auth.avisoSesion()).toBe(MSJ_SESION_INVALIDA);
    expect(router.navigate).toHaveBeenCalledWith(['/login'], jasmine.objectContaining({ queryParams: jasmine.any(Object) }));
    expect(error).toBeDefined(); // el error sigue hasta la pantalla
  });

  it('con un 401 del admin lo manda al login del panel', () => {
    configurar(crearToken({ rol: 'ADMIN', sub: 'admin' }));

    http.get('/api/v1/admin/resumen').subscribe({ error: () => undefined });
    backend.expectOne('/api/v1/admin/resumen').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(router.navigate).toHaveBeenCalledWith(['/admin/login'], jasmine.any(Object));
  });

  it('un 401 en el login no cierra nada (son credenciales incorrectas)', () => {
    configurar(crearToken());

    http.post('/api/v1/auth/login', {}).subscribe({ error: () => undefined });
    backend.expectOne('/api/v1/auth/login').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(TestBed.inject(AuthService).estaLogueado()).toBeTrue();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('"contraseña actual incorrecta" al cambiarla no cierra la sesión', () => {
    configurar(crearToken({ id: 4 }));

    http.patch('/api/v1/cliente/4/password', {}).subscribe({ error: () => undefined });
    backend
      .expectOne('/api/v1/cliente/4/password')
      .flush({ message: 'La contraseña actual es incorrecta.' }, { status: 401, statusText: 'Unauthorized' });

    expect(TestBed.inject(AuthService).estaLogueado()).toBeTrue();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('otros errores no tocan la sesión', () => {
    configurar(crearToken());

    http.get('/api/v1/carrito').subscribe({ error: () => undefined });
    backend.expectOne('/api/v1/carrito').flush(null, { status: 500, statusText: 'Server Error' });

    expect(TestBed.inject(AuthService).estaLogueado()).toBeTrue();
  });
});
