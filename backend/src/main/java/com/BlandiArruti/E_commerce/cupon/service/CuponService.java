package com.BlandiArruti.E_commerce.cupon.service;

import com.BlandiArruti.E_commerce.auth.service.UsuarioDetails;
import com.BlandiArruti.E_commerce.carrito.entity.Carrito;
import com.BlandiArruti.E_commerce.carrito.repository.CarritoRepository;
import com.BlandiArruti.E_commerce.cupon.dto.request.CuponRequest;
import com.BlandiArruti.E_commerce.cupon.dto.response.CuponResponse;
import com.BlandiArruti.E_commerce.cupon.dto.response.CuponResponse.Estado;
import com.BlandiArruti.E_commerce.cupon.dto.response.ValidacionCuponResponse;
import com.BlandiArruti.E_commerce.cupon.entity.Cupon;
import com.BlandiArruti.E_commerce.cupon.repository.CuponRepository;
import com.BlandiArruti.E_commerce.enums.TipoCupon;
import com.BlandiArruti.E_commerce.exception.ConflictoException;
import com.BlandiArruti.E_commerce.exception.CuponInvalidoException;
import com.BlandiArruti.E_commerce.exception.DuplicadoException;
import com.BlandiArruti.E_commerce.exception.EcommerceException;
import com.BlandiArruti.E_commerce.exception.EntidadNoEncontradaException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
@RequiredArgsConstructor
public class CuponService implements ICuponService {

    private final CuponRepository cuponRepository;
    private final CarritoRepository carritoRepository;

    // ── Panel ──────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<CuponResponse> listar() {
        LocalDate hoy = LocalDate.now();
        return cuponRepository.findAllByOrderByFechaCreacionDescIdDesc().stream()
                .map(cupon -> aResponse(cupon, hoy))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CuponResponse buscarPorId(Long id) {
        return aResponse(buscar(id), LocalDate.now());
    }

    @Override
    public CuponResponse crear(CuponRequest request) {
        String codigo = normalizar(request.codigo());
        if (cuponRepository.existsByCodigo(codigo)) {
            throw DuplicadoException.cupon(codigo);
        }
        validarReglas(request);
        validarFechasNoPasadas(request, null, LocalDate.now());
        Cupon cupon = Cupon.builder().codigo(codigo).build();
        copiar(request, cupon);
        return aResponse(cuponRepository.save(cupon), LocalDate.now());
    }

