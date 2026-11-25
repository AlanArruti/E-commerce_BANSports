import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { CotizacionDolar } from '../../models/dolar';
import { AdminDolar } from './admin-dolar';

const URL = `${environment.apiUrl}/dolar`;

function cotizacion(cambios: Partial<CotizacionDolar> = {}): CotizacionDolar {
  return {
    casa: 'oficial',
    compra: 1565,
    venta: 1615,
    fechaActualizacion: '2026-11-24T18:00:00Z',
    fechaConsulta: '2026-11-24T18:05:00Z',
    cotizacionReferencia: 1607,
    fechaUltimoAjuste: '2026-11-20T13:30:00Z',
    variacionPorcentaje: 0.5,
    ajusteMinimoPorcentaje: 1,
    variacionMaximaPorcentaje: 20,
    ...cambios
  };
}

describe('AdminDolar (panel del dólar)', () => {
  let fixture: ComponentFixture<AdminDolar>;
  let http: HttpTestingController;
  let pantalla: HTMLElement;

  function cargar(c: CotizacionDolar): void {
    http.expectOne(URL).flush(c);
    http.expectOne(`${URL}/historial`).flush([
      { casa: 'oficial', venta: 1607, porcentajeAjuste: 1.2, productosAjustados: 48, fechaAjuste: '2026-11-20T13:30:00Z' },
      { casa: 'oficial', venta: 1588, porcentajeAjuste: 0, productosAjustados: 0, fechaAjuste: '2026-11-12T12:00:00Z' }
    ]);
    fixture.detectChanges();
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AdminDolar],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    fixture = TestBed.createComponent(AdminDolar);
    http = TestBed.inject(HttpTestingController);
    pantalla = fixture.nativeElement;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('muestra la cotización, la referencia, la variación y el historial', () => {
    cargar(cotizacion());

    const texto = pantalla.textContent ?? '';
    expect(texto).toContain('$ 1.615');
    expect(texto).toContain('$ 1.607');
    expect(texto).toContain('▲ 0,5%');
    expect(texto).toContain('Referencia inicial');
    expect(pantalla.querySelectorAll('tbody tr').length).toBe(2);
    // A mitad de camino del 1%: la barra va por la mitad.
    expect(pantalla.querySelector('[role="progressbar"]')?.getAttribute('aria-valuenow')).toBe('50');
    expect(pantalla.querySelector('.alerta-aviso')).toBeNull();
  });

  it('con un salto mayor al máximo pide confirmación y manda forzar=true', () => {
    cargar(cotizacion({ venta: 2200, variacionPorcentaje: 36.9 }));

    const aplicar = Array.from(pantalla.querySelectorAll('button')).find((b) => b.textContent?.includes('Aplicar igual'));
    expect(aplicar).toBeDefined();
    aplicar!.click();
    fixture.detectChanges();

    const confirmar = Array.from(pantalla.querySelectorAll('button')).find((b) => b.textContent?.includes('Sí, ajustar'));
    confirmar!.click();

    const pedido = http.expectOne((r) => r.url === `${URL}/actualizar`);
    expect(pedido.request.method).toBe('POST');
    expect(pedido.request.params.get('forzar')).toBe('true');
    pedido.flush({
      cotizacion: cotizacion({ venta: 2200, cotizacionReferencia: 2200, variacionPorcentaje: 0 }),
      resultado: 'AJUSTADO',
      porcentajeAjuste: 36.9,
      productosAjustados: 48,
      mensaje: 'Se ajustaron los precios un 36.9%.'
    });
    http.expectOne(`${URL}/historial`).flush([]);
    fixture.detectChanges();

    expect(pantalla.textContent).toContain('Precios ajustados: 48 productos');
    expect(pantalla.querySelector('.alerta-aviso')).toBeNull();
  });

  it('si el back no tiene cotización todavía, muestra el motivo y permite reintentar', () => {
    // El historial primero: cuando falla la cotización, forkJoin cancela lo que quede pendiente.
    http.expectOne(`${URL}/historial`).flush([]);
    http.expectOne(URL).flush(
      { status: 503, message: 'Todavía no tenemos la cotización del dólar. Probá de nuevo en unos minutos.' },
      { status: 503, statusText: 'Service Unavailable' }
    );
    fixture.detectChanges();

    expect(pantalla.textContent).toContain('Todavía no tenemos la cotización del dólar');
    expect(Array.from(pantalla.querySelectorAll('button')).some((b) => b.textContent?.includes('Reintentar'))).toBeTrue();
  });
});
