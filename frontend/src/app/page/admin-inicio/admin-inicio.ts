import { Component, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { AdminService } from '../../services/admin.service';
import { ResumenAdmin } from '../../models/resumen-admin';
import { PrecioPipe } from '../../pipes/precio.pipe';
import { ErrorTraducido, traducirErrorResumen } from '../../validaciones/errores-api';

/**
 * Inicio del panel de administración: resumen de la tienda (ventas, pedidos,
 * stock y ofertas) con enlaces a las pantallas filtradas correspondientes.
 */
@Component({
  selector: 'app-admin-inicio',
  imports: [RouterLink, PrecioPipe],
  templateUrl: './admin-inicio.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './admin-inicio.css'
})
export class AdminInicio implements OnInit {
  protected readonly resumen = signal<ResumenAdmin | null>(null);
  protected readonly cargando = signal(true);
  protected readonly errorCarga = signal<ErrorTraducido | null>(null);

  constructor(
    protected authService: AuthService,
    private adminService: AdminService
  ) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.errorCarga.set(null);
    this.adminService.resumen().subscribe({
      next: (resumen) => {
        this.resumen.set(resumen);
        this.cargando.set(false);
      },
      error: (e: unknown) => {
        this.cargando.set(false);
        this.errorCarga.set(
          e instanceof HttpErrorResponse
            ? traducirErrorResumen(e)
            : { titulo: 'No pudimos cargar el resumen', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
        );
      }
    });
  }
}
