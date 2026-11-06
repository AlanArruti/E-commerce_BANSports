import { ChangeDetectionStrategy, Component, OnInit, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { PanelAuth } from '../../componente/panel-auth/panel-auth';
import { AuthService } from '../../services/auth.service';
import { ErrorTraducido, traducirErrorRecuperacion } from '../../validaciones/errores-api';
import { mensajeDeError } from '../../validaciones/mensajes';
import { coincideCon, requerido } from '../../validaciones/validadores';

/** Paso 2 de "¿Olvidaste tu contraseña?": la página del enlace del mail, para elegir la contraseña nueva. */
@Component({
  selector: 'app-restablecer-contrasena',
  imports: [ReactiveFormsModule, RouterLink, NgClass, PanelAuth],
  templateUrl: './restablecer-contrasena.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: '../recuperar-contrasena/recuperar-contrasena.css'
})
export class RestablecerContrasena implements OnInit {
  protected readonly contraseniaNueva = new FormControl('', {
    nonNullable: true,
    validators: [requerido, Validators.minLength(6), Validators.maxLength(50)]
  });
  protected readonly repetirContrasenia = new FormControl('', {
    nonNullable: true,
    validators: [requerido, coincideCon('contraseniaNueva')]
  });
  protected readonly formulario = new FormGroup({
    contraseniaNueva: this.contraseniaNueva,
    repetirContrasenia: this.repetirContrasenia
  });

  protected readonly mostrarContrasenia = signal(false);
  protected readonly cargando = signal(false);
  protected readonly intentoEnviar = signal(false);
  protected readonly errorServidor = signal<ErrorTraducido | null>(null);
  protected readonly listo = signal(false);
  /** El enlace llegó sin token (link cortado o copiado a medias). */
  protected readonly sinToken = signal(false);

  private token = '';

  constructor(
    private authService: AuthService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    // Si cambia la primera, la repetición se vuelve a comparar.
    this.contraseniaNueva.valueChanges.subscribe(() => this.repetirContrasenia.updateValueAndValidity());
  }

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token')?.trim() ?? '';
    this.sinToken.set(this.token === '');
    if (this.token) {
      // Saca el token de la barra de direcciones y del historial del navegador.
      this.router.navigate([], { relativeTo: this.route, queryParams: {}, replaceUrl: true });
    }
  }

  error(control: FormControl, campo: string): string | null {
    if (!control.touched && !this.intentoEnviar()) {
      return null;
    }
    return mensajeDeError(control, campo);
  }

  alternarContrasenia(): void {
    this.mostrarContrasenia.update((valor) => !valor);
  }

  guardar(): void {
    this.intentoEnviar.set(true);
    this.errorServidor.set(null);
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    this.cargando.set(true);
    this.authService.restablecerContrasenia(this.token, this.contraseniaNueva.value).subscribe({
      next: () => {
        this.cargando.set(false);
        this.listo.set(true);
      },
      error: (e: HttpErrorResponse) => {
        this.cargando.set(false);
        this.errorServidor.set(traducirErrorRecuperacion(e));
      }
    });
  }
}
