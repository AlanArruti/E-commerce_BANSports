import { ChangeDetectionStrategy, Component, ElementRef, OnInit, effect, signal, viewChild } from '@angular/core';
import { DatePipe, NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Cupon, CuponRequest, ETIQUETAS_ESTADO_CUPON, TipoCupon } from '../../models/cupon';
import { PrecioPipe, formatearPrecio } from '../../pipes/precio.pipe';
import { CuponService } from '../../services/cupon.service';
import {
  ErrorTraducido,
  traducirErrorCupones,
  traducirErrorGuardarCupon
} from '../../validaciones/errores-api';
import { requerido } from '../../validaciones/validadores';

type Modal = { tipo: 'formulario'; cupon: Cupon | null } | { tipo: 'baja'; cupon: Cupon };

const CODIGO = /^[A-Za-z0-9_-]{3,30}$/;
const ENTERO = /^\d+$/;
/** Número con coma o punto decimal, sin separador de miles: "15", "2500,50". */
const NUMERO = /^\d+([.,]\d{1,2})?$/;

const leerNumero = (texto: string) => Number(texto.trim().replace(',', '.'));

/** Valor del descuento: número > 0 y, si es porcentaje, hasta 100. Se valida contra el tipo elegido. */
function valorValido(grupo: AbstractControl): ValidationErrors | null {
  const tipo = grupo.get('tipo')?.value as TipoCupon;
  const texto = String(grupo.get('valor')?.value ?? '').trim();
  if (!texto) return null; // lo marca "requerido" en el campo
  if (!NUMERO.test(texto)) return { valorFormato: true };
  const valor = leerNumero(texto);
  if (valor <= 0) return { valorCero: true };
  if (tipo === 'PORCENTAJE' && valor > 100) return { porcentajeAlto: true };
  return null;
}

function fechasEnOrden(grupo: AbstractControl): ValidationErrors | null {
  const desde = grupo.get('vigenteDesde')?.value as string;
  const hasta = grupo.get('vigenteHasta')?.value as string;
  return desde && hasta && hasta < desde ? { fechasInvertidas: true } : null;
}

/**
 * Cupones de descuento del panel: listado con su estado, alta, edición y baja (lógica).
 * El cupón descuenta sobre el subtotal de productos del pedido, no sobre el envío.
 */
@Component({
  selector: 'app-admin-cupones',
  imports: [DatePipe, NgClass, ReactiveFormsModule, RouterLink, PrecioPipe],
  templateUrl: './admin-cupones.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './admin-cupones.css',
  host: { '(document:keydown.escape)': 'cerrarModal()' }
})
export class AdminCupones implements OnInit {
  protected readonly etiquetasEstado = ETIQUETAS_ESTADO_CUPON;

  protected readonly cupones = signal<Cupon[]>([]);
  protected readonly cargando = signal(true);
  protected readonly errorCarga = signal<ErrorTraducido | null>(null);
  protected readonly aviso = signal<string | null>(null);

  protected readonly modal = signal<Modal | null>(null);
  protected readonly enviando = signal(false);
  protected readonly errorModal = signal<ErrorTraducido | null>(null);
  protected readonly intentoGuardar = signal(false);

  readonly formulario = new FormGroup(
    {
      codigo: new FormControl('', { nonNullable: true, validators: [requerido, Validators.pattern(CODIGO)] }),
      tipo: new FormControl<TipoCupon>('PORCENTAJE', { nonNullable: true }),
      valor: new FormControl('', { nonNullable: true, validators: [requerido] }),
      vigenteDesde: new FormControl('', { nonNullable: true }),
      vigenteHasta: new FormControl('', { nonNullable: true }),
      montoMinimo: new FormControl('', { nonNullable: true, validators: [Validators.pattern(NUMERO)] }),
      usosMaximos: new FormControl('', { nonNullable: true, validators: [Validators.pattern(ENTERO), Validators.min(1)] }),
      activo: new FormControl(true, { nonNullable: true })
    },
    { validators: [valorValido, fechasEnOrden] }
  );

