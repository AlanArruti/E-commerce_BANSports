# BAN Sports — E-Commerce 

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

El sistema resuelve la problemática de una tienda online que necesita gestionar su catálogo de productos, procesar pedidos, emitir facturas y coordinar el despacho de envíos. Integra pagos en línea con **pasarelas de pago intercambiables** (hoy **MercadoPago Checkout Pro**, vía API de Orders) y notifica a los clientes por email en los eventos clave del ciclo de vida del pedido.

### Reglas de negocio principales

- Un pedido se crea en estado `PENDIENTE_PAGO`. El stock **no** se descuenta hasta que el pago es confirmado.
- El cliente paga con una pasarela (MercadoPago). Cada intento queda registrado como un `Pago`. El pedido pasa a `PAGADO` cuando la pasarela confirma el cobro, ya sea al volver el cliente a la tienda o por el webhook. Un admin también puede marcarlo pagado a mano.
- Al confirmar el pago se genera automáticamente una **Factura** (tipo A, B o C) y el pedido pasa a `PAGADO`.
- El envío solo puede crearse para pedidos en estado `PAGADO`. Al crear el envío el pedido pasa a `DESPACHADO`.
- Las transiciones de estado de envío siguen la secuencia `DESPACHADO → EN_CAMINO → ENTREGADO`.
- Cada **variante** puede tener su propio precio (por ejemplo, la de 2 kg cuesta más que la de 1 kg); si no tiene, cuesta lo mismo que el producto. La oferta del producto se aplica a todas sus variantes. Un pedido no acepta la variante de otro producto.
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
| MercadoPago | API de Orders (REST, con `RestClient`) |
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
├── config/               ← SecurityConfig
├── cotizacion/           ← Cotización de envío (Correo Argentino)
├── envio/                ← Gestión de envíos
├── enums/                ← EstadoPedido, EstadoEnvio, TipoFactura, OperacionStock
├── exception/            ← Excepciones custom + GlobalExceptionHandler
├── factura/              ← Generación y consulta de facturas
├── geo/                  ← País, Provincia, Ciudad (datos geográficos AR)
├── notificacion/         ← Envío de emails (JavaMailSender)
├── pago/                 ← Pagos con pasarelas (ver "Pasarelas de pago")
│   ├── pasarela/         ← PasarelaPago, PasarelaPagoBase, RegistroPasarelas
│   │   └── mercadopago/  ← MercadoPagoPasarela (hija) + cliente de la API de Orders
│   ├── service/          ← PagoService
│   └── controller/       ← PagoController, WebhookController
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
| `GET` | `/auth/proveedores` | Proveedores de login social con credenciales cargadas (`github`, `google`) | Público |
| `GET` | `/oauth2/authorization/{github\|google}` | Inicia el login con GitHub o Google (lo abre el navegador) | Público |
| `POST` | `/auth/recuperar` | Manda por mail un enlace para elegir contraseña nueva (responde igual exista o no la cuenta) | Público |
| `POST` | `/auth/restablecer` | Guarda la contraseña nueva con el token del enlace (un solo uso, vence a los 30 min) | Público |
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
| `GET` | `/pago/pasarelas` | Pasarelas de pago disponibles (con credenciales cargadas) | CLIENTE / ADMIN |
| `POST` | `/pago/pedido/{pedidoId}` | Iniciar el pago `{ pasarela, tipoFactura }` → `{ pagoId, urlPago }` | CLIENTE / ADMIN |
| `POST` | `/pago/{pagoId}/confirmar` | Confirmar el pago al volver de la pasarela (consulta el estado real) | CLIENTE / ADMIN |
| `GET` | `/pago/pedido/{pedidoId}` | Intentos de pago de un pedido | CLIENTE / ADMIN |
| `POST` | `/webhook/{pasarela}` | Aviso de una pasarela (ej. `/webhook/mercadopago`) | Público |
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

La base arranca vacía: la app crea las tablas, pero no carga datos de prueba.

- **Clientes:** se crean desde "Registrarse" en el front (o `POST /api/v1/auth/register`).
- **Administradores:** no tienen registro público. El primero se crea a mano con
  un `INSERT` en `administradores`, con la contraseña hasheada en BCrypt; los
  siguientes, desde la API (`POST /api/v1/administrador`, solo ADMIN).
