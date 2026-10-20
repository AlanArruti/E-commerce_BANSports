import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { LoginRequest } from '../../models/login-request';

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink],
  templateUrl: './login.html'
})
export class Login {
  protected credenciales: LoginRequest = {
    username: '',
    password: ''
  };

  protected onSubmit(): void {
    // TODO: conectar con AuthService cuando se vea como manejar el JWT
  }
}
