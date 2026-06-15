# games-service (Uti Bunna / uti bunna)

Microservicio de juegos 1‑vs‑1 para la plataforma académica **Uti Bunna**. Ejecuta toda la lógica de
los juegos en el backend; el frontend (React) solo renderiza el estado que recibe.

Juegos soportados: **Tic Tac Toe, Connect Four, Checkers (damas), Chess (ajedrez vía chesslib)**.

## Stack

Java 21 · Spring Boot 3.5.x · Maven · PostgreSQL · Redis · Spring Data JPA · Spring WebSocket + STOMP · Docker.

> El **API Gateway** y el **Auth Service** ya existen (otros equipos). Los usuarios llegan autenticados;
> este servicio recibe el `userId` (UUID) en la cabecera `X-User-Id` reenviada por el gateway. **No** hace auth.

## Arquitectura

```
React ──HTTP REST──►  /api/games, /api/rooms ...      (lobby / ciclo de vida de la sala)
      ──WS/STOMP──►   /ws  →  /app/rooms/{id}/move     (movimientos en partida)
                                   │
        GameRoomService (orquestador, @Transactional)
        ├── GameEngineRegistry → GameEngine (TicTacToe | ConnectFour | Checkers | Chess)
        ├── GameStateService  → Redis  game:{roomId}   (estado vivo, JSON, TTL)
        ├── GameRoomRepository→ PostgreSQL games_rooms (persistencia + resultado)
        └── SimpMessagingTemplate → /topic/rooms/{id}  (broadcast del estado)
```

- **Redis** guarda el estado temporal de la partida en la clave `game:{roomId}` (JSON, con TTL).
- **PostgreSQL** guarda el catálogo `games` y las salas `game_rooms` (resultado persistente).
- Al finalizar: se persiste el resultado en PostgreSQL (`winner_user_id`, `status=FINISHED`,
  `finished_at`) y **luego** se borra la clave de Redis.

## Cómo ejecutar

### Opción A — Docker (no requiere JDK/Maven locales)

```bash
docker compose up --build
```

Levanta PostgreSQL, Redis y el servicio en `http://localhost:8080`. El build se hace dentro de la
imagen Maven (`maven:3.9-eclipse-temurin-21`). La **primera** compilación descarga `chesslib` desde
JitPack y puede tardar ~1 minuto.

### Opción B — local (requiere JDK 21)

```bash
docker compose up -d postgres redis      # solo las dependencias
./mvnw spring-boot:run                    # perfil 'local' (localhost)
```

> Este repo incluye el Maven Wrapper (`mvnw`), así que no necesitas Maven instalado, solo un JDK 21.

### Configuración (variables de entorno)

| Variable | Default (local) | Descripción |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `local` | `local` o `docker` |
| `SERVER_PORT` | `8080` | Puerto HTTP/WS |
| `DB_URL` | `jdbc:postgresql://localhost:5432/games_db` | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `games` / `games` | Credenciales DB |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Redis |
| `GAME_STATE_TTL_MINUTES` | `120` | TTL del estado en Redis |

Al arrancar, `GameSeeder` inserta los 4 juegos (idempotente). Verifica: `GET /api/games`.

## API REST (lobby / ciclo de vida)

Todas requieren la cabecera `X-User-Id: <uuid>` (la inyecta el gateway).

| Método | Ruta | Descripción |
|---|---|---|
| `GET`  | `/api/games` | Lista los juegos activos |
| `POST` | `/api/rooms` | Crea sala (host). Body: `{ "gameCode": "...", "bunnaTokens": 100 }` |
| `GET`  | `/api/rooms` | Lista salas en estado `WAITING` |
| `GET`  | `/api/rooms/{roomId}` | Detalle de una sala |
| `POST` | `/api/rooms/{roomId}/join` | El invitado se une → **inicia la partida** automáticamente |

`gameCode` ∈ `TIC_TAC_TOE` | `CONNECT_FOUR` | `CHECKERS` | `CHESS`.

