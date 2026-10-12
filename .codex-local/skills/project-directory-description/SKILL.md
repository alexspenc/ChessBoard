---
name: project-directory-description
description: Use for this ChessBoard project when deciding where new code belongs. Describes persistence, UI, chesscore, chesslib-adapter, and domain package boundaries so code does not drift across layers.
---

# Project Directory Description

Follow these directory-role rules when adding or moving code in this project.

## Service Layer Role

- `app/src/main/java/com/example/chessboard/service` is for persistence-related logic.
- This layer may contain logic that is needed to save, load, normalize, serialize, deserialize, or validate database-backed data.
- Typical examples for this directory:
  - wrapping multiple database operations in one transaction
  - duplicate checks before saving games or positions
  - JSON serialization and deserialization for database fields
  - converting database records into editor or screen-ready data structures
  - helper logic that exists because of how data is stored or restored
- This layer is not for UI logic.
- This layer is not for screen workflow orchestration.
- This layer is not for broad business logic that is independent from persistence concerns.

## Practical Rule

- If the logic exists mainly because data is stored in a database, read from a database, or reconstructed from database fields, `service` is usually the right place.
- If the logic exists mainly because of screen behavior, navigation, or UI state transitions, it should stay outside `service`.


## Repository Layer Role

- `app/src/main/java/com/example/chessboard/repository` is for Room database access definitions.
- This directory contains DAO interfaces and database-level wiring such as `DataBaseProvider`.
- Typical examples for this directory:
  - `@Dao` interfaces with Room queries
  - database provider or factory classes that expose DAO or service creation
  - persistence access definitions that directly describe how the app talks to Room
- This layer is not for UI logic.
- This layer is not for business workflows.
- This layer is not for persistence helper logic that combines multiple operations or performs validation; that logic belongs in `service`.

## Repository Practical Rule

- If the code defines Room queries, DAO contracts, or database access wiring, `repository` is usually the right place.
- If the code exists to coordinate multiple DAO calls, validate data before save, or transform stored data, prefer `service` instead.


## Entity Layer Role

- `app/src/main/java/com/example/chessboard/entity` is for Room storage models and small persistence-facing value definitions.
- This directory contains `@Entity` classes that describe database tables, their stored fields, indexes, primary keys, defaults, and closely related persistence constants or masks used by those records.
- Typical examples for this directory:
  - `@Entity` data classes that describe database tables
  - storage-only fields such as serialized JSON columns, timestamps, hashes, and side masks
  - small persistence-related constants or value definitions that exist because of the database model
- This layer is not for DAO interfaces.
- This layer is not for persistence orchestration or validation logic.
- This layer is not for UI-facing view models or screen state.
- This layer is not for domain models that are independent from storage.

## Entity Practical Rule

- If the code primarily describes how a record is stored in the database, `entity` is usually the right place.
- If the code defines how records are queried, prefer `repository`.
- If the code defines how multiple records are validated, transformed, or saved together, prefer `service`.


## Analysis Layer Role

- `app/src/main/java/com/example/chessboard/analysis` is for pure chess-analysis logic that is independent from UI rendering and database access.
- This layer may contain algorithms that inspect loaded chess data, replay moves, compare positions, or derive analysis results from already available models.
- Typical examples for this directory:
  - opening deviation detection
  - move replay helpers used by analysis algorithms
  - builders that convert algorithm output into analysis-facing result structures
- This layer is not for Room DAO access or database persistence.
- This layer is not for composable UI or screen workflow orchestration.
- This layer is not for app-session runtime state.

## Analysis Practical Rule

- If the code answers a chess-analysis question from data already passed into it, `analysis` is usually the right place.
- If the code loads or saves the data being analyzed, prefer `service` or `repository`.
- If the code exists mainly to display analysis results or navigate between analysis screens, keep it in `ui/screen/...`.


## Chesscore Layer Role

