import { ChangeDetectionStrategy, Component, ElementRef, OnInit, computed, effect, signal, viewChild } from '@angular/core';
import { NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  ADMIN_CONTRASENIA_MAX,
  ADMIN_CONTRASENIA_MIN,
  ADMIN_USUARIO_MAX,
  Administrador
} from '../../models/administrador';
import { AdminService } from '../../services/admin.service';
import { AuthService } from '../../services/auth.service';
import {
  ErrorTraducido,
  traducirErrorAdministradores,
  traducirErrorBajaAdministrador,
  traducirErrorCrearAdministrador
} from '../../validaciones/errores-api';
import { mensajeDeError } from '../../validaciones/mensajes';
import { coincideCon, requerido } from '../../validaciones/validadores';

type Modal = { tipo: 'alta' } | { tipo: 'baja'; admin: Administrador };

/**
 * Administradores del panel: lista, alta (mismas reglas que el back) y baja.
 * El administrador logueado no puede darse de baja a sí mismo.
 */
@Component({
  selector: 'app-admin-administradores',
  imports: [NgClass, ReactiveFormsModule, RouterLink],
  templateUrl: './admin-administradores.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './admin-administradores.css',
  host: { '(document:keydown.escape)': 'cerrarModal()' }
})
export class AdminAdministradores implements OnInit {
  protected readonly usuarioMax = ADMIN_USUARIO_MAX;
  protected readonly contraseniaMin = ADMIN_CONTRASENIA_MIN;

  protected readonly administradores = signal<Administrador[]>([]);
  protected readonly cargando = signal(true);
  protected readonly errorCarga = signal<ErrorTraducido | null>(null);
  protected readonly aviso = signal<string | null>(null);

  protected readonly modal = signal<Modal | null>(null);
  protected readonly enviando = signal(false);
  protected readonly errorModal = signal<ErrorTraducido | null>(null);
  protected readonly intentoGuardar = signal(false);
  protected readonly mostrarContrasenia = signal(false);

  /** Id del administrador logueado (del JWT). */
  protected readonly idPropio = computed(() => this.authService.usuario()?.id ?? null);

  /** Primero el propio, después el resto por usuario. */
  protected readonly ordenados = computed(() =>
    [...this.administradores()].sort((a, b) => {
      if (a.idAdministrador === this.idPropio()) return -1;
      if (b.idAdministrador === this.idPropio()) return 1;
      return a.username.localeCompare(b.username, 'es');
    })
  );

  readonly formulario = new FormGroup({
    usuario: new FormControl('', {
      nonNullable: true,
      validators: [requerido, Validators.maxLength(ADMIN_USUARIO_MAX)]
    }),
    contrasenia: new FormControl('', {
      nonNullable: true,
      validators: [requerido, Validators.minLength(ADMIN_CONTRASENIA_MIN), Validators.maxLength(ADMIN_CONTRASENIA_MAX)]
    }),
    repetirContrasenia: new FormControl('', {
      nonNullable: true,
      validators: [requerido, coincideCon('contrasenia')]
    })
  });

  private disparador: HTMLElement | null = null;
  private readonly campoUsuario = viewChild<ElementRef<HTMLInputElement>>('campoUsuario');
  private readonly cancelarBajaBtn = viewChild<ElementRef<HTMLButtonElement>>('cancelarBajaBtn');

  constructor(
    protected authService: AuthService,
    private adminService: AdminService
  ) {
    // "Repetir" se vuelve a validar cuando cambia la contraseña de arriba.
    this.formulario.controls.contrasenia.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.formulario.controls.repetirContrasenia.updateValueAndValidity({ emitEvent: false }));

