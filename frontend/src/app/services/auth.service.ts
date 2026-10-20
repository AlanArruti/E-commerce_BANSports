import { Injectable } from '@angular/core';
import { LoginRequest } from '../models/login-request';
import { RegistroRequest } from '../models/registro-request';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly apiUrl = 'http://localhost:8080/api/v1/auth';

  login(credenciales: LoginRequest): void {
    // TODO: POST a `${this.apiUrl}/login` y guardar el JWT (TokenResponse)
  }

  registrar(cliente: RegistroRequest): void {
    // TODO: POST a `${this.apiUrl}/register`
  }

  logout(): void {
    // TODO: eliminar el JWT guardado
  }
}
