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
  cantidadResenas: 0,
  novedad: false
};

const CHOCOLATE: Variante = {
  id: 1,
  uuidVariante: 'uuid-1',
  atributos: { sabor: 'Chocolate', peso: '1 kg' },
  stock: 4,
  activo: true,
  idProducto: ID_PRODUCTO,
  precio: null,
  precioFinal: 30000,
  imagenUrl: null
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

  /** Escribe un valor en el campo de una opción y aprieta Enter. */
  function agregarValor(clave: string, valor: string): void {
    const input = pantalla.querySelector<HTMLInputElement>(`#valor-${clave}`)!;
    input.value = valor;
    input.dispatchEvent(new Event('input'));
    input.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));
    fixture.detectChanges();
  }

  function filasTabla(): string[] {
    // Texto visible de cada celda (sin las etiquetas para lectores de pantalla), separado por espacios.
    return [...pantalla.querySelectorAll('.tabla-combinaciones tbody tr')].map((fila) =>
      [...fila.querySelectorAll('td')]
        .map((celda) => [...celda.childNodes].filter((n) => n.nodeType !== Node.COMMENT_NODE && !(n instanceof HTMLLabelElement)).map((n) => n.textContent).join('').trim())
        .filter(Boolean)
        .join(' ')
    );
  }

  function valores(clave: string): string[] {
    const opcion = [...pantalla.querySelectorAll('.opcion')].find((o) => o.querySelector(`#valor-${clave}`));
    return [...(opcion?.querySelectorAll('.valor') ?? [])].map((v) => v.textContent!.replace('×', '').trim());
  }

  // ── Tests ───────────────────────────────────────────

  it('muestra las variantes que trae el back', () => {
    expect(pantalla.textContent).toContain('Chocolate');
  });

  it('el alta arranca con las opciones que ya usa el producto, y la combinación creada figura como existente', () => {
    apretar('.btn-nuevo');

    expect(valores('sabor')).toEqual(['Chocolate']);
    expect(valores('peso')).toEqual(['1 kg']);
    expect(filasTabla()).toEqual(['Chocolate 1 kg Ya existe']);
    expect(pantalla.querySelector('.form-alta button[type=submit]')!.textContent).toContain('Crear 0 variantes');
  });

  it('sumar un valor arma las combinaciones nuevas, tildadas', () => {
    apretar('.btn-nuevo');
    agregarValor('sabor', 'Vainilla');
    agregarValor('peso', '2 kg');

    // 2 sabores × 2 pesos: una ya existe, se crean las otras tres.
    expect(filasTabla().length).toBe(4);
    expect(pantalla.querySelectorAll('.tabla-combinaciones input[type=checkbox]:checked').length).toBe(3);
    expect(pantalla.querySelector('.form-alta button[type=submit]')!.textContent).toContain('Crear 3 variantes');
  });

  it('no deja cargar dos veces el mismo valor (sin importar mayúsculas)', () => {
    apretar('.btn-nuevo');
    agregarValor('sabor', 'chocolate');

    expect(errores()).toContain('"chocolate" ya está en Sabor.');
    expect(valores('sabor')).toEqual(['Chocolate']);
  });

  it('pide el stock de cada combinación tildada y no manda nada si falta', () => {
    apretar('.btn-nuevo');
    agregarValor('sabor', 'Frutilla');
    escribir('#stock-1', '2,5');

    enviar('form.form-alta');

    expect(errores()).toContain('Solo números enteros, sin puntos ni comas.');
    expect(errores()).toContain('Revisá el stock y el precio de las combinaciones marcadas.');
    backend.expectNone(`${URL_PRODUCTO}/variante`);
  });

  it('una opción nueva sin valores no deja crear', () => {
    apretar('.btn-nuevo');
    pantalla.querySelector<HTMLButtonElement>('.tipo[aria-pressed=false]')!.click(); // Talle
    fixture.detectChanges();

    enviar('form.form-alta');

    expect(errores()).toContain('Cargá al menos un valor de Talle o sacá esa opción.');
    backend.expectNone(`${URL_PRODUCTO}/variante`);
  });

  it('crea cada combinación tildada con su stock y su precio', () => {
    apretar('.btn-nuevo');
    agregarValor('sabor', 'Vainilla');
    agregarValor('peso', '2 kg');
    // Filas: Choc 1kg (existe), Choc 2kg, Vain 1kg, Vain 2kg. Se destilda Vainilla 2 kg.
    const checks = pantalla.querySelectorAll<HTMLInputElement>('.tabla-combinaciones input[type=checkbox]');
    checks[2].click();
    fixture.detectChanges();
    escribir('#stock-1', '8');
    escribir('#precio-1', '82.000');
    escribir('#stock-2', '12');

    enviar('form.form-alta');

    const pedidos = backend.match(`${URL_PRODUCTO}/variante`);
    expect(pedidos.map((p) => p.request.body)).toEqual([
      { atributos: { sabor: 'Chocolate', peso: '2 kg' }, stock: 8, precio: 82000 },
      { atributos: { sabor: 'Vainilla', peso: '1 kg' }, stock: 12, precio: null }
    ]);
    pedidos[0].flush({ ...CHOCOLATE, id: 2, atributos: { sabor: 'Chocolate', peso: '2 kg' }, stock: 8 });
    pedidos[1].flush({ ...CHOCOLATE, id: 3, atributos: { sabor: 'Vainilla', peso: '1 kg' }, stock: 12 });
    fixture.detectChanges();

    expect(pantalla.textContent).toContain('Se agregaron 2 variantes.');
    expect(pantalla.querySelector('.form-alta')).toBeNull();
  });

  it('si una falla, las otras quedan creadas y la tabla las marca como existentes', () => {
    apretar('.btn-nuevo');
    agregarValor('sabor', 'Vainilla');
    agregarValor('sabor', 'Frutilla');
    escribir('#stock-1', '5');
    escribir('#stock-2', '5');

    enviar('form.form-alta');

    const [vainilla, frutilla] = backend.match(`${URL_PRODUCTO}/variante`);
    vainilla.flush({ ...CHOCOLATE, id: 2, atributos: { sabor: 'Vainilla', peso: '1 kg' }, stock: 5 });
    frutilla.flush({ status: 500, message: 'Error' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(pantalla.querySelector('.form-alta .alerta-error-titulo')!.textContent).toContain('Se agregaron 1, pero una no se pudo crear');
    expect(filasTabla()).toContain('Vainilla 1 kg Ya existe');
  });

  it('al editar, cada opción es un desplegable y se puede escribir un valor nuevo', () => {
    apretar('.btn-accion[title=Editar]');
    const sabor = pantalla.querySelector<HTMLSelectElement>('#editar-sabor')!;
    expect(sabor.value).toBe('Chocolate');

    sabor.value = '__nuevo__';
    sabor.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    escribir('#nuevo-sabor', ' Dulce de leche ');
    enviar('form.tarjeta-form:not(.form-alta)');

    const pedido = backend.expectOne(`${URL_PRODUCTO}/variante/${CHOCOLATE.id}`);
    expect(pedido.request.method).toBe('PUT');
    expect(pedido.request.body).toEqual({ atributos: { sabor: 'Dulce de leche', peso: '1 kg' }, stock: 4, precio: null });
    pedido.flush({ ...CHOCOLATE, atributos: { sabor: 'Dulce de leche', peso: '1 kg' } });
    fixture.detectChanges();
    expect(pantalla.textContent).toContain('Variante actualizada.');
  });

  it('al editar, pide escribir el valor nuevo si se eligió "Nuevo valor"', () => {
    apretar('.btn-accion[title=Editar]');
    const sabor = pantalla.querySelector<HTMLSelectElement>('#editar-sabor')!;
    sabor.value = '__nuevo__';
    sabor.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    enviar('form.tarjeta-form:not(.form-alta)');

    expect(errores()).toContain('Escribí el nuevo valor de Sabor.');
    backend.expectNone(`${URL_PRODUCTO}/variante/${CHOCOLATE.id}`);
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
