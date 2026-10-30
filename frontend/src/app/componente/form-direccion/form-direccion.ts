import { Component, input, output, signal, ChangeDetectionStrategy } from '@angular/core';
import { NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, debounceTime, distinctUntilChanged, map, of, switchMap, tap } from 'rxjs';
import { Direccion } from '../../models/direccion';
import { Localidad } from '../../models/geo';
import { DireccionService } from '../../services/direccion.service';
import { GeoService } from '../../services/geo.service';
import { ErrorTraducido, traducirErrorDireccion } from '../../validaciones/errores-api';
import { mensajeDeError } from '../../validaciones/mensajes';
import { requerido, soloNumeros } from '../../validaciones/validadores';

/** Caracteres mínimos para buscar localidades (el back tampoco busca con menos). */
const MINIMO_BUSQUEDA = 3;

/**
 * Formulario "Agregar dirección".
 * El cliente busca su localidad (Georef) y la elige de la lista; calle, altura y CP los escribe.
 * La ciudad y la provincia las resuelve el back con el id de la localidad.
 * Se usa en el checkout y en "Mis direcciones".
 */
@Component({
  selector: 'app-form-direccion',
  imports: [ReactiveFormsModule, NgClass],
  templateUrl: './form-direccion.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './form-direccion.css'
})
export class FormDireccion {
  /** Muestra "Cancelar" (no tiene sentido si el cliente todavía no tiene ninguna dirección). */
  readonly puedeCancelar = input(true);
  readonly guardada = output<Direccion>();
  readonly cancelar = output<void>();

  /** Lo que escribe el cliente en el buscador (no se manda al back al guardar). */
  busqueda = new FormControl('', { nonNullable: true });
  /** Id de la localidad elegida: sin él no se puede guardar. */
  idLocalidad = new FormControl<string | null>(null, { validators: [Validators.required] });
  // Mismas reglas que DireccionRequest en el back.
  calle = new FormControl('', { nonNullable: true, validators: [requerido, Validators.maxLength(100)] });
  numeroCalle = new FormControl('', { nonNullable: true, validators: [requerido, soloNumeros, Validators.min(1)] });
  codigoPostal = new FormControl('', { nonNullable: true, validators: [requerido, Validators.maxLength(8)] });

  formulario = new FormGroup({
    idLocalidad: this.idLocalidad,
    calle: this.calle,
    numeroCalle: this.numeroCalle,
    codigoPostal: this.codigoPostal
  });

  protected readonly sugerencias = signal<Localidad[]>([]);
  protected readonly listaAbierta = signal(false);
  protected readonly activa = signal(-1);
  protected readonly buscando = signal(false);
  protected readonly sinResultados = signal(false);
  protected readonly elegida = signal<Localidad | null>(null);

  protected readonly guardando = signal(false);
  protected readonly intentoEnviar = signal(false);
  protected readonly errorServidor = signal<ErrorTraducido | null>(null);

  constructor(
    private geoService: GeoService,
    private direccionService: DireccionService
  ) {
    this.busqueda.valueChanges
      .pipe(
        // Escribir de nuevo invalida la localidad elegida: hay que volver a elegir de la lista.
        tap(() => this.olvidarLocalidad()),
        map((texto) => texto.trim()),
        debounceTime(300),
        distinctUntilChanged(),
        switchMap((texto) => {
          if (texto.length < MINIMO_BUSQUEDA) {
            this.buscando.set(false);
            return of<Localidad[] | null>([]);
          }
          this.buscando.set(true);
          return this.geoService.localidades(texto).pipe(
            catchError((e: unknown) => {
              this.mostrarError(e);
              return of(null);
            })
          );
        }),
        takeUntilDestroyed()
      )
      .subscribe((resultado) => {
        this.buscando.set(false);
        if (resultado === null) {
          this.cerrarLista();
          return;
        }
        this.errorServidor.set(null);
        this.sugerencias.set(resultado);
        this.activa.set(resultado.length ? 0 : -1);
        this.sinResultados.set(this.busqueda.value.trim().length >= MINIMO_BUSQUEDA && !resultado.length);
        this.listaAbierta.set(resultado.length > 0 || this.sinResultados());
      });
  }

  /** Mensaje de error del campo, solo si el usuario ya lo tocó o intentó guardar. */
  error(control: FormControl, campo: string): string | null {
    if (!control.touched && !this.intentoEnviar()) {
      return null;
    }
    return mensajeDeError(control, campo);
  }

  /** "General Pueyrredón, Buenos Aires" (el departamento se omite si se llama igual que la localidad). */
  protected detalle(localidad: Localidad): string {
    const partes = [localidad.departamento, localidad.provincia]
      .filter((p): p is string => !!p && p !== localidad.nombre);
    return partes.join(', ') || localidad.provincia;
  }

  // ── Buscador (combobox) ────────────────────────────────

  alTeclear(evento: KeyboardEvent): void {
    const total = this.sugerencias().length;
    if (evento.key === 'ArrowDown' && total) {
      evento.preventDefault();
      this.listaAbierta.set(true);
      this.activa.update((i) => (i + 1) % total);
    } else if (evento.key === 'ArrowUp' && total) {
      evento.preventDefault();
      this.listaAbierta.set(true);
      this.activa.update((i) => (i <= 0 ? total - 1 : i - 1));
    } else if (evento.key === 'Enter' && this.listaAbierta() && this.activa() >= 0) {
      // Enter elige la sugerencia marcada en vez de enviar el formulario.
      evento.preventDefault();
      this.elegir(this.sugerencias()[this.activa()]);
    } else if (evento.key === 'Escape' && this.listaAbierta()) {
      evento.preventDefault();
      this.cerrarLista();
    }
  }

  /** Se usa mousedown (no click) para que corra antes del blur del input. */
  alApretarSugerencia(evento: MouseEvent, localidad: Localidad): void {
    evento.preventDefault();
    this.elegir(localidad);
  }

  alSalirDelBuscador(): void {
    this.cerrarLista();
    this.idLocalidad.markAsTouched();
  }

  elegir(localidad: Localidad): void {
    this.cerrarLista();
    this.busqueda.setValue(localidad.nombre, { emitEvent: false });
    this.errorServidor.set(null);
    this.elegida.set(localidad);
    this.idLocalidad.setValue(localidad.id);
  }

  // ── Guardar ────────────────────────────────────────────

  guardar(): void {
    this.intentoEnviar.set(true);
    this.errorServidor.set(null);
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    this.guardando.set(true);
    this.direccionService
      .agregar({
        nombreCalle: this.calle.value.trim(),
        numeroCalle: Number(this.numeroCalle.value),
        codigoPostal: this.codigoPostal.value.trim(),
        idLocalidad: this.idLocalidad.value!
      })
      .subscribe({
        next: (direccion) => {
          this.guardando.set(false);
          this.guardada.emit(direccion);
        },
        error: (e: unknown) => {
          this.guardando.set(false);
          this.mostrarError(e);
        }
      });
  }

  // ── Internos ───────────────────────────────────────────

  private olvidarLocalidad(): void {
    if (this.elegida() !== null || this.idLocalidad.value !== null) {
      this.elegida.set(null);
      this.idLocalidad.setValue(null);
    }
  }

  private cerrarLista(): void {
    this.listaAbierta.set(false);
    this.activa.set(-1);
  }

  private mostrarError(e: unknown): void {
    this.errorServidor.set(
      e instanceof HttpErrorResponse
        ? traducirErrorDireccion(e)
        : { titulo: 'No pudimos guardar la dirección', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
    );
  }
}
