import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { RegistroRequest } from '../../models/registro-request';

@Component({
  selector: 'app-registro',
  imports: [FormsModule, RouterLink],
  templateUrl: './registro.html'
})
export class Registro {
  protected cliente: RegistroRequest = {
    nombre: '',
    apellido: '',
    dni: '',
    email: '',
    contrasenia: ''
  };

  protected onSubmit(): void {
    // TODO: conectar con AuthService cuando se vea como manejar el JWT
  }
}
