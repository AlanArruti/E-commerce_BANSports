import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject } from '@angular/core';
import { DolarService } from '../../services/dolar.service';
import { PrecioPipe } from '../../pipes/precio.pipe';
import { formatearFecha, formatearVariacion, nombreDolar, tendencia } from '../../models/dolar';

/** Cada cuánto se vuelve a pedir la cotización mientras la página está abierta. */
const INTERVALO_CONSULTA_MS = 10 * 60_000;

/**
 * Cotización del dólar en la franja de arriba de la tienda (RF01). Al pasar el mouse o
 * enfocarla muestra compra, venta y con qué dólar se ajustaron los precios.
 * Si el back todavía no tiene cotización (o falla), no se muestra nada.
 */
@Component({
  selector: 'app-cotizacion-dolar',
  imports: [PrecioPipe],
  templateUrl: './cotizacion-dolar.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './cotizacion-dolar.css'
})
export class CotizacionDolarFranja {
  protected readonly dolarService = inject(DolarService);

  protected readonly nombre = computed(() => {
    const c = this.dolarService.cotizacion();
    return c ? nombreDolar(c.casa) : '';
  });

  protected readonly variacion = computed(() => {
    const v = this.dolarService.cotizacion()?.variacionPorcentaje;
    return v === null || v === undefined ? null : { texto: formatearVariacion(v), tendencia: tendencia(v) };
  });

  protected readonly actualizado = computed(() =>
    formatearFecha(this.dolarService.cotizacion()?.fechaActualizacion ?? null)
  );

  protected readonly ultimoAjuste = computed(() =>
    formatearFecha(this.dolarService.cotizacion()?.fechaUltimoAjuste ?? null)
  );

  constructor() {
    this.dolarService.refrescar();
    const intervalo = setInterval(() => this.dolarService.refrescar(), INTERVALO_CONSULTA_MS);
    inject(DestroyRef).onDestroy(() => clearInterval(intervalo));
  }
}
