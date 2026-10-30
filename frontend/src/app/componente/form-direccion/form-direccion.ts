import { Component, OnInit, input, output, signal, ChangeDetectionStrategy } from '@angular/core';
import { NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Direccion } from '../../models/direccion';
import { Ciudad, Pais, Provincia } from '../../models/geo';
import { DireccionService } from '../../services/direccion.service';
import { GeoService } from '../../services/geo.service';
import { ErrorTraducido, traducirErrorDireccion } from '../../validaciones/errores-api';
import { mensajeDeError } from '../../validaciones/mensajes';
import { requerido, soloNumeros } from '../../validaciones/validadores';

/**
 * Formulario "Agregar dirección" con selects encadenados país → provincia → ciudad.
 * Guarda en el back y avisa con (guardada). Se usa en el checkout y en "Mis direcciones".
 */
@Component({
  selector: 'app-form-direccion',
  imports: [ReactiveFormsModule, NgClass],
  templateUrl: './form-direccion.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './form-direccion.css'
})
export class FormDireccion implements OnInit {
  /** Muestra "Cancelar" (no tiene sentido si el cliente todavía no tiene ninguna dirección). */
  readonly puedeCancelar = input(true);
  readonly guardada = output<Direccion>();
  readonly cancelar = output<void>();

  pais = new FormControl<number | null>(null, { validators: [Validators.required] });
  provincia = new FormControl<number | null>({ value: null, disabled: true }, { validators: [Validators.required] });
  ciudad = new FormControl<number | null>({ value: null, disabled: true }, { validators: [Validators.required] });
  // Mismas reglas que DireccionRequest en el back.
  calle = new FormControl('', { nonNullable: true, validators: [requerido, Validators.maxLength(100)] });
  numeroCalle = new FormControl('', { nonNullable: true, validators: [requerido, soloNumeros, Validators.min(1)] });
  codigoPostal = new FormControl('', { nonNullable: true, validators: [requerido, Validators.maxLength(8)] });

  formulario = new FormGroup({
    pais: this.pais,
    provincia: this.provincia,
    ciudad: this.ciudad,
    calle: this.calle,
    numeroCalle: this.numeroCalle,
    codigoPostal: this.codigoPostal
  });

  protected readonly paises = signal<Pais[]>([]);
  protected readonly provincias = signal<Provincia[]>([]);
  protected readonly ciudades = signal<Ciudad[]>([]);
  protected readonly cargandoLista = signal<'provincias' | 'ciudades' | null>(null);
  protected readonly guardando = signal(false);
  protected readonly intentoEnviar = signal(false);
  protected readonly errorServidor = signal<ErrorTraducido | null>(null);

  constructor(
    private geoService: GeoService,
    private direccionService: DireccionService
  ) {
    // Al cambiar de país se resetean provincia y ciudad; al cambiar de provincia, la ciudad.
    this.pais.valueChanges.pipe(takeUntilDestroyed()).subscribe((idPais) => {
      this.reiniciar(this.provincia);
      this.reiniciar(this.ciudad);
      this.provincias.set([]);
      this.ciudades.set([]);
      if (idPais !== null) {
        this.cargarProvincias(idPais);
      }
    });
    this.provincia.valueChanges.pipe(takeUntilDestroyed()).subscribe((idProvincia) => {
      this.reiniciar(this.ciudad);
      this.ciudades.set([]);
      if (idProvincia !== null) {
        this.cargarCiudades(idProvincia);
      }
    });
  }

  ngOnInit(): void {
    this.geoService.paises().subscribe({
      next: (paises) => {
        this.paises.set(paises);
        // Hoy solo se envía dentro de Argentina: si hay un único país, ya queda elegido.
        if (paises.length === 1) {
          this.pais.setValue(paises[0].idPais);
        }
      },
      error: (e: unknown) => this.mostrarError(e)
    });
  }

  /** Mensaje de error del campo, solo si el usuario ya lo tocó o intentó guardar. */
  error(control: FormControl, campo: string): string | null {
    if (!control.touched && !this.intentoEnviar()) {
      return null;
    }
    return mensajeDeError(control, campo);
  }

  guardar(): void {
    this.intentoEnviar.set(true);
    this.errorServidor.set(null);
    // Un select deshabilitado (todavía cargando) no cuenta como inválido: por eso se revisa la ciudad aparte.
    if (this.formulario.invalid || this.ciudad.value === null) {
      this.formulario.markAllAsTouched();
      return;
    }

    this.guardando.set(true);
    this.direccionService
      .agregar({
        nombreCalle: this.calle.value.trim(),
        numeroCalle: Number(this.numeroCalle.value),
        codigoPostal: this.codigoPostal.value.trim(),
        idCiudad: this.ciudad.value!
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

  private cargarProvincias(idPais: number): void {
    this.cargandoLista.set('provincias');
    this.geoService.provincias(idPais).subscribe({
      next: (provincias) => {
        this.cargandoLista.set(null);
        this.provincias.set(provincias);
        this.provincia.enable({ emitEvent: false });
      },
      error: (e: unknown) => {
        this.cargandoLista.set(null);
        this.mostrarError(e);
      }
    });
  }

  private cargarCiudades(idProvincia: number): void {
    this.cargandoLista.set('ciudades');
    this.geoService.ciudades(idProvincia).subscribe({
      next: (ciudades) => {
        this.cargandoLista.set(null);
        this.ciudades.set(ciudades);
        this.ciudad.enable({ emitEvent: false });
      },
      error: (e: unknown) => {
        this.cargandoLista.set(null);
        this.mostrarError(e);
      }
    });
  }

  /** Vacía y deshabilita un select dependiente hasta que se cargue su lista. */
  private reiniciar(control: FormControl<number | null>): void {
    control.reset(null, { emitEvent: false });
    control.disable({ emitEvent: false });
  }

  private mostrarError(e: unknown): void {
    this.errorServidor.set(
      e instanceof HttpErrorResponse
        ? traducirErrorDireccion(e)
        : { titulo: 'No pudimos guardar la dirección', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
    );
  }
}
