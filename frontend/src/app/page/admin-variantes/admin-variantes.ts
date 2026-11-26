import { ChangeDetectionStrategy, Component, OnInit, computed, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors } from '@angular/forms';
import { Observable, catchError, forkJoin, map, of } from 'rxjs';
import { Producto } from '../../models/producto';
import { STOCK_BAJO, Variante, atributosOrdenados, etiquetaAtributo } from '../../models/variante';
import { OperacionStock, VarianteRequest } from '../../models/variante-request';
import {
  LARGO_OPCION,
  OPCIONES_SUGERIDAS,
  Opcion,
  claveCombinacion,
  claveDeOpcion,
  combinar,
  mismoTexto,
  nombreDeOpcion,
  opcionesDe,
  sugeridosPara
} from '../../models/opciones-variante';
import { ProductoService } from '../../services/producto.service';
import { CargadorImagen } from '../../componente/cargador-imagen/cargador-imagen';
import { ErrorTraducido, traducirErrorProducto, traducirErrorVariante } from '../../validaciones/errores-api';
import { leerPrecio, precioValido } from '../../validaciones/validadores';
import { PrecioPipe, formatearPrecio } from '../../pipes/precio.pipe';

/** Una combinación de la tabla del alta: "Chocolate · 1 kg". */
interface FilaCombinacion {
  clave: string;
  atributos: Record<string, string>;
  /** Ya hay una variante con esos valores: no se vuelve a crear. */
  existe: boolean;
}

interface DatosFila {
  incluir: boolean;
  stock: string;
  precio: string;
}

/** Valor del desplegable de edición que abre el campo para escribir un valor nuevo. */
export const NUEVO_VALOR = '__nuevo__';

type Resultado = { ok: true; variante: Variante } | { ok: false; error: unknown };

/** Entero ≥ min, escrito solo con dígitos. */
function entero(min: number) {
  return (control: AbstractControl): ValidationErrors | null => {
    const valor = String(control.value ?? '').trim();
    if (!valor) return { required: true };
    if (!/^\d+$/.test(valor)) return { entero: true };
    return Number(valor) < min ? { min: { min } } : null;
  };
}

/**
 * Variantes y stock de un producto: /admin/productos/:id/variantes.
 * SCRUM-116 (alta, edición y baja de variantes con atributos libres) y
 * SCRUM-117 (ajuste rápido de stock y aviso de stock bajo).
 */
@Component({
  selector: 'app-admin-variantes',
  imports: [RouterLink, ReactiveFormsModule, NgClass, PrecioPipe, CargadorImagen],
  templateUrl: './admin-variantes.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './admin-variantes.css',
  host: { '(document:keydown.escape)': 'cerrarModales()' }
})
export class AdminVariantes implements OnInit {
  protected readonly stockBajo = STOCK_BAJO;
  protected readonly etiqueta = etiquetaAtributo;
  protected readonly ordenados = atributosOrdenados;

  protected readonly producto = signal<Producto | null>(null);
  protected readonly variantes = signal<Variante[]>([]);
  protected readonly cargando = signal(true);
  protected readonly noExiste = signal(false);
  protected readonly errorCarga = signal<ErrorTraducido | null>(null);
  protected readonly aviso = signal<string | null>(null);

  protected readonly stockTotal = computed(() => this.variantes().reduce((total, v) => total + v.stock, 0));
  protected readonly cantidadStockBajo = computed(
    () => this.variantes().filter((v) => v.stock <= STOCK_BAJO).length
  );

  // ── Formulario de alta / edición ───────────────────────
  /** null = formulario cerrado; 'nueva' = alta por opciones; una variante = edición. */
  protected readonly editando = signal<Variante | 'nueva' | null>(null);
  protected readonly guardando = signal(false);
  protected readonly intentoGuardar = signal(false);
  protected readonly errorForm = signal<ErrorTraducido | null>(null);

  // Edición de una variante: stock y precio.
  stock = new FormControl('0', { nonNullable: true, validators: [entero(0)] });
  /** Precio propio de la variante (SCRUM-154). Vacío = cuesta lo mismo que el producto. */
  precio = new FormControl('', { nonNullable: true, validators: [precioValido] });
  formulario = new FormGroup({ stock: this.stock, precio: this.precio });

