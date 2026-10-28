# BAN Sports — E-Commerce (Blandi & Arruti)

Tienda online de suplementos deportivos. El backend es una API REST desarrollada como Trabajo Práctico Final de Programación III, y el frontend en Angular 22 es el Trabajo Práctico Final de Programación IV — UTN Mar del Plata.

## Estructura del repositorio

```
backend/        ← API REST Spring Boot (Maven, src/, Dockerfile, colección Postman)
frontend/       ← SPA Angular 22 (nginx en Docker)
scripts/        ← SQL de la base (init de Docker, reparación de esquema) y config de pgAdmin
compose.yaml    ← levanta todo el stack con Docker
.env.example    ← variables de entorno de ejemplo
```

Los comandos de Maven (`./mvnw ...`) se corren desde `backend/`; los de npm/Angular, desde `frontend/`. Docker Compose, desde la raíz.

## Descripción del proyecto

El sistema resuelve la problemática de una tienda online que necesita gestionar su catálogo de productos, procesar pedidos, emitir facturas y coordinar el despacho de envíos. Integra pagos en línea mediante **MercadoPago Checkout Pro** y notifica a los clientes por email en los eventos clave del ciclo de vida del pedido.

### Reglas de negocio principales

- Un pedido se crea en estado `PENDIENTE_PAGO`. El stock **no** se descuenta hasta que el pago es confirmado.
- El pago puede realizarse de forma directa (endpoint propio) o a través de MercadoPago (flujo de dos pasos con webhook de confirmación).
- Al confirmar el pago se genera automáticamente una **Factura** (tipo A, B o C) y el pedido pasa a `PAGADO`.
- El envío solo puede crearse para pedidos en estado `PAGADO`. Al crear el envío el pedido pasa a `DESPACHADO`.
- Las transiciones de estado de envío siguen la secuencia `DESPACHADO → EN_CAMINO → ENTREGADO`.
- Las bajas de clientes y productos son **lógicas** (soft-delete mediante campo `activo`).
- Los clientes solo pueden acceder a sus propios recursos; los administradores tienen acceso irrestricto.

---

## Stack tecnológico

| Tecnología | Versión |
|---|---|
| Java | 21 |
| Spring Boot | 4.0.5 |
| Spring Security + JWT | jjwt 0.12.6 |
| Spring Data JPA | Hibernate 7.x |
| PostgreSQL | 17+ |
| MapStruct | 1.6.3 |
| MercadoPago SDK | 3.2.1 |
| SpringDoc OpenAPI | 3.0.2 |
| Spring AOP | (transversal) |
| Maven | 3.9+ |

---

## Arquitectura

El proyecto aplica una arquitectura en capas desacoplada mediante interfaces:

```
Controller → IService (interface) → ServiceImpl → Repository
```

- **Controllers** (`/controller`): reciben requests HTTP, delegan en interfaces de servicio.
- **Services** (`/service`): contienen la lógica de negocio; cada uno implementa su interfaz (`IXxxService`).
- **Repositories** (`/repository`): acceso a datos con Spring Data JPA.
- **DTOs** (`/dto`): Java records para entrada (`*Request`) y salida (`*Response`).
- **Mappers** (`/mapper`): conversión entre entidades y DTOs mediante MapStruct.
- **AOP** (`/aop`): `LoggingAspect` intercepta todos los métodos de servicio para registrar duración y errores.

### Patrones de diseño aplicados

| Patrón | Dónde |
|---|---|
| **Builder** | Todas las entidades JPA (Lombok `@Builder`) |
| **Singleton** | Todos los `@Service`, `@Component`, `@Repository` (Spring IoC) |
| **Factory Method** | `UsuarioDetails.fromCliente()` / `UsuarioDetails.fromAdmin()` |
| **Strategy** | Lógica de validación de stock (`StockRequest.operacion`) |
| **Template Method** | `GlobalExceptionHandler` — flujo uniforme de respuesta de error |

---

## Estructura de paquetes

```
backend/src/main/java/com/BlandiArruti/E_commerce/
├── aop/                  ← LoggingAspect (AOP transversal)
├── auth/                 ← JWT, OAuth2 GitHub, AuthController
├── administrador/        ← CRUD de administradores
├── carrito/              ← Carrito de compras
├── categoria/            ← Categorías de productos
├── cliente/              ← Clientes, direcciones
├── config/               ← DataLoader, SecurityConfig
├── cotizacion/           ← Cotización de envío (Correo Argentino)
├── envio/                ← Gestión de envíos
├── enums/                ← EstadoPedido, EstadoEnvio, TipoFactura, OperacionStock
├── exception/            ← Excepciones custom + GlobalExceptionHandler
├── factura/              ← Generación y consulta de facturas
├── geo/                  ← País, Provincia, Ciudad (datos geográficos AR)
├── mercadopago/          ← Checkout Pro + Webhook
├── notificacion/         ← Envío de emails (JavaMailSender)
├── pedido/               ← Pedidos e ítems
├── producto/             ← Productos y variantes con JSONB
└── shared/               ← PageResponse<T> genérico
```

