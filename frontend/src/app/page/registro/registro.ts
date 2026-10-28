import { Component, computed, signal, ChangeDetectionStrategy } from '@angular/core';
import { NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { PanelAuth } from '../../componente/panel-auth/panel-auth';
import { RegistroRequest } from '../../models/registro-request';
import { AuthService } from '../../services/auth.service';
import { ErrorTraducido, traducirErrorRegistro } from '../../validaciones/errores-api';
import { mensajeDeError } from '../../validaciones/mensajes';
import { emailValido, largoDni, requerido, soloLetras, soloNumeros } from '../../validaciones/validadores';

interface Requisito {
  texto: string;
  cumple: boolean;
  obligatorio: boolean;
}

@Component({
  selector: 'app-registro',
  imports: [ReactiveFormsModule, RouterLink, NgClass, PanelAuth],
  templateUrl: './registro.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './registro.css'
})
export class Registro {
  registroForm: FormGroup;
  nombre: FormControl<string>;
  apellido: FormControl<string>;
  dni: FormControl<string>;
  email: FormControl<string>;
  contrasenia: FormControl<string>;
  terminos: FormControl<boolean>;

  protected readonly mostrarContrasenia = signal(false);
  protected readonly cargando = signal(false);
  protected readonly intentoEnviar = signal(false);
  protected readonly errorServidor = signal<ErrorTraducido | null>(null);

  /** Valor de la contraseña como signal, para calcular los requisitos en vivo. */
  private readonly valorContrasenia;
  protected readonly requisitos;
  protected readonly fuerza;
  protected readonly etiquetaFuerza;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {
    // Mismas reglas que valida el back (ClienteRequest), con mensajes propios para cada una.
    this.nombre = new FormControl('', {
      nonNullable: true,
      validators: [requerido, soloLetras, Validators.maxLength(50)]
    });
    this.apellido = new FormControl('', {
      nonNullable: true,
      validators: [requerido, soloLetras, Validators.maxLength(50)]
    });
    this.dni = new FormControl('', {
      nonNullable: true,
      validators: [requerido, soloNumeros, largoDni]
    });
    this.email = new FormControl('', {
      nonNullable: true,
      validators: [requerido, emailValido, Validators.maxLength(100)]
    });
    this.contrasenia = new FormControl('', {
      nonNullable: true,
      validators: [requerido, Validators.minLength(6), Validators.maxLength(50)]
    });
    this.terminos = new FormControl(false, { nonNullable: true, validators: [Validators.requiredTrue] });

    this.registroForm = new FormGroup({
      nombre: this.nombre,
      apellido: this.apellido,
      dni: this.dni,
      email: this.email,
      contrasenia: this.contrasenia,
      terminos: this.terminos
    });

    this.valorContrasenia = toSignal(this.contrasenia.valueChanges, { initialValue: '' });

    this.requisitos = computed<Requisito[]>(() => {
      const valor = this.valorContrasenia();
      return [
        { texto: 'Entre 6 y 50 caracteres', cumple: valor.length >= 6 && valor.length <= 50, obligatorio: true },
        { texto: 'Mayúsculas, minúsculas y números', cumple: /[A-Z]/.test(valor) && /[a-z]/.test(valor) && /[0-9]/.test(valor), obligatorio: false },
        { texto: 'Al menos un símbolo (por ejemplo ! @ # $)', cumple: /[^A-Za-z0-9]/.test(valor), obligatorio: false }
      ];
    });

    this.fuerza = computed(() => {
      const valor = this.valorContrasenia();
      let puntos = 0;
      if (valor.length >= 6) puntos++;
      if (/[0-9]/.test(valor)) puntos++;
      if (/[A-Z]/.test(valor) && /[a-z]/.test(valor)) puntos++;
      if (valor.length >= 10 && /[^A-Za-z0-9]/.test(valor)) puntos++;
      return puntos;
    });

    this.etiquetaFuerza = computed(() => ['Muy débil', 'Débil', 'Media', 'Buena', 'Fuerte'][this.fuerza()]);
  }

  /** Mensaje de error del campo, solo si el usuario ya lo tocó o intentó enviar. */
  error(control: FormControl, campo: string): string | null {
    if (!control.touched && !this.intentoEnviar()) {
      return null;
    }
    return mensajeDeError(control, campo);
  }

  /** Campo completo y sin errores: se marca en verde. */
  correcto(control: FormControl): boolean {
    return control.touched && control.valid && control.value !== '';
  }

  /** Cantidad de campos con error, para el resumen arriba del botón. */
  camposConError(): number {
    return Object.values(this.registroForm.controls).filter((control) => control.invalid).length;
  }

  alternarContrasenia(): void {
    this.mostrarContrasenia.update((valor) => !valor);
  }

  registrarse(): void {
    this.intentoEnviar.set(true);
    this.errorServidor.set(null);

    if (this.registroForm.invalid) {
      this.registroForm.markAllAsTouched();
      return;
    }

    const datos: RegistroRequest = {
      nombre: this.nombre.value.trim(),
      apellido: this.apellido.value.trim(),
      dni: this.dni.value.trim(),
      email: this.email.value.trim(),
      contrasenia: this.contrasenia.value
    };

    this.cargando.set(true);
    this.authService.registrar(datos).subscribe({
      next: () => this.iniciarSesionNueva(datos),
      error: (e: unknown) => {
        this.cargando.set(false);
        if (e instanceof HttpErrorResponse) {
          const traducido = traducirErrorRegistro(e);
          if (traducido.campo) {
            // El error queda pegado al campo hasta que el usuario lo cambie.
            const control = traducido.campo === 'email' ? this.email : this.dni;
            control.setErrors({ servidor: traducido.detalle });
            control.markAsTouched();
          } else {
            this.errorServidor.set(traducido);
          }
          return;
        }
        this.errorServidor.set({
          titulo: 'No pudimos crear tu cuenta',
          detalle: 'Ocurrió un error inesperado. Intentá de nuevo.'
        });
      }
    });
  }

  /** La cuenta ya se creó: inicia sesión con los mismos datos. */
  private iniciarSesionNueva(datos: RegistroRequest): void {
    this.authService.login({ username: datos.email, password: datos.contrasenia }).subscribe({
      next: () => {
        this.cargando.set(false);
        this.router.navigate(['/']);
      },
      error: () => {
        // La cuenta existe, pero no se pudo entrar automáticamente: que lo haga desde el login.
        this.cargando.set(false);
        this.authService.avisoSesion.set(null);
        this.router.navigate(['/login'], { queryParams: { cuentaCreada: 1 } });
      }
    });
  }
}
