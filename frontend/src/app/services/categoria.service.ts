import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, shareReplay } from 'rxjs';
import { environment } from '../../environments/environment';
import { Categoria } from '../models/categoria';

@Injectable({
  providedIn: 'root'
})
export class CategoriaService {
  private readonly API_URL = environment.apiUrl;

  /** Las categorías casi no cambian: se piden una vez y se comparten (header y catálogo). */
  private categorias$?: Observable<Categoria[]>;

  constructor(private http: HttpClient) {}

  /** Lista de categorías. Si el back falla devuelve una lista vacía y se reintenta la próxima vez. */
  listar(): Observable<Categoria[]> {
    this.categorias$ ??= this.http.get<Categoria[]>(`${this.API_URL}/categorias`).pipe(shareReplay(1));
    return this.categorias$.pipe(
      catchError(() => {
        this.categorias$ = undefined;
        return of([]);
      })
    );
  }
}