---

## Endpoints principales

Base URL: `http://localhost:8080/api/v1`

| Método | Endpoint | Descripción | Auth |
|---|---|---|---|
| `POST` | `/auth/login` | Autenticación — devuelve JWT | Público |
| `POST` | `/auth/register` | Registro de cliente | Público |
| `GET` | `/auth/me` | Usuario detrás del token (id, username, rol) | Autenticado |
| `GET` | `/producto` | Listar productos (filtros: categoría, precio, búsqueda; orden por `sortBy`/`sortDir`) | Público |
| `GET` | `/producto/{id}` | Detalle de producto | Público |
| `POST` | `/producto` | Crear producto | ADMIN |
| `PATCH` | `/producto/{id}/variante/{vid}/stock` | Ajustar stock | ADMIN |
| `POST` | `/producto/{id}/imagen` | Subir/reemplazar imagen (multipart, JPEG/PNG/WEBP, máx 2 MB) | ADMIN |
| `GET` | `/producto/{id}/imagen` | Descargar la imagen del producto | Público |
| `DELETE` | `/producto/{id}/imagen` | Eliminar la imagen del producto | ADMIN |
| `GET` | `/categorias` | Listar categorías | Público |
| `GET` | `/cliente/{id}` | Ver perfil del cliente | CLIENTE / ADMIN |
| `PUT` | `/cliente/{id}` | Editar perfil (nombre, apellido, email) | CLIENTE / ADMIN |
| `PATCH` | `/cliente/{id}/password` | Cambiar contraseña (requiere la actual) | CLIENTE / ADMIN |
| `POST` | `/cliente/{id}/direccion` | Agregar dirección de envío | CLIENTE / ADMIN |
| `GET` | `/cliente/{id}/pedido` | Historial de pedidos del cliente | CLIENTE / ADMIN |
| `GET` | `/cliente/{id}/carrito` | Ver carrito | CLIENTE / ADMIN |
| `POST` | `/cliente/{id}/carrito/items` | Agregar ítem al carrito | CLIENTE / ADMIN |
| `POST` | `/cliente/{id}/carrito/checkout` | Crear pedido desde el carrito | CLIENTE / ADMIN |
| `PATCH` | `/pedido/{id}/pagar` | Pagar directamente (genera factura) | CLIENTE / ADMIN |
| `POST` | `/pedido/{id}/iniciar-pago` | Iniciar pago con MercadoPago | CLIENTE / ADMIN |
| `POST` | `/webhook/mercadopago` | Confirmación de pago MP | Público |
| `PATCH` | `/pedido/{id}/cancelar` | Cancelar pedido | CLIENTE / ADMIN |
| `PATCH` | `/pedido/{id}/estado` | Cambiar estado manualmente (no permite saltar a `PAGADO`) | ADMIN |
| `POST` | `/pedido/{id}/envio` | Crear envío para pedido PAGADO | ADMIN |
| `PATCH` | `/pedido/{id}/envio/estado` | Avanzar estado del envío | ADMIN |
| `GET` | `/factura` | Listar facturas (filtros: tipo, pedido, cliente) | ADMIN |
| `GET` | `/cotizacion` | Cotizar envío (Correo Argentino) | CLIENTE / ADMIN |

Documentación interactiva completa disponible en: `http://localhost:8080/swagger-ui.html`

---

## Prerrequisitos

Con Docker no hace falta nada más que **Docker Desktop**: Java, Maven y
PostgreSQL corren adentro de los contenedores.

Para correrlo sin Docker: **Java 21**, **Maven 3.9+** y **PostgreSQL 17+**.

---

## Ejecución con Docker (recomendado)

Con Docker Desktop abierto, desde la raíz del repo:

```bash
docker compose up -d --build
```

Eso es todo — no hace falta crear el `.env`: cada variable tiene un default en
`compose.yaml`. La primera vez tarda unos minutos (baja las imágenes y compila
la app); después arranca en segundos.

