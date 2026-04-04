# CLAUDE.md - ChessBoard Project

Loaded automatically at the start of every Claude Code session. Read this before touching any code.

---

## Project Overview

Android chess opening trainer. Users save games (openings) and review/train them on an interactive board.
**Stack:** Kotlin · Jetpack Compose · Room · chesslib (bhlangonijr)
**Package root:** `com.example.chessboard`

---

## File Map

### Claude Workspace (`.claude/`)
| Path | Role |
|---|---|
| `.claude/agents/` | Repo-local specialist agent prompts for focused tasks such as chess logic, DB schema work, and screen building. |
| `.claude/commands/` | Repo-local reusable command prompts for recurring workflows such as adding screens/entities and debugging board or training flows. |
| `.claude/settings.local.json` | Local Claude workspace settings for this repository. |

### Entry
| File | Role |
|---|---|
| `MainActivity.kt` | Single-Activity host. Holds `currentScreen` and screen-specific state used for screen routing, renders the active screen via `when`. |

### Screens (`ui/screen/`)
| File | Role |
|---|---|
| `HomeScreen.kt` | Lists all saved games. Opens a game in `GameEditorScreen`. |
| `CreateOpeningScreen.kt` | Form to input and save a new game/opening. Uses `PgnImportService` for PGN header extraction, SAN-to-UCI import, and batch-saving imported variation lines as separate games. |
| `CreateTrainingScreen.kt` | Creation-only training flow: loads all games with weight 1, paginated list with weight +/- and delete, Save calls `createTrainingFromGames` directly. No `trainingId` param, no edit-mode code. Exports shared `internal` helpers used by `EditTrainingScreen`: `DEFAULT_TRAINING_NAME`, `TrainingGameEditorItem`, `CreateTrainingEditorState`, `decreaseTrainingGameWeight`, `increaseTrainingGameWeight`, `GameEntity.toTrainingGameEditorItem`. |
| `EditTrainingScreen.kt` | Edit-only training flow: loads only games already in the training with their saved weights, "Random" button in top bar, GO button per row, Save calls `updateTrainingFromGames`. Missing training shows dialog navigating to Training list. |
| `TrainingListScreen.kt` | Loads and displays all saved trainings as cards with name, training ID, and games count. |
| `GamesExplorerScreen.kt` | Loads all saved games, shows each as a `GameBlock` (title + move-chip row + nav). Clicking any chip loads that game at that ply on the shared board. |
| `GameEditorScreen.kt` | Loads a single `GameEntity`, replays its PGN, shows move-chip row, allows undo/redo/save/delete. |
| `TrainingComponents.kt` | Shared composables and pure helpers reused by both training-related and editor screens (see below). |
| `ScrenTypes.kt` | `sealed class ScreenType` - Home, Training, GamesExplorer, CreateOpening, CreateTraining, EditTraining, GameEditor, TrainSingleGame, Stats, Profile. |
| `ProfileScreen.kt` | Profile screen - hero card with avatar/level/progress, quick stats, achievements list, settings/clear-data action rows. Container + Stateless pattern backed by `ProfileViewModel`. |
| `ProfileViewModel.kt` | Holds `ProfileState` (userName, level, totalMoves, accuracy, bestStreak, achievements) via `StateFlow`. No DB access - state is static defaults for now. |

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
| `BoardModel.kt` / `ChesslibMapper.kt` | Maps chesslib FEN to `BoardPosition` used for rendering. |

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
| `PgnImportService.kt` | Extracts PGN headers, expands nested PGN variations into separate playable lines, converts SAN text into UCI moves, and builds stored PGN/move lists for `CreateOpeningScreen`. |
| `TrainingService.kt` | Creates trainings from game lists, lists trainings, validates training integrity, decreases line weight after completion. |
| `TrainSingleGameService.kt` | Loads game data for single-game training, finishes a trained line, resolves first launchable training/game pair. |

---

## Shared PGN helpers — `service/PgnImportService.kt`

All PGN/UCI logic lives here. Check before writing new helpers.

| Symbol | Type | Purpose |
|---|---|---|
| `ParsedGame` | data class | `GameEntity` + `uciMoves: List<String>` + `moveLabels: List<String>` |
| `parsePgnMoves(pgn)` | fun | Regex-extracts UCI tokens from the app's stored PGN format |
| `computeLabel(move, fen)` | fun | Returns algebraic notation (incl. promotion/check/mate) for a move given the FEN before it |
| `buildMoveLabels(uciMoves)` | fun | Replays UCI list from start, returns `List<String>` of algebraic labels |
| `parsePgnToUci(pgnText)` | fun | Converts standard SAN PGN to a single UCI move list |
| `parsePgnToUciLines(pgnText)` | fun | Expands all PGN variations into separate UCI lines |
| `buildStoredPgnFromUci(uciMoves, event, …)` | fun | Builds the app's stored PGN format from UCI strings |
| `extractPgnHeaders(pgnText)` | fun | Extracts PGN header tag map (e.g. "Event", "ECO") |
| `uciMovesToMoves(uciMoves)` | fun | Converts UCI strings to chesslib `Move` objects |

