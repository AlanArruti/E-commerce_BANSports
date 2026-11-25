import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { DolarService } from '../../services/dolar.service';
import { PrecioPipe } from '../../pipes/precio.pipe';
import {
  ActualizacionDolar,
  AjustePrecios,
  formatearFecha,
  formatearVariacion,
  nombreDolar,
  tendencia
} from '../../models/dolar';
import { ErrorTraducido, traducirErrorDolar } from '../../validaciones/errores-api';

/**
 * Panel del admin para el RF01: cotización actual del dólar, con qué valor se ajustaron los
 * precios, cuánto falta para el próximo ajuste, historial de ajustes y actualización manual.
 */
@Component({
  selector: 'app-admin-dolar',
  imports: [PrecioPipe],
  templateUrl: './admin-dolar.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './admin-dolar.css'
})
export class AdminDolar implements OnInit {
  private readonly dolarService = inject(DolarService);

  protected readonly cotizacion = this.dolarService.cotizacion;
  protected readonly historial = signal<AjustePrecios[]>([]);
  protected readonly cargando = signal(true);
  protected readonly errorCarga = signal<ErrorTraducido | null>(null);

  protected readonly actualizando = signal(false);
  protected readonly resultado = signal<ActualizacionDolar | null>(null);
  protected readonly errorAccion = signal<ErrorTraducido | null>(null);
  /** Paso de confirmación de "Aplicar igual" (salto mayor al máximo). */
  protected readonly confirmandoForzar = signal(false);

  protected readonly nombre = computed(() => {
    const c = this.cotizacion();
    return c ? nombreDolar(c.casa) : 'Dólar';
  });

  protected readonly variacion = computed(() => {
    const c = this.cotizacion();
    if (!c || c.variacionPorcentaje === null) {
      return null;
    }
    const absoluta = Math.abs(c.variacionPorcentaje);
    return {
      texto: formatearVariacion(c.variacionPorcentaje),
      tendencia: tendencia(c.variacionPorcentaje),
      // Barra: cuánto falta para el próximo ajuste (100% = llega al mínimo).
      progreso: Math.min(100, (absoluta / c.ajusteMinimoPorcentaje) * 100),
      porcentajeExacto: Math.round(c.variacionPorcentaje * 10) / 10,
      superaMaximo: absoluta > c.variacionMaximaPorcentaje
    };
  });

  protected readonly historialFilas = computed(() =>
    this.historial().map((a, i, lista) => ({
      ...a,
      fecha: formatearFecha(a.fechaAjuste),
      // La fila más vieja con 0% es la cotización que quedó como referencia inicial.
      esBase: a.porcentajeAjuste === 0 && a.productosAjustados === 0,
      variacion: formatearVariacion(a.porcentajeAjuste),
      tendencia: tendencia(a.porcentajeAjuste),
      clave: `${a.fechaAjuste}-${i}-${lista.length}`
    }))
  );

  protected readonly formatearFecha = formatearFecha;
  protected readonly formatearVariacion = formatearVariacion;

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.errorCarga.set(null);
    forkJoin({ cotizacion: this.dolarService.cargar(), historial: this.dolarService.historial() }).subscribe({
      next: ({ historial }) => {
        this.historial.set(historial);
        this.cargando.set(false);
      },
      error: (e: unknown) => {
        this.cargando.set(false);
        this.errorCarga.set(this.traducir(e));
      }
    });
  }

  actualizar(forzar = false): void {
    if (this.actualizando()) {
      return;
    }
    this.actualizando.set(true);
    this.errorAccion.set(null);
    this.resultado.set(null);
    this.dolarService.actualizar(forzar).subscribe({
      next: (r) => {
        this.resultado.set(r);
        this.actualizando.set(false);
        this.confirmandoForzar.set(false);
        if (r.resultado === 'AJUSTADO' || r.resultado === 'BASE') {
          this.dolarService.historial().subscribe({ next: (h) => this.historial.set(h), error: () => {} });
        }
      },
      error: (e: unknown) => {
        this.actualizando.set(false);
        this.confirmandoForzar.set(false);
        this.errorAccion.set(this.traducir(e));
      }
    });
  }

  private traducir(e: unknown): ErrorTraducido {
    return e instanceof HttpErrorResponse
      ? traducirErrorDolar(e)
      : { titulo: 'No pudimos cargar la cotización', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' };
  }
}
