# CLAUDE.md — ChessBoard Project

Loaded automatically at the start of every Claude Code session. Read this before touching any code.

---

## Project Overview

Android chess opening trainer. Users save games (openings) and review/train them on an interactive board.
**Stack:** Kotlin · Jetpack Compose · Room · chesslib (bhlangonijr)
**Package root:** `com.example.chessboard`

---

## File Map

### Entry
| File | Role |
|---|---|
| `MainActivity.kt` | Single-Activity host. Holds `currentScreen` and screen-specific state used for screen routing, renders the active screen via `when`. |

### Screens (`ui/screen/`)
| File | Role |
|---|---|
| `HomeScreen.kt` | Lists all saved games. Opens a game in `GameEditorScreen`. |
| `CreateOpeningScreen.kt` | Form to input and save a new game/opening. |
| `CreateTrainingScreen.kt` | Creates a training from selected games with editable weights. |
| `TrainingListScreen.kt` | Loads and displays all saved trainings as cards with name, training ID, and games count. |
| `GamesExplorerScreen.kt` | Loads all saved games, shows each as a `GameBlock` (title + move-chip row + nav). Clicking any chip loads that game at that ply on the shared board. |
| `GameEditorScreen.kt` | Loads a single `GameEntity`, replays its PGN, shows move-chip row, allows undo/redo/save/delete. |
| `TrainingComponents.kt` | Shared composables and pure helpers reused by both training-related and editor screens (see below). |
| `ScrenTypes.kt` | `sealed class ScreenType` — Home, Training, GamesExplorer, CreateOpening, CreateTraining, GameEditor, TrainSingleGame, Stats, Profile. |

### Single-game training (`ui/screen/trainSingleGame/`)
| File | Role |
|---|---|
| `TrainSingleGameScreen.kt` | Main single-game training screen orchestration and screen shell. |
| `TrainSingleGameModels.kt` | Session models, enums, results, constants, and pure helpers. |
| `TrainSingleGameLogic.kt` | Training session flow helpers and state transitions. |
| `TrainSingleGameComponents.kt` | UI composables for the single-game training screen. |
| `TemporaryWrongWayStartOneSingleTraining.kt` | Temporary launcher that resolves the first valid training/game pair before opening `TrainSingleGameScreen`. |

### Board (`ui/`)
| File | Role |
|---|---|
| `ChessBoard.kt` | Canvas-based board renderer + touch input. Driven by `GameController`. |

### Game Logic (`boardmodel/`)
| File | Role |
|---|---|
| `GameController.kt` | Wraps `chesslib.Board`. Manages move list, undo/redo, `boardState` (Compose int-state that triggers recomposition), `canUndo`, `canRedo`. |
| `BoardModel.kt` / `ChesslibMapper.kt` | Maps chesslib FEN → `BoardPosition` used for rendering. |

### Data (`entity/`)
| Entity | Table | Key fields |
|---|---|---|
| `GameEntity` | `games` | id, white, black, event, eco, pgn, initialFen, sideMask |
| `PositionEntity` | `positions` | zobristHash, fen, sideMask |
| `GamePositionEntity` | `game_positions` | gameId, positionId, ply |
| `TrainingTemplateEntity` | `training_templates` | id, name, gamesJson |
| `TrainingEntity` | `trainings` | id, name, gamesJson (copy of template, weights decremented as user completes games) |

### Repository (`repository/`)
| File | Role |
|---|---|
| `DatabaseProvider.kt` | Singleton facade over Room DB. Public API includes game add/update/delete, training creation/list loading, and single-game training launch/finish helpers. |
| `GameDao`, `PositionDao`, `GamePositionDao`, etc. | Room DAO interfaces. |

### Services (`service/`)
| File | Role |
|---|---|
| `GameSaver.kt` | Transactional save: checks position uniqueness (Zobrist), inserts game + all positions. |
| `GameUpdater.kt` | Transactional game replacement flow: removes old links/positions as needed, deletes old game row, saves edited game again through `GameSaver`. |
| `GameDeleter.kt` | Transactional delete: removes game links, deletes the game, then deletes or updates affected positions. |
| `GameUniqueChecker.kt` | Returns true if at least one position in the game hasn't been seen for this side. |
| `TrainingService.kt` | Creates trainings from game lists, lists trainings, validates training integrity, decreases line weight after completion. |
| `TrainSingleGameService.kt` | Loads game data for single-game training, finishes a trained line, resolves first launchable training/game pair. |

---

## Shared Logic in TrainingComponents.kt

Check here before writing any new helper — it may already exist.

| Symbol | Type | Purpose |
|---|---|---|
| `ParsedGame` | data class | `GameEntity` + `uciMoves: List<String>` + `moveLabels: List<String>` |
| `parsePgnMoves(pgn)` | fun | Regex-extracts UCI tokens from stored PGN string |
| `computeLabel(move, fen)` | fun | Returns algebraic notation for a move given the board FEN before it |
| `buildMoveLabels(uciMoves)` | fun | Replays UCI list, returns `List<String>` of algebraic labels |
| `MoveChip(label, isSelected, onClick, unselectedBackground)` | @Composable | Teal when selected, `unselectedBackground` (default `TrainingSurfaceDark`) otherwise |
| `ChessBoardSection(gameController)` | @Composable | Square-aspect-ratio board with rounded corners |

---

## GameController Key API

