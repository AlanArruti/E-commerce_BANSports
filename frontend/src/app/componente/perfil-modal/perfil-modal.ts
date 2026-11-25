import {
  ChangeDetectionStrategy,
  Component,
  HostListener,
  OnInit,
  computed,
  input,
  signal,
  output
} from '@angular/core';
import { DatePipe, DecimalPipe, NgClass } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { APELLIDO_PENDIENTE, faltanDatos } from '../../models/cliente';
import { Direccion } from '../../models/direccion';
import { ETIQUETAS_ESTADO_PEDIDO, EstadoPedido, Pedido } from '../../models/pedido';
import { AuthService } from '../../services/auth.service';
import { ClienteService } from '../../services/cliente.service';
import { DireccionService } from '../../services/direccion.service';
import { FavoritoService } from '../../services/favorito.service';
import { FormDireccion } from '../form-direccion/form-direccion';
import {
  ErrorTraducido,
  traducirErrorBajaDireccion,
  traducirErrorPassword,
  traducirErrorPerfil
} from '../../validaciones/errores-api';
import { mensajeDeError } from '../../validaciones/mensajes';
import { coincideCon, largoDni, requerido, soloLetras, soloNumeros } from '../../validaciones/validadores';

export type ModoPerfil = 'ver' | 'editar' | 'password';
type Modo = ModoPerfil;

@Component({
  selector: 'app-perfil-modal',
  imports: [ReactiveFormsModule, NgClass, DatePipe, DecimalPipe, RouterLink, FormDireccion],
  templateUrl: './perfil-modal.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './perfil-modal.css'
})
export class PerfilModal implements OnInit {
  readonly cerrar = output<void>();
  /** Con qué pantalla abre. El aviso "Completá tus datos" del header lo abre directo en 'editar'. */
  readonly inicio = input<ModoPerfil>('ver');

  protected readonly modo = signal<Modo>('ver');
  protected readonly cargando = signal(false);
  protected readonly exito = signal<string | null>(null);
  protected readonly errorServidor = signal<ErrorTraducido | null>(null);
  protected readonly mostrarPassword = signal(false);

  protected readonly pedidos = signal<Pedido[]>([]);
  protected readonly cargandoPedidos = signal(false);
  protected readonly errorPedidos = signal<string | null>(null);

  /** Direcciones: alta abierta, la que pide confirmación para borrarse y la que se está borrando. */
  protected readonly agregandoDireccion = signal(false);
  protected readonly confirmandoBaja = signal<number | null>(null);
  protected readonly eliminandoDireccion = signal<number | null>(null);
  protected readonly errorDireccion = signal<ErrorTraducido | null>(null);

  private readonly intentoPerfil = signal(false);
  private readonly intentoPassword = signal(false);

  protected readonly cliente = computed(() => this.authService.cliente());
  /** Cuenta creada con Google o GitHub que todavía no tiene contraseña propia. */
  protected readonly sinContrasenia = computed(() => this.authService.cliente()?.tieneContrasenia === false);
  /** Le falta el DNI (obligatorio para comprar) o el apellido. */
  protected readonly faltanDatos = computed(() => faltanDatos(this.authService.cliente()));
  protected readonly sinDni = computed(() => !this.authService.cliente()?.dni);
  protected readonly nombreCompleto = computed(() => {
    const cliente = this.authService.cliente();
    return cliente ? `${cliente.nombre} ${this.apellidoReal(cliente.apellido)}`.trim() : '';
  });
  protected readonly iniciales = computed(() => {
    const cliente = this.authService.cliente();
    if (!cliente) {
      return '';
    }
    return `${cliente.nombre.charAt(0)}${this.apellidoReal(cliente.apellido).charAt(0)}`.toUpperCase();
  });

  protected readonly perfilForm: FormGroup;
  protected readonly nombre: FormControl<string>;
  protected readonly apellido: FormControl<string>;
  /** Solo se edita si todavía no tiene DNI; una vez cargado no se cambia. */
  protected readonly dni: FormControl<string>;

  protected readonly passwordForm: FormGroup;
  protected readonly contraseniaActual: FormControl<string>;
  protected readonly contraseniaNueva: FormControl<string>;
  protected readonly repetirContrasenia: FormControl<string>;

