# 🎮 Guía de Endpoints — Utibunna Games Service

> **Base URL:** `http://localhost:8080`
> **Tech Stack:** Spring Boot · PostgreSQL · Redis · WebSocket (STOMP)

---

## 📖 Explicación para niños (¿Qué hace esta API?)

Imagina que estás en un recreo digital. Esta API es como el **director de juegos** del colegio:

1. 🗂️ Primero puedes **ver qué juegos existen** (Tic Tac Toe, Ajedrez, etc.)
2. 🏠 Luego puedes **crear un cuarto** donde vas a jugar (como reservar una mesa de ping pong)
3. 👀 Otros jugadores pueden **ver los cuartos disponibles** y **entrar al tuyo**
4. ♟️ Una vez dentro, los dos jugadores **se conectan por WebSocket** (como un walkie-talkie en tiempo real) para enviar sus movimientos
5. 🏆 Cuando alguien gana, el cuarto se cierra y se guarda el resultado

El truco especial: la API no tiene login propio. En cambio, espera que **otro servicio (el Gateway)** ya haya verificado quién eres, y te manda tu ID de usuario en un header llamado `X-User-Id`.

---

## 🧑‍💻 Explicación para Frontend Developer

Esta API sigue un modelo de **trusted-gateway**: el servicio de autenticación externo inyecta el `userId` como header `X-User-Id` en cada request. Esto significa que en Postman debes simularlo manualmente.

La arquitectura tiene **dos capas de comunicación**:
- **REST** (`/api/...`) → para el lobby: listar juegos, crear/unirse a salas
- **WebSocket/STOMP** (`ws://localhost:8080/ws`) → para el juego en tiempo real: movimientos, renuncias, estado del tablero

El estado en vivo vive en **Redis** (TTL de 2 horas por defecto). El resultado final persiste en **PostgreSQL**.

Los 4 juegos disponibles (seeded automáticamente al arrancar):
| `gameCode` | Juego |
|---|---|
| `TIC_TAC_TOE` | Tic Tac Toe |
| `CONNECT_FOUR` | Connect Four |
| `CHECKERS` | Damas |
| `CHESS` | Ajedrez |

---

## ⚙️ Setup en Postman

### Variables de entorno recomendadas

Crea un Environment en Postman con estas variables:

| Variable | Valor |
|---|---|
| `baseUrl` | `http://localhost:8080` |
| `userId_host` | `550e8400-e29b-41d4-a716-446655440001` |
| `userId_guest` | `550e8400-e29b-41d4-a716-446655440002` |
| `roomId` | *(se llena automáticamente con el script de Post-response)* |

> ⚠️ Los `userId` son UUIDs. Puedes usar cualquier UUID v4 válido mientras sean distintos entre sí.

---

---

# 📋 ENDPOINTS REST

---

## 1. `GET /api/games` — Listar juegos disponibles

### 👶 Para niños
*"Muéstrame todos los juegos que puedo jugar"*

### 🧑‍💻 Para el developer
Retorna el catálogo de juegos activos. No requiere autenticación. Úsalo para obtener los `gameCode` válidos antes de crear una sala.

---

**Request**

| Campo | Valor |
|---|---|
| Método | `GET` |
| URL | `{{baseUrl}}/api/games` |
| Headers | *(ninguno requerido)* |
| Body | *(ninguno)* |

---

**Ejemplo de Response `200 OK`**

```json
[
  {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "code": "TIC_TAC_TOE",
    "name": "Tic Tac Toe",
    "description": "Classic 3x3 game",
    "active": true
  },
  {
    "id": "b2c3d4e5-f6a7-8901-bcde-f01234567891",
    "code": "CONNECT_FOUR",
    "name": "Connect Four",
    "description": "Drop discs, connect four in a row",
    "active": true
  },
  {
    "id": "c3d4e5f6-a7b8-9012-cdef-012345678912",
    "code": "CHECKERS",
    "name": "Checkers",
    "description": "Draughts on an 8x8 board",
    "active": true
  },
  {
    "id": "d4e5f6a7-b8c9-0123-defa-123456789023",
    "code": "CHESS",
    "name": "Chess",
    "description": "Full chess rules powered by chesslib",
    "active": true
  }
]
```

---

**Script Post-response (Tests tab en Postman)**

