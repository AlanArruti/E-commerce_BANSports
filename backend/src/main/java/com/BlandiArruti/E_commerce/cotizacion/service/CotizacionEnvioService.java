package com.BlandiArruti.E_commerce.cotizacion.service;

import com.BlandiArruti.E_commerce.auth.service.UsuarioDetails;
import com.BlandiArruti.E_commerce.cliente.entity.Direccion;
import com.BlandiArruti.E_commerce.cliente.repository.DireccionRepository;
import com.BlandiArruti.E_commerce.cotizacion.client.CorreoArgentinoClient;
import com.BlandiArruti.E_commerce.cotizacion.client.dto.CorreoArgentinoRatesRequest;
import com.BlandiArruti.E_commerce.cotizacion.client.dto.CorreoArgentinoRatesResponse;
import com.BlandiArruti.E_commerce.cotizacion.config.CorreoArgentinoProperties;
import com.BlandiArruti.E_commerce.cotizacion.dto.response.CotizacionEnvioResponse;
import com.BlandiArruti.E_commerce.cotizacion.entity.TarifaEnvio;
import com.BlandiArruti.E_commerce.cotizacion.repository.TarifaEnvioRepository;
import com.BlandiArruti.E_commerce.enums.Rol;
import com.BlandiArruti.E_commerce.exception.EcommerceException;
import com.BlandiArruti.E_commerce.exception.EntidadNoEncontradaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CotizacionEnvioService {

    // Paquete estándar usado como referencia para cotizar (no se manejan dimensiones por producto).
    private static final CorreoArgentinoRatesRequest.Dimension PAQUETE_ESTANDAR =
            new CorreoArgentinoRatesRequest.Dimension(1.0, 20.0, 20.0, 20.0, 1);

    private static final String TRANSPORTISTA_RESPALDO = "Envío estándar";

    private final DireccionRepository direccionRepository;
    private final CorreoArgentinoClient correoArgentinoClient;
    private final CorreoArgentinoProperties properties;
    private final TarifaEnvioRepository tarifaEnvioRepository;

    public List<CotizacionEnvioResponse> cotizarPorDireccion(Long idDireccion) {
        Direccion direccion = direccionRepository.findById(idDireccion)
                .orElseThrow(() -> EntidadNoEncontradaException.direccion(idDireccion));

        verificarPropietario(direccion);

        List<CotizacionEnvioResponse> externas = cotizarExterna(direccion);
        if (!externas.isEmpty()) {
            return externas;
        }

        return List.of(cotizarDesdeTabla(direccion));
    }

    /**
     * Intenta cotizar con Correo Argentino. Si no está configurado, no hay código
     * postal o la llamada falla, devuelve vacío para que se use la tabla propia.
     */
    private List<CotizacionEnvioResponse> cotizarExterna(Direccion direccion) {
        if (direccion.getCodigoPostal() == null || direccion.getCodigoPostal().isBlank()) {
            return List.of();
        }

        try {
            CorreoArgentinoRatesRequest request = new CorreoArgentinoRatesRequest(
                    properties.getCustomerId(),
                    properties.getCodigoPostalOrigen(),
                    direccion.getCodigoPostal(),
                    "D",
                    List.of(PAQUETE_ESTANDAR)
            );

            CorreoArgentinoRatesResponse response = correoArgentinoClient.cotizar(request);
            if (response == null || response.rates() == null || response.rates().isEmpty()) {
                return List.of();
            }

            return response.rates().stream()
                    .map(rate -> new CotizacionEnvioResponse(
                            "Correo Argentino",
                            rate.deliveredType(),
                            rate.productName(),
                            rate.price(),
                            rate.deliveryTimeMin(),
                            rate.deliveryTimeMax()
                    ))
                    .toList();
        } catch (Exception e) {
            log.warn("Cotización externa no disponible (dirección {}): {}. Se usa la tabla propia.",
                    direccion.getId(), e.getMessage());
            return List.of();
        }
    }

    /** Respaldo: tarifa por provincia de destino, con una por defecto para el resto. */
    private CotizacionEnvioResponse cotizarDesdeTabla(Direccion direccion) {
        String provincia = direccion.getCiudad().getProvincia().getNombre();

        TarifaEnvio tarifa = tarifaEnvioRepository.findByProvinciaIgnoreCase(provincia)
                .orElseGet(() -> tarifaEnvioRepository.findByProvinciaIsNull()
                        .orElseThrow(() -> new EcommerceException(
                                "No hay tarifas de envío configuradas. Contactá al administrador.")));

        return new CotizacionEnvioResponse(
                TRANSPORTISTA_RESPALDO,
                "D",
                "Envío a domicilio",
                tarifa.getPrecio(),
                tarifa.getPlazoMinimoDias(),
                tarifa.getPlazoMaximoDias()
        );
    }

    private void verificarPropietario(Direccion direccion) {
        UsuarioDetails principal = (UsuarioDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal.getRol() == Rol.CLIENTE && !principal.getId().equals(direccion.getCliente().getId())) {
            throw new AccessDeniedException("No tenés permiso para cotizar esta dirección.");
        }
    }
}
