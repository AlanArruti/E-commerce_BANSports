import { Component, ChangeDetectionStrategy } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { CategoriaService } from '../../services/categoria.service';

/** Las mismas categorías que muestra el header. */
const CATEGORIAS_EN_FOOTER = 4;

@Component({
  selector: 'app-footer',
  imports: [RouterLink],
  templateUrl: './footer.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './footer.css'
})
export class Footer {
  protected readonly anio = new Date().getFullYear();
  protected readonly categorias;

  constructor(
    protected authService: AuthService,
    categoriaService: CategoriaService
  ) {
    this.categorias = toSignal(
      categoriaService.listar().pipe(map((categorias) => categorias.slice(0, CATEGORIAS_EN_FOOTER))),
      { initialValue: [] }
    );
  }
}
