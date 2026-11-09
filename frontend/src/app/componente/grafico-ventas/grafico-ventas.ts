import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnDestroy,
  computed,
  input,
  signal,
  viewChild
} from '@angular/core';
import { VentaDiaria } from '../../models/metricas-admin';
import { formatearPrecio } from '../../pipes/precio.pipe';

const ALTO = 260;
const MARGEN = { arriba: 16, derecha: 16, abajo: 32, izquierda: 64 };
/** Ancho mínimo por etiqueta del eje X, para que las fechas no se pisen. */
const ANCHO_ETIQUETA = 64;

const FORMATO_DECIMAL = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 1 });

/** Montos cortos para el eje: 150000 → "$ 150 mil", 1200000 → "$ 1,2 M". */
function precioCorto(valor: number): string {
  if (valor >= 1_000_000) {
    return `$ ${FORMATO_DECIMAL.format(valor / 1_000_000)} M`;
  }
  if (valor >= 1_000) {
    return `$ ${FORMATO_DECIMAL.format(valor / 1_000)} mil`;
  }
  return `$ ${FORMATO_DECIMAL.format(valor)}`;
}
const FORMATO_EJE_X = new Intl.DateTimeFormat('es-AR', { day: 'numeric', month: 'short' });
const FORMATO_TOOLTIP = new Intl.DateTimeFormat('es-AR', { weekday: 'long', day: 'numeric', month: 'long' });

/** "2026-11-09" → fecha local (sin el corrimiento de zona horaria de new Date(string)). */
function aFecha(texto: string): Date {
  const [anio, mes, dia] = texto.split('-').map(Number);
  return new Date(anio, mes - 1, dia);
}

/** Marcas "redondas" del eje Y (0, 2.500, 5.000...) que cubren el máximo. */
function marcasEjeY(maximo: number, cantidad = 4): number[] {
  if (maximo <= 0) {
    return [0, 1];
  }
  const crudo = maximo / cantidad;
  const potencia = 10 ** Math.floor(Math.log10(crudo));
  const paso = [1, 2, 2.5, 5, 10].map((m) => m * potencia).find((p) => p >= crudo) ?? crudo;
  const pasos = Math.ceil(maximo / paso);
  return Array.from({ length: pasos + 1 }, (_, i) => i * paso);
}

interface Punto {
  x: number;
  y: number;
  venta: VentaDiaria;
}

/**
 * Gráfico de área con las ventas día por día. Al pasar el mouse (o moverse con
 * las flechas del teclado) muestra el total y los pedidos de cada día.
 */
