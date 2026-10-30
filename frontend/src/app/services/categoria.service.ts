import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, shareReplay, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { Categoria } from '../models/categoria';
import { EliminacionResponse } from '../models/eliminacion-response';

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

  /** Para el panel: siempre pide la lista al back y, a diferencia de listar(), deja pasar el error. */
  listarActualizadas(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>(`${this.API_URL}/categorias`);
  }

  /** Solo ADMIN. Si el nombre ya existe, el back responde 409. */
  crear(nombre: string): Observable<Categoria> {
    return this.http
      .post<Categoria>(`${this.API_URL}/categorias`, { nombre })
      .pipe(tap(() => this.olvidarCache()));
  }

  /** Solo ADMIN. Si el nombre ya lo usa otra categoría, el back responde 409. */
  renombrar(id: number, nombre: string): Observable<Categoria> {
    return this.http
      .put<Categoria>(`${this.API_URL}/categorias/${id}`, { nombre })
      .pipe(tap(() => this.olvidarCache()));
  }

  /**
   * Solo ADMIN. Con confirmar=false no borra: devuelve qué productos quedarían sin categoría.
   * Con confirmar=true desvincula esos productos y borra la categoría.
   */
  eliminar(id: number, confirmar: boolean): Observable<EliminacionResponse> {
    return this.http
      .delete<EliminacionResponse>(`${this.API_URL}/categorias/${id}`, { params: { confirmar } })
      .pipe(tap(() => confirmar && this.olvidarCache()));
  }

  /** Después de un cambio, la próxima visita al catálogo vuelve a pedir la lista. */
  private olvidarCache(): void {
    this.categorias$ = undefined;
  }
}