- **Catálogo:** categorías y productos se cargan desde el panel de administración.
- **Provincias y ciudades:** no se precargan. El formulario de dirección busca la
  localidad en [Georef](https://www.argentina.gob.ar/datos-abiertos/georef), la API
  de datos geográficos del Estado argentino (gratis, sin key ni registro), y al
  guardar el back crea la provincia y la ciudad si todavía no existen.

### Servicios opcionales

Los servicios que no son parte del núcleo están detrás de un *profile*: si no
los pedís, ni se descargan.

```bash
docker compose --profile tools up -d    # suma pgAdmin
docker compose --profile mp up -d       # suma ngrok (webhook de MercadoPago)
docker compose --profile front up -d    # suma el front de Progra 4
docker compose --profile all up -d      # todo junto
```

### Pasarelas de pago

El pago no está atado a MercadoPago. El paquete `pago/` define una **pasarela de pago**
abstracta, y MercadoPago es su primera implementación.

```
PasarelaPago (interfaz)
  └── PasarelaPagoBase (clase abstracta: algoritmo común, Template Method)
        └── MercadoPagoPasarela (hija: adapta la API de Orders de MercadoPago)
RegistroPasarelas   → todas las pasarelas, inyectadas por Spring, por código
PagoService         → caso de uso: iniciar, confirmar al volver, procesar el webhook
Pago (entidad)      → cada intento: pasarela, id externo, estado, monto
```

**Flujo**
1. El cliente elige la pasarela → `POST /pago/pedido/{id}` registra un `Pago` y devuelve la URL de la pasarela.
2. Paga allá y vuelve a `/pago/resultado?pago={id}` del front.
3. Esa pantalla llama a `POST /pago/{id}/confirmar`. El back **consulta el cobro en la pasarela**,
   no confía en la URL, y si está aprobado y cubre el total el pedido pasa a `PAGADO`.
4. El webhook `POST /webhook/{pasarela}` hace lo mismo cuando la pasarela avisa. Es opcional:
   sin él, el paso 3 alcanza.

**Sumar una pasarela nueva** (Stripe, MODO…): crear una clase `@Component` que extienda
`PasarelaPagoBase` e implemente `codigo()`, `nombre()`, `estaDisponible()`, `crearCobro()`,
`buscarCobro()` y `extraerIdDeNotificacion()`. No hay que tocar nada más, ni en el back ni en el front:
el front muestra un botón por cada pasarela disponible.

| Patrón / principio | Dónde |
|---|---|
| Template Method | `PasarelaPagoBase`: los métodos públicos son `final` y llaman a los pasos que completa cada hija |
| Strategy + Polimorfismo (GRASP) | Cada pasarela es intercambiable detrás de `PasarelaPago` |
| Adapter | `MercadoPagoPasarela` traduce los estados de MercadoPago a `EstadoPago` |
| Registry · Pure Fabrication (GRASP) | `RegistroPasarelas` |
| Controller (GRASP) | `PagoService` coordina el caso de uso |
| Experto en Información (GRASP) | El pedido decide cuándo puede pasar a `PAGADO` (`registrarPagoAprobado`) |
| Variaciones Protegidas · Bajo Acoplamiento | Pedido, front y webhook no conocen ninguna pasarela concreta |
| SOLID | **S**: una responsabilidad por clase · **O**: pasarela nueva = clase nueva · **L**: cualquier hija reemplaza a la base · **D**: todo depende de la interfaz |

### Pagos con MercadoPago

Se usa **Checkout Pro con la API de Orders** (la que recomienda MercadoPago; la de
Preferences quedó como legacy).

**Credenciales de prueba.** Para probar no se usa la cuenta real (los pagos serían
reales y las tarjetas de prueba fallan):

1. En [Tus integraciones](https://www.mercadopago.com.ar/developers/panel/app) → tu
   aplicación → **Cuentas de prueba**, creá un **Vendedor** y un **Comprador**.
2. En una ventana de incógnito, entrá al panel con el usuario del **vendedor de prueba**,
   creá una aplicación (Checkout Pro → **API de Orders**) y copiá su **Access Token de
   producción** (`APP_USR-...`; es de una cuenta ficticia).
3. En el `.env`: `MP_ACCESS_TOKEN=APP_USR-...` y `docker compose up -d app`.
4. Para pagar, entrá en MercadoPago con el **comprador de prueba** y usá una
   [tarjeta de prueba](https://www.mercadopago.com.ar/developers/es/docs/checkout-pro/integration-test/test-purchases)
   (Mastercard `5031 7557 3453 0604`, CVV `123`, vto. `11/30`, DNI `12345678`).
   Titular `APRO` → aprobado; `OTHE` → rechazado.

**Webhook (opcional).** MercadoPago tiene que poder llegar al backend, y `localhost` no
le sirve. El profile `mp` levanta **ngrok** con tu dominio estático gratuito:

```bash
NGROK_AUTHTOKEN=tu_token
NGROK_DOMAIN=tu-dominio.ngrok-free.dev
```

```bash
docker compose --profile mp up -d
```

Después, en el panel de MercadoPago → tu aplicación → **Webhooks**, cargá
`https://tu-dominio.ngrok-free.dev/api/v1/webhook/mercadopago` con el evento **Order**.
El inspector de ngrok (**http://localhost:4040**) muestra cada aviso que llega.

### Mails reales con Gmail

Por defecto todos los mails caen en Mailpit y no salen a internet. Para que
lleguen a casillas reales se usa el SMTP de Gmail, que es gratis (unos 500
destinatarios por día).

1. En la cuenta de Gmail del proyecto, activá la
   [verificación en 2 pasos](https://myaccount.google.com/signinoptions/twosv).
2. Creá una [contraseña de aplicación](https://myaccount.google.com/apppasswords)
   (nombre: "BAN Sports"). Google muestra 16 letras **una sola vez**.
3. Agregá esto a tu `.env` (nunca al repo: es público):

```bash
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=la-casilla@gmail.com
SMTP_PASSWORD=las16letrassinespacios
```

```bash
docker compose up -d app
```

Con `SMTP_HOST` definido se activan solos la autenticación y STARTTLS, y el
remitente pasa a ser `SMTP_USERNAME` (Gmail reescribe cualquier otro). Para
volver a Mailpit, borrá esas líneas y repetí el `up`.

> Dentro de Docker se usan `SMTP_*` y no `MAIL_*` a propósito: las `MAIL_*` del
> `.env` son para correr fuera de Docker (Mailtrap), y si se colaran al
> contenedor los mails se perderían en silencio.

Si Gmail rechaza el login (`535 Username and Password not accepted`), revisá
que la verificación en 2 pasos esté activa y que la contraseña sea la de
aplicación, no la de la cuenta.

`FRONT_URL` (default `http://localhost:4200`) es la base de los enlaces que van
en los mails, como el de recuperar la contraseña. En un deploy tiene que ser la
URL pública del front.

### Login con GitHub y Google

Los botones "Continuar con GitHub / Google" aparecen solo si el `.env` tiene las
credenciales de ese proveedor. Las dos son gratis.

**GitHub** — https://github.com/settings/developers → OAuth Apps → New OAuth App:

| Campo | Valor |
|---|---|
| Homepage URL | `http://localhost:4200` |
| Authorization callback URL | `http://localhost:4200/login/oauth2/code/github` |

**Google** — https://console.cloud.google.com → Google Auth Platform (no pide tarjeta):

1. **Información de la marca:** nombre `BAN Sports` y tu mail.
2. **Público:** Externo, **en prueba**. Agregá como usuarios de prueba los Gmail que
   van a entrar (solo esos pueden mientras la app no se publique).
3. **Clientes → Crear cliente**, aplicación web:
   - Orígenes autorizados: `http://localhost:4200`
   - URI de redireccionamiento: `http://localhost:4200/login/oauth2/code/google`

Después, en el `.env` (nunca en el repo):

```bash
GITHUB_CLIENT_ID=...
GITHUB_CLIENT_SECRET=...
GOOGLE_CLIENT_ID=....apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-...
```

```bash
docker compose up -d app
```

Cómo funciona: el botón lleva a `/oauth2/authorization/{proveedor}` (nginx lo pasa
al back), el proveedor vuelve a `/login/oauth2/code/{proveedor}` y el back redirige
a `/ingreso-social?token=...` del front, que guarda la sesión. La cuenta se busca por
email: si existe se usa esa, si no se crea. Si el usuario cancela o algo falla, vuelve
al login con un mensaje.

> Las URLs de vuelta tienen que coincidir **exactamente** con las registradas
> (`http`, puerto `4200`, sin `/` final). En un deploy hay que agregar las de la URL
> pública y poner `FRONT_URL` con esa dirección.

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

## Deploy en la nube

Todo con planes gratis:

| Parte | Servicio | Archivo |
|---|---|---|
| Base | [Neon](https://neon.tech) (Postgres) | — |
| Back | [Render](https://render.com) (web service desde el `Dockerfile`) | `render.yaml` |
| Front | [Vercel](https://vercel.com) (sitio estático) | `frontend/vercel.json` |
| Mails | [Brevo](https://www.brevo.com) (SMTP, 300 mails/día) | — |

El front reenvía `/api`, `/oauth2` y `/login/oauth2` al back (igual que nginx en Docker),
así que el navegador ve un solo dominio y no hace falta tocar el código.

**Demo:** https://bansports.vercel.app (si Render "duerme" el back, el primer pedido tarda hasta un minuto).

> Los secretos (base, JWT, OAuth, MercadoPago, mail) se cargan **solo** en el panel de
> Render. El repo es público: nunca van en `render.yaml` ni en ningún archivo.

### Primera vez

1. **Base (Neon).** Crear un proyecto (región AWS US East) y copiar la *connection string*.
   Para Render se separa en tres variables:
   `DB_URL=jdbc:postgresql://<host>/<base>?sslmode=require`, `DB_USERNAME` y `DB_PASSWORD`.
2. **Back (Render).** New → Blueprint → este repo. Render lee `render.yaml` y pide las
   variables marcadas `sync: false`. `JWT_SECRET` la genera Render sola. Las opcionales
   (GitHub, Google, MercadoPago) se pueden dejar vacías: esa función no aparece.
   La URL queda `https://bansports-api.onrender.com`.
3. **Front (Vercel).** Add New → Project → este repo, con *Root Directory* `frontend`.
   El resto sale de `vercel.json`. Si Render le dio otro nombre al back, cambiarlo en
   los `rewrites`; si Vercel le da otro dominio al front, cambiarlo en las 4 variables
   `FRONT_*` / `OAUTH2_*` de Render.
4. **Catálogo y primer admin.** Con el back ya arrancado una vez (crea las tablas), copiar
   los de la base local de Docker. No copia clientes ni pedidos:

   ```bash
   NUBE_DB_URL='postgresql://...' ./scripts/copiar-catalogo-a-la-nube.sh
   ```
5. **Mails (Brevo).** Render gratis bloquea los puertos SMTP 25, 465 y 587, así que Gmail
   no anda desde ahí; Brevo acepta el 2525. Crear la cuenta, verificar el remitente en
   *Senders* y generar una *SMTP key*. En Render: `MAIL_USERNAME` = el login SMTP de Brevo,
   `MAIL_PASSWORD` = la SMTP key, `MAIL_FROM` = el remitente verificado.
6. **Login social.** Sumar en GitHub y en Google la URL de vuelta
   `https://bansports.vercel.app/login/oauth2/code/github` (y `.../google`). En Google,
   además, el origen `https://bansports.vercel.app`.
7. **Webhook de MercadoPago.** En el panel de MP → Webhooks, tópico *Order*:
   `https://bansports-api.onrender.com/api/v1/webhook/mercadopago`.

### Volver a deployar

Cada push a `main` redeploya solo: Render el back y Vercel el front. Para forzarlo:
Render → *Manual Deploy* y Vercel → *Redeploy*.

---

## Variables de entorno

| Variable | Descripción | Ejemplo |
|---|---|---|
| `DB_URL` | URL JDBC de la base de datos | `jdbc:postgresql://localhost:5432/ecommerce` |
| `DB_USERNAME` | Usuario de PostgreSQL | `postgres` |
| `DB_PASSWORD` | Contraseña de PostgreSQL | `secreto` |
| `JWT_SECRET` | Clave secreta para firmar tokens (mín. 32 chars) | `clave-super-secreta-de-64-caracteres...` |
| `MP_ACCESS_TOKEN` | Access token de MercadoPago | `APP_USR-...` |
| `MAIL_HOST` | Host SMTP | `smtp.gmail.com` |
| `MAIL_PORT` | Puerto SMTP | `587` |
| `MAIL_USERNAME` | Cuenta de correo | `tu@gmail.com` |
| `MAIL_PASSWORD` | Contraseña / app password | `xxxx xxxx xxxx xxxx` |
| `MAIL_FROM` | Remitente de los mails (con Gmail, la misma casilla) | `tu@gmail.com` |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM` | Solo Docker: SMTP real. Sin `SMTP_HOST` los mails van a Mailpit (ver "Mails reales con Gmail") | `smtp.gmail.com` |
| `FRONT_URL` | URL del front, para armar los enlaces de los mails | `http://localhost:4200` |
| `CORREO_ARGENTINO_USER_TOKEN` | Token API Correo Argentino | `...` |
| `GITHUB_CLIENT_ID` | Client ID de GitHub OAuth2 | `Ov23li...` |
| `GITHUB_CLIENT_SECRET` | Client Secret de GitHub OAuth2 | `...` |
| `GOOGLE_CLIENT_ID` | ID de cliente OAuth de Google | `...apps.googleusercontent.com` |
| `GOOGLE_CLIENT_SECRET` | Secreto del cliente OAuth de Google | `GOCSPX-...` |
| `FRONT_ORIGINS` | Orígenes del front habilitados por CORS (separados por coma) | `http://localhost:5173` |
| `OAUTH2_REDIRECT_URI` | A dónde vuelve el navegador tras el login con GitHub/Google. En Docker por defecto `${FRONT_URL}/ingreso-social`; fuera de Docker vacío = responde JSON (Postman) | `http://localhost:4200/ingreso-social` |
| `OAUTH2_CALLBACK_BASE` | Solo en la nube: base de la URL a la que vuelven GitHub y Google (`<base>/login/oauth2/code/{proveedor}`). Vacía = la del navegador | `https://bansports.vercel.app` |
| `PORT` | Puerto en el que escucha el back (lo pone Render). Por defecto 8080 | `10000` |

Copiar `.env.example` como `.env` y completar con los valores reales.

---

## Tests

El proyecto incluye **208 tests** (207 unitarios en 22 clases + 1 de integración), usando JUnit 5, Mockito, AssertJ y Testcontainers.

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
- `OAuth2ClienteServiceTest` — 10 tests (GitHub y Google: alta, vinculación por email, email privado de GitHub, email sin verificar, cuenta desactivada)
- `RecuperacionPasswordServiceTest` — 9 tests (enlace por mail, email inexistente o dado de baja, pedido repetido, token vencido, usado o inexistente)
- `LoginAttemptServiceTest` — 8 tests (contador de intentos, bloqueo a los 5 fallos, reset tras login exitoso)
- `CarritoServiceTest` — 13 tests (obtener/crear carrito, agregar/fusionar ítems, stock, actualizar, eliminar, checkout)
- `CategoriaServiceTest` — 12 tests (listar con/sin filtro, crear, duplicado, actualizar, eliminar con confirmación)
- `PagoServiceTest` — 9 tests (iniciar, confirmar aprobado/rechazado/repetido, cobro de otro pago, webhook)
- `PasarelaPagoBaseTest` — 11 tests (algoritmo de la clase padre con una pasarela inventada, registro de pasarelas)
- `MercadoPagoPasarelaTest` — 13 tests (order con total exacto, traducción de estados, avisos del webhook)
- `WebhookControllerTest` — 3 tests (aviso válido, pasarela inexistente, error sin reintentos)
- `PedidoServiceTest` — 22 tests (cancelación, pago, envío, estado, acreditación de pagos)
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

**Carrera:** Tecnicatura Universitaria en Programación — UTN Mar del Plata

| Materia | Parte del proyecto | Integrantes | Docente |
|---|---|---|---|
| Programación III | Backend (API REST Spring Boot) | Alan Arruti, Iara Blandi | Lic. Mango Eduardo |
| Programación IV | Frontend (Angular 22) | Alan Arruti, Iara Blandi, Octavio Noval | Lucrecia Bazán |