## WebSocket / STOMP

- **Endpoint**: `ws://<host>/ws` (WebSocket nativo, sin SockJS). Cliente recomendado: `@stomp/stompjs`.
- **Identidad**: cabecera `X-User-Id` en el frame `CONNECT` → se expone como `Principal`.
- **Prefijos**: enviar a `/app`, suscribirse a `/topic` (sala) y `/user/queue/errors` (errores privados).

| Acción | Destino |
|---|---|
| Suscribirse al estado de la sala | `SUBSCRIBE /topic/rooms/{roomId}` |
| Suscribirse a errores propios | `SUBSCRIBE /user/queue/errors` |
| Enviar movimiento | `SEND /app/rooms/{roomId}/move` |
| (Re)pedir el estado actual | `SEND /app/rooms/{roomId}/join` |
| Rendirse | `SEND /app/rooms/{roomId}/resign` |

### Formato de movimiento por juego (`Move`)

| Juego | Campos usados | Ejemplo |
|---|---|---|
| Tic Tac Toe | `toRow`, `toCol` | `{ "toRow": 0, "toCol": 2 }` |
| Connect Four | `column` | `{ "column": 3 }` |
| Checkers | `fromRow`, `fromCol`, `toRow`, `toCol` | `{ "fromRow": 5, "fromCol": 0, "toRow": 4, "toCol": 1 }` |
| Chess | `uci` (preferido) o from/to + `promotion` | `{ "uci": "e2e4" }` · `{ "uci": "e7e8", "promotion": "q" }` |

### Estado de tablero (`GameStateMessage`)

- Tic Tac Toe / Connect Four / Checkers: `board` es una matriz `int[][]`.
  - Tic Tac Toe: `0` vacío, `1` host (X), `2` guest (O).
  - Connect Four: `0` vacío, `1` host (RED), `2` guest (YELLOW).
  - Checkers: `0` vacío, `1` host man, `2` guest man, `3` host king, `4` guest king.
- Chess: `fen` es la posición (string FEN de chesslib); `board` va `null`.
- `status` ∈ `IN_PROGRESS` | `FINISHED` | `DRAW`. Ganador = `winnerUserId` (null en empate).

## Ejemplos de mensajes WebSocket

**CONNECT** (con `@stomp/stompjs`):
```js
const client = new Client({
  brokerURL: 'ws://localhost:8080/ws',
  connectHeaders: { 'X-User-Id': 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa' },
});
client.onConnect = () => {
  client.subscribe('/topic/rooms/' + roomId, m => render(JSON.parse(m.body)));
  client.subscribe('/user/queue/errors', m => showError(JSON.parse(m.body)));
  client.publish({ destination: '/app/rooms/' + roomId + '/join', body: '{}' });
};
client.activate();
```

**Enviar un movimiento** (Tic Tac Toe):
```js
client.publish({ destination: `/app/rooms/${roomId}/move`, body: JSON.stringify({ toRow: 0, toCol: 2 }) });
```

**Broadcast de estado** (en `/topic/rooms/{id}`, Tic Tac Toe en juego):
```json
{
  "roomId": "11111111-1111-1111-1111-111111111111",
  "gameCode": "TIC_TAC_TOE",
  "board": [[1,0,0],[0,2,0],[0,0,1]],
  "fen": null,
  "currentTurnUserId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
  "status": "IN_PROGRESS",
  "winnerUserId": null,
  "symbols": { "aaaaaaaa-...": "X", "bbbbbbbb-...": "O" },
  "moveCount": 3
}
```

**Broadcast de estado** (Chess, tablero como FEN):
```json
{
  "roomId": "11111111-1111-1111-1111-111111111111",
  "gameCode": "CHESS",
  "board": null,
  "fen": "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
  "currentTurnUserId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "status": "IN_PROGRESS",
  "winnerUserId": null,
  "symbols": { "aaaaaaaa-...": "WHITE", "bbbbbbbb-...": "BLACK" },
  "moveCount": 2
}
```

