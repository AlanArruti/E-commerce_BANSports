import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-footer',
  imports: [RouterLink],
  templateUrl: './footer.html',
  styleUrl: './footer.css'
})
export class Footer {
  protected readonly anio = new Date().getFullYear();
  protected readonly linksTienda = ['Proteínas', 'Creatinas', 'Pre-entrenos', 'Ofertas'];
  protected readonly linksAyuda = ['Envíos', 'Medios de pago', 'Seguí tu pedido', 'Contacto'];
}
