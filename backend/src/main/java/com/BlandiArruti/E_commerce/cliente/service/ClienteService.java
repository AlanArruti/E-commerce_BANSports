package com.BlandiArruti.E_commerce.cliente.service;

import com.BlandiArruti.E_commerce.auth.service.UsuarioDetails;
import com.BlandiArruti.E_commerce.cliente.dto.request.CambioPasswordRequest;
import com.BlandiArruti.E_commerce.cliente.dto.request.ClienteUpdateRequest;
import com.BlandiArruti.E_commerce.cliente.dto.request.ClienteRequest;
import com.BlandiArruti.E_commerce.cliente.dto.request.DireccionRequest;
import com.BlandiArruti.E_commerce.cliente.dto.response.ClienteResponse;
import com.BlandiArruti.E_commerce.cliente.dto.response.DireccionResponse;
import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import com.BlandiArruti.E_commerce.cliente.entity.Direccion;
import com.BlandiArruti.E_commerce.cliente.mapper.ClienteMapper;
import com.BlandiArruti.E_commerce.cliente.mapper.DireccionMapper;
import com.BlandiArruti.E_commerce.cliente.repository.ClienteRepository;
import com.BlandiArruti.E_commerce.cliente.repository.DireccionRepository;
import com.BlandiArruti.E_commerce.enums.EstadoPedido;
import com.BlandiArruti.E_commerce.enums.Rol;
import com.BlandiArruti.E_commerce.exception.CredencialesInvalidasException;
import com.BlandiArruti.E_commerce.exception.DuplicadoException;
import com.BlandiArruti.E_commerce.exception.EntidadNoEncontradaException;
import com.BlandiArruti.E_commerce.geo.entity.Ciudad;
import com.BlandiArruti.E_commerce.geo.repository.CiudadRepository;
import com.BlandiArruti.E_commerce.notificacion.service.NotificacionService;
import com.BlandiArruti.E_commerce.pedido.dto.response.PedidoResponse;
import com.BlandiArruti.E_commerce.pedido.mapper.PedidoMapper;
import com.BlandiArruti.E_commerce.pedido.repository.PedidoRepository;
import com.BlandiArruti.E_commerce.shared.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ClienteService implements IClienteService {

    private final ClienteRepository clienteRepository;
    private final DireccionRepository direccionRepository;
    private final CiudadRepository ciudadRepository;
    private final PedidoRepository pedidoRepository;
    private final ClienteMapper clienteMapper;
    private final DireccionMapper direccionMapper;
    private final PedidoMapper pedidoMapper;
    private final PasswordEncoder passwordEncoder;
    private final NotificacionService notificacionService;

    private UsuarioDetails getPrincipal() {
        return (UsuarioDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private void verificarPropietario(Long idCliente) {
        UsuarioDetails principal = getPrincipal();
        if (principal.getRol() == Rol.CLIENTE && !principal.getId().equals(idCliente)) {
            throw new AccessDeniedException("No tenés permiso para acceder a este recurso.");
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<ClienteResponse> listarTodos(Pageable pageable) {
        return PageResponse.from(
                clienteRepository.findAllByActivoTrue(pageable).map(clienteMapper::toResponse)
        );
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorId(Long id) {
        verificarPropietario(id);
        return clienteMapper.toResponse(
                clienteRepository.findByIdAndActivoTrue(id)
                        .orElseThrow(() -> EntidadNoEncontradaException.cliente(id))
        );
    }

    public ClienteResponse crear(ClienteRequest request) {
        if (clienteRepository.existsByEmail(request.email())) {
            throw DuplicadoException.email(request.email());
        }
        if (clienteRepository.existsByDni(request.dni())) {
            throw DuplicadoException.dni(request.dni());
        }
        Cliente cliente = clienteMapper.toEntity(request);
        cliente.setContrasenia(passwordEncoder.encode(request.contrasenia()));
        cliente = clienteRepository.save(cliente);

        notificacionService.notificarRegistroExitoso(cliente);

        return clienteMapper.toResponse(cliente);
    }

    public ClienteResponse actualizar(Long id, ClienteUpdateRequest request) {
        verificarPropietario(id);
        Cliente cliente = clienteRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> EntidadNoEncontradaException.cliente(id));

        if (!cliente.getEmail().equals(request.email())
                && clienteRepository.existsByEmail(request.email())) {
            throw DuplicadoException.email(request.email());
        }

        cliente.setNombre(request.nombre());
        cliente.setApellido(request.apellido());
        cliente.setEmail(request.email());
        return clienteMapper.toResponse(clienteRepository.save(cliente));
    }

    public void cambiarPassword(Long id, CambioPasswordRequest request) {
        verificarPropietario(id);
        Cliente cliente = clienteRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> EntidadNoEncontradaException.cliente(id));

        if (!passwordEncoder.matches(request.contraseniaActual(), cliente.getContrasenia())) {
            throw new CredencialesInvalidasException("La contraseña actual no es correcta.");
        }

        cliente.setContrasenia(passwordEncoder.encode(request.contraseniaNueva()));
        clienteRepository.save(cliente);
    }

    public void eliminar(Long id) {
        verificarPropietario(id);
        Cliente cliente = clienteRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> EntidadNoEncontradaException.cliente(id));
        cliente.setActivo(false);
        clienteRepository.save(cliente);
    }

    @Transactional(readOnly = true)
    public List<DireccionResponse> listarDirecciones(Long idCliente) {
        verificarPropietario(idCliente);
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> EntidadNoEncontradaException.cliente(idCliente));
        return cliente.getDirecciones().stream()
                .map(direccionMapper::toResponse)
                .toList();
    }

    public DireccionResponse agregarDireccion(Long idCliente, DireccionRequest request) {
        verificarPropietario(idCliente);
        Cliente cliente = clienteRepository.findByIdAndActivoTrue(idCliente)
                .orElseThrow(() -> EntidadNoEncontradaException.cliente(idCliente));
        Ciudad ciudad = ciudadRepository.findById(request.idCiudad())
                .orElseThrow(() -> new EntidadNoEncontradaException(
                        "Ciudad con id " + request.idCiudad() + " no encontrada."));

        Direccion direccion = Direccion.builder()
                .nombreCalle(request.nombreCalle())
                .numeroCalle(request.numeroCalle())
                .codigoPostal(request.codigoPostal())
                .ciudad(ciudad)
                .cliente(cliente)
                .build();

        return direccionMapper.toResponse(direccionRepository.save(direccion));
    }

    public void eliminarDireccion(Long idCliente, Long idDireccion) {
        verificarPropietario(idCliente);
        if (clienteRepository.findByIdAndActivoTrue(idCliente).isEmpty()) {
            throw EntidadNoEncontradaException.cliente(idCliente);
        }
        Direccion direccion = direccionRepository.findById(idDireccion)
                .orElseThrow(() -> EntidadNoEncontradaException.direccion(idDireccion));
        if (!direccion.getCliente().getId().equals(idCliente)) {
            throw new AccessDeniedException("La dirección no pertenece al cliente indicado.");
        }
        direccionRepository.delete(direccion);
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> historialPedidos(Long idCliente, EstadoPedido estado) {
        verificarPropietario(idCliente);
        if (clienteRepository.findByIdAndActivoTrue(idCliente).isEmpty()) {
            throw EntidadNoEncontradaException.cliente(idCliente);
        }
        var pedidos = estado != null
                ? pedidoRepository.findByClienteIdAndEstadoPedido(idCliente, estado)
                : pedidoRepository.findByClienteId(idCliente);

        return pedidos.stream()
                .map(pedidoMapper::toResponse)
                .toList();
    }
}
