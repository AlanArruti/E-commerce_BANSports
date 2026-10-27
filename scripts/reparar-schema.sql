-- =============================================================================
-- Reparación de esquema para bases creadas con versiones anteriores del proyecto.
--
-- ¿Por qué hace falta? `ddl-auto: update` agrega tablas y columnas nuevas, pero
-- NUNCA afloja una restricción que ya existe, y falla en silencio al agregar una
-- columna NOT NULL sobre una tabla que ya tiene filas. El resultado son 500 que
-- aparecen recién en runtime.
--
-- Si arrancás con una base vacía no necesitás correr esto: Hibernate genera el
-- esquema correcto de cero.
--
-- Uso:
--   psql -U postgres -d ecommerce -f scripts/reparar-schema.sql
--
-- Es idempotente: se puede correr varias veces sin romper nada.
-- =============================================================================

BEGIN;

-- 1) items_carrito.id_variante debe aceptar NULL.
--    Un producto sin variantes (un libro, por ejemplo) se agrega al carrito sin
--    variante. La entidad ya lo declara opcional, pero las bases viejas quedaron
--    con la columna NOT NULL y todo POST al carrito de esos productos da 500.
ALTER TABLE items_carrito ALTER COLUMN id_variante DROP NOT NULL;

-- 2) items_pedido.id_variante, por el mismo motivo.
ALTER TABLE items_pedido ALTER COLUMN id_variante DROP NOT NULL;

-- 3) direcciones.codigo_postal: se agregó después de que la tabla ya tenía
--    filas, así que el ALTER original falló y la columna nunca se creó.
--    Cualquier consulta que devuelva direcciones terminaba en 500.
ALTER TABLE direcciones ADD COLUMN IF NOT EXISTS codigo_postal VARCHAR(8);
UPDATE direcciones SET codigo_postal = '0000' WHERE codigo_postal IS NULL;
ALTER TABLE direcciones ALTER COLUMN codigo_postal SET DEFAULT '0000';
ALTER TABLE direcciones ALTER COLUMN codigo_postal SET NOT NULL;

-- 4) productos.tiene_imagen: bandera que indica si hay imagen cargada.
ALTER TABLE productos ADD COLUMN IF NOT EXISTS tiene_imagen BOOLEAN DEFAULT FALSE;
UPDATE productos SET tiene_imagen = FALSE WHERE tiene_imagen IS NULL;
ALTER TABLE productos ALTER COLUMN tiene_imagen SET NOT NULL;

-- 5) pedidos.fecha_creacion: los pedidos que ya existían quedan con la fecha
--    de la migración, que es lo mejor que se puede reconstruir.
ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS fecha_creacion TIMESTAMP DEFAULT NOW();
UPDATE pedidos SET fecha_creacion = NOW() WHERE fecha_creacion IS NULL;
ALTER TABLE pedidos ALTER COLUMN fecha_creacion SET NOT NULL;

COMMIT;

-- Verificación rápida: estas cinco filas deberían aparecer con los valores
-- esperados (is_nullable = YES en los id_variante, NO en el resto).
SELECT table_name, column_name, is_nullable, column_default
FROM information_schema.columns
WHERE (table_name = 'items_carrito' AND column_name = 'id_variante')
   OR (table_name = 'items_pedido'  AND column_name = 'id_variante')
   OR (table_name = 'direcciones'   AND column_name = 'codigo_postal')
   OR (table_name = 'productos'     AND column_name = 'tiene_imagen')
   OR (table_name = 'pedidos'       AND column_name = 'fecha_creacion')
ORDER BY table_name, column_name;
