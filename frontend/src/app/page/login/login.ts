import { Component, OnInit, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { PanelAuth } from '../../componente/panel-auth/panel-auth';
import { AuthService, SoloClientesError } from '../../services/auth.service';
import {
  ErrorTraducido,
  MSJ_SOLO_CLIENTES,
  traducirErrorLogin
} from '../../validaciones/errores-api';
import { mensajeDeError } from '../../validaciones/mensajes';
import { emailValido, requerido } from '../../validaciones/validadores';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, NgClass, PanelAuth],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login implements OnInit {
  loginForm: FormGroup;
  email: FormControl<string>;
  contrasenia: FormControl<string>;
  recordar: FormControl<boolean>;

  protected readonly beneficios = [
    'Seguí el estado de tus pedidos',
    'Guardá tus direcciones de envío',
    'Comprá más rápido la próxima vez'
  ];

  protected readonly mostrarContrasenia = signal(false);
  protected readonly cargando = signal(false);
  protected readonly intentoEnviar = signal(false);
  protected readonly errorServidor = signal<ErrorTraducido | null>(null);
  protected readonly aviso = signal<string | null>(null);
  protected readonly cuentaCreada = signal(false);

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.email = new FormControl('', { nonNullable: true, validators: [requerido, emailValido] });
    this.contrasenia = new FormControl('', { nonNullable: true, validators: [requerido] });
    this.recordar = new FormControl(true, { nonNullable: true });

    this.loginForm = new FormGroup({
      email: this.email,
      contrasenia: this.contrasenia,
      recordar: this.recordar
    });
  }

  ngOnInit(): void {
    // Si llegó acá porque la sesión se cerró (token vencido o alterado), se le explica por qué.
    this.aviso.set(this.authService.avisoSesion());
    this.authService.avisoSesion.set(null);

    // Si viene del registro y no se pudo entrar automáticamente.
    this.cuentaCreada.set(this.route.snapshot.queryParamMap.has('cuentaCreada'));
  }

  /** Mensaje de error del campo, solo si el usuario ya lo tocó o intentó enviar. */
  error(control: FormControl, campo: string): string | null {
    if (!control.touched && !this.intentoEnviar()) {
      return null;
    }
    return mensajeDeError(control, campo);
  }

  alternarContrasenia(): void {
    this.mostrarContrasenia.update((valor) => !valor);
  }

  iniciarSesion(): void {
    this.intentoEnviar.set(true);
    this.errorServidor.set(null);
    this.aviso.set(null);
    this.cuentaCreada.set(false);

    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.cargando.set(true);
    const credenciales = {
      username: this.email.value.trim(),
      password: this.contrasenia.value
    };

    this.authService.login(credenciales, this.recordar.value).subscribe({
      next: () => {
        this.cargando.set(false);
        this.router.navigateByUrl(this.destinoSeguro());
      },
      error: (e: unknown) => {
        this.cargando.set(false);
        if (e instanceof SoloClientesError) {
          this.errorServidor.set(MSJ_SOLO_CLIENTES);
        } else if (e instanceof HttpErrorResponse) {
          this.errorServidor.set(traducirErrorLogin(e));
        } else {
          this.errorServidor.set({
            titulo: 'No pudimos iniciar sesión',
            detalle: 'Ocurrió un error inesperado. Intentá de nuevo.'
          });
        }
      }
    });
  }

  /** Solo redirige a rutas internas, para evitar que un link lleve a otro sitio. */
  private destinoSeguro(): string {
    const redirect = this.route.snapshot.queryParamMap.get('redirect');
    return redirect && redirect.startsWith('/') && !redirect.startsWith('//') ? redirect : '/';
  }
}
