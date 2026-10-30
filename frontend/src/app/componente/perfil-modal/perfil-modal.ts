import {
  ChangeDetectionStrategy,
  Component,
  HostListener,
  computed,
  signal,
  output
} from '@angular/core';
import { DatePipe, DecimalPipe, NgClass } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Direccion } from '../../models/direccion';
import { ETIQUETAS_ESTADO_PEDIDO, EstadoPedido, Pedido } from '../../models/pedido';
import { AuthService } from '../../services/auth.service';
import { ClienteService } from '../../services/cliente.service';
import {
  ErrorTraducido,
  traducirErrorPassword,
  traducirErrorPerfil
} from '../../validaciones/errores-api';
import { mensajeDeError } from '../../validaciones/mensajes';
import { coincideCon, requerido, soloLetras } from '../../validaciones/validadores';

type Modo = 'ver' | 'editar' | 'password';

@Component({
  selector: 'app-perfil-modal',
  imports: [ReactiveFormsModule, NgClass, DatePipe, DecimalPipe],
  templateUrl: './perfil-modal.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './perfil-modal.css'
})
export class PerfilModal {
  readonly cerrar = output<void>();

  protected readonly modo = signal<Modo>('ver');
  protected readonly cargando = signal(false);
  protected readonly exito = signal<string | null>(null);
  protected readonly errorServidor = signal<ErrorTraducido | null>(null);
  protected readonly mostrarPassword = signal(false);

  protected readonly pedidos = signal<Pedido[]>([]);
  protected readonly cargandoPedidos = signal(false);
  protected readonly errorPedidos = signal<string | null>(null);

  private readonly intentoPerfil = signal(false);
  private readonly intentoPassword = signal(false);

  protected readonly cliente = computed(() => this.authService.cliente());
  protected readonly nombreCompleto = computed(() => {
    const cliente = this.authService.cliente();
    return cliente ? `${cliente.nombre} ${cliente.apellido}`.trim() : '';
  });
  protected readonly iniciales = computed(() => {
    const cliente = this.authService.cliente();
    if (!cliente) {
      return '';
    }
    return `${cliente.nombre.charAt(0)}${cliente.apellido.charAt(0)}`.toUpperCase();
  });

  protected readonly perfilForm: FormGroup;
  protected readonly nombre: FormControl<string>;
  protected readonly apellido: FormControl<string>;

  protected readonly passwordForm: FormGroup;
  protected readonly contraseniaActual: FormControl<string>;
  protected readonly contraseniaNueva: FormControl<string>;
  protected readonly repetirContrasenia: FormControl<string>;

  private readonly etiquetasEstado = ETIQUETAS_ESTADO_PEDIDO;

  constructor(
    private authService: AuthService,
    private clienteService: ClienteService
  ) {
    const cliente = this.authService.cliente();

    this.nombre = new FormControl(cliente?.nombre ?? '', {
      nonNullable: true,
      validators: [requerido, soloLetras, Validators.maxLength(50)]
    });
    this.apellido = new FormControl(cliente?.apellido ?? '', {
      nonNullable: true,
      validators: [requerido, soloLetras, Validators.maxLength(50)]
    });
    this.perfilForm = new FormGroup({
      nombre: this.nombre,
      apellido: this.apellido
    });

    this.contraseniaActual = new FormControl('', {
      nonNullable: true,
      validators: [requerido]
    });
    this.contraseniaNueva = new FormControl('', {
      nonNullable: true,
      validators: [requerido, Validators.minLength(6), Validators.maxLength(50)]
    });
    this.repetirContrasenia = new FormControl('', {
      nonNullable: true,
      validators: [requerido, coincideCon('contraseniaNueva')]
    });
    this.passwordForm = new FormGroup({
      contraseniaActual: this.contraseniaActual,
      contraseniaNueva: this.contraseniaNueva,
      repetirContrasenia: this.repetirContrasenia
    });

    // Si cambia la contraseña nueva, la repetición tiene que volver a validarse.
    this.contraseniaNueva.valueChanges.subscribe(() =>
      this.repetirContrasenia.updateValueAndValidity()
    );

    this.cargarPedidos();
  }

  @HostListener('document:keydown.escape')
  protected cerrarConEscape(): void {
    this.cerrar.emit();
  }