```javascript
// Guarda el code del primer juego para usarlo después
const games = pm.response.json();
pm.environment.set("gameCode", games[0].code);
console.log("Juegos disponibles:", games.map(g => g.code));
```

---

---

## 2. `POST /api/rooms` — Crear una sala de juego

### 👶 Para niños
*"Quiero reservar una mesa para jugar Ajedrez y apostar 100 fichas"*

### 🧑‍💻 Para el developer
Crea una sala en estado `WAITING`. El jugador que la crea es el `host`. El campo `bunnaTokens` representa las fichas que se apuestan. La sala queda esperando que un segundo jugador se una. Requiere `X-User-Id` header.

---

**Request**

| Campo | Valor |
|---|---|
| Método | `POST` |
| URL | `{{baseUrl}}/api/rooms` |
| Header `Content-Type` | `application/json` |
| Header `X-User-Id` | `{{userId_host}}` |

**Body (raw JSON)**

```json
{
  "gameCode": "TIC_TAC_TOE",
  "bunnaTokens": 100
}
```

> 💡 Cambia `gameCode` por cualquiera de: `TIC_TAC_TOE`, `CONNECT_FOUR`, `CHECKERS`, `CHESS`
> `bunnaTokens` debe ser un número entero ≥ 0

**Validaciones del body:**
- `gameCode` → obligatorio, no puede estar vacío (`@NotBlank`)
- `bunnaTokens` → obligatorio, debe ser ≥ 0 (`@NotNull @PositiveOrZero`)

---

**Ejemplo de Response `200 OK`**

```json
{
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "gameId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "hostUserId": "550e8400-e29b-41d4-a716-446655440001",
  "guestUserId": null,
  "bunnaTokens": 100,
  "status": "WAITING",
  "winnerUserId": null,
  "createdAt": "2024-01-15T10:30:00Z",
  "startedAt": null,
  "finishedAt": null
}
```

> `guestUserId` es `null` porque nadie se ha unido aún. `status: "WAITING"` confirma que la sala está abierta.

---

**Script Post-response (Tests tab)**

```javascript
// Guarda el roomId para los siguientes requests
const room = pm.response.json();
pm.environment.set("roomId", room.id);
console.log("Sala creada con ID:", room.id);
pm.test("Sala creada exitosamente", () => {
  pm.expect(room.status).to.equal("WAITING");
  pm.expect(room.guestUserId).to.be.null;
});
```

---

---

## 3. `GET /api/rooms` — Ver salas disponibles (esperando jugadores)

### 👶 Para niños
*"¿Qué mesas están libres para que me pueda unir?"*

### 🧑‍💻 Para el developer
Retorna todas las salas con `status: WAITING`. Úsalo en el lobby para mostrar las partidas disponibles a las que puede unirse un usuario. No requiere autenticación.

---

**Request**

| Campo | Valor |
|---|---|
| Método | `GET` |
| URL | `{{baseUrl}}/api/rooms` |
| Headers | *(ninguno requerido)* |
| Body | *(ninguno)* |

---

**Ejemplo de Response `200 OK`**

```json
[
  {
    "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "gameId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "hostUserId": "550e8400-e29b-41d4-a716-446655440001",
    "guestUserId": null,
    "bunnaTokens": 100,
    "status": "WAITING",
    "winnerUserId": null,
    "createdAt": "2024-01-15T10:30:00Z",
    "startedAt": null,
    "finishedAt": null
  }
]
```

> Si no hay salas esperando, retorna un array vacío `[]`

---

---

## 4. `GET /api/rooms/{roomId}` — Ver detalle de una sala

### 👶 Para niños
*"Cuéntame todo sobre esa mesa de juego en particular"*

### 🧑‍💻 Para el developer
Retorna el estado completo de una sala específica. Útil para polling del estado o para recuperar info de una partida finalizada. No requiere autenticación.

---

**Request**

| Campo | Valor |
|---|---|
| Método | `GET` |
| URL | `{{baseUrl}}/api/rooms/{{roomId}}` |
| Headers | *(ninguno requerido)* |
| Body | *(ninguno)* |

> Sustituye `{{roomId}}` por el UUID de la sala (ej: `f47ac10b-58cc-4372-a567-0e02b2c3d479`)

---

**Ejemplo de Response `200 OK` (sala en progreso)**

