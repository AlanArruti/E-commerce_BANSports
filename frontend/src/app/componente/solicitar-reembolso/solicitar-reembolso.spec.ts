import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { EstadoPedido, Pedido, puedeSolicitarReembolso } from '../../models/pedido';
import { SolicitarReembolso } from './solicitar-reembolso';

const EN_5_DIAS = new Date(Date.now() + 5 * 86_400_000).toISOString();
const HACE_1_DIA = new Date(Date.now() - 86_400_000).toISOString();

function pedido(estado: EstadoPedido, extra: Partial<Pedido> = {}): Pedido {
  return {
    id: 7,
    uuidPedido: 'uuid-7',
    fechaCreacion: '2026-11-20T10:00:00',
    items: [],
    costoEnvio: 0,
    total: 15000,
    estado,
    direccionEnvio: null,
    envio: null,
    cliente: null,
    factura: null,
    fechaLimiteReembolso: EN_5_DIAS,
    estadoReembolso: null,
    ...extra
  };
}

describe('Reembolso de un pedido entregado (RF05)', () => {
  it('solo se puede pedir con el pedido entregado, una vez y dentro del plazo', () => {
    expect(puedeSolicitarReembolso(pedido('ENTREGADO'))).toBeTrue();
    expect(puedeSolicitarReembolso(pedido('DESPACHADO'))).toBeFalse();
    expect(puedeSolicitarReembolso(pedido('ENTREGADO', { estadoReembolso: 'SOLICITADO' }))).toBeFalse();
    expect(puedeSolicitarReembolso(pedido('ENTREGADO', { fechaLimiteReembolso: HACE_1_DIA }))).toBeFalse();
    expect(puedeSolicitarReembolso(pedido('ENTREGADO', { fechaLimiteReembolso: null }))).toBeFalse();
  });

  describe('SolicitarReembolso (componente)', () => {
    let fixture: ComponentFixture<SolicitarReembolso>;
    let backend: HttpTestingController;
    let pantalla: HTMLElement;
    let emitido: Pedido | null;

    function mostrar(p: Pedido): void {
      fixture.componentRef.setInput('pedido', p);
      fixture.detectChanges();
    }

    function boton(texto: string): HTMLButtonElement {
      const encontrado = [...pantalla.querySelectorAll('button')].find((b) => b.textContent!.trim() === texto);
      expect(encontrado).withContext(`botón "${texto}"`).toBeDefined();
      return encontrado!;
    }

    function escribir(texto: string): void {
      const area = pantalla.querySelector('textarea')!;
      area.value = texto;
      area.dispatchEvent(new Event('input'));
      fixture.detectChanges();
    }

    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [SolicitarReembolso],
        providers: [provideHttpClient(), provideHttpClientTesting()]
      });
      fixture = TestBed.createComponent(SolicitarReembolso);
      backend = TestBed.inject(HttpTestingController);
      pantalla = fixture.nativeElement;
      emitido = null;
      fixture.componentInstance.solicitado.subscribe((p) => (emitido = p));
    });

    afterEach(() => backend.verify());

    it('no muestra nada si el pedido no está entregado o se venció el plazo sin pedirlo', () => {
      mostrar(pedido('DESPACHADO'));
      expect(pantalla.textContent!.trim()).toBe('');

      mostrar(pedido('ENTREGADO', { fechaLimiteReembolso: HACE_1_DIA }));
      expect(pantalla.textContent!.trim()).toBe('');
    });

    it('pide un motivo de al menos 10 caracteres antes de mandarlo', () => {
      mostrar(pedido('ENTREGADO'));
      boton('Solicitar reembolso').click();
      fixture.detectChanges();

      escribir('corto');
      boton('Enviar pedido de reembolso').click();
      fixture.detectChanges();

      expect(pantalla.textContent).toContain('al menos 10 caracteres');
      backend.expectNone(`${environment.apiUrl}/pedido/7/reembolso`);
    });

    it('manda el motivo y avisa con el pedido actualizado', () => {
      mostrar(pedido('ENTREGADO'));
      boton('Solicitar reembolso').click();
      fixture.detectChanges();
      escribir('  El talle no me quedó bien  ');
      boton('Enviar pedido de reembolso').click();

      const req = backend.expectOne(`${environment.apiUrl}/pedido/7/reembolso`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ motivo: 'El talle no me quedó bien' });
      const actualizado = pedido('ENTREGADO', { estadoReembolso: 'SOLICITADO' });
      req.flush(actualizado);

      expect(emitido).toEqual(actualizado);
    });

    it('muestra cómo lo resolvió la tienda', () => {
      mostrar(pedido('ENTREGADO', { estadoReembolso: 'SOLICITADO' }));
      expect(pantalla.textContent).toContain('La tienda lo está revisando');

      mostrar(pedido('ENTREGADO', { estadoReembolso: 'APROBADO', tipoReembolso: 'AUTOMATICO', montoReembolsado: 15000 }));
      expect(pantalla.textContent).toContain('Te devolvimos $ 15.000 en Mercado Pago');

      mostrar(pedido('ENTREGADO', { estadoReembolso: 'RECHAZADO', respuestaReembolso: 'El producto se usó.' }));
      expect(pantalla.textContent).toContain('Reembolso rechazado');
      expect(pantalla.textContent).toContain('El producto se usó.');
    });

    it('si el back dice que ya lo pidió, lo explica', () => {
      mostrar(pedido('ENTREGADO'));
      boton('Solicitar reembolso').click();
      fixture.detectChanges();
      escribir('Me arrepentí de la compra');
      boton('Enviar pedido de reembolso').click();

      backend
        .expectOne(`${environment.apiUrl}/pedido/7/reembolso`)
        .flush({ status: 409, message: 'Ya pediste el reembolso del pedido #7. Lo estamos revisando.' }, { status: 409, statusText: 'Conflict' });
      fixture.detectChanges();

      expect(pantalla.querySelector('[role="alert"]')!.textContent).toContain('Ya pediste el reembolso');
      expect(emitido).toBeNull();
    });
  });
});
