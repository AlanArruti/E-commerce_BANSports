import { Component, computed, effect, linkedSignal, ChangeDetectionStrategy } from '@angular/core';
import { NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { BehaviorSubject, catchError, combineLatest, forkJoin, map, of, startWith, switchMap } from 'rxjs';
import { PoteProducto, abreviarNombre } from '../../componente/pote-producto/pote-producto';
import { Producto } from '../../models/producto';
import { STOCK_BAJO, Variante, atributosOrdenados, etiquetaAtributo } from '../../models/variante';
import { PrecioPipe } from '../../pipes/precio.pipe';
import { ProductoService } from '../../services/producto.service';
import { ErrorTraducido, traducirErrorProducto } from '../../validaciones/errores-api';

interface EstadoDetalle {
  cargando: boolean;
  producto: Producto | null;
  variantes: Variante[];
  noEncontrado: boolean;
  error: ErrorTraducido | null;
}

/** Un valor elegible de un atributo (por ejemplo, sabor "Chocolate"). */
interface OpcionVariante {
  valor: string;
  elegida: boolean;
  /** Ninguna variante con este valor tiene stock (por ejemplo, el sabor Frutilla en cualquier presentación). */
  agotada: boolean;
}

interface GrupoVariante {
  clave: string;
  etiqueta: string;
  opciones: OpcionVariante[];
}

type Seleccion = Record<string, string>;

const CARGANDO: EstadoDetalle = { cargando: true, producto: null, variantes: [], noEncontrado: false, error: null };
const NO_ENCONTRADO: EstadoDetalle = { cargando: false, producto: null, variantes: [], noEncontrado: true, error: null };

/** La variante tiene exactamente los atributos elegidos. */
function coincide(variante: Variante, seleccion: Seleccion): boolean {
  const claves = Object.keys(seleccion);
  return (
    claves.length === Object.keys(variante.atributos).length &&
    claves.every((clave) => variante.atributos[clave] === seleccion[clave])
  );
}

/** Al entrar se elige la primera variante con stock (o la primera, si ninguna tiene). */
function varianteInicial(variantes: Variante[]): Variante | null {
  return variantes.find((variante) => variante.stock > 0) ?? variantes[0] ?? null;
}

@Component({
  selector: 'app-detalle-producto',
  imports: [NgClass, RouterLink, PoteProducto, PrecioPipe],
  templateUrl: './detalle-producto.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './detalle-producto.css'
})
export class DetalleProducto {
  protected readonly estado;
  protected readonly abreviatura;
  protected readonly seleccion;
  protected readonly varianteElegida;
  protected readonly grupos;
  protected readonly productoAgotado;
  protected readonly stockBajo;
  /** La imagen del back no cargó: se muestra el dibujo del pote. */
  protected readonly imagenFallo;

  /** Emite para volver a pedir el producto (botón "Reintentar"). */
  private readonly recargar$ = new BehaviorSubject<void>(undefined);

  constructor(
    private productoService: ProductoService,
    private route: ActivatedRoute,
    private title: Title
  ) {
    const id$ = this.route.paramMap.pipe(map((params) => params.get('id')));

    // Producto y variantes juntos. Si el id no es un número, ni se consulta: no existe.
    this.estado = toSignal(
      combineLatest([id$, this.recargar$]).pipe(
        switchMap(([texto]) => {
          const id = Number(texto);
          // Siempre un objeto nuevo: si quedara el mismo, el título no se actualiza entre dos productos inexistentes.
          if (!Number.isInteger(id) || id <= 0) {
            return of({ ...NO_ENCONTRADO });
          }
          return forkJoin([this.productoService.obtener(id), this.productoService.listarVariantes(id)]).pipe(
            map(([producto, variantes]): EstadoDetalle => ({ ...CARGANDO, cargando: false, producto, variantes })),
            catchError((e: unknown) => {
              if (e instanceof HttpErrorResponse && e.status === 404) {
                return of({ ...NO_ENCONTRADO });
              }
              return of<EstadoDetalle>({
                ...CARGANDO,
                cargando: false,
                error:
                  e instanceof HttpErrorResponse
                    ? traducirErrorProducto(e)
                    : { titulo: 'No pudimos cargar el producto', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
              });
            }),
            startWith(CARGANDO)
          );
        })
      ),
      { initialValue: CARGANDO }
    );

    this.abreviatura = computed(() => abreviarNombre(this.estado().producto?.nombre ?? ''));
    this.imagenFallo = linkedSignal({ source: () => this.estado().producto?.id, computation: () => false });

    // Atributos elegidos. Vuelve a la variante inicial cada vez que se carga otro producto.
    this.seleccion = linkedSignal<Seleccion>(() => ({ ...varianteInicial(this.estado().variantes)?.atributos }));

    this.varianteElegida = computed(
      () => this.estado().variantes.find((variante) => coincide(variante, this.seleccion())) ?? null
    );

    // Un grupo por atributo, con sus valores en el orden en que aparecen: Presentación [1 kg], Sabor [Chocolate, ...].
    this.grupos = computed<GrupoVariante[]>(() => {
      const variantes = this.estado().variantes;
      const seleccion = this.seleccion();
      const valoresPorClave = new Map<string, string[]>();
      for (const variante of variantes) {
        for (const [clave, valor] of atributosOrdenados(variante)) {
          const valores = valoresPorClave.get(clave) ?? [];
          if (!valores.includes(valor)) {
            valores.push(valor);
          }
          valoresPorClave.set(clave, valores);
        }
      }
      return [...valoresPorClave].map(([clave, valores]) => ({
        clave,
        etiqueta: etiquetaAtributo(clave),
        opciones: valores.map((valor) => ({
          valor,
          elegida: seleccion[clave] === valor,
          agotada: variantes.every((variante) => variante.atributos[clave] !== valor || variante.stock === 0)
        }))
      }));
    });

    this.productoAgotado = computed(() => this.estado().variantes.every((variante) => variante.stock === 0));

    this.stockBajo = computed(() => {
      const stock = this.varianteElegida()?.stock ?? 0;
      return stock > 0 && stock <= STOCK_BAJO;
    });

    // El título de la pestaña muestra el producto.
    effect(() => {
      const { producto, noEncontrado } = this.estado();
      if (producto) {
        this.title.setTitle(`${producto.nombre} | BAN Sports`);
      } else if (noEncontrado) {
        this.title.setTitle('Producto no encontrado | BAN Sports');
      }
    });
  }

  /**
   * Elige un valor de un atributo. Si con lo ya elegido esa combinación no existe,
   * se pasa a la primera variante que tenga ese valor (preferentemente con stock).
   */
  elegir(clave: string, valor: string): void {
    const variantes = this.estado().variantes;
    const deseada = { ...this.seleccion(), [clave]: valor };
    const conValor = variantes.filter((variante) => variante.atributos[clave] === valor);
    const variante =
      variantes.find((v) => coincide(v, deseada)) ?? conValor.find((v) => v.stock > 0) ?? conValor[0];
    if (variante) {
      this.seleccion.set({ ...variante.atributos });
    }
  }

  reintentar(): void {
    this.recargar$.next();
  }
}