## Shared composables — `ui/screen/training/TrainingComponents.kt`

| Symbol | Type | Purpose |
|---|---|---|
| `MoveChip(label, isSelected, onClick, unselectedBackground)` | @Composable | Teal when selected, `unselectedBackground` (default `SurfaceDark`) otherwise |
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
var boardState: Int
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

```text
MainActivity
|- currentScreen: ScreenType  (mutableStateOf)
|- selectedGame: GameEntity?  (passed to GameEditor)
|
\- when(currentScreen) {
     Home            -> HomeScreenContainer
     Training        -> TrainingListScreenContainer
     GamesExplorer   -> GamesExplorerScreenContainer
     CreateOpening   -> CreateOpeningScreenContainer
     CreateTraining  -> CreateTrainingScreenContainer
     GameEditor      -> GameEditorScreenContainer(selectedGame)
     TrainSingleGame -> TemporaryWrongWayStartOneSingleTraining
     Stats / Profile -> (placeholder)
   }
```

---

## Training System Design

- **TrainingTemplateEntity** - immutable set of games with weights (`gamesJson: [{gameId, weight}]`)
- **TrainingEntity** - mutable copy of a template; weights decrease as user completes games; entry removed when weight hits 0; training deleted when list is empty
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
| Kotlin | 2.2.10 |
| AGP | 9.1.0 |
| KSP | 2.2.10-2.0.2 |
| Room | 2.6.1 |
| Compose BOM | 2026.03.01 |

---

## Rules

### Patterns to follow
- Screen files use **Container + Stateless** - state and DB calls in `XyzScreenContainer`, pure UI in `XyzScreen`
- DB operations on `Dispatchers.IO`, CPU-heavy work on `Dispatchers.Default`, UI state updates on `Dispatchers.Main`
- All DB access through `DatabaseProvider` - never call DAOs directly from screens
- Transactional game save/update/delete through `GameSaver`, `GameUpdater`, `GameDeleter`
- Navigation state lives in `MainActivity`; add new screens to `ScreenType` and the `when` block
- Check `.claude/agents/` and `.claude/commands/` before inventing new repo guidance; prefer reusing those repo-local prompts when they match the task

### Repo-local Claude Agents
- `.claude/agents/chess-logic.md` - use for move generation, PGN/FEN handling, legality, and board-state reasoning
- `.claude/agents/db-schema.md` - use for Room entities, DAOs, schema shape, and persistence design changes
- `.claude/agents/screen-builder.md` - use for Compose screen structure, screen wiring, and UI implementation patterns

### Repo-local Claude Commands
- `.claude/commands/add-entity.md` - workflow for introducing a new entity/table and associated persistence wiring
- `.claude/commands/add-screen.md` - workflow for adding a new screen and routing it through the app
- `.claude/commands/board-debug.md` - debugging checklist for board interaction and game-state issues
- `.claude/commands/training-check.md` - validation checklist for training flows and data integrity

### Business logic placement
- **All business logic lives in `service/`** - screens and containers hold only UI state and dispatch calls; they never compute, validate, or transform domain data themselves
- When adding logic to a screen, ask: "could this be a pure function in a service?" If yes, put it there
- New service methods should be **small and single-purpose** - one method per operation, named after what it does (`resolveX`, `buildX`, `validateX`), not after who calls it
- Helpers that are only used inside one composable file are fine to stay private in that file **only if they are pure UI helpers** (formatting, layout math); anything touching domain models or DB results belongs in a service

### Before starting non-trivial work
- **Always ask clarifying questions before taking big actions** — if the task is ambiguous, multi-step, or touches several files, ask first instead of diving in
- When the user says "continue", ask what they want to continue rather than inferring from memory and acting immediately

### Don't
- Don't duplicate logic that belongs in `TrainingComponents.kt`
- Don't create new files unless no existing file is a reasonable home
- Don't add comments or docstrings to code you didn't change
- Don't over-engineer; solve only what was asked
- Don't put business logic in screens - if a container does more than load state and call a service, extract the logic

### Keeping this file up to date
After any non-trivial change update the relevant section above - correct file paths, API signatures, or add to Recent Changes.

---

## Recent Changes

