import { Component, input, output, signal, ChangeDetectionStrategy } from '@angular/core';
import { NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, debounceTime, distinctUntilChanged, map, of, switchMap, tap } from 'rxjs';
import { Direccion } from '../../models/direccion';
import { Lugar, SugerenciaDireccion } from '../../models/geo';
import { DireccionService } from '../../services/direccion.service';
import { GeoService } from '../../services/geo.service';
import { ErrorTraducido, traducirErrorDireccion } from '../../validaciones/errores-api';
import { mensajeDeError } from '../../validaciones/mensajes';
import { requerido, soloNumeros } from '../../validaciones/validadores';

/** Caracteres mínimos para pedir sugerencias (el back tampoco busca con menos). */
const MINIMO_BUSQUEDA = 3;

/**
 * Formulario "Agregar dirección" con búsqueda de Google Maps.
 * El cliente busca su dirección, elige una sugerencia y se completan calle, altura y CP
 * (corregibles a mano). La ciudad y la provincia las resuelve el back con el placeId.
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
  /** placeId de la sugerencia elegida: sin él no se puede guardar. */
  placeId = new FormControl<string | null>(null, { validators: [Validators.required] });
  // Mismas reglas que DireccionRequest en el back.
  calle = new FormControl('', { nonNullable: true, validators: [requerido, Validators.maxLength(100)] });
  numeroCalle = new FormControl('', { nonNullable: true, validators: [requerido, soloNumeros, Validators.min(1)] });
  codigoPostal = new FormControl('', { nonNullable: true, validators: [requerido, Validators.maxLength(8)] });

  formulario = new FormGroup({
    placeId: this.placeId,
    calle: this.calle,
    numeroCalle: this.numeroCalle,
    codigoPostal: this.codigoPostal
  });

  protected readonly sugerencias = signal<SugerenciaDireccion[]>([]);
  protected readonly listaAbierta = signal(false);
  protected readonly activa = signal(-1);
  protected readonly buscando = signal(false);
  protected readonly sinResultados = signal(false);
  protected readonly cargandoLugar = signal(false);
  protected readonly lugar = signal<Lugar | null>(null);

  protected readonly guardando = signal(false);
  protected readonly intentoEnviar = signal(false);
  protected readonly errorServidor = signal<ErrorTraducido | null>(null);

  /** Id de esta búsqueda para Google: se renueva después de guardar. */
  private sesion = crypto.randomUUID();

  constructor(
    private geoService: GeoService,
    private direccionService: DireccionService
  ) {
    this.busqueda.valueChanges
      .pipe(
        // Escribir de nuevo invalida la dirección elegida: hay que volver a elegir de la lista.
        tap(() => this.olvidarLugar()),
        map((texto) => texto.trim()),
        debounceTime(300),
        distinctUntilChanged(),
        switchMap((texto) => {
          if (texto.length < MINIMO_BUSQUEDA) {
            this.buscando.set(false);
            return of<SugerenciaDireccion[] | null>([]);
          }
          this.buscando.set(true);
          return this.geoService.sugerencias(texto, this.sesion).pipe(
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
  alApretarSugerencia(evento: MouseEvent, sugerencia: SugerenciaDireccion): void {
    evento.preventDefault();
    this.elegir(sugerencia);
  }

  alSalirDelBuscador(): void {
    this.cerrarLista();
    this.placeId.markAsTouched();
  }

  elegir(sugerencia: SugerenciaDireccion): void {
    this.cerrarLista();
    const texto = sugerencia.secundario ? `${sugerencia.principal}, ${sugerencia.secundario}` : sugerencia.principal;
    this.busqueda.setValue(texto, { emitEvent: false });
    this.errorServidor.set(null);
    this.cargandoLugar.set(true);

    this.geoService.lugar(sugerencia.placeId, this.sesion).subscribe({
      next: (lugar) => {
        this.cargandoLugar.set(false);
        this.lugar.set(lugar);
        this.placeId.setValue(lugar.placeId);
        // Se precarga lo que mandó Google; lo que falte (altura, CP) lo completa el cliente.
        this.calle.setValue(lugar.nombreCalle ?? '');
        this.numeroCalle.setValue(lugar.numeroCalle !== null ? String(lugar.numeroCalle) : '');
        this.codigoPostal.setValue(lugar.codigoPostal ?? '');
      },
      error: (e: unknown) => {
        this.cargandoLugar.set(false);
        this.mostrarError(e);
      }
    });
  }

  // ── Guardar ────────────────────────────────────────────

  guardar(): void {
    this.intentoEnviar.set(true);
    this.errorServidor.set(null);
    if (this.formulario.invalid || this.cargandoLugar()) {
      this.formulario.markAllAsTouched();
      return;
    }

    this.guardando.set(true);
    this.direccionService
      .agregar({
        nombreCalle: this.calle.value.trim(),
        numeroCalle: Number(this.numeroCalle.value),
        codigoPostal: this.codigoPostal.value.trim(),
        placeId: this.placeId.value!,
        sesion: this.sesion
      })
      .subscribe({
        next: (direccion) => {
          this.guardando.set(false);
          this.sesion = crypto.randomUUID();
          this.guardada.emit(direccion);
        },
        error: (e: unknown) => {
          this.guardando.set(false);
          this.mostrarError(e);
        }
      });
  }

  // ── Internos ───────────────────────────────────────────

  private olvidarLugar(): void {
    if (this.lugar() !== null || this.placeId.value !== null) {
      this.lugar.set(null);
      this.placeId.setValue(null);
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
