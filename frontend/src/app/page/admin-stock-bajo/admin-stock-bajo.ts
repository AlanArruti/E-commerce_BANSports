import { Component, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { AdminService } from '../../services/admin.service';
import { VarianteStockBajo } from '../../models/variante-stock-bajo';
import { ErrorTraducido, traducirErrorStock } from '../../validaciones/errores-api';

/** Variantes con stock bajo (o sin stock) de todo el catálogo. */
@Component({
  selector: 'app-admin-stock-bajo',
  imports: [NgClass, RouterLink],
  templateUrl: './admin-stock-bajo.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './admin-stock-bajo.css'
})
export class AdminStockBajo implements OnInit {
  protected readonly variantes = signal<VarianteStockBajo[]>([]);
  protected readonly cargando = signal(true);
  protected readonly errorCarga = signal<ErrorTraducido | null>(null);

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.errorCarga.set(null);
    this.adminService.variantesStockBajo().subscribe({
      next: (variantes) => {
        this.variantes.set(variantes);
        this.cargando.set(false);
      },
      error: (e: unknown) => {
        this.cargando.set(false);
        this.errorCarga.set(
          e instanceof HttpErrorResponse
            ? traducirErrorStock(e)
            : { titulo: 'No pudimos cargar el stock', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
        );
      }
    });
  }

  protected descripcion(variante: VarianteStockBajo): string {
    return Object.entries(variante.atributos)
      .sort(([a], [b]) => Number(b === 'presentacion') - Number(a === 'presentacion'))
      .map(([, valor]) => valor)
      .join(' · ');
  }

  protected nivel(stock: number): 'cero' | 'bajo' {
    return stock === 0 ? 'cero' : 'bajo';
  }
}
