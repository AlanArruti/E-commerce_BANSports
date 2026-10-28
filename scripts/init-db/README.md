# Scripts de inicialización de la base

Todo archivo `.sql` que dejes acá se ejecuta **una única vez**: cuando Postgres
crea la base por primera vez (o sea, cuando el volumen `ecommerce-pgdata` está
vacío). Si el volumen ya existe, Docker ignora esta carpeta por completo.

Por eso **`reparar-schema.sql` no va acá**: ese script arregla tablas que
Hibernate crea al arrancar la app, que todavía no existen en este momento.
Se corre a mano contra una base ya poblada (ver el README principal).

Para empezar de cero y que esta carpeta vuelva a ejecutarse:

```bash
docker compose down -v && docker compose up -d --build
```