**Victoria**:
```json
{ "...": "...", "status": "FINISHED", "winnerUserId": "aaaaaaaa-...", "currentTurnUserId": null }
```

**Empate** (p.ej. tablero lleno en Connect Four o ahogado en ajedrez):
```json
{ "...": "...", "status": "DRAW", "winnerUserId": null, "currentTurnUserId": null }
```

**Error** (solo al jugador que lo causó, en `/user/queue/errors`):
```json
{ "code": "NOT_YOUR_TURN", "message": "It is not your turn" }
```
Códigos: `NOT_YOUR_TURN`, `INVALID_MOVE`, `GAME_FINISHED`, `UNKNOWN_GAME`, `NO_ACTIVE_GAME`, `INTERNAL`.

## Flujo completo de una partida

1. `GET /api/games` → el lobby muestra los juegos.
2. Host: `POST /api/rooms {gameCode, bunnaTokens}` → fila `game_rooms` en `WAITING`.
3. Host: conecta WS (`X-User-Id`) y se suscribe a `/topic/rooms/{id}` y `/user/queue/errors`.
4. Invitado: `GET /api/rooms` (ve la sala) → `POST /api/rooms/{id}/join`.
5. **Auto-start**: se crea el estado inicial en Redis (`game:{id}`), la fila pasa a `IN_PROGRESS`
   + `started_at`, y se hace broadcast del tablero inicial a `/topic/rooms/{id}`.
6. Los jugadores alternan: `SEND /app/rooms/{id}/move`. Por cada movimiento el backend:
   carga el estado de Redis → `engine.applyMove` (valida turno + legalidad) → guarda en Redis →
   broadcast del nuevo estado. Movimiento ilegal/fuera de turno → error solo al ofensor.
7. En la jugada terminal el motor marca `FINISHED`/`DRAW`. El servicio persiste el resultado en
   PostgreSQL (`winner_user_id`, `FINISHED`, `finished_at`) y borra la clave de Redis.
8. Broadcast del estado final → ambos clientes muestran el resultado.

> **`bunna_tokens`**: este servicio solo **registra** la apuesta y el ganador. La liquidación de
> tokens pertenece a otro servicio (wallet/economy); aquí no se implementa.

## Tests

```bash
./mvnw test
```

Cobertura de los motores (sin infraestructura): para cada juego hay tests de victoria forzada, empate
(donde aplica), movimiento ilegal y movimiento fuera de turno. Énfasis en Checkers (captura
obligatoria, multi-jump, promoción) y Chess (jaque mate por ambos lados, vía chesslib).

## Estructura del proyecto

```
src/main/java/com/utibunna/games
├── config/      RedisConfig, WebSocketConfig, StompAuthChannelInterceptor, UserPrincipal
├── controller/  GameController, GameRoomController (REST) · GameWsController (STOMP)
├── dto/         CreateRoomRequest, GameResponse, GameRoomResponse, GameStateMessage, GameErrorMessage
├── entity/      Game, GameRoom · enums/RoomStatus
├── repository/  GameRepository, GameRoomRepository
├── service/     GameService, GameRoomService, GameStateService, GameSeeder
├── engine/      GameEngine, GameEngineRegistry, *Engine, CheckersRules
├── model/       GameState, Move, GameStatus
└── exception/   GameDomainException + subtipos, GlobalExceptionHandler
```

## Limitaciones conocidas (MVP)

- **Broker STOMP en memoria**: válido para una sola instancia. Escalar a varias requeriría sticky
  sessions en el gateway o un relay externo / puente Redis pub-sub.
- **Checkers**: sin tablas automáticas (40 movimientos / triple repetición). Damas de rango corto.
- **Identidad confiada (`X-User-Id`)**: seguro solo porque el gateway es el único ingress y
  sobreescribe esa cabecera para llamadas externas.
