import { Component, ChangeDetectionStrategy } from '@angular/core';
import { NgClass } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PoteProducto } from '../../componente/pote-producto/pote-producto';
import { CategoriaDestacada } from '../../models/categoria-destacada';
import { ProductoDestacado } from '../../models/producto-destacado';

interface Beneficio {
  titulo: string;
  detalle: string;
  icono: 'envio' | 'pago' | 'original' | 'asesoria';
}

@Component({
  selector: 'app-home',
  imports: [NgClass, RouterLink, PoteProducto],
  templateUrl: './home.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './home.css'
})
export class Home {
  // Por ahora son datos de ejemplo del mockup. Más adelante vendrán del back.
  protected readonly beneficios: Beneficio[] = [
    { titulo: 'Envío a todo el país', detalle: 'Seguimiento con Correo Argentino', icono: 'envio' },
    { titulo: 'Pagá con Mercado Pago', detalle: 'Tarjeta, débito o dinero en cuenta', icono: 'pago' },
    { titulo: 'Productos originales', detalle: 'Con lote y vencimiento visibles', icono: 'original' },
    { titulo: 'Te asesoramos', detalle: 'Elegí el suplemento para tu objetivo', icono: 'asesoria' }
  ];

  protected readonly categorias: CategoriaDestacada[] = [
    { numero: '01', nombre: 'Proteínas', descripcion: 'Masa muscular y recuperación', destacada: true },
    { numero: '02', nombre: 'Creatinas', descripcion: 'Fuerza y potencia', destacada: false },
    { numero: '03', nombre: 'Pre-entrenos', descripcion: 'Energía y enfoque', destacada: false },
    { numero: '04', nombre: 'Vitaminas y más', descripcion: 'Salud y bienestar diario', destacada: false }
  ];

  protected readonly productos: ProductoDestacado[] = [
    { etiqueta: 'Más vendido', abreviatura: 'WHEY', categoria: 'Proteínas', nombre: 'Whey Protein Concentrada', variante: '1 kg · Chocolate' },
    { etiqueta: '', abreviatura: 'CREA', categoria: 'Creatinas', nombre: 'Creatina Monohidrato', variante: '300 g · Sin sabor' },
    { etiqueta: 'Nuevo', abreviatura: 'PRE', categoria: 'Pre-entrenos', nombre: 'Pre-entreno Explosive', variante: '300 g · Frutos rojos' },
    { etiqueta: 'Oferta', abreviatura: 'BCAA', categoria: 'Aminoácidos', nombre: 'BCAA 2:1:1', variante: '200 g · Limón' }
  ];
}