    effect(() => this.campoUsuario()?.nativeElement.focus({ preventScroll: true }));
    effect(() => this.cancelarBajaBtn()?.nativeElement.focus({ preventScroll: true }));
  }

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.errorCarga.set(null);
    this.adminService.administradores().subscribe({
      next: (lista) => {
        this.administradores.set(lista);
        this.cargando.set(false);
      },
      error: (e: unknown) => {
        this.cargando.set(false);
        this.errorCarga.set(
          e instanceof HttpErrorResponse
            ? traducirErrorAdministradores(e)
            : { titulo: 'No pudimos cargar los administradores', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
        );
      }
    });
  }

  protected esPropio(admin: Administrador): boolean {
    return admin.idAdministrador === this.idPropio();
  }

  protected inicial(admin: Administrador): string {
    return admin.username.charAt(0).toUpperCase();
  }

  // ── Modales ───────────────────────────────────────────────

  abrirAlta(evento: Event): void {
    this.disparador = evento.currentTarget as HTMLElement;
    this.formulario.reset();
    this.intentoGuardar.set(false);
    this.mostrarContrasenia.set(false);
    this.errorModal.set(null);
    this.aviso.set(null);
    this.modal.set({ tipo: 'alta' });
  }

  pedirBaja(admin: Administrador, evento: Event): void {
    if (this.esPropio(admin)) {
      return;
    }
    this.disparador = evento.currentTarget as HTMLElement;
    this.errorModal.set(null);
    this.aviso.set(null);
    this.modal.set({ tipo: 'baja', admin });
  }

  cerrarModal(): void {
    if (this.modal() && !this.enviando()) {
      this.modal.set(null);
      this.disparador?.focus({ preventScroll: true });
      this.disparador = null;
    }
  }

  alternarContrasenia(): void {
    this.mostrarContrasenia.update((v) => !v);
  }

  protected error(campo: 'usuario' | 'contrasenia' | 'repetirContrasenia'): string | null {
    const control = this.formulario.controls[campo];
    if (!control.touched && !this.intentoGuardar()) {
      return null;
    }
    const mensajes = { usuario: 'adminUsuario', contrasenia: 'adminContrasenia', repetirContrasenia: 'adminRepetirContrasenia' };
    return mensajeDeError(control, mensajes[campo]);
  }

  // ── Alta ──────────────────────────────────────────────────

  crear(): void {
    this.intentoGuardar.set(true);
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      // El foco va al primer campo con error, después de que se pinten los mensajes.
      setTimeout(() => document.querySelector<HTMLElement>('.modal [aria-invalid="true"]')?.focus());
      return;
    }
    const username = this.formulario.controls.usuario.value.trim();
    this.enviando.set(true);
    this.errorModal.set(null);
    this.adminService.crearAdministrador({ username, contrasenia: this.formulario.controls.contrasenia.value }).subscribe({
      next: (admin) => {
        this.enviando.set(false);
        this.modal.set(null);
        this.disparador?.focus({ preventScroll: true });
        this.disparador = null;
        this.administradores.update((lista) => [...lista, admin]);
        this.aviso.set(`Creaste el administrador "${admin.username}". Ya puede ingresar desde el acceso de administradores.`);
      },
      error: (e: unknown) => {
        this.enviando.set(false);
        if (e instanceof HttpErrorResponse && e.status === 409) {
          // Usuario repetido: el error va pegado al campo.
          const usuario = this.formulario.controls.usuario;
          usuario.setErrors({ servidor: `Ya existe un administrador con el usuario "${username}". Elegí otro.` });
          usuario.markAsTouched();
          this.campoUsuario()?.nativeElement.focus({ preventScroll: true });
          return;
        }
        this.errorModal.set(
          e instanceof HttpErrorResponse
            ? traducirErrorCrearAdministrador(e)
            : { titulo: 'No pudimos crear el administrador', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
        );
      }
    });
  }

  // ── Baja ──────────────────────────────────────────────────

  confirmarBaja(): void {
    const modal = this.modal();
    if (modal?.tipo !== 'baja') {
      return;
    }
    const admin = modal.admin;
    this.enviando.set(true);
    this.errorModal.set(null);
    this.adminService.bajaAdministrador(admin.idAdministrador).subscribe({
      next: () => {
        this.enviando.set(false);
        this.modal.set(null);
        this.disparador = null;
        this.administradores.update((lista) => lista.filter((a) => a.idAdministrador !== admin.idAdministrador));
        this.aviso.set(`Diste de baja a "${admin.username}". Ya no puede ingresar al panel.`);
      },
      error: (e: unknown) => {
        this.enviando.set(false);
        this.errorModal.set(
          e instanceof HttpErrorResponse
            ? traducirErrorBajaAdministrador(e)
            : { titulo: 'No pudimos dar de baja al administrador', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
        );
        if (e instanceof HttpErrorResponse && e.status === 404) {
          this.cargar();
        }
      }
    });
  }
}
