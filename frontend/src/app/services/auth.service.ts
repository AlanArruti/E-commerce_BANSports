import { Injectable, computed, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, map, of, switchMap, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { Cliente } from '../models/cliente';
import { JwtPayload } from '../models/jwt-payload';
import { LoginRequest } from '../models/login-request';
import { RegistroRequest } from '../models/registro-request';
import { TokenResponse } from '../models/token-response';
import { MSJ_SESION_INVALIDA } from '../validaciones/errores-api';

/** Error que se lanza cuando alguien que no es cliente usa el login de clientes. */
export class SoloClientesError extends Error {
  constructor() {
    super('SOLO_CLIENTES');
  }
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = environment.apiUrl;
  private readonly CLAVE_TOKEN = 'ban_token';

  /** Token actual en memoria. Se carga desde localStorage/sessionStorage al iniciar. */
  private readonly token = signal<string | null>(null);

  /** Datos del token (rol, id, vencimiento). Siempre salen del token, nunca de otra clave editable. */
  readonly usuario = computed<JwtPayload | null>(() => this.decodificar(this.token()));
  readonly estaLogueado = computed(() => this.usuario() !== null);
  readonly esCliente = computed(() => this.usuario()?.rol === 'CLIENTE');

  /** Datos del cliente que confirmó el back. Se usan en el header. */
  readonly cliente = signal<Cliente | null>(null);

  /** Mensaje para mostrar en el login cuando se cerró la sesión por token inválido o vencido. */
  readonly avisoSesion = signal<string | null>(null);

  /** Último token que el back confirmó como válido (para no consultarlo en cada navegación). */
  private tokenValidado: string | null = null;

  constructor(private http: HttpClient) {
    this.token.set(this.leerTokenGuardado());
  }

  getToken(): string | null {
    return this.token();
  }

  /**
   * @param recordar true: el token queda guardado al cerrar el navegador (localStorage).
   *                 false: se borra al cerrar la pestaña (sessionStorage).
   */
  login(credenciales: LoginRequest, recordar = true): Observable<TokenResponse> {
    return this.http.post<TokenResponse>(`${this.API_URL}/auth/login`, credenciales).pipe(
      tap((respuesta) => {
        // Por ahora esta app es solo para clientes; el admin tendrá su propio acceso.
        if (respuesta.rol !== 'CLIENTE' || this.decodificar(respuesta.token)?.rol !== 'CLIENTE') {
          throw new SoloClientesError();
        }
        this.guardarSesion(respuesta.token, recordar);
      }),
      // Confirma el token con el back y trae el nombre del cliente para el header.
      switchMap((respuesta) => this.validarSesion().pipe(map(() => respuesta)))
    );
  }

  /** Crea la cuenta del cliente. No inicia sesión: eso lo hace la página con login(). */
  registrar(datos: RegistroRequest): Observable<Cliente> {
    return this.http.post<Cliente>(`${this.API_URL}/auth/register`, datos);
  }

  /**
   * Cierra la sesión y borra el token.
   * @param porSesionInvalida si es true, el login muestra un aviso explicando por qué se cerró.
   */
  logout(porSesionInvalida = false): void {
    this.borrarTokenGuardado();
    this.token.set(null);
    this.cliente.set(null);
    this.tokenValidado = null;
    this.avisoSesion.set(porSesionInvalida ? MSJ_SESION_INVALIDA : null);
  }

  /**
   * Confirma con el back que el token es válido.
   * El front no puede verificar la firma del JWT (la clave está en el servidor),
   * así que le pregunta al back con un endpoint protegido. Si alguien editó el token
   * en el navegador, el back responde 401 y se cierra la sesión.
   */
  validarSesion(): Observable<boolean> {
    // Si el token guardado cambió (por ejemplo, editado desde las herramientas del navegador),
    // se toma el guardado para validarlo de nuevo.
    const guardado = this.leerTokenGuardado();
    if (guardado !== this.token()) {
      this.token.set(guardado);
      this.tokenValidado = null;
    }

    const token = this.token();
    if (!token) {
      return of(false);
    }

    const payload = this.usuario();
    if (!payload || this.estaVencido(payload)) {
      this.logout(true);
      return of(false);
    }

    if (this.tokenValidado === token) {
      return of(true);
    }

    if (payload.rol !== 'CLIENTE') {
      // Esta app todavía no maneja sesiones de admin.
      this.logout(true);
      return of(false);
    }

    return this.http.get<Cliente>(`${this.API_URL}/cliente/${payload.id}`).pipe(
      map((cliente) => {
        if (cliente.email !== payload.sub) {
          throw new Error('El token no corresponde al cliente.');
        }
        this.cliente.set(cliente);
        this.tokenValidado = token;
        return true;
      }),
      catchError((error: unknown) => {
        const status = error instanceof HttpErrorResponse ? error.status : -1;
        // Sin conexión o error del servidor: no se puede confirmar, pero tampoco se borra la sesión.
        if (status === 0 || status >= 500) {
          return of(false);
        }
        this.logout(true);
        return of(false);
      })
    );
  }

  private guardarSesion(token: string, recordar: boolean): void {
    this.borrarTokenGuardado();
    (recordar ? localStorage : sessionStorage).setItem(this.CLAVE_TOKEN, token);
    this.token.set(token);
    this.tokenValidado = null;
    this.avisoSesion.set(null);
  }

  private leerTokenGuardado(): string | null {
    try {
      return localStorage.getItem(this.CLAVE_TOKEN) ?? sessionStorage.getItem(this.CLAVE_TOKEN);
    } catch {
      return null;
    }
  }

  private borrarTokenGuardado(): void {
    try {
      localStorage.removeItem(this.CLAVE_TOKEN);
      sessionStorage.removeItem(this.CLAVE_TOKEN);
    } catch {
      // El navegador bloquea el almacenamiento: no hay nada que borrar.
    }
  }

  private estaVencido(payload: JwtPayload): boolean {
    return payload.exp * 1000 <= Date.now();
  }

  /** Lee el contenido del JWT (no verifica la firma: eso lo hace el back). */
  private decodificar(token: string | null): JwtPayload | null {
    if (!token) {
      return null;
    }
    const partes = token.split('.');
    if (partes.length !== 3) {
      return null;
    }
    try {
      const base64 = partes[1].replace(/-/g, '+').replace(/_/g, '/');
      const relleno = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
      const json = decodeURIComponent(
        atob(relleno)
          .split('')
          .map((c) => '%' + c.charCodeAt(0).toString(16).padStart(2, '0'))
          .join('')
      );
      const datos = JSON.parse(json) as Partial<JwtPayload>;
      const valido =
        typeof datos.sub === 'string' &&
        (datos.rol === 'CLIENTE' || datos.rol === 'ADMIN') &&
        typeof datos.id === 'number' &&
        typeof datos.exp === 'number';
      return valido ? (datos as JwtPayload) : null;
    } catch {
      return null;
    }
  }
}

