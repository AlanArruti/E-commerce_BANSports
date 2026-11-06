package com.BlandiArruti.E_commerce.favorito.repository;

import com.BlandiArruti.E_commerce.favorito.entity.Favorito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoritoRepository extends JpaRepository<Favorito, Long> {

    // Los productos dados de baja (activo = false) no se listan, pero el favorito
    // queda guardado por si el producto se vuelve a activar.
    List<Favorito> findByClienteIdAndProductoActivoTrueOrderByFechaAgregadoDesc(Long idCliente);

    Optional<Favorito> findByClienteIdAndProductoId(Long idCliente, Long idProducto);
}
