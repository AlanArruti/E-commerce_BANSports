import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { Cliente } from '../models/cliente';
import { TokenResponse } from '../models/token-response';
import { MSJ_SESION_INVALIDA } from '../validaciones/errores-api';
import { CLAVE_TOKEN, crearToken, guardarToken, limpiarSesion } from '../../testing/sesion-de-prueba';
import { AuthService, SoloAdminsError, SoloClientesError } from './auth.service';

const ANA: Cliente = {
  id: 7,
  uuidCliente: 'uuid-ana',
  nombre: 'Ana',
  apellido: 'García',
  dni: '30111222',
  email: 'ana@mail.com',
  direcciones: []
};

describe('AuthService', () => {
  let backend: HttpTestingController;

  /** Crea el servicio con una sesión guardada (o ninguna): el token se lee al construirlo. */
  function crearServicio(tokenGuardado: string | null = null): AuthService {
    limpiarSesion();
    if (tokenGuardado) guardarToken(tokenGuardado);
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    backend = TestBed.inject(HttpTestingController);
    return TestBed.inject(AuthService);
  }

  afterEach(() => {
    backend.verify();
    limpiarSesion();
  });

  // ── login ────────────────────────────────────────────

  describe('login', () => {
    it('guarda el token, confirma la sesión con el back y trae el nombre del cliente', async () => {
      const auth = crearServicio();
      const token = crearToken({ sub: ANA.email, id: ANA.id });

      const respuesta = firstValueFrom(auth.login({ username: ANA.email, password: 'secreta' }));
      const pedido = backend.expectOne('/api/v1/auth/login');
      expect(pedido.request.method).toBe('POST');
      pedido.flush({ token, tipo: 'Bearer', rol: 'CLIENTE', id: ANA.id } satisfies TokenResponse);
      backend.expectOne(`/api/v1/cliente/${ANA.id}`).flush(ANA);

      expect((await respuesta).token).toBe(token);
      expect(localStorage.getItem(CLAVE_TOKEN)).toBe(token);
      expect(auth.estaLogueado()).toBeTrue();
      expect(auth.esCliente()).toBeTrue();
      expect(auth.cliente()?.nombre).toBe('Ana');
    });

    it('sin "recordarme" guarda el token solo para esta pestaña', async () => {
      const auth = crearServicio();
      const token = crearToken({ sub: ANA.email, id: ANA.id });

      const respuesta = firstValueFrom(auth.login({ username: ANA.email, password: 'secreta' }, false));
      backend.expectOne('/api/v1/auth/login').flush({ token, tipo: 'Bearer', rol: 'CLIENTE', id: ANA.id });
      backend.expectOne(`/api/v1/cliente/${ANA.id}`).flush(ANA);
      await respuesta;

      expect(sessionStorage.getItem(CLAVE_TOKEN)).toBe(token);
      expect(localStorage.getItem(CLAVE_TOKEN)).toBeNull();
    });

    it('un admin en el login de clientes es rechazado y no queda sesión', async () => {
      const auth = crearServicio();
      const token = crearToken({ rol: 'ADMIN', sub: 'admin' });

      const respuesta = firstValueFrom(auth.login({ username: 'admin', password: 'x' }));
      backend.expectOne('/api/v1/auth/login').flush({ token, tipo: 'Bearer', rol: 'ADMIN', id: 1 });

      await expectAsync(respuesta).toBeRejectedWithError(SoloClientesError);
      expect(auth.estaLogueado()).toBeFalse();
      expect(localStorage.getItem(CLAVE_TOKEN)).toBeNull();
    });

    it('un cliente en el login del panel es rechazado', async () => {
      const auth = crearServicio();

      const respuesta = firstValueFrom(auth.login({ username: ANA.email, password: 'x' }, true, 'ADMIN'));
      backend
        .expectOne('/api/v1/auth/login')
        .flush({ token: crearToken({ sub: ANA.email }), tipo: 'Bearer', rol: 'CLIENTE', id: ANA.id });

      await expectAsync(respuesta).toBeRejectedWithError(SoloAdminsError);
      expect(auth.estaLogueado()).toBeFalse();
    });

    it('no acepta una respuesta cuyo rol no coincide con el del token', async () => {
      const auth = crearServicio();

      const respuesta = firstValueFrom(auth.login({ username: 'x', password: 'x' }));
      // El back dice CLIENTE pero el token es de ADMIN: algo no cierra, no se guarda nada.
      backend.expectOne('/api/v1/auth/login').flush({ token: crearToken({ rol: 'ADMIN' }), tipo: 'Bearer', rol: 'CLIENTE', id: 1 });

      await expectAsync(respuesta).toBeRejected();
      expect(auth.estaLogueado()).toBeFalse();
    });
  });

  // ── logout ───────────────────────────────────────────

  describe('logout', () => {
    it('borra el token de los dos almacenamientos y los datos del cliente', () => {
      const auth = crearServicio(crearToken());
      sessionStorage.setItem(CLAVE_TOKEN, 'otro');
      auth.cliente.set(ANA);

      auth.logout();

      expect(localStorage.getItem(CLAVE_TOKEN)).toBeNull();
      expect(sessionStorage.getItem(CLAVE_TOKEN)).toBeNull();
      expect(auth.estaLogueado()).toBeFalse();
      expect(auth.cliente()).toBeNull();
      expect(auth.avisoSesion()).toBeNull();
    });

    it('si se cerró por sesión inválida, deja el aviso para el login', () => {
      const auth = crearServicio(crearToken());

      auth.logout(true);

      expect(auth.avisoSesion()).toBe(MSJ_SESION_INVALIDA);
    });
  });

  // ── validarSesion ────────────────────────────────────

  describe('validarSesion', () => {
    it('sin token guardado es false y no llama al back', async () => {
      const auth = crearServicio();

      expect(await firstValueFrom(auth.validarSesion())).toBeFalse();
    });

    it('con un token vencido cierra la sesión sin llamar al back', async () => {
      const auth = crearServicio(crearToken({ venceEn: -60 }));

      expect(await firstValueFrom(auth.validarSesion())).toBeFalse();
      expect(auth.estaLogueado()).toBeFalse();
      expect(auth.avisoSesion()).toBe(MSJ_SESION_INVALIDA);
    });

    it('con un token que no es un JWT es false', async () => {
      const auth = crearServicio('esto-no-es-un-jwt');

      expect(await firstValueFrom(auth.validarSesion())).toBeFalse();
      expect(auth.usuario()).toBeNull();
    });

    it('confirma al cliente con su ficha y no vuelve a preguntar por el mismo token', async () => {
      const auth = crearServicio(crearToken({ sub: ANA.email, id: ANA.id }));

      const primera = firstValueFrom(auth.validarSesion());
      backend.expectOne(`/api/v1/cliente/${ANA.id}`).flush(ANA);
      expect(await primera).toBeTrue();

      // Segunda vez: ya está confirmado, no sale ningún pedido (backend.verify lo controla).
      expect(await firstValueFrom(auth.validarSesion())).toBeTrue();
    });

    it('si la ficha es de otro email (token editado a mano) cierra la sesión', async () => {
      const auth = crearServicio(crearToken({ sub: 'otra@mail.com', id: ANA.id }));

      const resultado = firstValueFrom(auth.validarSesion());
      backend.expectOne(`/api/v1/cliente/${ANA.id}`).flush(ANA);

      expect(await resultado).toBeFalse();
      expect(auth.estaLogueado()).toBeFalse();
    });

    it('confirma al admin con /auth/me', async () => {
      const auth = crearServicio(crearToken({ rol: 'ADMIN', sub: 'admin', id: 1 }));

      const resultado = firstValueFrom(auth.validarSesion());
      backend.expectOne('/api/v1/auth/me').flush({ id: 1, username: 'admin', rol: 'ADMIN' });

      expect(await resultado).toBeTrue();
      expect(auth.esAdmin()).toBeTrue();
    });

    it('con un 401 del back cierra la sesión', async () => {
      const auth = crearServicio(crearToken({ id: ANA.id, sub: ANA.email }));

      const resultado = firstValueFrom(auth.validarSesion());
      backend.expectOne(`/api/v1/cliente/${ANA.id}`).flush(null, { status: 401, statusText: 'Unauthorized' });

      expect(await resultado).toBeFalse();
      expect(auth.estaLogueado()).toBeFalse();
      expect(localStorage.getItem(CLAVE_TOKEN)).toBeNull();
    });

    it('sin conexión no puede confirmar, pero tampoco borra la sesión', async () => {
      const token = crearToken({ id: ANA.id, sub: ANA.email });
      const auth = crearServicio(token);

      const resultado = firstValueFrom(auth.validarSesion());
      backend.expectOne(`/api/v1/cliente/${ANA.id}`).error(new ProgressEvent('error'), { status: 0 });

      expect(await resultado).toBeFalse();
      expect(auth.estaLogueado()).toBeTrue();
      expect(localStorage.getItem(CLAVE_TOKEN)).toBe(token);
    });

    it('si cambiaron el token guardado, lo vuelve a confirmar', async () => {
      const auth = crearServicio(crearToken({ id: ANA.id, sub: ANA.email }));
      const primera = firstValueFrom(auth.validarSesion());
      backend.expectOne(`/api/v1/cliente/${ANA.id}`).flush(ANA);
      await primera;

      // Alguien pega otro token desde las herramientas del navegador.
      guardarToken(crearToken({ rol: 'ADMIN', sub: 'admin', id: 1 }));
      const segunda = firstValueFrom(auth.validarSesion());
      backend.expectOne('/api/v1/auth/me').flush(null, { status: 401, statusText: 'Unauthorized' });

      expect(await segunda).toBeFalse();
      expect(auth.esAdmin()).toBeFalse();
    });
  });
});
