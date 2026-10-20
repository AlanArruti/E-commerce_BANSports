import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Header } from './componente/header/header';
import { Footer } from './componente/footer/footer';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Header, Footer],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {}
