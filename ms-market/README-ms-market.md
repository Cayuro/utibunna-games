# ms-market — Marketplace de Uti-Bunna

Microservicio que permite a los usuarios de la plataforma **publicar y ofrecer sus propios productos o servicios** dentro de la comunidad Uti-Bunna. Cualquier usuario autenticado puede registrarse como seller y comenzar a listar lo que vende: artículos físicos, servicios profesionales, comida, clases, lo que sea.

Este servicio no procesa pagos directamente. Eso le corresponde a `ms-payment`. `ms-market` es el catálogo: crea, guarda y expone los productos/servicios, y registra el resultado de cada transacción cuando `ms-payment` lo notifica.

---

## Tabla de contenido

- [Cómo correrlo](#cómo-correrlo)
- [Variables de entorno](#variables-de-entorno)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Endpoints disponibles](#endpoints-disponibles)
- [Cómo funciona el header X-User-Id](#cómo-funciona-el-header-x-user-id)
- [Errores comunes y solución](#errores-comunes-y-solución)

---

## Cómo correrlo

### Opción A — Con todo el stack (recomendado)

`ms-market` depende de PostgreSQL, que ya está definido en el `docker-compose.yml` raíz del proyecto. Primero agrega el servicio al compose:

```yaml
# docker-compose.yml (dentro de services:)
ms-market:
  build: ./ms-market
  container_name: utibunna-ms-market
  restart: unless-stopped
  environment:
    DATABASE_URL:      jdbc:postgresql://postgres:5432/${POSTGRES_DB}
    DATABASE_USERNAME: ${POSTGRES_USER}
    DATABASE_PASSWORD: ${POSTGRES_PASSWORD}
  ports:
    - "8083:8083"
  depends_on:
    postgres:
      condition: service_healthy
  networks:
    - utibunna-net
```

Luego desde la raíz del proyecto:

```bash
# Primera vez o cuando haya cambios en el código
docker compose up --build

# A partir de la segunda vez (si no hubo cambios)
docker compose up -d

# Ver logs solo de ms-market
docker compose logs -f ms-market

# Ver si todos los servicios están healthy
docker compose ps
```

### Opción B — Solo ms-market (para desarrollo aislado)

Útil si solo quieres probar este microservicio sin levantar todo el stack.

```bash
cd ms-market

# Necesitas un Postgres corriendo localmente o en Docker
# Si no tienes uno, levanta solo la base de datos:
docker run -d \
  --name pg-market-dev \
  -e POSTGRES_DB=utibunna \
  -e POSTGRES_USER=utibunna \
  -e POSTGRES_PASSWORD=utibunna \
  -p 5432:5432 \
  postgis/postgis:16-3.4

# Exporta las variables de entorno
export DATABASE_URL=jdbc:postgresql://localhost:5432/utibunna
export DATABASE_USERNAME=utibunna
export DATABASE_PASSWORD=utibunna

# Corre la app con Maven
./mvnw spring-boot:run
```

La app arranca en `http://localhost:8083`.

### Verificar que está corriendo

```bash
# Health check
curl http://localhost:8083/v3/api-docs

# Swagger UI (abrir en el navegador)
open http://localhost:8083/swagger-ui.html
```

---

## Variables de entorno

Todas van en el `.env` de la raíz del proyecto. `ms-market` solo necesita las de Postgres — las mismas que ya usan `ms-auth` y `ms-carpooling`.

| Variable | Descripción | Ejemplo |
|---|---|---|
| `DATABASE_URL` | URL JDBC de PostgreSQL | `jdbc:postgresql://postgres:5432/utibunna` |
| `DATABASE_USERNAME` | Usuario de la base de datos | `utibunna` |
| `DATABASE_PASSWORD` | Contraseña de la base de datos | `utibunna` |

> `ms-market` **no necesita** `JWT_SECRET` ni ningún token de MercadoPago — eso le corresponde al gateway y a `ms-payment` respectivamente.

---

## Estructura del proyecto

```
ms-market/
├── Dockerfile
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/utibunna/market/
    │   │   ├── MsMarketApplication.java
    │   │   ├── config/
    │   │   │   ├── SecurityConfig.java       ← permite todo (el Gateway ya validó el JWT)
    │   │   │   └── SwaggerConfig.java
    │   │   ├── controller/
    │   │   │   ├── ProductController.java    ← /api/market/products
    │   │   │   ├── SellerController.java     ← /api/market/sellers
    │   │   │   └── SaleController.java       ← /api/market/sales
    │   │   ├── service/
    │   │   │   ├── ProductService.java
    │   │   │   ├── SellerService.java
    │   │   │   └── SaleService.java
    │   │   ├── entity/
    │   │   │   ├── SellerEntity.java
    │   │   │   ├── ProductEntity.java
    │   │   │   └── SaleEntity.java
    │   │   ├── repository/
    │   │   │   ├── SellerRepository.java
    │   │   │   ├── ProductRepository.java
    │   │   │   └── SaleRepository.java
    │   │   ├── dto/
    │   │   │   ├── request/                  ← lo que entra
    │   │   │   └── response/                 ← lo que sale
    │   │   └── exception/
    │   │       ├── ApiException.java
    │   │       └── GlobalExceptionHandler.java
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/
    │           └── V1__create_market_tables.sql
    └── test/
```

---

## Endpoints disponibles

Todos los endpoints pasan por el **Gateway en el puerto 8080**. No llames directamente al 8083 desde el frontend.

### Sellers — `/api/market/sellers`

| Método | Ruta | Header requerido | Descripción |
|--------|------|-----------------|-------------|
| `POST` | `/api/market/sellers` | `X-User-Id` | Registrarme como vendedor |
| `GET` | `/api/market/sellers/me` | `X-User-Id` | Ver mi perfil de vendedor |

**Registrarse como seller — ejemplo:**
```bash
curl -X POST http://localhost:8080/api/market/sellers \
  -H "Authorization: Bearer <tu-jwt>" 
```
El Gateway convierte el JWT en `X-User-Id` automáticamente — no tienes que mandarlo tú.

---

### Productos — `/api/market/products`

| Método | Ruta | Header requerido | Descripción |
|--------|------|-----------------|-------------|
| `GET` | `/api/market/products` | — | Catálogo público (todos los productos ACTIVE) |
| `GET` | `/api/market/products/{id}` | — | Detalle de un producto |
| `GET` | `/api/market/products/my` | `X-User-Id` | Mis productos como vendedor |
| `POST` | `/api/market/products` | `X-User-Id` | Publicar un producto o servicio |
| `PATCH` | `/api/market/products/{id}/stock` | `X-User-Id` | Actualizar stock |
| `PATCH` | `/api/market/products/{id}/deactivate` | `X-User-Id` | Ocultar producto del catálogo |

**Publicar un producto — body:**
```json
{
  "name": "Clases de inglés — nivel básico",
  "price": 25000.00,
  "description": "Clases de 1 hora, modalidad virtual. Incluye material de estudio.",
  "imgUrl": "https://...",
  "stock": 10
}
```
> El campo `stock` en servicios representa cupos disponibles. Si ofreces un servicio ilimitado, pon un número alto (ej. `9999`) y actualízalo manualmente.

---

### Ventas — `/api/market/sales`

> Los endpoints `POST` y `PATCH /{id}/status` son llamados internamente por `ms-payment`, no por el frontend.

| Método | Ruta | Quién llama | Descripción |
|--------|------|-------------|-------------|
| `POST` | `/api/market/sales` | `ms-payment` | Crear orden PENDING al iniciar checkout |
| `PATCH` | `/api/market/sales/{id}/status` | `ms-payment` | Actualizar a PAID / REJECTED cuando MP responde |
| `GET` | `/api/market/sales/my-purchases` | Frontend | Mis compras como comprador |
| `GET` | `/api/market/sales/my-sales` | Frontend | Mis ventas como seller |

---

## Cómo funciona el header X-User-Id

El Gateway (`gateway/`) lee el JWT del header `Authorization: Bearer <token>`, lo valida, y extrae el `sub` (que es el UUID del usuario). Luego inyecta ese UUID en el header `X-User-Id` antes de reenviar la petición a `ms-market`.

Puedes verlo en `gateway/src/.../filter/JwtAuthFilter.java`:
```java
ServerWebExchange mutated = exchange.mutate()
    .request(r -> r.header("X-User-Id", claims.getSubject()))
    .build();
```

Entonces en `ms-market` los controllers simplemente leen:
```java
@GetMapping("/me")
public ResponseEntity<SellerResponse> getMyProfile(
    @RequestHeader("X-User-Id") UUID userId
) { ... }
```

**Nunca mandes `X-User-Id` manualmente desde el frontend.** Si el Gateway está bien configurado, él lo pone solo. Si lo mandas desde el cliente, el Gateway lo sobreescribe igual.

### Agregar las rutas al Gateway

En `gateway/src/main/resources/application.yml`, agrega debajo de las rutas existentes:

```yaml
        - id: ms-market
          uri: http://ms-market:8083
          predicates:
            - Path=/api/market/**
          filters:
            - JwtAuthFilter
```

---

## Errores comunes y solución

### 1. `ms-market` en `Restarting (1)` al hacer `docker compose up`

**Causa más probable:** Flyway intenta correr las migraciones pero la base de datos no está lista todavía, o las variables de entorno `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` no están definidas en el `.env`.

**Diagnóstico:**
```bash
docker compose logs ms-market
```
Busca estas frases en los logs:

| Mensaje en los logs | Causa | Solución |
|---|---|---|
| `Connection refused` | Postgres no está listo | Verifica que el healthcheck de postgres pase antes |
| `password authentication failed` | Credenciales incorrectas | Revisa `DATABASE_USERNAME` y `DATABASE_PASSWORD` en `.env` |
| `database "utibunna" does not exist` | La DB no existe | El postgres se creó sin `POSTGRES_DB`, recrea el volumen |
| `org.flywaydb.core.api.exception` | Migración SQL falló | Lee el error de Flyway en el log para ver qué línea del SQL falló |
| `APPLICATION FAILED TO START` sin más detalle | Variable de entorno vacía | Revisa que el `.env` no tenga valores vacíos |

**Solución rápida:**
```bash
# Bajar todo y borrar los volúmenes (ojo: borra datos)
docker compose down -v

# Volver a levantar desde cero
docker compose up --build
```

---

### 2. `403 Forbidden` en endpoints que requieren `X-User-Id`

**Causa:** Estás llamando directamente al puerto `8083` en lugar de pasar por el Gateway en `8080`. Si vas directo al servicio, el header `X-User-Id` no existe y Spring lanza `MissingRequestHeaderException`, que el `GlobalExceptionHandler` devuelve como `500`.

**Solución:** Siempre llama por el Gateway:
```bash
# ❌ MAL — vas directo al servicio
curl http://localhost:8083/api/market/sellers/me

# ✅ BIEN — pasas por el Gateway con tu JWT
curl http://localhost:8080/api/market/sellers/me \
  -H "Authorization: Bearer <tu-jwt>"
```

---

### 3. `409 Conflict — Este usuario ya tiene un perfil de vendedor`

**Causa:** Intentaste hacer `POST /api/market/sellers` dos veces con el mismo usuario.

**Solución:** Usa `GET /api/market/sellers/me` para consultar el perfil existente. El registro de seller es una acción de una sola vez por usuario.

---

### 4. `404 Not Found — Seller no encontrado` al intentar publicar un producto

**Causa:** Estás intentando publicar un producto pero tu usuario todavía no está registrado como seller.

**Flujo correcto:**
```
1. POST /api/market/sellers        ← primero regístrate como seller
2. POST /api/market/products       ← luego publica tu producto
```

---

### 5. `400 Bad Request — Stock insuficiente` al iniciar un pago

**Causa:** El producto tiene `stock = 0` o `status = SOLD_OUT`. El sistema bloquea el inicio del pago antes de crear la orden.

**Solución:** El seller debe actualizar el stock primero:
```bash
curl -X PATCH http://localhost:8080/api/market/products/{id}/stock \
  -H "Authorization: Bearer <jwt-del-seller>" \
  -H "Content-Type: application/json" \
  -d '{"stock": 5}'
```

---

### 6. El producto se muestra en el catálogo pero no debería

**Causa:** El campo `status` es `ACTIVE` aunque el seller no quiera mostrarlo.

**Solución:** Usa el endpoint de desactivación:
```bash
curl -X PATCH http://localhost:8080/api/market/products/{id}/deactivate \
  -H "Authorization: Bearer <jwt-del-seller>"
```
Esto cambia el `status` a `INACTIVE` y desaparece del catálogo público (`GET /api/market/products`) sin borrarlo de la base de datos.

---

### 7. Flyway: `Found non-empty schema(s) ... without schema history table`

**Causa:** Ya existían las tablas en la base de datos (de una versión anterior o de otro servicio que las creó a mano) y Flyway no reconoce que ya fueron migradas.

**Solución A (desarrollo):** Borra el volumen y recrea:
```bash
docker compose down -v && docker compose up --build
```

**Solución B (si no puedes borrar los datos):** El `application.yml` ya tiene `baseline-on-migrate: true`, que le dice a Flyway "acepta lo que ya existe y registra desde aquí". Si el error persiste, conéctate a la DB y crea la tabla manualmente:
```sql
CREATE TABLE flyway_schema_history (
    installed_rank INT NOT NULL,
    version VARCHAR(50),
    description VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    script VARCHAR(1000) NOT NULL,
    checksum INT,
    installed_by VARCHAR(100) NOT NULL,
    installed_on TIMESTAMP NOT NULL DEFAULT NOW(),
    execution_time INT NOT NULL,
    success BOOLEAN NOT NULL,
    CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank)
);
```

---

### 8. `java.lang.IllegalArgumentException: No enum constant` en el status de una sale

**Causa:** `ms-payment` está mandando un status que no existe en la lógica del servicio — por ejemplo `"approved"` (el valor de MercadoPago) en lugar de `"PAID"` (el valor interno).

**Solución:** `ms-payment` debe mapear los estados de MP a los estados internos antes de llamar al endpoint de ms-market:

| Estado de MercadoPago | Estado interno en ms-market |
|---|---|
| `approved` | `PAID` |
| `rejected` | `REJECTED` |
| `pending` | `PENDING` |
| `cancelled` | `CANCELLED` |

---

### 9. El catálogo público devuelve lista vacía

**Causa normal:** Todavía no hay productos publicados.

**Diagnóstico:**
```bash
# Conéctate a la DB y verifica
docker exec -it utibunna-postgres psql -U utibunna -d utibunna \
  -c "SELECT id, name, status FROM products;"
```

Si hay productos pero todos tienen `status = 'INACTIVE'` o `'SOLD_OUT'`, el catálogo los filtra correctamente. El endpoint `GET /api/market/products` solo devuelve `status = 'ACTIVE'`.

---

### 10. Puerto `8083` ya está en uso

**Causa:** Otro proceso en tu máquina usa ese puerto.

```bash
# Ver qué proceso usa el puerto
lsof -i :8083     # macOS / Linux
netstat -ano | findstr :8083   # Windows

# Matar el proceso (reemplaza PID con el número que apareció)
kill -9 <PID>
```

O simplemente cambia el puerto externo en `docker-compose.yml` a algo libre:
```yaml
ports:
  - "8093:8083"   # mapea al 8093 en tu máquina, 8083 dentro del container
```

---

## Swagger UI

Útil para probar los endpoints directamente sin curl:

- **ms-market:** http://localhost:8083/swagger-ui.html
- **ms-auth:** http://localhost:8081/swagger-ui.html
- **ms-carpooling:** http://localhost:8082/swagger-ui.html

> Recuerda que desde Swagger estás hablando directo al servicio (sin pasar por el Gateway), así que los endpoints que requieren `X-User-Id` necesitan que lo escribas tú manualmente en el campo del header dentro de Swagger.
