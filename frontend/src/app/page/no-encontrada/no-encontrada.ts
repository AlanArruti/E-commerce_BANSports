import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NoEncontrado } from '../../componente/no-encontrado/no-encontrado';

/** Ruta que no existe (el "**" del router). */
@Component({
  selector: 'app-no-encontrada',
  imports: [RouterLink, NoEncontrado],
  templateUrl: './no-encontrada.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './no-encontrada.css'
})
export class NoEncontrada {}
