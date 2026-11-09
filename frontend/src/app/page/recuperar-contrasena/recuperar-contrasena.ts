import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { PanelAuth } from '../../componente/panel-auth/panel-auth';
import { AuthService } from '../../services/auth.service';
import { ErrorTraducido, traducirErrorRecuperacion } from '../../validaciones/errores-api';
import { mensajeDeError } from '../../validaciones/mensajes';
import { emailValido, requerido } from '../../validaciones/validadores';

/** Paso 1 de "¿Olvidaste tu contraseña?": pedir el mail con el enlace. */
@Component({
  selector: 'app-recuperar-contrasena',
  imports: [ReactiveFormsModule, RouterLink, NgClass, PanelAuth],
  templateUrl: './recuperar-contrasena.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './recuperar-contrasena.css'
})
export class RecuperarContrasena {
  protected readonly pasos = [
    'Te mandamos un enlace a tu email',
    'Elegís una contraseña nueva',
    'El enlace vence en 30 minutos'
  ];

  protected readonly email = new FormControl('', {
    nonNullable: true,
    validators: [requerido, emailValido, Validators.maxLength(100)]
  });
  protected readonly formulario = new FormGroup({ email: this.email });

  protected readonly cargando = signal(false);
  protected readonly intentoEnviar = signal(false);
  protected readonly errorServidor = signal<ErrorTraducido | null>(null);
  /** Email al que se mandó el enlace. Si está, se muestra la confirmación en lugar del formulario. */
  protected readonly enviadoA = signal<string | null>(null);

  constructor(private authService: AuthService) {}

  error(): string | null {
    if (!this.email.touched && !this.intentoEnviar()) {
      return null;
    }
    return mensajeDeError(this.email, 'loginEmail');
  }

  enviar(): void {
    this.intentoEnviar.set(true);
    this.errorServidor.set(null);
    if (this.email.invalid) {
      this.email.markAsTouched();
      return;
    }

    const email = this.email.value.trim();
    this.cargando.set(true);
    this.authService.solicitarRecuperacion(email).subscribe({
      next: () => {
        this.cargando.set(false);
        this.enviadoA.set(email);
      },
      error: (e: HttpErrorResponse) => {
        this.cargando.set(false);
        this.errorServidor.set(traducirErrorRecuperacion(e));
      }
    });
  }

  /** Vuelve al formulario para corregir el email o pedir otro enlace. */
  otroEmail(): void {
    this.enviadoA.set(null);
    this.intentoEnviar.set(false);
  }
}