| Servicio | URL | Para qué |
|---|---|---|
| **Backend** | http://localhost:8080 | La API |
| Swagger UI | http://localhost:8080/swagger-ui.html | Documentación interactiva |
| **Mailpit** | http://localhost:8025 | Bandeja de entrada: acá caen todos los mails que manda la app |
| **PostgreSQL** | `localhost:5432` | Base de datos (`ecommerce` / `ecommerce`) |
| pgAdmin | http://localhost:5050 | Cliente web de la base (profile `tools`) |
| ngrok | http://localhost:4040 | Túnel público para el webhook de MP (profile `mp`) |
| **Frontend** | http://localhost:4200 | Angular, servido por nginx (profile `front`) |

La base arranca con datos de prueba: usuario **`admin` / `admin123`**, 17
productos, categorías, clientes y pedidos.

### Servicios opcionales

Los servicios que no son parte del núcleo están detrás de un *profile*: si no
los pedís, ni se descargan.

```bash
docker compose --profile tools up -d    # suma pgAdmin
docker compose --profile mp up -d       # suma ngrok (webhook de MercadoPago)
docker compose --profile front up -d    # suma el front de Progra 4
docker compose --profile all up -d      # todo junto
```

### Pagos con MercadoPago

MP avisa que un pago se aprobó pegándole a un webhook, y para eso necesita una
URL pública: `localhost` no le sirve. El profile `mp` levanta **ngrok**, que
publica el backend en internet mientras el stack está corriendo.

