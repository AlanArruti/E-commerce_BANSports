import { CancelacionPedido, clientePuedeCancelar } from './pedido';
import { avisoCancelacion } from '../page/detalle-pedido/detalle-pedido';
import { textoReembolsoAdmin } from '../page/admin-pedido/admin-pedido';

function cancelacion(cambios: Partial<CancelacionPedido>): CancelacionPedido {
  return { idPedido: 7, estado: 'CANCELADO', reembolso: 'NO_CORRESPONDE', montoReembolsado: 0, mensaje: '', ...cambios };
}

describe('Cancelar un pedido (RF04)', () => {
  it('el cliente puede cancelar mientras el pedido no se despachó, aunque esté pagado', () => {
    expect(clientePuedeCancelar('PENDIENTE_PAGO')).toBeTrue();
    expect(clientePuedeCancelar('EN_PREPARACION')).toBeTrue();
    expect(clientePuedeCancelar('PAGADO')).toBeTrue();
    expect(clientePuedeCancelar('DESPACHADO')).toBeFalse();
    expect(clientePuedeCancelar('ENTREGADO')).toBeFalse();
    expect(clientePuedeCancelar('CANCELADO')).toBeFalse();
  });

  it('al cliente le cuenta cuánto se le devolvió y por dónde', () => {
    expect(avisoCancelacion(cancelacion({}))).toBe('Cancelaste el pedido #7.');
    expect(avisoCancelacion(cancelacion({ reembolso: 'AUTOMATICO', montoReembolsado: 15000 }))).toContain(
      'Te devolvimos $ 15.000 en Mercado Pago'
    );
    expect(avisoCancelacion(cancelacion({ reembolso: 'MANUAL', montoReembolsado: 15000 }))).toContain(
      'la tienda se va a comunicar con vos para devolverte $ 15.000'
    );
  });

  it('al admin le avisa si la devolución la tiene que hacer él', () => {
    expect(textoReembolsoAdmin(cancelacion({ reembolso: 'AUTOMATICO', montoReembolsado: 999.5 }))).toBe(
      'Se le devolvieron $ 999,50 al cliente en Mercado Pago.'
    );
    expect(textoReembolsoAdmin(cancelacion({ reembolso: 'MANUAL', montoReembolsado: 999.5 }))).toContain(
      'tenés que devolverle $ 999,50'
    );
  });
});
