package com.BlandiArruti.E_commerce.producto.repository;

import com.BlandiArruti.E_commerce.producto.entity.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    // Texto en minúsculas y sin tildes, para que "proteina" encuentre "Proteína".
    // Se reemplazan también las mayúsculas acentuadas por si LOWER no las convierte
    // (depende del locale de la base). La ñ se deja: es otra letra.
    // El texto buscado se normaliza igual en ProductoService.normalizarBusqueda.
    String SIN_TILDES_INICIO =
            "REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(" +
            "REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(LOWER(";
    String SIN_TILDES_FIN =
            "), 'Á', 'a'), 'É', 'e'), 'Í', 'i'), 'Ó', 'o'), 'Ú', 'u'), 'Ü', 'u'), " +
            "'á', 'a'), 'é', 'e'), 'í', 'i'), 'ó', 'o'), 'ú', 'u'), 'ü', 'u')";
    String NOMBRE_SIN_TILDES = SIN_TILDES_INICIO + "p.nombre" + SIN_TILDES_FIN;
    String CATEGORIA_SIN_TILDES = SIN_TILDES_INICIO + "c.nombre" + SIN_TILDES_FIN;

    Optional<Producto> findByUuid(String uuid);

    Optional<Producto> findByIdAndActivoTrue(Long id);

    List<Producto> findAllByActivoTrue();

    List<Producto> findByCategoriaId(Long idCategoria);

    List<Producto> findByActivoTrueAndCategoriaId(Long idCategoria);

    List<Producto> findByActivoTrueAndNombreContainingIgnoreCase(String texto);

    List<Producto> findByActivoTrueAndNombreContainingIgnoreCaseAndCategoriaId(String texto, Long categoriaId);

    // La búsqueda mira el nombre del producto y el de su categoría ("proteinas" trae
    // todo lo de Proteínas). LEFT JOIN para no perder productos sin categoría.
    @Query("SELECT p FROM Producto p LEFT JOIN p.categoria c WHERE p.activo = true " +
           "AND (:categoriaId IS NULL OR p.categoria.id = :categoriaId) " +
           "AND (:precioMin IS NULL OR p.precio >= :precioMin) " +
           "AND (:precioMax IS NULL OR p.precio <= :precioMax) " +
           "AND (" + NOMBRE_SIN_TILDES + " LIKE CONCAT('%', :search, '%') " +
           "OR " + CATEGORIA_SIN_TILDES + " LIKE CONCAT('%', :search, '%'))")
    Page<Producto> buscarConFiltros(@Param("categoriaId") Long categoriaId,
                                    @Param("precioMin") Double precioMin,
                                    @Param("precioMax") Double precioMax,
                                    @Param("search") String search,
                                    Pageable pageable);
}