- `app/src/main/java/com/example/chessboard/chesscore` is for shared chess-domain contracts, project-owned chess model types, and reusable chess text parsers that are intended to move toward a library boundary.
- This layer may contain pure PGN, SAN, UCI, and FEN parsing helpers when they depend only on Kotlin standard APIs and chesscore types.
- Typical examples for this directory:
  - project-owned chess model types such as side, piece, square, move, and promotion
  - position interfaces used by parsers, such as `Position` and `PositionFactory`
  - parser error types that expose technical data rather than localized app strings
  - SAN token-line parsing and PGN move-text helpers that are reusable outside one app workflow
- This layer is not for chesslib, Android, Compose, Room, app entities, localized UI strings, or persistence services.
- This layer is not for app import policy such as duplicate-line handling, stored-record metadata, database validation, or screen-specific error presentation.
- This layer should not expose chesslib types from public contracts.

## Chesscore Practical Rule

- If code is reusable chess parsing or chess-domain contract logic and can be expressed with chesscore types only, `chesscore` is usually the right place.
- If code needs legal move generation or board mutation, depend on `Position` or `PositionFactory` interfaces rather than a concrete chess engine.
- If code decides how imported lines are saved, deduplicated, localized, or shown to the user, keep that outside `chesscore`.


## Chesscore Chesslib Adapter Role

- `app/src/main/java/com/example/chessboard/chesscorechesslib` is for adapting chesslib to the `chesscore` interfaces.
- This layer may depend on both `chesscore` and chesslib.
- Typical examples for this directory:
  - `PositionFactory` implementations backed by chesslib
  - `Position` implementations backed by a chesslib board
  - conversion between chesslib pieces, squares, moves, sides, and chesscore model types
  - chesslib-backed FEN loading and validation behind a chesscore interface
- This layer is not for reusable PGN, SAN, UCI, or FEN parser logic.
- This layer is not for Android, Compose, Room, UI strings, persistence services, or app import workflows.
- This layer should keep chesslib objects internal unless a separately approved boundary requires otherwise.

## Chesscore Chesslib Adapter Practical Rule

- If code exists only to bridge chesslib into chesscore contracts, `chesscorechesslib` is usually the right place.
- `chesscore` must not import or instantiate `chesscorechesslib`; application-level code should create the adapter and pass it through chesscore interfaces.
- Keep parser decisions in `chesscore` when possible, and keep concrete chesslib translation in this adapter.


## Board Model Layer Role

- `app/src/main/java/com/example/chessboard/boardmodel` is for board-facing models and controllers that represent chessboard state, line drafts, moves, FEN constants, and chesslib-to-app model mapping.
- This layer may contain stateful board controllers when their state represents the board or move line itself rather than one concrete screen workflow.
- Typical examples for this directory:
  - board position and piece models
  - initial-board FEN constants
  - line draft models used while creating or editing a chess line
  - move-line controllers that apply, undo, redo, preview, or restrict chess moves
  - mapping chesslib board data into app board models
- This layer is not for composable UI rendering.
- This layer is not for Room DAO access or persistence services.
- This layer is not for screen navigation or dialog flow orchestration.
- This layer is not for app-session runtime state that should survive moving between screens.

## Board Model Practical Rule

- If the code models or mutates chessboard or line state independently from one concrete screen, `boardmodel` is usually the right place.
- If the code exists mainly to render the board, keep it in `ui` or `ui/components`.
- If the code exists mainly to save, load, or validate stored lines, prefer `service`, `repository`, or `entity`.
- If the state exists mainly to restore a screen/session after navigation, prefer `runtimecontext`.


## Root UI Layer Role

- `app/src/main/java/com/example/chessboard/ui` is for app-wide UI primitives that are shared directly by screens or reusable UI components but do not belong to one specific screen package.
- This package may contain the core chessboard renderer, UI-facing visual helpers, board orientation types, and shared UI test tag constants.
- Typical examples for this package:
  - the interactive chessboard composable and its board-rendering helpers
  - piece glyph and piece-color drawing helpers
  - shared test tag constants and test tag builders
  - small UI-facing enums used across board rendering and board interaction
