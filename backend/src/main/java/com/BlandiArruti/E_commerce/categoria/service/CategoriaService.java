package com.BlandiArruti.E_commerce.categoria.service;

import com.BlandiArruti.E_commerce.categoria.dto.request.CategoriaRequest;
import com.BlandiArruti.E_commerce.categoria.dto.response.CategoriaResponse;
import com.BlandiArruti.E_commerce.categoria.dto.response.EliminacionResponse;
import com.BlandiArruti.E_commerce.categoria.entity.Categoria;
import com.BlandiArruti.E_commerce.categoria.mapper.CategoriaMapper;
import com.BlandiArruti.E_commerce.categoria.repository.CategoriaRepository;
import com.BlandiArruti.E_commerce.exception.DuplicadoException;
import com.BlandiArruti.E_commerce.exception.EntidadNoEncontradaException;
import com.BlandiArruti.E_commerce.producto.entity.Producto;
import com.BlandiArruti.E_commerce.producto.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CategoriaService implements ICategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final CategoriaMapper categoriaMapper;

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarTodas(String nombre) {
        List<Categoria> categorias = (nombre != null && !nombre.isBlank())
                ? categoriaRepository.findByNombreContainingIgnoreCase(nombre.trim())
                : categoriaRepository.findAll();

        return categorias.stream()
                .map(categoriaMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse buscarPorId(Long id) {
        return categoriaMapper.toResponse(
                categoriaRepository.findById(id)
                        .orElseThrow(() -> EntidadNoEncontradaException.categoria(id))
        );
    }

    public CategoriaResponse crear(CategoriaRequest request) {
        if (categoriaRepository.findByNombre(request.nombre()).isPresent()) {
            throw new DuplicadoException("La categoría '" + request.nombre() + "' ya existe.");
        }
        Categoria categoria = categoriaMapper.toEntity(request);
        return categoriaMapper.toResponse(categoriaRepository.save(categoria));
    }

    public CategoriaResponse actualizar(Long id, CategoriaRequest request) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> EntidadNoEncontradaException.categoria(id));

        if (!categoria.getNombre().equals(request.nombre())
                && categoriaRepository.findByNombre(request.nombre()).isPresent()) {
            throw new DuplicadoException("La categoría '" + request.nombre() + "' ya existe.");
        }

        categoria.setNombre(request.nombre());
        return categoriaMapper.toResponse(categoriaRepository.save(categoria));
    }

    public EliminacionResponse eliminar(Long id, boolean confirmar) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> EntidadNoEncontradaException.categoria(id));

        List<Producto> productosAfectados = productoRepository.findByCategoriaId(id);

        List<String> nombresAfectados = productosAfectados.stream()
                .map(p -> "id " + p.getId() + " — " + p.getNombre())
                .toList();

        if (!confirmar) {
            if (productosAfectados.isEmpty()) {
                return new EliminacionResponse(
                        "¿Está seguro que desea eliminar la categoría '" + categoria.getNombre() + "'? " +
                        "No tiene productos asociados. Para confirmar, reenvíe la solicitud con ?confirmar=true.",
                        List.of()
                );
            }
            return new EliminacionResponse(
                    "¿Está seguro que desea eliminar la categoría '" + categoria.getNombre() + "'? " +
                    "Los siguientes " + productosAfectados.size() + " producto(s) quedarán sin categoría asignada. " +
                    "Para confirmar, reenvíe la solicitud con ?confirmar=true.",
                    nombresAfectados
            );
        }

        for (Producto producto : productosAfectados) {
            producto.setCategoria(null);
            productoRepository.save(producto);
        }
        categoriaRepository.delete(categoria);

        if (productosAfectados.isEmpty()) {
            return new EliminacionResponse(
                    "Categoría '" + categoria.getNombre() + "' eliminada correctamente.",
                    List.of()
            );
        }
        return new EliminacionResponse(
                "Categoría '" + categoria.getNombre() + "' eliminada. " +
                productosAfectados.size() + " producto(s) quedaron sin categoría asignada.",
                nombresAfectados
        );
    }
}