    @Override
    public CuponResponse actualizar(Long id, CuponRequest request) {
        Cupon cupon = buscar(id);
        String codigo = normalizar(request.codigo());
        cuponRepository.findByCodigo(codigo)
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> { throw DuplicadoException.cupon(codigo); });
        // Cambiarle el código a un cupón ya usado confundiría los pedidos que lo muestran.
        if (cupon.getUsos() > 0 && !cupon.getCodigo().equals(codigo)) {
            throw new ConflictoException("El cupón ya se usó en " + cupon.getUsos()
                    + " pedido(s): no se le puede cambiar el código. Creá uno nuevo.");
        }
        validarReglas(request);
        validarFechasNoPasadas(request, cupon, LocalDate.now());
        cupon.setCodigo(codigo);
        copiar(request, cupon);
        return aResponse(cuponRepository.save(cupon), LocalDate.now());
    }

    @Override
    public void eliminar(Long id) {
        Cupon cupon = buscar(id);
        cupon.setActivo(false);
        cuponRepository.save(cupon);
    }

    // ── Checkout ───────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ValidacionCuponResponse validarParaCarrito(String codigo) {
        Cupon cupon = cuponRepository.findByCodigo(normalizar(codigo))
                .orElseThrow(() -> CuponInvalidoException.inexistente(normalizar(codigo)));
        double subtotal = subtotalDelCarrito();
        double descuento = calcularDescuento(cupon, subtotal, LocalDate.now());
        return new ValidacionCuponResponse(cupon.getCodigo(), cupon.getTipo(), cupon.getValor(), subtotal, descuento);
    }

    @Override
    public CuponAplicado aplicar(String codigo, double subtotal) {
        Cupon cupon = cuponRepository.findByCodigoParaActualizar(normalizar(codigo))
                .orElseThrow(() -> CuponInvalidoException.inexistente(normalizar(codigo)));
        double descuento = calcularDescuento(cupon, subtotal, LocalDate.now());
        cupon.setUsos(cupon.getUsos() + 1);
        cuponRepository.save(cupon);
        return new CuponAplicado(cupon, descuento);
    }

    @Override
    public void devolverUso(Cupon cupon) {
        if (cupon.getUsos() > 0) {
            cupon.setUsos(cupon.getUsos() - 1);
            cuponRepository.save(cupon);
        }
    }

    /**
     * Cuánto descuenta el cupón sobre ese subtotal de productos, o la razón por la que no sirve.
     * Porcentaje: redondeado a centavos. Monto fijo: nunca más que el subtotal.
     */
    public double calcularDescuento(Cupon cupon, double subtotal, LocalDate hoy) {
        String codigo = cupon.getCodigo();
        if (!cupon.isActivo()) {
            throw CuponInvalidoException.inactivo(codigo);
        }
        if (cupon.getVigenteDesde() != null && hoy.isBefore(cupon.getVigenteDesde())) {
            throw CuponInvalidoException.noEmpezo(codigo, cupon.getVigenteDesde());
        }
        if (cupon.getVigenteHasta() != null && hoy.isAfter(cupon.getVigenteHasta())) {
            throw CuponInvalidoException.vencido(codigo, cupon.getVigenteHasta());
        }
        if (cupon.estaAgotado()) {
            throw CuponInvalidoException.agotado(codigo);
        }
        double minimo = cupon.getMontoMinimo() != null ? cupon.getMontoMinimo() : 0.0;
        if (subtotal < minimo) {
            throw CuponInvalidoException.montoMinimo(codigo, minimo, subtotal);
        }
        double descuento = cupon.getTipo() == TipoCupon.PORCENTAJE
                ? subtotal * cupon.getValor() / 100.0
                : Math.min(cupon.getValor(), subtotal);
        return Math.round(descuento * 100.0) / 100.0;
    }

    // ── Internos ───────────────────────────────────────────

    private Cupon buscar(Long id) {
        return cuponRepository.findById(id).orElseThrow(() -> EntidadNoEncontradaException.cupon(id));
    }

    private static String normalizar(String codigo) {
        return codigo == null ? "" : codigo.trim().toUpperCase(Locale.ROOT);
    }

    /** Lo que la validación por anotaciones no puede ver: reglas entre campos. */
    private static void validarReglas(CuponRequest request) {
        if (request.tipo() == TipoCupon.PORCENTAJE && request.valor() > 100) {
            throw new EcommerceException("Un cupón de porcentaje no puede descontar más del 100%.");
        }
        if (request.vigenteDesde() != null && request.vigenteHasta() != null
                && request.vigenteHasta().isBefore(request.vigenteDesde())) {
            throw new EcommerceException("La fecha de fin no puede ser anterior a la de inicio.");
        }
    }

    /**
     * Un cupón nuevo no puede empezar ni vencer antes de hoy. Al editar, una fecha pasada se acepta solo si
     * es la que ya tenía (así se puede editar un cupón que ya empezó o venció sin tocarle las fechas).
     */
    static void validarFechasNoPasadas(CuponRequest request, Cupon actual, LocalDate hoy) {
        LocalDate desdeAnterior = actual != null ? actual.getVigenteDesde() : null;
        LocalDate hastaAnterior = actual != null ? actual.getVigenteHasta() : null;
        if (esPasadaYNueva(request.vigenteDesde(), desdeAnterior, hoy)) {
            throw new EcommerceException("La fecha de inicio no puede ser anterior a hoy.");
        }
        if (esPasadaYNueva(request.vigenteHasta(), hastaAnterior, hoy)) {
            throw new EcommerceException("La fecha de fin no puede ser anterior a hoy.");
        }
    }

    private static boolean esPasadaYNueva(LocalDate fecha, LocalDate anterior, LocalDate hoy) {
        return fecha != null && fecha.isBefore(hoy) && !fecha.equals(anterior);
    }

    private static void copiar(CuponRequest request, Cupon cupon) {
        cupon.setTipo(request.tipo());
        cupon.setValor(request.valor());
        cupon.setVigenteDesde(request.vigenteDesde());
        cupon.setVigenteHasta(request.vigenteHasta());
        cupon.setMontoMinimo(request.montoMinimo() != null ? request.montoMinimo() : 0.0);
        cupon.setUsosMaximos(request.usosMaximos());
        cupon.setActivo(request.activo() == null || request.activo());
    }

    /** Subtotal de productos del carrito del cliente logueado, con los mismos precios que el pedido. */
    private double subtotalDelCarrito() {
        UsuarioDetails principal = (UsuarioDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return carritoRepository.findByClienteId(principal.getId())
                .map(Carrito::getItems)
                .map(items -> items.stream()
                        .mapToDouble(item -> item.getProducto().precioUnitario(item.getVariante()) * item.getCantidad())
                        .sum())
                .orElse(0.0);
    }

    static Estado estado(Cupon cupon, LocalDate hoy) {
        if (!cupon.isActivo()) return Estado.INACTIVO;
        if (cupon.getVigenteHasta() != null && hoy.isAfter(cupon.getVigenteHasta())) return Estado.VENCIDO;
        if (cupon.estaAgotado()) return Estado.AGOTADO;
        if (cupon.getVigenteDesde() != null && hoy.isBefore(cupon.getVigenteDesde())) return Estado.PROGRAMADO;
        return Estado.VIGENTE;
    }

    private static CuponResponse aResponse(Cupon cupon, LocalDate hoy) {
        return new CuponResponse(cupon.getId(), cupon.getCodigo(), cupon.getTipo(), cupon.getValor(),
                cupon.getVigenteDesde(), cupon.getVigenteHasta(), cupon.getMontoMinimo(), cupon.getUsosMaximos(),
                cupon.getUsos(), cupon.isActivo(), estado(cupon, hoy));
    }
}