- This layer is not for screen-specific containers or navigation flows.
- This layer is not for persistence, repository, or service logic.
- This layer is not for pure board/domain models that belong in `boardmodel`.
- This layer is not for reusable design-system components that belong more specifically in `ui/components`.

## Root UI Practical Rule

- If the code is shared UI infrastructure used across screens and does not fit a narrower UI package, the root `ui` package is acceptable.
- If the code is a reusable Material/design-system building block, prefer `ui/components`.
- If the code is tied to one concrete screen or flow, keep it under `ui/screen/...`.
- If the code models chessboard state without rendering it, prefer `boardmodel`.


## UI Components Layer Role

- `app/src/main/java/com/example/chessboard/ui/components` is for reusable general-purpose UI components.
- This directory contains composables that can be shared across multiple screens without being tied to one specific screen workflow.
- Typical examples for this directory:
  - shared buttons, dialogs, text fields, top bars, and bottom navigation
  - reusable layout blocks and visual wrappers
  - small composable building blocks used by multiple screens
- This layer is not for screen-specific workflow logic.
- This layer is not for navigation logic.
- This layer is not for persistence or business logic.
- This layer is not for components that only make sense inside one screen, unless they are likely to become shared.

## UI Components Practical Rule

- If a composable can be reused across multiple screens without knowing one concrete screen scenario, `ui/components` is usually the right place.
- If a composable is tightly coupled to one screen or one flow, keep it near that screen in `ui/screen/...` instead.


## Screen Layer Role

- `app/src/main/java/com/example/chessboard/ui/screen` is for screen-level UI and screen orchestration.
- This directory contains screen composables, screen containers, navigation wiring, screen-specific state, and screen-specific interaction flows.
- Typical examples for this directory:
  - screen composables and their container functions
  - loading, saving, dialog, and unsaved-changes flows tied to one screen
  - screen-specific UI pieces that do not belong in `ui/components`
  - screen-level orchestration that connects UI callbacks to lower layers
- This layer is not for DAO definitions.
- This layer is not for persistence helper logic.
- This layer is not for reusable general-purpose UI components.

## Screen Layer Project Note

- The current project still has some logic coupled to screen files more strongly than the target architecture would prefer.
- This is a legacy state of the project, not the desired direction.
- Future work should move toward clearer separation between UI and logic whenever that can be done safely and without unnecessary duplication.

## Screen Practical Rule

- If the code exists mainly because one concrete screen must load, display, save, confirm, or navigate something, `ui/screen` is usually the right place.
- If the code can be reused outside one screen or does not directly belong to screen orchestration, prefer moving it out of `ui/screen` into a more specific layer.

## Training Screen Area Role

- `app/src/main/java/com/example/chessboard/ui/screen/training` is for screen-level UI, containers, shared screen helpers, and feature-flow coordination for multi-line training and training templates.
- This area is split by feature responsibility:
  - `training/create` is for screens and containers that create trainings from all lines, selected lines, templates, or statistics.
  - `training/train` is for training list, training settings, and editing an existing training.
  - `training/template` is for training-template browsing, selection, creation, and editing.
  - `training/common` is for reusable training-editor UI and state shared by training and template editors.
  - `training/loadsave` is for screen-facing load/save mapping, unsaved-change checks, and save-result helpers used by training editor flows.
  - `training/flow` is for coordinators and result models that describe higher-level training navigation flows.
- Root files directly under `training` should be limited to shared training UI pieces or constants that do not fit one narrower subpackage.
- Active single-line training remains in `ui/screen/trainSingleLine`; not every training-related screen belongs under `ui/screen/training`.

## Training Screen Area Practical Rule