  private readonly etiquetasEstado = ETIQUETAS_ESTADO_PEDIDO;

  constructor(
    private authService: AuthService,
    private clienteService: ClienteService,
    private direccionService: DireccionService,
    protected favoritoService: FavoritoService
  ) {
    const cliente = this.authService.cliente();

    this.nombre = new FormControl(cliente?.nombre ?? '', {
      nonNullable: true,
      validators: [requerido, soloLetras, Validators.maxLength(50)]
    });
    this.apellido = new FormControl(this.apellidoReal(cliente?.apellido), {
      nonNullable: true,
      validators: [requerido, soloLetras, Validators.maxLength(50)]
    });
    // Opcional al guardar (se pueden cambiar otros datos sin cargarlo), pero si se escribe tiene que ser válido.
    this.dni = new FormControl('', {
      nonNullable: true,
      validators: [soloNumeros, largoDni]
    });
    this.perfilForm = new FormGroup({
      nombre: this.nombre,
      apellido: this.apellido,
      dni: this.dni
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

  ngOnInit(): void {
    // La sesión puede tener la lista vieja (por ejemplo, si se cargó una dirección desde otra pestaña).
    this.direccionService.listar().subscribe({ error: () => undefined });
    if (this.inicio() === 'editar') {
      this.editar();
    } else if (this.inicio() === 'password') {
      this.irPassword();
    }
  }

  /** El "-" que deja el login con GitHub no se muestra en el formulario: se pide el apellido de verdad. */
  private apellidoReal(apellido: string | undefined): string {
    return !apellido || apellido === APELLIDO_PENDIENTE ? '' : apellido;
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
    this.apellido.setValue(this.apellidoReal(cliente?.apellido));
    this.dni.setValue('');
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
        email: this.authService.cliente()?.email ?? '',
        // Solo se manda si todavía no tenía: una vez cargado no se cambia.
        dni: this.sinDni() ? this.dni.value.trim() || null : null
      })
      .subscribe({
        next: () => {
          this.cargando.set(false);
          this.intentoPerfil.set(false);
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
    // Una cuenta creada con Google o GitHub no conoce su contraseña: crea la primera sin la "actual".
    this.contraseniaActual.setValidators(this.sinContrasenia() ? [] : [requerido]);
    this.contraseniaActual.updateValueAndValidity();
    this.passwordForm.reset();
    // Sin esto, un guardado anterior del perfil dejaba los errores a la vista antes de escribir.
    this.intentoPerfil.set(false);
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
    // Se lee antes de mandar: al responder, el servicio ya marca que tiene contraseña.
    const creada = this.sinContrasenia();
    this.clienteService
      .cambiarPassword({
        contraseniaActual: creada ? null : this.contraseniaActual.value,
        contraseniaNueva: this.contraseniaNueva.value
      })
      .subscribe({
        next: () => {
          this.cargando.set(false);
          this.passwordForm.reset();
          this.intentoPassword.set(false);
          this.exito.set(
            creada
              ? 'Creaste tu contraseña. Ahora también podés entrar con tu email.'
              : 'Tu contraseña se actualizó correctamente.'
          );
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

  protected abrirAltaDireccion(): void {
    this.confirmandoBaja.set(null);
    this.errorDireccion.set(null);
    this.agregandoDireccion.set(true);
  }

  /** El servicio ya la sumó a la sesión. */
  protected direccionGuardada(): void {
    this.agregandoDireccion.set(false);
  }

  protected pedirBaja(id: number): void {
    this.errorDireccion.set(null);
    this.confirmandoBaja.set(id);
  }

  protected eliminarDireccion(id: number): void {
    this.eliminandoDireccion.set(id);
    this.errorDireccion.set(null);
    this.direccionService.eliminar(id).subscribe({
      next: () => this.terminarBaja(),
      error: (error) => {
        this.terminarBaja();
        // 404: ya estaba dada de baja (por ejemplo, desde otra pestaña).
        if (error.status === 404) {
          this.direccionService.quitarDeLaSesion(id);
        }
        this.errorDireccion.set(traducirErrorBajaDireccion(error));
      }
    });
  }

  private terminarBaja(): void {
    this.eliminandoDireccion.set(null);
    this.confirmandoBaja.set(null);
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