  // ── Alta: opciones y combinaciones ─────────────────────
  protected readonly sugeridas = OPCIONES_SUGERIDAS;
  protected readonly nombreOpcion = nombreDeOpcion;
  protected readonly sugeridosDe = sugeridosPara;
  protected readonly nuevoValor = NUEVO_VALOR;
  /** Opciones elegidas con sus valores. */
  protected readonly opciones = signal<Opcion[]>([]);
  /** Lo que ya usan las variantes creadas: no se puede quitar desde el alta. */
  private existentes: Opcion[] = [];
  /** Texto que se está escribiendo en el campo de valor de cada opción. */
  protected readonly borradores = signal<Record<string, string>>({});
  /** null = cerrado; texto = el campo "Otra opción" abierto. */
  protected readonly otraOpcion = signal<string | null>(null);
  protected readonly errorOpciones = signal<string | null>(null);
  private readonly datosFilas = signal<Record<string, DatosFila>>({});

  protected readonly filas = computed<FilaCombinacion[]>(() => {
    const creadas = new Set(this.variantes().map((v) => claveCombinacion(v.atributos)));
    return combinar(this.opciones()).map((atributos) => {
      const clave = claveCombinacion(atributos);
      return { clave, atributos, existe: creadas.has(clave) };
    });
  });
  protected readonly filasACrear = computed(() => this.filas().filter((f) => !f.existe && this.datos(f).incluir));
  protected readonly stockACrear = computed(() =>
    this.filasACrear().reduce((total, f) => total + (Number(this.datos(f).stock) || 0), 0)
  );
  protected readonly columnas = computed(() => this.opciones().filter((o) => o.valores.length > 0));

  // ── Edición: un desplegable por opción ─────────────────
  protected readonly opcionesEdicion = signal<Opcion[]>([]);
  protected readonly seleccion = signal<Record<string, string>>({});
  protected readonly valoresNuevos = signal<Record<string, string>>({});
  protected readonly errorAtributos = signal<string | null>(null);

  // ── Ajuste de stock ────────────────────────────────────
  protected readonly ajustando = signal<Variante | null>(null);
  protected readonly enviandoAjuste = signal(false);
  protected readonly intentoAjuste = signal(false);
  protected readonly errorAjuste = signal<ErrorTraducido | null>(null);
  protected readonly operacion = signal<OperacionStock>('AGREGAR');
  cantidad = new FormControl('', { nonNullable: true, validators: [entero(1)] });
  ajusteForm = new FormGroup({ cantidad: this.cantidad });

  // ── Baja ───────────────────────────────────────────────
  protected readonly bajando = signal<Variante | null>(null);
  protected readonly dandoDeBaja = signal(false);
  protected readonly errorBaja = signal<ErrorTraducido | null>(null);

  // ── Foto de la variante ────────────────────────────────
  protected readonly fotografiando = signal<Variante | null>(null);

  protected idProducto = 0;