```kotlin
// Navigation
fun undoMove(): Boolean
fun redoMove(): Boolean
fun resetToStartPosition()

// Load a full game and park at targetPly (redo still works past targetPly)
fun loadFromUciMoves(uciMoves: List<String>, targetPly: Int = uciMoves.size)

// Move input (board interaction)
fun tryMove(from: String, to: String): Boolean
fun setStartSquare(square: String?): Boolean
fun setDestinationSquareAndTryMove(dest: String?): Boolean
fun setOrientation(orientation: BoardOrientation)

// Compose-observable state
var boardState: Int          // increments on every change → drives recomposition
var currentMoveIndex: Int
var canUndo: Boolean
var canRedo: Boolean

// Export
fun getFen(): String
fun getMovesCopy(): List<Move>
fun generatePgn(): String
```

---

## Navigation Pattern

```
MainActivity
├─ currentScreen: ScreenType  (mutableStateOf)
├─ selectedGame: GameEntity?  (passed to GameEditor)
│
└─ when(currentScreen) {
     Home            → HomeScreenContainer
     Training        → TrainingListScreenContainer
     GamesExplorer   → GamesExplorerScreenContainer
     CreateOpening   → CreateOpeningScreenContainer
     CreateTraining  → CreateTrainingScreenContainer
     GameEditor      → GameEditorScreenContainer(selectedGame)
     TrainSingleGame → TemporaryWrongWayStartOneSingleTraining
     Stats / Profile → (placeholder)
   }
```

---

## Training System Design

- **TrainingTemplateEntity** — immutable set of games with weights (`gamesJson: [{gameId, weight}]`)
- **TrainingEntity** — mutable copy of a template; weights decrease as user completes games; entry removed when weight hits 0; training deleted when list is empty
- `TrainingService.validateTraining(trainingId)` removes broken game references and deletes empty trainings
- `TrainSingleGameScreen` decrements weight through `TrainSingleGameService.finishTraining(...)`
- There is also direct training creation from UI via `createTrainingFromGames(...)`, not only template-copy creation

---

## Theme Tokens

| Token | Use |
|---|---|
| `Background.ScreenDark` | Screen background |
| `Background.CardDark` | Card / elevated surface |
| `Background.SurfaceDark` | Bottom bar, chip bg, input/container surface |
| `ButtonColor.PrimaryContainer` | Primary button container |
| `ButtonColor.DestructiveContainer` | Destructive button container |
| `ButtonColor.Content` | Button text/icon content |
| `TrainingAccentTeal` | General accent / selected state outside button semantics |
| `TextColor.Primary` | Main text |
| `TextColor.Secondary` | Subtitles, labels |
| `TrainingIconInactive` | Disabled icons |
| `TrainingErrorRed` | General destructive/error accent outside button semantics |
| `TrainingSuccessGreen` | Correct answers |
| `TrainingWarningOrange` | Streak counter |

---

## Key Dependencies

| Dependency | Version |
|---|---|
| Kotlin | 2.0.21 |
| AGP | 8.13.2 |
| KSP | 2.0.21-1.0.28 |
| Room | 2.6.1 |
| Compose BOM | 2024.09.00 |

---

## Rules

### Patterns to follow
- Screen files use **Container + Stateless** — state and DB calls in `XyzScreenContainer`, pure UI in `XyzScreen`
- DB operations on `Dispatchers.IO`, CPU-heavy work on `Dispatchers.Default`, UI state updates on `Dispatchers.Main`
- All DB access through `DatabaseProvider` — never call DAOs directly from screens
- Transactional game save/update/delete through `GameSaver`, `GameUpdater`, `GameDeleter`
- Navigation state lives in `MainActivity`; add new screens to `ScreenType` and the `when` block

### Don't
- Don't duplicate logic that belongs in `TrainingComponents.kt`
- Don't create new files unless no existing file is a reasonable home
- Don't add comments or docstrings to code you didn't change
- Don't over-engineer; solve only what was asked

### Keeping this file up to date
After any non-trivial change update the relevant section above — correct file paths, API signatures, or add to Recent Changes.

---

## Recent Changes

- **`TrainingListScreen.kt` and training route split** — `ScreenType.Training` now opens a dedicated list of saved trainings. Each card shows the training name, DB ID, and games count parsed from `gamesJson`. The old `TrainingScreen.kt` role was renamed to `GamesExplorerScreen.kt` and moved under `ScreenType.GamesExplorer`.
- **`ChessBoard.kt` drag-and-drop** — replaced tap-only `detectTapGestures` with `awaitEachGesture` that distinguishes tap vs drag by `viewConfiguration.touchSlop`. During drag: piece at origin is skipped in normal draw pass and re-drawn centered on the finger (on top). On release, target square is bounds-checked (off-board = no move), then `setStartSquare` + `setDestinationSquareAndTryMove` validate and attempt the move. Two-tap flow still works for taps. All `GameController` mutations moved out of the Canvas draw block into gesture callbacks (`ChessBoardWithCoordinates`). `ChessBoard` is now a pure renderer.
- **`GameController.loadFromUciMoves(uciMoves, targetPly)`** — loads a full game from UCI strings and parks at `targetPly`; all moves kept so undo/redo works across the whole game; single `boardState++` at end
- **Shared logic extracted to `TrainingComponents.kt`** — `parsePgnMoves`, `computeLabel`, `buildMoveLabels`, `MoveChip`, `ParsedGame` moved from both screen files into one canonical location
