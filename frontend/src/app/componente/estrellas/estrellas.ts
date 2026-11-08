import { Component, computed, input, ChangeDetectionStrategy } from '@angular/core';

/**
 * Cinco estrellas con el puntaje pintado en proporción (4,7 → casi cinco llenas).
 * Solo muestra: para elegir estrellas, el formulario de reseña tiene sus propios botones.
 */
@Component({
  selector: 'app-estrellas',
  template: `
    <span class="estrellas" [class.chicas]="tamanio() === 'chico'" [class.grandes]="tamanio() === 'grande'" role="img" [attr.aria-label]="etiqueta()">
      <span class="fondo" aria-hidden="true">★★★★★</span>
      <span class="relleno" aria-hidden="true" [style.width.%]="porcentaje()">★★★★★</span>
    </span>
  `,
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './estrellas.css'
})
export class Estrellas {
  /** De 0 a 5; acepta decimales. */
  readonly valor = input.required<number>();
  readonly tamanio = input<'chico' | 'normal' | 'grande'>('normal');

  protected readonly porcentaje = computed(() => Math.min(Math.max(this.valor(), 0), 5) * 20);
  protected readonly etiqueta = computed(
    () => `${this.valor().toLocaleString('es-AR', { maximumFractionDigits: 1 })} de 5 estrellas`
  );
}