  private disparador: HTMLElement | null = null;
  private readonly campoCodigo = viewChild<ElementRef<HTMLInputElement>>('campoCodigo');
  private readonly cancelarBajaBtn = viewChild<ElementRef<HTMLButtonElement>>('cancelarBajaBtn');

  constructor(private cuponService: CuponService) {
    // Al cambiar el tipo, el valor se vuelve a validar (15 está bien como monto, 150 no como porcentaje).
    this.formulario.controls.tipo.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => this.formulario.updateValueAndValidity());
    effect(() => this.campoCodigo()?.nativeElement.focus({ preventScroll: true }));
    effect(() => this.cancelarBajaBtn()?.nativeElement.focus({ preventScroll: true }));
  }

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.errorCarga.set(null);
    this.cuponService.listar().subscribe({
      next: (lista) => {
        this.cupones.set(lista);
        this.cargando.set(false);
      },
      error: (e: unknown) => {
        this.cargando.set(false);
        this.errorCarga.set(
          e instanceof HttpErrorResponse
            ? traducirErrorCupones(e)
            : { titulo: 'No pudimos cargar los cupones', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
        );
      }
    });
  }

  // ── Vista ─────────────────────────────────────────────

  protected beneficio(cupon: Cupon): string {
    return cupon.tipo === 'PORCENTAJE' ? `${cupon.valor}%` : formatearPrecio(cupon.valor);
  }

  protected esPorcentaje(): boolean {
    return this.formulario.controls.tipo.value === 'PORCENTAJE';
  }

  /** Mensaje de un campo, solo si ya lo tocaron o se intentó guardar. */
  protected error(campo: 'codigo' | 'valor' | 'montoMinimo' | 'usosMaximos' | 'fechas'): string | null {
    const f = this.formulario;
    const mostrar = (c: AbstractControl) => c.touched || this.intentoGuardar();
    switch (campo) {
      case 'codigo': {
        const c = f.controls.codigo;
        if (!mostrar(c) || !c.errors) return null;
        if (c.errors['servidor']) return c.errors['servidor'];
        if (c.errors['requerido']) return 'Poné el código que van a escribir los clientes.';
        return 'De 3 a 30 letras, números, guiones o guiones bajos, sin espacios.';
      }
      case 'valor': {
        const c = f.controls.valor;
        if (!mostrar(c)) return null;
        if (c.errors?.['requerido']) return this.esPorcentaje() ? 'Poné el porcentaje.' : 'Poné el monto.';
        if (f.errors?.['valorFormato']) return 'Usá solo números, con coma para los centavos (por ejemplo 2500,50).';
        if (f.errors?.['valorCero']) return 'Tiene que ser mayor a 0.';
        if (f.errors?.['porcentajeAlto']) return 'Un porcentaje no puede ser más de 100.';
        return null;
      }
      case 'montoMinimo': {
        const c = f.controls.montoMinimo;
        return mostrar(c) && c.errors ? 'Usá solo números, con coma para los centavos. Vacío = sin mínimo.' : null;
      }
      case 'usosMaximos': {
        const c = f.controls.usosMaximos;
        return mostrar(c) && c.errors ? 'Un número entero, al menos 1. Vacío = sin límite.' : null;
      }
      case 'fechas':
        return (f.controls.vigenteHasta.touched || this.intentoGuardar()) && f.errors?.['fechasInvertidas']
          ? 'La fecha de fin no puede ser anterior a la de inicio.'
          : null;
    }
  }

  // ── Modales ───────────────────────────────────────────

  abrirFormulario(cupon: Cupon | null, evento: Event): void {
    this.disparador = evento.currentTarget as HTMLElement;
    this.formulario.reset({
      codigo: cupon?.codigo ?? '',
      tipo: cupon?.tipo ?? 'PORCENTAJE',
      valor: cupon ? String(cupon.valor).replace('.', ',') : '',
      vigenteDesde: cupon?.vigenteDesde ?? '',
      vigenteHasta: cupon?.vigenteHasta ?? '',
      montoMinimo: cupon && cupon.montoMinimo > 0 ? String(cupon.montoMinimo).replace('.', ',') : '',
      usosMaximos: cupon?.usosMaximos != null ? String(cupon.usosMaximos) : '',
      activo: cupon?.activo ?? true
    });
    this.intentoGuardar.set(false);
    this.errorModal.set(null);
    this.aviso.set(null);
    this.modal.set({ tipo: 'formulario', cupon });
  }

  pedirBaja(cupon: Cupon, evento: Event): void {
    this.disparador = evento.currentTarget as HTMLElement;
    this.errorModal.set(null);
    this.aviso.set(null);
    this.modal.set({ tipo: 'baja', cupon });
  }

  cerrarModal(): void {
    if (this.modal() && !this.enviando()) {
      this.modal.set(null);
      this.disparador?.focus({ preventScroll: true });
      this.disparador = null;
    }
  }

  // ── Guardar ───────────────────────────────────────────

  guardar(): void {
    const modal = this.modal();
    if (modal?.tipo !== 'formulario') return;
    this.intentoGuardar.set(true);
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      setTimeout(() => document.querySelector<HTMLElement>('.modal [aria-invalid="true"]')?.focus());
      return;
    }

    const v = this.formulario.getRawValue();
    const cuerpo: CuponRequest = {
      codigo: v.codigo.trim().toUpperCase(),
      tipo: v.tipo,
      valor: leerNumero(v.valor),
      vigenteDesde: v.vigenteDesde || null,
      vigenteHasta: v.vigenteHasta || null,
      montoMinimo: v.montoMinimo.trim() ? leerNumero(v.montoMinimo) : null,
      usosMaximos: v.usosMaximos.trim() ? Number(v.usosMaximos) : null,
      activo: v.activo
    };
    const pedido = modal.cupon
      ? this.cuponService.actualizar(modal.cupon.id, cuerpo)
      : this.cuponService.crear(cuerpo);

    this.enviando.set(true);
    this.errorModal.set(null);
    pedido.subscribe({
      next: (guardado) => {
        this.enviando.set(false);
        this.modal.set(null);
        this.disparador?.focus({ preventScroll: true });
        this.disparador = null;
        this.cupones.update((lista) =>
          modal.cupon ? lista.map((c) => (c.id === guardado.id ? guardado : c)) : [guardado, ...lista]
        );
        this.aviso.set(modal.cupon ? `Guardaste los cambios de ${guardado.codigo}.` : `Creaste el cupón ${guardado.codigo}.`);
      },
      error: (e: unknown) => {
        this.enviando.set(false);
        // Código repetido: el error va pegado al campo.
        if (e instanceof HttpErrorResponse && e.status === 409 && /código/i.test(e.error?.message ?? '')) {
          const codigo = this.formulario.controls.codigo;
          codigo.setErrors({ servidor: e.error.message });
          codigo.markAsTouched();
          this.campoCodigo()?.nativeElement.focus();
          return;
        }
        this.errorModal.set(
          e instanceof HttpErrorResponse
            ? traducirErrorGuardarCupon(e)
            : { titulo: 'No pudimos guardar el cupón', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
        );
      }
    });
  }

  confirmarBaja(): void {
    const modal = this.modal();
    if (modal?.tipo !== 'baja') return;
    const cupon = modal.cupon;
    this.enviando.set(true);
    this.errorModal.set(null);
    this.cuponService.eliminar(cupon.id).subscribe({
      next: () => {
        this.enviando.set(false);
        this.modal.set(null);
        this.disparador = null;
        this.cupones.update((lista) =>
          lista.map((c) => (c.id === cupon.id ? { ...c, activo: false, estado: 'INACTIVO' } : c))
        );
        this.aviso.set(`Diste de baja ${cupon.codigo}. Ya no se acepta en el checkout.`);
      },
      error: (e: unknown) => {
        this.enviando.set(false);
        this.errorModal.set(
          e instanceof HttpErrorResponse
            ? traducirErrorGuardarCupon(e)
            : { titulo: 'No pudimos dar de baja el cupón', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
        );
      }
    });
  }
}