@Component({
  selector: 'app-grafico-ventas',
  templateUrl: './grafico-ventas.html',
  styleUrl: './grafico-ventas.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GraficoVentas implements AfterViewInit, OnDestroy {
  readonly ventas = input.required<VentaDiaria[]>();

  private readonly contenedor = viewChild.required<ElementRef<HTMLElement>>('contenedor');
  private observador?: ResizeObserver;

  protected readonly alto = ALTO;
  protected readonly margen = MARGEN;
  protected readonly ancho = signal(720);
  protected readonly activo = signal<number | null>(null);

  protected readonly sinVentas = computed(() => this.ventas().every((v) => v.total === 0));

  private readonly marcasY = computed(() => marcasEjeY(Math.max(...this.ventas().map((v) => v.total), 0)));
  private readonly maximoY = computed(() => this.marcasY()[this.marcasY().length - 1]);
  private readonly base = ALTO - MARGEN.abajo;

  protected readonly puntos = computed<Punto[]>(() => {
    const ventas = this.ventas();
    const anchoPlot = this.ancho() - MARGEN.izquierda - MARGEN.derecha;
    const paso = ventas.length > 1 ? anchoPlot / (ventas.length - 1) : 0;
    const altoPlot = this.base - MARGEN.arriba;
    return ventas.map((venta, i) => ({
      x: MARGEN.izquierda + i * paso,
      y: this.base - (venta.total / this.maximoY()) * altoPlot,
      venta
    }));
  });

  protected readonly linea = computed(() =>
    this.puntos()
      .map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x.toFixed(1)},${p.y.toFixed(1)}`)
      .join(' ')
  );

  protected readonly area = computed(() => {
    const puntos = this.puntos();
    if (puntos.length === 0) {
      return '';
    }
    const ultimo = puntos[puntos.length - 1];
    return `${this.linea()} L${ultimo.x.toFixed(1)},${this.base} L${puntos[0].x.toFixed(1)},${this.base} Z`;
  });

  protected readonly lineasY = computed(() => {
    const altoPlot = this.base - MARGEN.arriba;
    return this.marcasY().map((valor) => ({
      y: this.base - (valor / this.maximoY()) * altoPlot,
      etiqueta: precioCorto(valor)
    }));
  });

  /** Etiquetas del eje X: se cuentan desde hoy hacia atrás para que hoy siempre tenga la suya. */
  protected readonly etiquetasX = computed(() => {
    const puntos = this.puntos();
    const anchoPlot = this.ancho() - MARGEN.izquierda - MARGEN.derecha;
    const entran = Math.max(2, Math.floor(anchoPlot / ANCHO_ETIQUETA));
    const salto = Math.ceil(puntos.length / entran);
    const etiquetas: { x: number; texto: string }[] = [];
    for (let i = puntos.length - 1; i >= 0; i -= salto) {
      etiquetas.push({ x: puntos[i].x, texto: FORMATO_EJE_X.format(aFecha(puntos[i].venta.fecha)) });
    }
    return etiquetas;
  });

  protected readonly puntoActivo = computed(() => {
    const i = this.activo();
    return i === null ? null : (this.puntos()[i] ?? null);
  });

  protected readonly resumenAccesible = computed(() => {
    const ventas = this.ventas();
    const total = ventas.reduce((suma, v) => suma + v.total, 0);
    const mejor = ventas.reduce((a, b) => (b.total > a.total ? b : a), ventas[0]);
    if (!mejor || total === 0) {
      return `Ventas de los últimos ${ventas.length} días: sin ventas.`;
    }
    return (
      `Ventas de los últimos ${ventas.length} días: ${formatearPrecio(total)} en total. ` +
      `El mejor día fue el ${FORMATO_TOOLTIP.format(aFecha(mejor.fecha))}, con ${formatearPrecio(mejor.total)}.`
    );
  });

  ngAfterViewInit(): void {
    const elemento = this.contenedor().nativeElement;
    this.ancho.set(elemento.clientWidth || 720);
    this.observador = new ResizeObserver(([entrada]) => {
      this.ancho.set(Math.max(280, Math.round(entrada.contentRect.width)));
    });
    this.observador.observe(elemento);
  }

  ngOnDestroy(): void {
    this.observador?.disconnect();
  }

  protected fechaLarga(fecha: string): string {
    return FORMATO_TOOLTIP.format(aFecha(fecha));
  }

  protected precio(valor: number): string {
    return formatearPrecio(valor);
  }

  /** Posición del tooltip: centrado sobre el punto, sin salirse del gráfico. */
  protected izquierdaTooltip(x: number): number {
    const mitad = 90;
    return Math.min(Math.max(x, mitad), this.ancho() - mitad);
  }

  protected alMover(evento: PointerEvent): void {
    const puntos = this.puntos();
    if (puntos.length === 0) {
      return;
    }
    const caja = (evento.currentTarget as SVGElement).getBoundingClientRect();
    const x = evento.clientX - caja.left;
    const paso = puntos.length > 1 ? puntos[1].x - puntos[0].x : 1;
    const indice = Math.round((x - MARGEN.izquierda) / paso);
    this.activo.set(Math.min(Math.max(indice, 0), puntos.length - 1));
  }

  protected alSalir(): void {
    this.activo.set(null);
  }

  protected alTeclear(evento: KeyboardEvent): void {
    const ultimo = this.puntos().length - 1;
    const actual = this.activo() ?? ultimo;
    const destinos: Record<string, number> = {
      ArrowLeft: actual - 1,
      ArrowRight: actual + 1,
      Home: 0,
      End: ultimo
    };
    if (evento.key in destinos) {
      evento.preventDefault();
      this.activo.set(Math.min(Math.max(destinos[evento.key], 0), ultimo));
    }
  }

  protected alEnfocar(): void {
    if (this.activo() === null) {
      this.activo.set(this.puntos().length - 1);
    }
  }
}