Hace falta una cuenta gratuita en [ngrok](https://dashboard.ngrok.com):

1. Copiá tu [authtoken](https://dashboard.ngrok.com/get-started/your-authtoken).
2. Reclamá tu [dominio estático](https://dashboard.ngrok.com/domains) (gratis, uno por cuenta).
3. Ponelos en el `.env`:

```bash
NGROK_AUTHTOKEN=tu_token
NGROK_DOMAIN=tu-dominio.ngrok-free.dev
MP_ACCESS_TOKEN=APP_USR-...
```

```bash
docker compose --profile mp up -d
```

`MP_NOTIFICATION_URL` y `MP_BACK_URL` se arman solas a partir de
`NGROK_DOMAIN` — no hace falta escribirlas. Si les ponés un valor a mano, ese
gana y el dominio se ignora.

> **El dominio estático no es opcional en la práctica.** Sin él ngrok da una URL
> distinta en cada arranque, y la app lee `MP_NOTIFICATION_URL` una sola vez al
> iniciar: habría que reconfigurar y reiniciar antes de cada demo.

Para ver si MP está llamando al webhook, abrí el inspector de ngrok en
**http://localhost:4040**: muestra cada request que entra por el túnel con su
body y su respuesta.

### Comandos del día a día

```bash
docker compose logs -f app              # ver los logs del backend en vivo
docker compose restart app              # reiniciar solo el backend
docker compose up -d --build app        # recompilar tras cambiar código Java
docker compose down                     # frenar todo (los datos se conservan)
docker compose down -v                  # frenar y BORRAR la base
docker compose ps                       # estado y salud de cada servicio
```

### Si algo no arranca

| Síntoma | Causa y solución |
|---|---|
| `port is already allocated` | Ya tenés algo en ese puerto (típico: un PostgreSQL propio en el 5432). Cambiá `DB_PORT` o `APP_PORT` en el `.env`. |
| `app` queda en `unhealthy` | Mirá `docker compose logs app`. Casi siempre es la base todavía inicializándose: la app espera hasta 60s antes de reportar el fallo. |
| `db` reinicia en loop | Si venís de una versión anterior, el volumen puede tener un formato viejo. `docker compose down -v` y volvé a levantar. |
| Cambié código Java y no se ve | La imagen hay que reconstruirla: `docker compose up -d --build app`. |

---

## Ejecución local (sin Docker)

```bash
# 1. Crear la base de datos en PostgreSQL
createdb ecommerce

# 2. Configurar variables de entorno o crear backend/src/main/resources/application-local.yaml
#    con los valores correspondientes (ver Variables de entorno)

# 3. Compilar y ejecutar
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Si ya tenías una base de una versión anterior

`ddl-auto: update` agrega tablas y columnas nuevas, pero **nunca afloja una
restricción existente** y falla en silencio al agregar una columna `NOT NULL`
sobre una tabla que ya tiene filas. Eso deja el esquema desfasado y produce
errores 500 que aparecen recién en runtime. Para repararlo:

```bash
psql -U postgres -d ecommerce -f scripts/reparar-schema.sql
```

El script es idempotente. Si arrancás con una base vacía no hace falta:
Hibernate genera el esquema correcto de cero.

---

## Variables de entorno

| Variable | Descripción | Ejemplo |
|---|---|---|
| `DB_URL` | URL JDBC de la base de datos | `jdbc:postgresql://localhost:5432/ecommerce` |
| `DB_USERNAME` | Usuario de PostgreSQL | `postgres` |
| `DB_PASSWORD` | Contraseña de PostgreSQL | `secreto` |
| `JWT_SECRET` | Clave secreta para firmar tokens (mín. 32 chars) | `clave-super-secreta-de-64-caracteres...` |
| `MP_ACCESS_TOKEN` | Access token de MercadoPago | `APP_USR-...` |
| `MP_NOTIFICATION_URL` | URL pública para webhook de MP | `https://tu-dominio.com/api/v1/webhook/mercadopago` |
| `MP_BACK_URL` | URL de retorno tras el pago | `http://localhost:8080` |
| `MAIL_HOST` | Host SMTP | `smtp.gmail.com` |
| `MAIL_PORT` | Puerto SMTP | `587` |
| `MAIL_USERNAME` | Cuenta de correo | `tu@gmail.com` |
| `MAIL_PASSWORD` | Contraseña / app password | `xxxx xxxx xxxx xxxx` |
| `CORREO_ARGENTINO_USER_TOKEN` | Token API Correo Argentino | `...` |
| `GITHUB_CLIENT_ID` | Client ID de GitHub OAuth2 | `Ov23li...` |
| `GITHUB_CLIENT_SECRET` | Client Secret de GitHub OAuth2 | `...` |
| `FRONT_ORIGINS` | Orígenes del front habilitados por CORS (separados por coma) | `http://localhost:5173` |
| `OAUTH2_REDIRECT_URI` | A dónde vuelve el navegador tras el login con GitHub (vacío = responde JSON) | `http://localhost:5173/oauth2/callback` |

Copiar `.env.example` como `.env` y completar con los valores reales.

---

## Tests

El proyecto incluye **129 tests** (128 unitarios en 13 clases + 1 de integración), usando JUnit 5, Mockito, AssertJ y Testcontainers.

> El test de integración levanta un PostgreSQL real en un contenedor, así que
> necesita Docker Desktop corriendo. Sin Docker, corré solo los unitarios:
> `./mvnw test -Dtest='!ECommerceApplicationTests'`

```bash
# Ejecutar todos los tests (desde backend/)
cd backend
./mvnw test

# El test de integración (contextLoads) requiere Docker para levantar PostgreSQL
# Si Docker no está disponible, se saltea automáticamente
```

Cobertura de tests unitarios:
- `AuthServiceTest` — 5 tests (login OK, credenciales inválidas, cuenta bloqueada, cuenta desactivada, registro)
- `LoginAttemptServiceTest` — 8 tests (contador de intentos, bloqueo a los 5 fallos, reset tras login exitoso)
- `CarritoServiceTest` — 13 tests (obtener/crear carrito, agregar/fusionar ítems, stock, actualizar, eliminar, checkout)
- `CategoriaServiceTest` — 12 tests (listar con/sin filtro, crear, duplicado, actualizar, eliminar con confirmación)
- `WebhookControllerTest` — 7 tests (tipos inválidos, pago aprobado/rechazado, reference malformada)
- `PedidoServiceTest` — 14 tests (cancelación, pago, envío, estado, confirmación MP)
- `ProductoServiceTest` — 11 tests (variantes, stock, ownership)
- `ClienteServiceTest` — 12 tests (CRUD, owner-check, acceso)
- `EnvioServiceTest` — 10 tests (ciclo de vida del envío)
- `AdministradorServiceTest` — 11 tests (CRUD, duplicados)
- `CotizacionEnvioServiceTest` — 5 tests (cotización Correo Argentino)
- `NotificacionServiceTest` — 4 tests (emails, fallo de conexión)

---

## Colección Postman

Importar el archivo `backend/postman/E-Commerce_Blandi_Arruti.postman_collection.json` en Postman para acceder a todas las solicitudes preconfiguradas.

La colección incluye:
- Variables de entorno (`baseUrl`, `token`)
- Script de login que guarda el JWT automáticamente en `{{token}}`
- Requests organizados por módulo (Auth, Productos, Pedidos, etc.)

---

## Integrantes

| ----------Nombre--------- |
|-------------|-------------|
| Alan Arruti | Iara Blandi |

**Carrera:** Tecnicatura Universitaria en Programación — UTN Mar del Plata  
**Materia:** Programación III  
**Docente:** Lic. Mango Eduardo
