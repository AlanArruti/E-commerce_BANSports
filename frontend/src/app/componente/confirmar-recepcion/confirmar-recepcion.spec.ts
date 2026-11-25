import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EstadoPedido, Pedido } from '../../models/pedido';
import { ConfirmarRecepcion } from './confirmar-recepcion';

function pedido(estado: EstadoPedido, extra: Partial<Pedido> = {}): Pedido {
  return {
    id: 7,
    uuidPedido: 'uuid-7',
    fechaCreacion: '2026-11-20T10:00:00',
    items: [],
    costoEnvio: 0,
    total: 1000,
    estado,
    direccionEnvio: null,
    envio: null,
    cliente: null,
    factura: null,
    confirmacionRecepcion: null,
    ...extra
  };
}

describe('ConfirmarRecepcion (RF03)', () => {
  let fixture: ComponentFixture<ConfirmarRecepcion>;
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

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ConfirmarRecepcion],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ConfirmarRecepcion);
    pantalla = fixture.nativeElement;
    emitido = null;
    fixture.componentInstance.confirmado.subscribe((p) => (emitido = p));
  });

  afterEach(() => backend.verify());

  it('no muestra nada si el pedido no está entregado', () => {
    mostrar(pedido('DESPACHADO'));
    expect(pantalla.querySelector('.recepcion')).toBeNull();
  });

  it('"Lo recibí" lo manda al back y avisa cómo quedó el pedido', () => {
    mostrar(pedido('ENTREGADO'));
    boton('Lo recibí').click();

    const pedidoHttp = backend.expectOne('/api/v1/pedido/7/recepcion');
    expect(pedidoHttp.request.method).toBe('PATCH');
    expect(pedidoHttp.request.body).toEqual({ recibido: true, comentario: null });
    const respuesta = pedido('ENTREGADO', { confirmacionRecepcion: 'RECIBIDO' });
    pedidoHttp.flush(respuesta);

    expect(emitido).toEqual(respuesta);
  });

  it('"No lo recibí" pide un comentario opcional y lo manda sin espacios de más', () => {
    mostrar(pedido('ENTREGADO'));
    boton('No lo recibí').click();
    fixture.detectChanges();

    const area = pantalla.querySelector<HTMLTextAreaElement>('textarea')!;
    area.value = '  No vino nadie  ';
    area.dispatchEvent(new Event('input'));
    boton('Avisar a la tienda').click();

    const pedidoHttp = backend.expectOne('/api/v1/pedido/7/recepcion');
    expect(pedidoHttp.request.body).toEqual({ recibido: false, comentario: 'No vino nadie' });
    pedidoHttp.flush(pedido('ENTREGADO', { confirmacionRecepcion: 'NO_RECIBIDO' }));
  });

  it('después de "No lo recibí" ofrece "Ya lo recibí", y con "Lo recibí" ya no hay botones', () => {
    mostrar(pedido('ENTREGADO', { confirmacionRecepcion: 'NO_RECIBIDO', comentarioRecepcion: 'No llegó' }));
    expect(pantalla.textContent).toContain('Nos avisaste que no te llegó');
    expect(pantalla.textContent).toContain('No llegó');
    expect(boton('Ya lo recibí')).toBeTruthy();

    mostrar(pedido('ENTREGADO', { confirmacionRecepcion: 'RECIBIDO' }));
    expect(pantalla.textContent).toContain('Confirmaste que lo recibiste');
    expect(pantalla.querySelector('button')).toBeNull();
  });

  it('si el back rechaza, muestra el motivo', () => {
    mostrar(pedido('ENTREGADO'));
    boton('Lo recibí').click();
    backend.expectOne('/api/v1/pedido/7/recepcion').flush(
      { message: 'Ya confirmaste que recibiste el pedido #7.' },
      { status: 409, statusText: 'Conflict' }
    );
    fixture.detectChanges();

    expect(pantalla.querySelector('[role="alert"]')?.textContent).toContain('Ya respondiste por este pedido');
    expect(emitido).toBeNull();
  });
});