  constructor(
    private route: ActivatedRoute,
    private productoService: ProductoService,
    private title: Title
  ) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.idProducto = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isInteger(this.idProducto) || this.idProducto <= 0) {
      this.cargando.set(false);
      this.noExiste.set(true);
      return;
    }

    this.cargando.set(true);
    this.errorCarga.set(null);
    forkJoin({
      producto: this.productoService.obtener(this.idProducto),
      variantes: this.productoService.listarVariantes(this.idProducto)
    }).subscribe({
      next: ({ producto, variantes }) => {
        this.producto.set(producto);
        this.variantes.set([...variantes].sort((a, b) => a.id - b.id));
        this.cargando.set(false);
        this.title.setTitle(`Variantes de ${producto.nombre} | Panel BAN Sports`);
      },
      error: (e: unknown) => {
        this.cargando.set(false);
        if (e instanceof HttpErrorResponse && e.status === 404) {
          this.noExiste.set(true);
        } else if (e instanceof HttpErrorResponse) {
          this.errorCarga.set(traducirErrorProducto(e));
        } else {
          this.errorCarga.set({ titulo: 'No pudimos cargar el producto', detalle: 'Intentá de nuevo.' });
        }
      }
    });
  }

  protected nivelStock(stock: number): 'cero' | 'bajo' | 'ok' {
    if (stock === 0) return 'cero';
    return stock <= STOCK_BAJO ? 'bajo' : 'ok';
  }

  protected descripcion(variante: Variante): string {
    return this.ordenados(variante)
      .map(([, valor]) => valor)
      .join(' · ');
  }

  // ── Alta / edición ─────────────────────────────────────

  /** Abre el alta: las opciones que ya usa el producto vienen cargadas, para sumarles valores. */
  nueva(): void {
    this.existentes = opcionesDe(this.variantes());
    this.opciones.set(this.existentes.map((o) => ({ ...o, valores: [...o.valores] })));
    this.borradores.set({});
    this.otraOpcion.set(null);
    this.errorOpciones.set(null);
    this.datosFilas.set({});
    this.abrirFormulario('nueva');
  }

  editar(variante: Variante): void {
    const opciones = opcionesDe(this.variantes());
    const seleccion: Record<string, string> = {};
    for (const opcion of opciones) {
      const actual = Object.entries(variante.atributos).find(([clave]) => claveDeOpcion(clave) === opcion.clave)?.[1];
      seleccion[opcion.clave] = actual ? (opcion.valores.find((v) => mismoTexto(v, actual)) ?? actual) : '';
    }
    this.opcionesEdicion.set(opciones);
    this.seleccion.set(seleccion);
    this.valoresNuevos.set({});
    this.errorAtributos.set(null);
    this.stock.reset(String(variante.stock));
    this.precio.reset(variante.precio != null ? formatearPrecio(variante.precio).replace('$ ', '') : '');
    this.abrirFormulario(variante);
  }

  cancelarFormulario(): void {
    if (!this.guardando()) {
      this.editando.set(null);
    }
  }

  /** Mensaje de un campo del formulario, solo si ya lo tocaron o se intentó guardar. */
  protected errorCampo(control: AbstractControl, campo: 'stock' | 'cantidad' | 'precio'): string | null {
    const intento = campo === 'cantidad' ? this.intentoAjuste() : this.intentoGuardar();
    if (!control.errors || (!control.touched && !intento)) return null;
    return this.mensajeError(control.errors, campo);
  }

  // ── Alta: opciones ─────────────────────────────────────

  protected tieneOpcion(clave: string): boolean {
    return this.opciones().some((o) => o.clave === clave);
  }

  /** Las opciones que ya usan variantes creadas no se pueden sacar desde acá. */
  protected opcionBloqueada(clave: string): boolean {
    return this.existentes.some((o) => o.clave === clave);
  }

  protected valorBloqueado(clave: string, valor: string): boolean {
    return this.existentes.some((o) => o.clave === clave && o.valores.some((v) => mismoTexto(v, valor)));
  }

  /** Botón de una opción sugerida: la agrega o la saca. */
  alternarOpcion(clave: string): void {
    this.errorOpciones.set(null);
    if (this.tieneOpcion(clave)) {
      if (!this.opcionBloqueada(clave)) {
        this.opciones.update((lista) => lista.filter((o) => o.clave !== clave));
      }
      return;
    }
    this.opciones.update((lista) => [...lista, { clave, valores: [] }]);
  }

  quitarOpcion(clave: string): void {
    if (!this.opcionBloqueada(clave)) {
      this.opciones.update((lista) => lista.filter((o) => o.clave !== clave));
    }
  }

  abrirOtraOpcion(): void {
    this.errorOpciones.set(null);
    this.otraOpcion.set('');
  }

  confirmarOtraOpcion(): void {
    const nombre = (this.otraOpcion() ?? '').trim();
    if (!nombre) {
      this.errorOpciones.set('Escribí el nombre de la opción, por ejemplo Cápsulas.');
      return;
    }
    if (nombre.length > LARGO_OPCION) {
      this.errorOpciones.set(`El nombre puede tener hasta ${LARGO_OPCION} caracteres.`);
      return;
    }
    const clave = claveDeOpcion(nombre);
    if (this.tieneOpcion(clave)) {
      this.errorOpciones.set(`Ya elegiste la opción ${nombreDeOpcion(clave)}.`);
      return;
    }
    this.opciones.update((lista) => [...lista, { clave, valores: [] }]);
    this.otraOpcion.set(null);
    this.errorOpciones.set(null);
  }

  protected borrador(clave: string): string {
    return this.borradores()[clave] ?? '';
  }

  escribirBorrador(clave: string, texto: string): void {
    this.borradores.update((b) => ({ ...b, [clave]: texto }));
    this.errorOpciones.set(null);
  }

  /** Enter en el campo de valor: lo agrega sin mandar el formulario. */
  enterEnValor(evento: Event, clave: string, campo?: HTMLInputElement): void {
    evento.preventDefault();
    this.agregarValor(clave, campo?.value ?? this.borrador(clave), campo);
  }

  /** Agrega el valor a la opción. Si se agregó, vacía el campo donde se escribió. */
  agregarValor(clave: string, texto: string, campo?: HTMLInputElement): void {
    const valor = texto.trim();
    const opcion = this.opciones().find((o) => o.clave === clave);
    if (!opcion || !valor) return;
    if (valor.length > LARGO_OPCION) {
      this.errorOpciones.set(`Cada valor puede tener hasta ${LARGO_OPCION} caracteres.`);
      return;
    }
    if (opcion.valores.some((v) => mismoTexto(v, valor))) {
      this.errorOpciones.set(`"${valor}" ya está en ${nombreDeOpcion(clave)}.`);
      return;
    }
    this.opciones.update((lista) => lista.map((o) => (o.clave === clave ? { ...o, valores: [...o.valores, valor] } : o)));
    this.borradores.update((b) => ({ ...b, [clave]: '' }));
    if (campo) campo.value = '';
    this.errorOpciones.set(null);
  }

  quitarValor(clave: string, valor: string): void {
    if (this.valorBloqueado(clave, valor)) return;
    this.opciones.update((lista) =>
      lista.map((o) => (o.clave === clave ? { ...o, valores: o.valores.filter((v) => v !== valor) } : o))
    );
  }

  // ── Alta: tabla de combinaciones ───────────────────────

  /** Stock, precio y si se crea. Por defecto se tildan las combinaciones con algún valor nuevo. */
  protected datos(fila: FilaCombinacion): DatosFila {
    return this.datosFilas()[fila.clave] ?? { incluir: !fila.existe && this.usaValorNuevo(fila), stock: '', precio: '' };
  }

  cambiarFila(fila: FilaCombinacion, cambios: Partial<DatosFila>): void {
    this.datosFilas.update((d) => ({ ...d, [fila.clave]: { ...this.datos(fila), ...cambios } }));
  }

  protected descripcionFila(fila: FilaCombinacion): string {
    return Object.values(fila.atributos).join(' · ');
  }

  protected valorDe(fila: FilaCombinacion, clave: string): string {
    return fila.atributos[clave] ?? '';
  }

  protected errorStockFila(fila: FilaCombinacion): string | null {
    const datos = this.datos(fila);
    if (!this.intentoGuardar() || !datos.incluir) return null;
    const errores = entero(0)(new FormControl(datos.stock));
    return errores ? this.mensajeError(errores, 'stock') : null;
  }

  protected errorPrecioFila(fila: FilaCombinacion): string | null {
    const datos = this.datos(fila);
    if (!this.intentoGuardar() || !datos.incluir) return null;
    const errores = precioValido(new FormControl(datos.precio));
    return errores ? this.mensajeError(errores, 'precio') : null;
  }

  /** Lo que falta para poder crear, en una línea. */
  protected errorAlta(): string | null {
    const sinValores = this.opciones().find((o) => o.valores.length === 0);
    if (sinValores) return `Cargá al menos un valor de ${nombreDeOpcion(sinValores.clave)} o sacá esa opción.`;
    if (this.filasACrear().length === 0) {
      return this.filas().every((f) => f.existe)
        ? 'Todas estas combinaciones ya existen. Sumá un valor nuevo para crear otra.'
        : 'Tildá al menos una combinación para crear.';
    }
    if (this.filasACrear().some((f) => this.errorStockFila(f) || this.errorPrecioFila(f))) {
      return 'Revisá el stock y el precio de las combinaciones marcadas.';
    }
    return null;
  }

  /** Crea todas las combinaciones tildadas. Si alguna falla, las otras quedan creadas. */
  crearVariantes(): void {
    this.intentoGuardar.set(true);
    this.errorForm.set(null);
    if (this.errorAlta()) return;

    const pedidos: Observable<Resultado>[] = this.filasACrear().map((fila) => {
      const datos = this.datos(fila);
      const cuerpo: VarianteRequest = {
        atributos: fila.atributos,
        stock: Number(datos.stock),
        precio: datos.precio.trim() ? leerPrecio(datos.precio) : null
      };
      return this.productoService.crearVariante(this.idProducto, cuerpo).pipe(
        map((variante): Resultado => ({ ok: true, variante })),
        catchError((error: unknown) => of<Resultado>({ ok: false, error }))
      );
    });

    this.guardando.set(true);
    forkJoin(pedidos).subscribe((resultados) => {
      this.guardando.set(false);
      const creadas = resultados.flatMap((r) => (r.ok ? [r.variante] : []));
      creadas.forEach((v) => this.reemplazar(v));
      const fallo = resultados.find((r) => !r.ok);
      if (!fallo) {
        this.editando.set(null);
        this.aviso.set(creadas.length === 1 ? 'Variante agregada.' : `Se agregaron ${creadas.length} variantes.`);
        return;
      }
      // Las creadas ya figuran como existentes en la tabla; quedan las que fallaron.
      const fallidas = resultados.length - creadas.length;
      const detalle = this.traducir(fallo.error).detalle;
      this.errorForm.set({
        titulo: creadas.length
          ? `Se agregaron ${creadas.length}, pero ${fallidas === 1 ? 'una no se pudo crear' : `${fallidas} no se pudieron crear`}`
          : 'No se pudieron crear las variantes',
        detalle
      });
    });
  }

  // ── Edición ────────────────────────────────────────────

  elegirValor(clave: string, valor: string): void {
    this.seleccion.update((s) => ({ ...s, [clave]: valor }));
    this.errorAtributos.set(null);
  }

  escribirValorNuevo(clave: string, texto: string): void {
    this.valoresNuevos.update((v) => ({ ...v, [clave]: texto }));
    this.errorAtributos.set(null);
  }

  guardarEdicion(): void {
    const actual = this.editando();
    if (actual === null || actual === 'nueva') return;
    this.intentoGuardar.set(true);
    this.errorForm.set(null);

    const atributos: Record<string, string> = {};
    for (const opcion of this.opcionesEdicion()) {
      const elegido = this.seleccion()[opcion.clave] ?? '';
      const valor = (elegido === NUEVO_VALOR ? (this.valoresNuevos()[opcion.clave] ?? '') : elegido).trim();
      if (elegido === NUEVO_VALOR && !valor) {
        this.errorAtributos.set(`Escribí el nuevo valor de ${nombreDeOpcion(opcion.clave)}.`);
        return;
      }
      if (valor.length > LARGO_OPCION) {
        this.errorAtributos.set(`Cada valor puede tener hasta ${LARGO_OPCION} caracteres.`);
        return;
      }
      if (valor) atributos[opcion.clave] = valor;
    }
    if (Object.keys(atributos).length === 0) {
      this.errorAtributos.set('Elegí al menos un valor.');
      return;
    }
    const repetida = this.variantes().find(
      (v) => v.id !== actual.id && claveCombinacion(v.atributos) === claveCombinacion(atributos)
    );
    if (repetida) {
      this.errorAtributos.set(`Ya hay una variante ${this.descripcion(repetida)}.`);
      return;
    }
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    const precioTexto = this.precio.value.trim();
    const cuerpo = { atributos, stock: Number(this.stock.value), precio: precioTexto ? leerPrecio(precioTexto) : null };
    this.guardando.set(true);
    this.productoService.actualizarVariante(this.idProducto, actual.id, cuerpo).subscribe({
      next: (variante) => {
        this.guardando.set(false);
        this.editando.set(null);
        this.reemplazar(variante);
        this.aviso.set('Variante actualizada.');
      },
      error: (e: unknown) => {
        this.guardando.set(false);
        this.errorForm.set(this.traducir(e));
      }
    });
  }

  // ── Ajuste de stock ────────────────────────────────────

  abrirAjuste(variante: Variante): void {
    this.ajustando.set(variante);
    this.operacion.set('AGREGAR');
    this.cantidad.reset('');
    this.intentoAjuste.set(false);
    this.errorAjuste.set(null);
  }

  cerrarAjuste(): void {
    if (!this.enviandoAjuste()) {
      this.ajustando.set(null);
    }
  }

  /** Stock que quedaría con la cantidad escrita (null si todavía no es un número válido). */
  protected stockResultante(): number | null {
    const variante = this.ajustando();
    if (!variante || this.cantidad.invalid) return null;
    const cantidad = Number(this.cantidad.value);
    return this.operacion() === 'AGREGAR' ? variante.stock + cantidad : variante.stock - cantidad;
  }

  confirmarAjuste(): void {
    const variante = this.ajustando();
    this.intentoAjuste.set(true);
    this.errorAjuste.set(null);
    if (!variante || this.cantidad.invalid) {
      this.cantidad.markAsTouched();
      return;
    }

    this.enviandoAjuste.set(true);
    this.productoService
      .ajustarStock(this.idProducto, variante.id, { operacion: this.operacion(), cantidad: Number(this.cantidad.value) })
      .subscribe({
        next: (actualizada) => {
          this.enviandoAjuste.set(false);
          this.ajustando.set(null);
          this.reemplazar(actualizada);
          this.aviso.set(`Stock de ${this.descripcion(actualizada)} actualizado: ${actualizada.stock} u.`);
        },
        error: (e: unknown) => {
          this.enviandoAjuste.set(false);
          this.errorAjuste.set(this.traducir(e));
        }
      });
  }

  // ── Baja ───────────────────────────────────────────────

  pedirBaja(variante: Variante): void {
    this.bajando.set(variante);
    this.errorBaja.set(null);
  }

  cerrarBaja(): void {
    if (!this.dandoDeBaja()) {
      this.bajando.set(null);
    }
  }

  confirmarBaja(): void {
    const variante = this.bajando();
    if (!variante) return;
    this.dandoDeBaja.set(true);
    this.errorBaja.set(null);
    this.productoService.eliminarVariante(this.idProducto, variante.id).subscribe({
      next: () => {
        this.dandoDeBaja.set(false);
        this.bajando.set(null);
        this.variantes.update((lista) => lista.filter((v) => v.id !== variante.id));
        if (this.editando() !== 'nueva' && (this.editando() as Variante | null)?.id === variante.id) {
          this.editando.set(null);
        }
        this.aviso.set('Variante dada de baja.');
      },
      error: (e: unknown) => {
        this.dandoDeBaja.set(false);
        this.errorBaja.set(this.traducir(e));
      }
    });
  }

  // ── Foto ───────────────────────────────────────────────

  abrirFoto(variante: Variante): void {
    this.aviso.set(null);
    this.fotografiando.set(variante);
  }

  cerrarFoto(): void {
    this.fotografiando.set(null);
  }

  /** El cargador subió o borró la foto: se refleja en la fila sin recargar la lista. */
  fotoCambiada(variante: Variante, url: string | null): void {
    this.variantes.update((lista) => lista.map((v) => (v.id === variante.id ? { ...v, imagenUrl: url } : v)));
  }

  cerrarModales(): void {
    this.cerrarAjuste();
    this.cerrarBaja();
    this.cerrarFoto();
  }

  // ── Internos ───────────────────────────────────────────

  private abrirFormulario(modo: Variante | 'nueva'): void {
    this.intentoGuardar.set(false);
    this.errorForm.set(null);
    this.aviso.set(null);
    this.editando.set(modo);
  }

  /** ¿La combinación usa algún valor que ninguna variante creada tiene? Sin variantes, todas son nuevas. */
  private usaValorNuevo(fila: FilaCombinacion): boolean {
    if (this.existentes.length === 0) return true;
    return Object.entries(fila.atributos).some(([clave, valor]) => !this.valorBloqueado(clave, valor));
  }

  private mensajeError(errores: ValidationErrors, campo: 'stock' | 'cantidad' | 'precio'): string | null {
    if (errores['requerido'] || errores['required']) {
      return { stock: 'Poné el stock.', cantidad: 'Poné una cantidad.', precio: 'Poné el precio.' }[campo];
    }
    if (errores['precioFormato']) return 'Usá solo números, puntos para los miles y coma para los centavos (por ejemplo 42.999,99).';
    if (errores['precioCero']) return 'El precio tiene que ser mayor a $ 0.';
    if (errores['entero']) return 'Solo números enteros, sin puntos ni comas.';
    if (errores['min']) return campo === 'cantidad' ? 'Tiene que ser al menos 1.' : 'No puede ser negativo.';
    return null;
  }

  /** Pone la variante devuelta por el back en la lista (la agrega si es nueva), sin recargar. */
  private reemplazar(variante: Variante): void {
    this.variantes.update((lista) => {
      const sinEsta = lista.filter((v) => v.id !== variante.id);
      return [...sinEsta, variante].sort((a, b) => a.id - b.id);
    });
  }

  private traducir(e: unknown): ErrorTraducido {
    return e instanceof HttpErrorResponse
      ? traducirErrorVariante(e)
      : { titulo: 'Algo salió mal', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' };
  }
}