- If the code belongs to one training subfeature, place it in the matching `create`, `train`, or `template` package.
- If the code is shared by training and template editor screens, prefer `training/common`.
- If the code maps database-loaded training data into screen-editor state or handles screen-level save semantics, prefer `training/loadsave`.
- If the code coordinates navigation between training screens at a feature-flow level, prefer `training/flow`.
- If the code is for the active single-line training experience, keep it under `ui/screen/trainSingleLine`.
- If the code becomes reusable outside training screens, consider `ui/components`, `service`, `runtimecontext`, or another broader layer instead.

## Runtime Context Role

- `app/src/main/java/com/example/chessboard/runtimecontext` is for in-memory runtime state that should survive screen changes during the current app process.
- This package contains the app-level runtime-context orchestrator and screen-specific runtime-context holders.
- Typical examples for this directory:
  - selected ids, page offsets, filters, and temporary navigation-return state
  - screen session state that should be restored when returning to a screen
  - validation-aware runtime models that exist only while the app is running
- This layer is not for Room entities, DAO access, or persistence services.
- This layer is not for composable UI rendering.
- This layer is not for long-term storage; persisted data still belongs in repository/service/entity layers.

## Runtime Context Practical Rule

- If the state exists mainly to let the user leave a screen and come back to a still-valid in-memory session, `runtimecontext` is usually the right place.
- If the state must survive app restart, it does not belong only in `runtimecontext`; persist it in the appropriate storage layer.

## Main Activity Role

- `app/src/main/java/com/example/chessboard/MainActivity.kt` is the Android entry activity and the app-level orchestration point for screen dispatch, app shell setup, shared runtime context creation, and top-level cross-screen navigation.
- This file may coordinate transitions between major app screens and connect screen containers, runtime contexts, services, and feature-flow coordinators.
- Typical code for this file:
  - Android activity setup such as edge-to-edge and system bar behavior
  - `setContent` and app theme entry
  - top-level `ScreenType` dispatch
  - construction of app-owned dependencies and `RuntimeContext`
  - top-level navigation decisions between app screens
  - thin adapters that map screen or flow results to app-level navigation state
- This file is not for persistence helper logic or DAO-level work.
- This file is not for reusable composable UI components.
- This file is not for pure chess-analysis or board-model logic.
- This file is not the preferred place for detailed feature-internal flow rules when those rules can live in dedicated screen, flow, or runtime-context files.

## Main Activity Project Note

- `MainActivity.kt` currently carries more responsibility than the target architecture would ideally keep in one file.
- This is an accepted current state of the project, not a reason to avoid all new top-level orchestration there.
- Future work should gradually extract detailed feature-specific flow logic when it can be done safely and without making navigation harder to follow.

## Main Activity Practical Rule

- If the code coordinates which major screen is shown next or connects app-level state to screen containers, `MainActivity.kt` is usually the right place.
- If the code defines detailed behavior inside one feature flow, prefer a dedicated coordinator, screen container, or runtime-context holder.
- If the code grows a reusable UI, domain, persistence, or analysis helper, move it to the appropriate `ui`, `boardmodel`, `analysis`, `service`, `repository`, `entity`, or `runtimecontext` layer.

## File Header Contract

- Project source files should have a file-level comment immediately after the `package` line when they are created or materially edited.
- Treat that header comment as the local contract for the file.
- The header should explain:
  - why the file exists
  - what code belongs in the file
  - what code should not be added to the file
- The header must describe the file as it actually exists now, not as an idealized future version.
- Include a validation date in the header so future edits can quickly judge whether the comment is still trustworthy.
- Existing files without headers are allowed as legacy or externally contributed files until they are touched.

## Read Header Before Edit

- Before adding functions, classes, helpers, or doing refactors in an existing file, read the file header first when it exists.
- Use the header comment as the first check for whether the new logic belongs in that file.
- If the requested change conflicts with the header, prefer moving the code to a more appropriate file instead of silently stretching the file's responsibility.
- If the file has no header and you need to edit it, first understand the file's current role and responsibility, then add a truthful header as part of the change.

