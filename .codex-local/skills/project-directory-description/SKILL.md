---
name: project-directory-description
description: Use for this ChessBoard project when deciding where new code belongs. Describes the role of project directories so persistence, UI, and domain code do not drift across layers.
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

- `app/src/main/java/com/example/chessboard/entity` is for database storage models and closely related persistence value types.
- This directory contains Room entities and small persistence-facing types that are part of stored records, such as side-mask constants used by stored data.
- Typical examples for this directory:
  - `@Entity` data classes that describe database tables
  - small persistence-related value definitions used by entities
  - fields and defaults that exist because of how data is stored in Room
- This layer is not for DAO interfaces.
- This layer is not for persistence orchestration or validation logic.
- This layer is not for UI-facing view models or screen state.

## Entity Practical Rule

- If the code primarily describes how a record is stored in the database, `entity` is usually the right place.
- If the code defines how records are queried, prefer `repository`.
- If the code defines how multiple records are validated, transformed, or saved together, prefer `service`.


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

## File Header Contract

- Every project source file should start with a file-level comment immediately after the `package` line.
- Treat that header comment as the local contract for the file.
- The header should explain:
  - why the file exists
  - what code belongs in the file
  - what code should not be added to the file
- The header must describe the file as it actually exists now, not as an idealized future version.
- Include a validation date in the header so future edits can quickly judge whether the comment is still trustworthy.

## Read Header Before Edit

- Before adding functions, classes, helpers, or doing refactors in an existing file, read the file header first.
- Use the header comment as the first check for whether the new logic belongs in that file.
- If the requested change conflicts with the header, prefer moving the code to a more appropriate file instead of silently stretching the file's responsibility.

## Mixed File Rule

- If an older file has no header comment, treat it as a legacy file that may contain mixed responsibilities.
- Before making substantial changes in such a file, add a header comment first.
- If the file already mixes multiple responsibilities, say that explicitly in the header instead of pretending the file is clean.
- In that case, the header should state:
  - that the file is currently a legacy mixed-responsibility file
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

## File Move Practical Rule

- When moving files that are already tracked by git, use `git mv` instead of plain `mv`.
- In this project, `git mv` is preferred because it makes code review clearer and keeps file moves easier to read in diffs.
