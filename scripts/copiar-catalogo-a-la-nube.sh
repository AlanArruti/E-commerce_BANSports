#!/usr/bin/env bash
# Copia el catálogo (categorías, productos, variantes, imágenes) y los administradores
# de la base local de Docker a la base de la nube. No copia clientes, carritos ni pedidos.
#
# Antes: el backend de la nube tiene que haber arrancado una vez (crea las tablas)
# y esas tablas tienen que estar vacías.
#
# Uso (la URL sale del panel de Neon → Connection string, formato postgresql://...):
#   NUBE_DB_URL='postgresql://...' ./scripts/copiar-catalogo-a-la-nube.sh
# La URL tiene la contraseña: pasala así, no la guardes en ningún archivo del repo.
set -euo pipefail

if [ -z "${NUBE_DB_URL:-}" ]; then
  echo "Falta NUBE_DB_URL (connection string de Neon, postgresql://...)." >&2
  exit 1
fi

TABLAS=(administradores categorias productos variantes imagenes_producto)
ARGS=()
for t in "${TABLAS[@]}"; do ARGS+=(-t "$t"); done

# pg_dump corre en el contenedor de la base local y psql en un contenedor descartable,
# así no hace falta instalar Postgres. Todo en una transacción: si algo falla, no queda nada a medias.
docker exec ecommerce-db pg_dump -U "${DB_USERNAME:-ecommerce}" -d "${DB_NAME:-ecommerce}" \
    --data-only --no-owner --no-privileges "${ARGS[@]}" \
  | docker run --rm -i -e NUBE_DB_URL postgres:18-alpine \
    sh -c 'psql "$NUBE_DB_URL" -v ON_ERROR_STOP=1 --single-transaction -q >/dev/null'

echo "Listo: catálogo y administradores copiados a la nube."
