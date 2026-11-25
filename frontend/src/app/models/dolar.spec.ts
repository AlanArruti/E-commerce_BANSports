import { formatearFecha, formatearVariacion, nombreDolar, tendencia } from './dolar';

describe('Formato de la cotización del dólar', () => {
  it('muestra la variación con flecha, coma decimal y sin signo', () => {
    expect(formatearVariacion(0.5)).toBe('▲ 0,5%');
    expect(formatearVariacion(-1.24)).toBe('▼ 1,2%');
    expect(formatearVariacion(12)).toBe('▲ 12%');
    expect(formatearVariacion(0.01)).toBe('0%');
  });

  it('clasifica la tendencia redondeando a un decimal', () => {
    expect(tendencia(0.5)).toBe('sube');
    expect(tendencia(-0.5)).toBe('baja');
    expect(tendencia(0.04)).toBe('igual');
    expect(tendencia(null)).toBe('igual');
  });

  it('nombra los tipos de dólar conocidos y deja pasar los demás', () => {
    expect(nombreDolar('oficial')).toBe('Dólar oficial');
    expect(nombreDolar('bolsa')).toBe('Dólar MEP');
    expect(nombreDolar('nuevo')).toBe('Dólar nuevo');
  });

  it('dice hoy o ayer, y si no la fecha', () => {
    const ahora = new Date(2026, 10, 24, 20, 0);
    expect(formatearFecha(new Date(2026, 10, 24, 15, 5).toISOString(), ahora)).toMatch(/^hoy 15:05/);
    expect(formatearFecha(new Date(2026, 10, 23, 9, 30).toISOString(), ahora)).toMatch(/^ayer 09:30/);
    expect(formatearFecha(new Date(2026, 10, 20, 10, 30).toISOString(), ahora)).toMatch(/^20\/11 10:30/);
    expect(formatearFecha(null, ahora)).toBe('');
  });
});