## Mixed File Rule

- If an older or externally contributed file has no header comment, treat it as a file whose responsibility must be rediscovered before editing.
- Before making meaningful changes in such a file, add a header comment first.
- If the file already mixes multiple responsibilities, say that explicitly in the header instead of pretending the file is clean.
- In that case, the header should state:
  - that the file is currently a legacy or mixed-responsibility file
  - what kinds of code are currently mixed together there
  - what kinds of new code should preferably not be added there

## Header Freshness Rule

- The header validation date should be updated when you verify that the header still matches the file.
- If the header is older than about one month, verify that it still matches the current file before relying on it.
- If the file appears stable and the header is still accurate, it is acceptable to update only the validation date.
- If there is doubt about whether the file drifted, check recent meaningful git history before refreshing the date.

## No Silent Scope Drift

- Do not quietly expand a file beyond the responsibility described in its header comment.
- If the file's real role has changed, either update the header to reflect the broader responsibility or move the new logic into a better file.

## Prefer New File Over Polluting Old File

- If a legacy file already mixes concerns, avoid adding yet another kind of responsibility there unless the task is tightly scoped and extraction would be unreasonable.
- When new logic is reasonably separable, prefer a new file with a correct header comment over making an already-messy file worse.

## Header Quality Rule

- Header comments must be specific enough to guide edit decisions.
- Avoid vague headers such as "contains helper logic" or "shared utilities" unless the file truly has that broad role and the allowed scope is still clearly defined.

## Refactor Trigger Rule

- If the header and the file contents keep drifting apart, treat that as a sign that the file should eventually be split or reorganized.
- That observation does not force an immediate refactor in every task, but it should be acknowledged in the header and in review reasoning when relevant.

## Header Examples

Example for a reusable UI component file:

```kotlin
/**
 * File role: groups reusable UI components for shared screen-level presentation.
 * Allowed here:
 * - reusable composables that can be used from multiple screens
 * - UI-only helpers for rendering, layout, and styling
 * Not allowed here:
 * - screen-specific workflow or navigation logic
 * - persistence, repository, or data-storage logic
 * Validation date: 2026-04-25
 */
```

Example for a screen file:

```kotlin
/**
 * File role: groups one concrete screen's UI and screen-specific interaction flow.
 * Allowed here:
 * - composables and state wiring for this screen
 * - screen-specific callbacks, dialogs, and orchestration
 * Not allowed here:
 * - reusable generic UI components that belong in ui/components
 * - persistence helpers or database-facing transformation logic
 * Validation date: 2026-04-25
 */
```

Example for a service file:

```kotlin
/**
 * File role: groups persistence-related logic for saving, loading, validating, or reconstructing stored data.
 * Allowed here:
 * - database-backed validation and normalization logic
 * - persistence-oriented transformations and save/load helpers
 * Not allowed here:
 * - composable UI or screen navigation logic
 * - broad screen workflow orchestration unrelated to persistence concerns
 * Validation date: 2026-04-25
 */
```

Example for a legacy mixed-responsibility file:

```kotlin
/**
 * Legacy mixed-responsibility file.
 * Current role: groups screen orchestration together with older helper logic that has not yet been cleanly extracted.
 * This file is not a clean target for new unrelated logic.
 * Allowed here for now:
 * - maintenance changes to the existing screen-specific flow
 * - narrowly scoped edits to the helper logic already living here
 * Prefer not to add here:
 * - new reusable components
 * - new persistence or algorithm-heavy logic that should live in a separate file
 * Validation date: 2026-04-25
 */
```

## File Move Practical Rule

- When moving files that are already tracked by git, use `git mv` instead of plain `mv`.
- In this project, `git mv` is preferred because it makes code review clearer and keeps file moves easier to read in diffs.