- **`CreateTrainingScreen.kt` cleaned up to creation-only** - Removed all edit-mode leftovers: `trainingId` param, `buildTrainingEditorItems`, `resolveCreateTrainingTitle`, `resolveRandomTrainingGameId`, `saveTraining` helper, `CreateTrainingScreenData`, `MissingTrainingDialog`, `TrainingSaveSuccess.wasUpdated`, `onStartGameTrainingClick`, `isEditMode` branches, and `showStartButton` from `TrainingGamesPage`/`TrainingGamePageRow`. `CreateTrainingScreenContainer` now calls `createTrainingFromGames` directly. Save success dialog always says "Training Created".

- **`EditTrainingScreen.kt` redesigned with per-game board blocks** - `EditTrainingScreen` body replaced with a `LazyColumn`. Each game in `editableGamesForTraining` is rendered as a `GameTrainingBlock` (private) that shows: header row with title/ECO/weight (left) and `-`/`+`/`GO` buttons (right); a live `ChessBoardSection` loading the game PGN via `parsePgnMoves` + `buildMoveLabels` on `Dispatchers.Default` and parking at ply 0; a "Move Sequence" label row with teal accent; a horizontally scrollable `MoveChip` row with chip selection synced to `gameController.currentMoveIndex`; and a bottom nav row with a `TextButton("Reset")` and `←`/`→` `IconButton`s using `Icons.AutoMirrored`. `TrainingGameEditorItem` extended with `eco: String?` and `pgn: String` fields; `GameEntity.toTrainingGameEditorItem` updated to populate them. Container logic is unchanged.

- **`CreateTrainingScreen.kt` split into `CreateTrainingScreen.kt` + `EditTrainingScreen.kt`** - Creation flow (`CreateTrainingScreenContainer` / `CreateTrainingScreen`) is now in `CreateTrainingScreen.kt` with no `trainingId` param; edit flow (`EditTrainingScreenContainer` / `EditTrainingScreen`) is in `EditTrainingScreen.kt`. Shared low-level composables (`TrainingGamesEditorSection`, `TrainingGamesPage`, `TrainingGameBlock`) and helpers stay in `CreateTrainingScreen.kt` with `internal` visibility. `ScreenType.EditTraining(trainingId: Long)` added to `ScrenTypes.kt`. `MainActivity` routes `EditTraining` to `EditTrainingScreenContainer` and returns to `EditTraining` after `TrainSingleGame`. `TrainingListScreen` and `HomeScreen` `onOpenTraining` now navigate to `ScreenType.EditTraining`. `TrainSingleGameLauncherScreen` error dismiss also navigates to `EditTraining`.

- **`ProfileScreen.kt` + `ProfileViewModel.kt`** - Profile feature added under `ScreenType.Profile`. `ProfileScreenContainer` uses `remember { ProfileViewModel() }` to collect `ProfileState` and delegates to stateless `ProfileScreen`. Screen contains hero card (avatar, level badge, progress bar), quick stats row (accuracy, best streak, achievements count), achievements list with lock/unlock state, and an action menu with Settings and Clear All Data rows. Wired into `MainActivity` before the `else` branch.

- **Multi-line PGN import** - `PgnImportService.kt` now expands nested PGN variations into separate lines, and `CreateOpeningScreen.kt` saves imported lines as separate games on Save while still previewing the first line on the board.
- **`.claude` workspace documented** - `CLAUDE.md` now documents the repo-local `.claude/agents`, `.claude/commands`, and local settings file so future work can reuse those prompts as project context.
- **`PgnImportService.kt` ownership** - PGN import parsing for `CreateOpeningScreen.kt` now lives in `service/PgnImportService.kt`. The screen handles only UI state, dispatching the import job, and applying imported headers/moves to the controller.
- **`TrainingListScreen.kt` and training route split** - `ScreenType.Training` now opens a dedicated list of saved trainings. Each card shows the training name, DB ID, and games count parsed from `gamesJson`. The old `TrainingScreen.kt` role was renamed to `GamesExplorerScreen.kt` and moved under `ScreenType.GamesExplorer`.
- **`ChessBoard.kt` drag-and-drop** - replaced tap-only `detectTapGestures` with `awaitEachGesture` that distinguishes tap vs drag by `viewConfiguration.touchSlop`. During drag: piece at origin is skipped in normal draw pass and re-drawn centered on the finger (on top). On release, target square is bounds-checked (off-board = no move), then `setStartSquare` + `setDestinationSquareAndTryMove` validate and attempt the move. Two-tap flow still works for taps. All `GameController` mutations moved out of the Canvas draw block into gesture callbacks (`ChessBoardWithCoordinates`). `ChessBoard` is now a pure renderer.
- **`GameController.loadFromUciMoves(uciMoves, targetPly)`** - loads a full game from UCI strings and parks at `targetPly`; all moves kept so undo/redo works across the whole game; single `boardState++` at end
- **Shared logic extracted to `TrainingComponents.kt`** - `parsePgnMoves`, `computeLabel`, `buildMoveLabels`, `MoveChip`, `ParsedGame` moved from both screen files into one canonical location