```json
{
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "gameId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "hostUserId": "550e8400-e29b-41d4-a716-446655440001",
  "guestUserId": "550e8400-e29b-41d4-a716-446655440002",
  "bunnaTokens": 100,
  "status": "IN_PROGRESS",
  "winnerUserId": null,
  "createdAt": "2024-01-15T10:30:00Z",
  "startedAt": "2024-01-15T10:31:00Z",
  "finishedAt": null
}
```

**Posibles valores de `status`:**

| Status | Significado |
|---|---|
| `WAITING` | Sala creada, esperando al segundo jugador |
| `IN_PROGRESS` | Ambos jugadores listos, partida en curso |
| `FINISHED` | Partida terminada (hay ganador o empate) |
| `CANCELLED` | Abandonada antes de terminar |

---

**Error `404 Not Found`**

```json
{
  "message": "Room not found: f47ac10b-58cc-4372-a567-0e02b2c3d479"
}
```

---

---

## 5. `POST /api/rooms/{roomId}/join` — Unirse a una sala

### 👶 Para niños
*"¡Quiero sentarme a jugar en esa mesa!"*

### 🧑‍💻 Para el developer
El segundo jugador (guest) se une a la sala. En cuanto esto ocurre, el juego arranca automáticamente: se crea el estado inicial en Redis y se cambia el status a `IN_PROGRESS`. Se hace broadcast por WebSocket del estado inicial a todos los suscriptores del topic de esa sala. Requiere `X-User-Id` header con un UUID diferente al del host.

---

**Request**

| Campo | Valor |
|---|---|
| Método | `POST` |
| URL | `{{baseUrl}}/api/rooms/{{roomId}}/join` |
| Header `X-User-Id` | `{{userId_guest}}` |
| Body | *(ninguno — body vacío)* |

> ⚠️ Usa un userId **diferente** al del host. Si usas el mismo, recibirás error.

---

**Ejemplo de Response `200 OK`**

```json
{
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "gameId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "hostUserId": "550e8400-e29b-41d4-a716-446655440001",
  "guestUserId": "550e8400-e29b-41d4-a716-446655440002",
  "bunnaTokens": 100,
  "status": "IN_PROGRESS",
  "winnerUserId": null,
  "createdAt": "2024-01-15T10:30:00Z",
  "startedAt": "2024-01-15T10:31:05Z",
  "finishedAt": null
}
```

**Errores posibles:**

| Código | Mensaje | Causa |
|---|---|---|
| `400` | `Room is not open for joining` | La sala no está en `WAITING` |
| `400` | `You cannot join your own room` | El `X-User-Id` es igual al del host |
| `400` | `Room is already full` | Ya hay un guest asignado |
| `404` | `Room not found: ...` | roomId inválido |

---

---

# 🔌 WEBSOCKET / STOMP (Juego en tiempo real)

