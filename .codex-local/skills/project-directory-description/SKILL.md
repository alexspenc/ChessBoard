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

## File Move Practical Rule

- When moving files that are already tracked by git, use `git mv` instead of plain `mv`.
- In this project, `git mv` is preferred because it makes code review clearer and keeps file moves easier to read in diffs.

