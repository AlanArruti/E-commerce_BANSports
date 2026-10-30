import { ChangeDetectionStrategy, Component, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CargadorImagen } from '../../componente/cargador-imagen/cargador-imagen';
import { Producto } from '../../models/producto';
import { ProductoService } from '../../services/producto.service';
import { ErrorTraducido, traducirErrorProducto } from '../../validaciones/errores-api';

/**
 * Imagen de un producto en el panel: /admin/productos/:id/imagen.
 * El listado de productos del panel (SCRUM-113) y el alta (SCRUM-114) enlazan acá.
 */
@Component({
  selector: 'app-admin-imagen-producto',
  imports: [RouterLink, CargadorImagen],
  templateUrl: './admin-imagen-producto.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './admin-imagen-producto.css'
})
export class AdminImagenProducto implements OnInit {
  protected readonly producto = signal<Producto | null>(null);
  protected readonly cargando = signal(true);
  protected readonly noExiste = signal(false);
  protected readonly errorCarga = signal<ErrorTraducido | null>(null);

  constructor(
    private route: ActivatedRoute,
    private productoService: ProductoService,
    private title: Title
  ) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isInteger(id) || id <= 0) {
      this.cargando.set(false);
      this.noExiste.set(true);
      return;
    }

    this.cargando.set(true);
    this.errorCarga.set(null);
    this.productoService.obtener(id).subscribe({
      next: (producto) => {
        this.producto.set(producto);
        this.cargando.set(false);
        this.title.setTitle(`Imagen de ${producto.nombre} | Panel BAN Sports`);
      },
      error: (e: unknown) => {
        this.cargando.set(false);
        if (e instanceof HttpErrorResponse && e.status === 404) {
          this.noExiste.set(true);
        } else if (e instanceof HttpErrorResponse) {
          this.errorCarga.set(traducirErrorProducto(e));
        } else {
          this.errorCarga.set({ titulo: 'No pudimos cargar el producto', detalle: 'Intentá de nuevo.' });
        }
      }
    });
  }
}
