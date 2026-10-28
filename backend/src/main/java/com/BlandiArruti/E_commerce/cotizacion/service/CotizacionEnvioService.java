package com.BlandiArruti.E_commerce.cotizacion.service;

import com.BlandiArruti.E_commerce.auth.service.UsuarioDetails;
import com.BlandiArruti.E_commerce.cliente.entity.Direccion;
import com.BlandiArruti.E_commerce.cliente.repository.DireccionRepository;
import com.BlandiArruti.E_commerce.cotizacion.client.CorreoArgentinoClient;
import com.BlandiArruti.E_commerce.cotizacion.client.dto.CorreoArgentinoRatesRequest;
import com.BlandiArruti.E_commerce.cotizacion.client.dto.CorreoArgentinoRatesResponse;
import com.BlandiArruti.E_commerce.cotizacion.config.CorreoArgentinoProperties;
import com.BlandiArruti.E_commerce.cotizacion.dto.response.CotizacionEnvioResponse;
import com.BlandiArruti.E_commerce.enums.Rol;
import com.BlandiArruti.E_commerce.exception.EcommerceException;
import com.BlandiArruti.E_commerce.exception.EntidadNoEncontradaException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CotizacionEnvioService {

    // Paquete estándar usado como referencia para cotizar (no se manejan dimensiones por producto).
    private static final CorreoArgentinoRatesRequest.Dimension PAQUETE_ESTANDAR =
            new CorreoArgentinoRatesRequest.Dimension(1.0, 20.0, 20.0, 20.0, 1);

    private final DireccionRepository direccionRepository;
    private final CorreoArgentinoClient correoArgentinoClient;
    private final CorreoArgentinoProperties properties;

    public List<CotizacionEnvioResponse> cotizarPorDireccion(Long idDireccion) {
        Direccion direccion = direccionRepository.findById(idDireccion)
                .orElseThrow(() -> EntidadNoEncontradaException.direccion(idDireccion));

        verificarPropietario(direccion);

        if (direccion.getCodigoPostal() == null || direccion.getCodigoPostal().isBlank()) {
            throw new EcommerceException("La dirección no tiene código postal configurado.");
        }

        CorreoArgentinoRatesRequest request = new CorreoArgentinoRatesRequest(
                properties.getCustomerId(),
                properties.getCodigoPostalOrigen(),
                direccion.getCodigoPostal(),
                "D",
                List.of(PAQUETE_ESTANDAR)
        );

        CorreoArgentinoRatesResponse response = correoArgentinoClient.cotizar(request);
        if (response == null || response.rates() == null) {
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
    }

    private void verificarPropietario(Direccion direccion) {
        UsuarioDetails principal = (UsuarioDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal.getRol() == Rol.CLIENTE && !principal.getId().equals(direccion.getCliente().getId())) {
            throw new AccessDeniedException("No tenés permiso para cotizar esta dirección.");
        }
    }
}