> ⚠️ Postman tiene soporte básico para WebSocket. Para probar STOMP, también puedes usar la extensión de Postman para WebSocket o una herramienta como **[STOMP Playground](https://jmesnil.net/stomp-websocket/doc/)** / **Websocat**.

## Datos de conexión

| Campo | Valor |
|---|---|
| URL | `ws://localhost:8080/ws` |
| Protocolo | STOMP sobre WebSocket nativo (no SockJS) |
| Header en CONNECT | `X-User-Id: <uuid-del-jugador>` |

---

## STOMP Frame de CONNECT

Al conectar, el cliente debe enviar el header `X-User-Id`. Este es el equivalente a "iniciar sesión" en el WebSocket.

```
CONNECT
X-User-Id:550e8400-e29b-41d4-a716-446655440001
accept-version:1.2
heart-beat:0,0

^@
```

> 🚨 Si falta `X-User-Id` en el CONNECT, el servidor lanza `IllegalArgumentException` y rechaza la conexión.

---

## Suscripciones (SUBSCRIBE)

Una vez conectado, suscríbete a estos topics para recibir eventos:

### 📡 Topic del tablero (broadcasts a todos)

```
SUBSCRIBE
destination:/topic/rooms/{roomId}
id:sub-0

^@
```

Recibirás aquí el **estado completo del juego** cada vez que alguien haga un movimiento, alguien se una, o el juego termine.

**Ejemplo de mensaje recibido (`GameStateMessage`):**

```json
{
  "roomId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "gameCode": "TIC_TAC_TOE",
  "board": [
    [0, 0, 0],
    [0, 0, 0],
    [0, 0, 0]
  ],
  "fen": null,
  "currentTurnUserId": "550e8400-e29b-41d4-a716-446655440001",
  "status": "IN_PROGRESS",
  "winnerUserId": null,
  "symbols": {
    "550e8400-e29b-41d4-a716-446655440001": "X",
    "550e8400-e29b-41d4-a716-446655440002": "O"
  },
  "moveCount": 0
}
```

> Para **Chess**, el campo `fen` contendrá la posición en notación FEN y `board` será `null`.
> Para el resto de juegos, `board` es la matriz del tablero y `fen` es `null`.

---

### 🚨 Cola de errores personales (solo para el usuario actual)

```
SUBSCRIBE
destination:/user/queue/errors
id:sub-1

^@
```

Solo tú recibirás mensajes aquí cuando cometas un error (movimiento ilegal, jugar fuera de turno, etc.).

**Ejemplo de error recibido (`GameErrorMessage`):**

```json
{
  "code": "NOT_YOUR_TURN",
  "message": "It's not your turn"
}
```

**Códigos de error conocidos:**

| Código | Descripción |
|---|---|
| `NOT_YOUR_TURN` | Intentaste mover fuera de tu turno |
| `INVALID_MOVE` | Movimiento ilegal según las reglas |
| `GAME_ALREADY_FINISHED` | El juego ya terminó |
| `NO_ACTIVE_GAME` | No hay estado activo en Redis para esa sala |
| `NOT_A_PLAYER` | Intentaste resignar pero no eres jugador de esa sala |
| `INTERNAL` | Error inesperado del servidor |

---

## 6. WS — `/app/rooms/{roomId}/join` — Solicitar estado actual

### 👶 Para niños
*"Acabo de entrar a la sala, ¿me puedes mostrar cómo está el tablero ahora mismo?"*

### 🧑‍💻 Para el developer
Útil para re-sincronizar el estado cuando el cliente se reconecta o suscribe tarde. El servidor re-emite el estado actual al topic de la sala. No necesita body.

```
SEND
destination:/app/rooms/f47ac10b-58cc-4372-a567-0e02b2c3d479/join

^@
```

---

## 7. WS — `/app/rooms/{roomId}/move` — Enviar un movimiento

### 👶 Para niños
*"¡Es mi turno! Pongo mi ficha en la fila 1, columna 2"*

### 🧑‍💻 Para el developer
El cuerpo del mensaje es un objeto JSON con el movimiento. Los campos usados dependen del juego. El servidor valida el turno y la legalidad del movimiento; si es inválido, el error llega solo a `/user/queue/errors`.

---

**Frame STOMP:**

```
SEND
destination:/app/rooms/{roomId}/move
content-type:application/json

{ ... move payload ... }
^@
```

---

### Payloads de movimiento por juego

#### 🟦 Tic Tac Toe

```json
{
  "toRow": 1,
  "toCol": 2
}
```
> Filas y columnas de 0 a 2. Coloca tu símbolo (X u O) en esa posición.

---

#### 🟡 Connect Four

```json
{
  "column": 3
}
```
> Solo necesitas indicar la columna (0 a 6). La ficha cae automáticamente al fondo.

---

#### 🔴 Checkers (Damas)

```json
{
  "fromRow": 5,
  "fromCol": 2,
  "toRow": 4,
  "toCol": 3
}
```
> Indica desde dónde y hacia dónde mueves la pieza. El tablero es 8x8 (índices 0–7).
> Para multi-capturas (capturar varias piezas seguidas), envías un movimiento a la vez; el servidor indica si debes continuar con la misma pieza.

---

#### ♟️ Chess (Ajedrez)

**Opción recomendada — notación UCI:**

```json
{
  "uci": "e2e4"
}
```

**Con promoción de peón:**

```json
{
  "uci": "e7e8q"
}
```

**Opción alternativa — coordenadas de tablero:**

```json
{
  "fromRow": 6,
  "fromCol": 4,
  "toRow": 4,
  "toCol": 4,
  "promotion": "q"
}
```

> `promotion` puede ser: `q` (reina), `r` (torre), `b` (alfil), `n` (caballo). Solo se usa al promover un peón.
> UCI: la casilla `e2` = columna e, fila 2. En el sistema de coordenadas del código, filas van de 0 (arriba) a 7 (abajo).

---

## 8. WS — `/app/rooms/{roomId}/resign` — Rendirse

### 👶 Para niños
*"Me rindo, el otro jugador gana"*

### 🧑‍💻 Para el developer
El jugador que llama a este endpoint pierde. El oponente es declarado ganador automáticamente. Se persiste el resultado en PostgreSQL y se limpia el estado de Redis. El broadcast final llega a `/topic/rooms/{roomId}`.

```
SEND
destination:/app/rooms/f47ac10b-58cc-4372-a567-0e02b2c3d479/resign

^@
```

> No requiere body. El servidor identifica al jugador por el `Principal` (establecido desde `X-User-Id` en el CONNECT).

---

---

# 🗺️ Flujo completo de una partida (orden recomendado en Postman)

```
Paso 1 ──► GET /api/games
              └─ Obtén el gameCode que quieres usar

Paso 2 ──► POST /api/rooms          (como host)
              └─ Guarda el roomId del response

Paso 3 ──► GET /api/rooms           (opcional: ver sala en lista)
Paso 4 ──► GET /api/rooms/{roomId}  (opcional: ver detalle)

Paso 5 ──► POST /api/rooms/{roomId}/join   (como guest, con X-User-Id diferente)
              └─ La sala cambia a IN_PROGRESS
              └─ El estado inicial se crea en Redis
              └─ Se hace broadcast WS automáticamente

Paso 6 ──► WS CONNECT (host)        X-User-Id: {userId_host}
Paso 7 ──► WS CONNECT (guest)       X-User-Id: {userId_guest}
Paso 8 ──► SUBSCRIBE /topic/rooms/{roomId}   (ambos jugadores)
Paso 9 ──► SUBSCRIBE /user/queue/errors      (ambos jugadores)

Paso 10 ─► SEND /app/rooms/{roomId}/join     (re-sync tablero)
Paso 11 ─► SEND /app/rooms/{roomId}/move     (host juega)
Paso 12 ─► SEND /app/rooms/{roomId}/move     (guest juega)
              └─ ... se repite hasta que alguien gane o se rinda

Paso 13 ─► SEND /app/rooms/{roomId}/resign   (opcional: rendirse)
              └─ O el juego termina solo cuando hay ganador

Paso 14 ─► GET /api/rooms/{roomId}   (ver resultado final con winnerUserId)
```

---

---

# 💡 Tips para el Frontend Developer

### Manejo del estado del tablero por juego

| `gameCode` | Campo en `GameStateMessage` | Estructura |
|---|---|---|
| `TIC_TAC_TOE` | `board` | `int[3][3]` → 0=vacío, 1=host, 2=guest |
| `CONNECT_FOUR` | `board` | `int[6][7]` → 0=vacío, 1=host, 2=guest |
| `CHECKERS` | `board` | `int[8][8]` → 0=vacío, valores positivos=host, negativos=guest |
| `CHESS` | `fen` | String en notación FEN estándar |

### El campo `symbols`

Siempre disponible en el `GameStateMessage`. Te dice qué símbolo/color tiene cada jugador:

```json
// Tic Tac Toe / Connect Four
{ "userId1": "X", "userId2": "O" }

// Chess
{ "userId1": "WHITE", "userId2": "BLACK" }
```

### ¿Cómo saber de quién es el turno?

El campo `currentTurnUserId` en `GameStateMessage` tiene el UUID del jugador que debe mover. Es `null` cuando el juego terminó.

### Reconexión WebSocket

Si el cliente pierde la conexión, al reconectar debe:
1. Hacer `CONNECT` con el header `X-User-Id`
2. Suscribirse de nuevo a `/topic/rooms/{roomId}` y `/user/queue/errors`
3. Enviar `SEND /app/rooms/{roomId}/join` para recibir el estado actual del tablero

### Cuándo el juego termina

El campo `status` en `GameStateMessage` cambiará de `IN_PROGRESS` a:
- `FINISHED` → hay un ganador, `winnerUserId` tendrá su UUID
- `DRAW` → empate, `winnerUserId` será `null`