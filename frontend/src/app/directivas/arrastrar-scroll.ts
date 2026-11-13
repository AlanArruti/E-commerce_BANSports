import { DestroyRef, Directive, ElementRef, inject } from '@angular/core';

/** Cuántos píxeles hay que mover el mouse para que cuente como arrastre y no como clic. */
const UMBRAL_ARRASTRE = 6;

/**
 * Deja deslizar con el mouse una fila con scroll horizontal (carruseles de productos, chips de categorías).
 * Con el dedo ya anda solo: el navegador lo resuelve. Con mouse no, porque arrastrar no scrollea y la
 * barra está oculta; sin esto la fila parece trabada en una notebook o en el modo celular de DevTools.
 */
@Directive({
  selector: '[appArrastrarScroll]',
  host: {
    '(pointerdown)': 'empezar($event)',
    '(pointermove)': 'mover($event)',
    '(pointerup)': 'terminar($event)',
    '(pointercancel)': 'terminar($event)',
    '(dragstart)': '$event.preventDefault()'
  }
})
export class ArrastrarScroll {
  private readonly elemento: HTMLElement = inject(ElementRef).nativeElement;

  private punteroId: number | null = null;
  private xInicial = 0;
  private scrollInicial = 0;
  private arrastrando = false;
  private huboArrastre = false;

  constructor() {
    // En captura: si se escuchara en burbuja, el routerLink de la tarjeta ya habría navegado.
    const frenar = (evento: MouseEvent) => this.frenarClicTrasArrastre(evento);
    this.elemento.addEventListener('click', frenar, true);
    inject(DestroyRef).onDestroy(() => this.elemento.removeEventListener('click', frenar, true));
  }

  empezar(evento: PointerEvent): void {
    if (evento.pointerType !== 'mouse' || evento.button !== 0) {
      return;
    }
    this.punteroId = evento.pointerId;
    this.xInicial = evento.clientX;
    this.scrollInicial = this.elemento.scrollLeft;
    this.arrastrando = false;
    this.huboArrastre = false;
  }

  mover(evento: PointerEvent): void {
    if (evento.pointerId !== this.punteroId) {
      return;
    }
    const desplazamiento = evento.clientX - this.xInicial;
    if (!this.arrastrando) {
      if (Math.abs(desplazamiento) < UMBRAL_ARRASTRE) {
        return;
      }
      this.arrastrando = true;
      this.huboArrastre = true;
      this.elemento.setPointerCapture(evento.pointerId);
      // El snap pelea con el scroll manual: se apaga mientras se arrastra y vuelve al soltar.
      this.elemento.style.scrollSnapType = 'none';
      this.elemento.style.cursor = 'grabbing';
    }
    this.elemento.scrollLeft = this.scrollInicial - desplazamiento;
  }

  terminar(evento: PointerEvent): void {
    if (evento.pointerId !== this.punteroId) {
      return;
    }
    this.punteroId = null;
    if (this.arrastrando) {
      this.arrastrando = false;
      if (this.elemento.hasPointerCapture(evento.pointerId)) {
        this.elemento.releasePointerCapture(evento.pointerId);
      }
      this.elemento.style.scrollSnapType = '';
      this.elemento.style.cursor = '';
    }
  }

  /** Soltar después de arrastrar sobre una tarjeta no tiene que abrir el producto. */
  private frenarClicTrasArrastre(evento: MouseEvent): void {
    if (this.huboArrastre) {
      evento.preventDefault();
      evento.stopPropagation();
      this.huboArrastre = false;
    }
  }
}
