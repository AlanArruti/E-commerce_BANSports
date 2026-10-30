import { Component, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { PanelAuth } from '../../componente/panel-auth/panel-auth';
import { AuthService, SoloAdminsError } from '../../services/auth.service';
import { ErrorTraducido, MSJ_SOLO_ADMINS, traducirErrorLogin } from '../../validaciones/errores-api';
import { mensajeDeError } from '../../validaciones/mensajes';
import { requerido } from '../../validaciones/validadores';

/** Login del panel de administración. Solo acepta cuentas con rol ADMIN. */
@Component({
  selector: 'app-admin-login',
  imports: [ReactiveFormsModule, RouterLink, NgClass, PanelAuth],
  templateUrl: './admin-login.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './admin-login.css'
})
export class AdminLogin implements OnInit {
  loginForm: FormGroup;
  usuario: FormControl<string>;
  contrasenia: FormControl<string>;

  protected readonly mostrarContrasenia = signal(false);
  protected readonly cargando = signal(false);
  protected readonly intentoEnviar = signal(false);
  protected readonly errorServidor = signal<ErrorTraducido | null>(null);
  protected readonly aviso = signal<string | null>(null);

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.usuario = new FormControl('', { nonNullable: true, validators: [requerido] });
    this.contrasenia = new FormControl('', { nonNullable: true, validators: [requerido] });
    this.loginForm = new FormGroup({ usuario: this.usuario, contrasenia: this.contrasenia });
  }

  ngOnInit(): void {
    // Si llegó acá porque la sesión se cerró (token vencido o alterado), se le explica por qué.
    this.aviso.set(this.authService.avisoSesion());
    this.authService.avisoSesion.set(null);
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

    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.cargando.set(true);
    const credenciales = { username: this.usuario.value.trim(), password: this.contrasenia.value };

    // La sesión del panel no se recuerda al cerrar el navegador (sessionStorage).
    this.authService.login(credenciales, false, 'ADMIN').subscribe({
      next: () => {
        this.cargando.set(false);
        this.router.navigateByUrl(this.destinoSeguro());
      },
      error: (e: unknown) => {
        this.cargando.set(false);
        if (e instanceof SoloAdminsError) {
          this.errorServidor.set(MSJ_SOLO_ADMINS);
        } else if (e instanceof HttpErrorResponse) {
          this.errorServidor.set(traducirErrorLogin(e, 'usuario'));
        } else {
          this.errorServidor.set({
            titulo: 'No pudimos iniciar sesión',
            detalle: 'Ocurrió un error inesperado. Intentá de nuevo.'
          });
        }
      }
    });
  }

  /** Solo redirige a rutas internas del panel, para evitar que un link lleve a otro sitio. */
  private destinoSeguro(): string {
    const redirect = this.route.snapshot.queryParamMap.get('redirect');
    return redirect && redirect.startsWith('/admin') ? redirect : '/admin';
  }
}
