import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { Categoria } from '../../models/categoria';
import { Producto } from '../../models/producto';
import { AdminProducto } from './admin-producto';

const CATEGORIAS: Categoria[] = [{ id: 1, nombre: 'Proteínas' } as Categoria];

const CREADO: Producto = {
  id: 20,
  uuidProducto: 'uuid-20',
  nombre: 'Whey Isolate',
  descripcion: 'Proteína aislada',
  precioBase: 40000,
  descuento: null,
  precioFinal: 40000,
  activo: true,
  imagenUrl: null,
  categoria: null,
  variantes: [],
  promedioResenas: 0,
  cantidadResenas: 0,
  novedad: true
};

describe('AdminProducto (alta con foto y novedad)', () => {
  let fixture: ComponentFixture<AdminProducto>;
  let backend: HttpTestingController;
  let pantalla: HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AdminProducto],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({}) } } }
      ]
    });
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(AdminProducto);
    pantalla = fixture.nativeElement;
    fixture.detectChanges();
    backend.expectOne('/api/v1/categorias').flush(CATEGORIAS);
    fixture.detectChanges();
  });

  afterEach(() => backend.verify());

  function escribir(selector: string, texto: string): void {
    const input = pantalla.querySelector<HTMLInputElement | HTMLTextAreaElement>(selector)!;
    input.value = texto;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function completarDatos(): void {
    escribir('#prod-nombre', 'Whey Isolate');
    const categoria = pantalla.querySelector<HTMLSelectElement>('#prod-categoria')!;
    categoria.selectedIndex = 1;
    categoria.dispatchEvent(new Event('change'));
    escribir('#prod-precio', '40000');
    escribir('#prod-descripcion', 'Proteína aislada');
  }

  function elegirArchivo(archivo: File): void {
    const input = pantalla.querySelector<HTMLInputElement>('#prod-foto')!;
    const datos = new DataTransfer();
    datos.items.add(archivo);
    input.files = datos.files;
    input.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  }

  function marcarNovedad(): void {
    pantalla.querySelector<HTMLInputElement>('input[formControlName="novedad"]')!.click();
    fixture.detectChanges();
  }

  function crear(): void {
    pantalla.querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  const foto = () => new File([new Uint8Array([1, 2, 3])], 'whey.png', { type: 'image/png' });

  it('crea el producto como novedad y después le sube la foto elegida', () => {
    completarDatos();
    marcarNovedad();
    elegirArchivo(foto());
    expect(pantalla.querySelector('.foto-nombre')?.textContent).toContain('whey.png');

    crear();

    const alta = backend.expectOne({ method: 'POST', url: '/api/v1/producto' });
    expect(alta.request.body.novedad).toBeTrue();
    alta.flush(CREADO);

    const subida = backend.expectOne({ method: 'POST', url: '/api/v1/producto/20/imagen' });
    expect((subida.request.body as FormData).get('archivo')).toEqual(jasmine.any(File));
    subida.flush({ idProducto: 20, nombreArchivo: 'whey.png', contentType: 'image/png', tamanioBytes: 3, url: '/api/v1/producto/20/imagen' });
    fixture.detectChanges();

    expect(pantalla.textContent).toContain('Producto creado');
    expect(pantalla.textContent).toContain('Imagen cargada');
  });

  it('sin foto, crea el producto y no sube nada', () => {
    completarDatos();

    crear();

    const alta = backend.expectOne({ method: 'POST', url: '/api/v1/producto' });
    expect(alta.request.body.novedad).toBeFalse();
    alta.flush({ ...CREADO, novedad: false });
    backend.expectNone('/api/v1/producto/20/imagen');
    fixture.detectChanges();

    expect(pantalla.textContent).toContain('Cargar imagen');
  });

  it('si la foto no sube, el producto queda creado y avisa el motivo', () => {
    completarDatos();
    elegirArchivo(foto());

    crear();
    backend.expectOne({ method: 'POST', url: '/api/v1/producto' }).flush(CREADO);
    backend
      .expectOne({ method: 'POST', url: '/api/v1/producto/20/imagen' })
      .flush({ message: 'La imagen no se pudo leer.' }, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();

    expect(pantalla.textContent).toContain('Producto creado');
    expect(pantalla.textContent).toContain('la foto no se pudo subir: La imagen no se pudo leer.');
  });

  it('rechaza un archivo que no es imagen antes de crear', () => {
    elegirArchivo(new File(['hola'], 'precios.pdf', { type: 'application/pdf' }));

    expect(pantalla.textContent).toContain('La imagen tiene que ser JPG, PNG o WEBP.');
    expect(pantalla.querySelector('.foto-elegida')).toBeNull();
  });
});
