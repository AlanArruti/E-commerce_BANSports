import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { Producto } from '../../models/producto';
import { Variante } from '../../models/variante';
import { AdminVariantes } from './admin-variantes';

const ID_PRODUCTO = 7;
const URL_PRODUCTO = `/api/v1/producto/${ID_PRODUCTO}`;

const PRODUCTO: Producto = {
  id: ID_PRODUCTO,
  uuidProducto: 'uuid-whey',
  nombre: 'Whey Protein',
  descripcion: 'Proteína de suero',
  precioBase: 30000,
  descuento: null,
  precioFinal: 30000,
  activo: true,
  imagenUrl: null,
  categoria: null,
  variantes: [],
  promedioResenas: 0,
  cantidadResenas: 0
};

const CHOCOLATE: Variante = {
  id: 1,
  uuidVariante: 'uuid-1',
  atributos: { sabor: 'Chocolate', peso: '1 kg' },
  stock: 4,
  activo: true,
  idProducto: ID_PRODUCTO,
  precio: null,
  precioFinal: 30000
};

describe('AdminVariantes (formulario de variantes)', () => {
  let fixture: ComponentFixture<AdminVariantes>;
  let backend: HttpTestingController;
  let pantalla: HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AdminVariantes],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: String(ID_PRODUCTO) }) } } }
      ]
    });
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(AdminVariantes);
    pantalla = fixture.nativeElement;
    fixture.detectChanges();

    // Al abrir la pantalla pide el producto y sus variantes.
    backend.expectOne(URL_PRODUCTO).flush(PRODUCTO);
    backend.expectOne(`${URL_PRODUCTO}/variante`).flush([CHOCOLATE]);
    fixture.detectChanges();
  });

  afterEach(() => backend.verify());

  // ── Ayudas para usar la pantalla como un admin ───────

  function apretar(selector: string): void {
    const boton = pantalla.querySelector<HTMLButtonElement>(selector);
    expect(boton).withContext(`no está el botón ${selector}`).not.toBeNull();
    boton!.click();
    fixture.detectChanges();
  }

  function escribir(selector: string, texto: string): void {
    const input = pantalla.querySelector<HTMLInputElement>(selector)!;
    input.value = texto;
    input.dispatchEvent(new Event('input'));
    input.dispatchEvent(new Event('blur'));
    fixture.detectChanges();
  }

  function enviar(selector: string): void {
    pantalla.querySelector<HTMLFormElement>(selector)!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  function errores(): string[] {
    return [...pantalla.querySelectorAll('.msj-error')].map((e) => e.textContent!.trim());
  }

  /** Abre "Nueva variante" y carga dos atributos. */
  function cargarNueva(atributos: [string, string][], stock: string): void {
    apretar('.btn-nuevo');
    for (let i = 1; i < atributos.length; i++) apretar('.agregar-atributo');
    atributos.forEach(([clave, valor], i) => {
      escribir(`#atr-clave-${i}`, clave);
      escribir(`#atr-valor-${i}`, valor);
    });
    escribir('#var-stock', stock);
  }

  // ── Tests ───────────────────────────────────────────

  it('muestra las variantes que trae el back', () => {
    expect(pantalla.textContent).toContain('Chocolate');
  });

  it('no deja guardar dos atributos con el mismo nombre (sin importar mayúsculas)', () => {
    cargarNueva(
      [
        ['Sabor', 'Chocolate'],
        ['sabor', 'Vainilla']
      ],
      '10'
    );

    enviar('form.tarjeta-form');

    expect(errores()).toContain('El atributo "sabor" está repetido.');
    backend.expectNone(`${URL_PRODUCTO}/variante`); // no llegó a pedirle nada al back
  });

  it('no acepta stock negativo', () => {
    cargarNueva([['sabor', 'Frutilla']], '-3');

    enviar('form.tarjeta-form');

    expect(errores()).toContain('Solo números enteros, sin puntos ni comas.');
    backend.expectNone(`${URL_PRODUCTO}/variante`);
  });

  it('no acepta stock con decimales ni vacío', () => {
    cargarNueva([['sabor', 'Frutilla']], '2,5');
    enviar('form.tarjeta-form');
    expect(errores()).toContain('Solo números enteros, sin puntos ni comas.');

    escribir('#var-stock', '');
    expect(errores()).toContain('Poné el stock.');
    backend.expectNone(`${URL_PRODUCTO}/variante`);
  });

  it('pide nombre y valor de cada atributo', () => {
    cargarNueva([['', '']], '5');

    enviar('form.tarjeta-form');

    expect(errores()).toEqual(jasmine.arrayContaining(['Poné el nombre del atributo.', 'Poné el valor.']));
  });

  it('con datos válidos manda la variante con las claves en minúscula y el stock como número', () => {
    cargarNueva(
      [
        ['Sabor', ' Vainilla '],
        ['Peso', '2 kg']
      ],
      '12'
    );
    escribir('#var-precio', '45.999,50');

    enviar('form.tarjeta-form');

    const pedido = backend.expectOne(`${URL_PRODUCTO}/variante`);
    expect(pedido.request.method).toBe('POST');
    expect(pedido.request.body).toEqual({ atributos: { sabor: 'Vainilla', peso: '2 kg' }, stock: 12, precio: 45999.5 });

    pedido.flush({ ...CHOCOLATE, id: 2, atributos: { sabor: 'Vainilla', peso: '2 kg' }, stock: 12 });
    fixture.detectChanges();
    expect(pantalla.textContent).toContain('Variante agregada.');
    expect(pantalla.textContent).toContain('Vainilla');
  });

  it('si el back rechaza el ajuste por falta de stock, lo explica', () => {
    apretar('.btn-ajustar');
    apretar('[role=radio][aria-checked=false]'); // pasa a "Reducir"
    escribir('#ajuste-cantidad', '10');
    expect(pantalla.querySelector('.resultado')!.textContent).toContain('-6 u.');

    enviar('form.modal');
    backend
      .expectOne(`${URL_PRODUCTO}/variante/${CHOCOLATE.id}/stock`)
      .flush(
        { status: 400, message: 'Stock insuficiente para la variante id 1. Disponible: 4, solicitado: 10.' },
        { status: 400, statusText: 'Bad Request' }
      );
    fixture.detectChanges();

    expect(pantalla.querySelector('.modal .alerta-error-titulo')!.textContent).toContain('Stock insuficiente');
    expect(pantalla.querySelector('.modal .alerta-error-texto')!.textContent).toContain('tiene 4 unidades');
  });
});