  protected error(control: FormControl, campo: string): string | null {
    if (!control.touched && !this.intentoPerfil() && !this.intentoPassword()) {
      return null;
    }
    return mensajeDeError(control, campo);
  }

  protected correcto(control: FormControl): boolean {
    return control.touched && control.valid && control.value !== '';
  }

  protected editar(): void {
    const cliente = this.authService.cliente();
    this.nombre.setValue(cliente?.nombre ?? '');
    this.apellido.setValue(cliente?.apellido ?? '');
    this.intentoPerfil.set(false);
    this.errorServidor.set(null);
    this.exito.set(null);
    this.modo.set('editar');
  }

  protected cancelarEdicion(): void {
    this.intentoPerfil.set(false);
    this.errorServidor.set(null);
    this.modo.set('ver');
  }

  protected guardarPerfil(): void {
    this.intentoPerfil.set(true);
    this.errorServidor.set(null);
    this.exito.set(null);

    if (this.perfilForm.invalid) {
      this.perfilForm.markAllAsTouched();
      return;
    }

    this.cargando.set(true);
    this.clienteService
      .actualizarPerfil({
        nombre: this.nombre.value.trim(),
        apellido: this.apellido.value.trim(),
        email: this.authService.cliente()?.email ?? ''
      })
      .subscribe({
        next: () => {
          this.cargando.set(false);
          this.exito.set('Tus datos se actualizaron correctamente.');
          this.modo.set('ver');
        },
        error: (e) => {
          this.cargando.set(false);
          this.errorServidor.set(
            e?.status ? traducirErrorPerfil(e) : {
              titulo: 'No pudimos actualizar tus datos',
              detalle: 'Ocurrió un error inesperado. Intentá de nuevo.'
            }
          );
        }
      });
  }

  protected irPassword(): void {
    this.passwordForm.reset();
    this.intentoPassword.set(false);
    this.errorServidor.set(null);
    this.exito.set(null);
    this.mostrarPassword.set(false);
    this.modo.set('password');
  }

  protected cancelarPassword(): void {
    this.passwordForm.reset();
    this.intentoPassword.set(false);
    this.errorServidor.set(null);
    this.modo.set('ver');
  }

  protected guardarPassword(): void {
    this.intentoPassword.set(true);
    this.errorServidor.set(null);
    this.exito.set(null);

    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    this.cargando.set(true);
    this.clienteService
      .cambiarPassword({
        contraseniaActual: this.contraseniaActual.value,
        contraseniaNueva: this.contraseniaNueva.value
      })
      .subscribe({
        next: () => {
          this.cargando.set(false);
          this.passwordForm.reset();
          this.intentoPassword.set(false);
          this.exito.set('Tu contraseña se actualizó correctamente.');
          this.modo.set('ver');
        },
        error: (e) => {
          this.cargando.set(false);
          this.errorServidor.set(
            e?.status ? traducirErrorPassword(e) : {
              titulo: 'No pudimos cambiar la contraseña',
              detalle: 'Ocurrió un error inesperado. Intentá de nuevo.'
            }
          );
        }
      });
  }

  protected alternarPassword(): void {
    this.mostrarPassword.update((valor) => !valor);
  }

  protected estadoPedido(estado: EstadoPedido): string {
    return this.etiquetasEstado[estado] ?? estado;
  }

  protected estadoClase(estado: EstadoPedido): string {
    return `estado-${estado.toLowerCase().replace('_', '-')}`;
  }

  protected cantidadProductos(pedido: Pedido): number {
    return (pedido.items ?? []).reduce((total, item) => total + item.cantidad, 0);
  }

  protected direccionTexto(direccion: Direccion): string {
    return `${direccion.nombreCalle} ${direccion.numeroCalle}, ${direccion.ciudad.nombreCiudad}`;
  }

  private cargarPedidos(): void {
    this.cargandoPedidos.set(true);
    this.errorPedidos.set(null);

    this.clienteService.listarPedidos().subscribe({
      next: (pedidos) => {
        this.pedidos.set(pedidos);
        this.cargandoPedidos.set(false);
      },
      error: (e) => {
        this.cargandoPedidos.set(false);
        this.errorPedidos.set(
          e?.status
            ? 'No pudimos cargar tus pedidos. Intentá de nuevo.'
            : 'Revisá tu conexión a internet e intentá de nuevo.'
        );
      }
    });
  }
}
