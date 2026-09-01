---
name: chessboard-kotlin-style
description: Use for Kotlin and Jetpack Compose work in this ChessBoard project when writing or refactoring code. Prefer early returns over if/else by extracting small local helper functions, and define functions/data classes inside enclosing functions when they are not needed outside that scope.
---

# ChessBoard Kotlin Style

Follow these style rules for Kotlin and Compose code in this project.

## Early Return First

- Prefer early return over `if / else`.
- If that makes inline code noisy, extract a small local helper function and use early return there.
- Prefer the shape:

```kotlin
private fun resolveTitle(value: String?): String {
    if (value.isNullOrBlank()) {
        return "Untitled"
    }

    return value
}
```

- Avoid branching like:

```kotlin
if (value.isNullOrBlank()) {
    return "Untitled"
} else {
    return value
}
```

- Prefer a regular function body with early return over expression-bodied `= if (...)` helpers when the branch is non-trivial.
- Prefer initializing a mutable local with the default value and then overriding it in one `if` over a split inline `if/else` assignment.

Examples:

Prefer this:

```kotlin
private fun resolvePageArrowTint(isEnabled: Boolean): Color {
    if (isEnabled) {
        return TrainingTextPrimary
    }

    return TrainingIconInactive
}
```

Instead of:

```kotlin
private fun resolvePageArrowTint(isEnabled: Boolean) = if (isEnabled) {
    TrainingTextPrimary
} else {
    TrainingIconInactive
}
```

Prefer this:

```kotlin
var currentPage = 1
if (totalGamesCount != 0) {
    currentPage = observableGamesState.offset / RuntimeContext.GamesExplorerPageLimit + 1
}
```

Instead of:

```kotlin
val currentPage = if (totalGamesCount == 0) {
    1
} else {
    observableGamesState.offset / RuntimeContext.GamesExplorerPageLimit + 1
}
```

## Keep Scope Narrow

- If a function is only needed inside another function, define it inside that function.
- If a data class or other structure is only needed inside another function, define it inside that function.
- Prefer the narrowest reasonable scope so local helpers do not leak into file-level API.
- If indentation grows beyond 5 levels because of nesting, stop narrowing scope and move functions or structures out to file scope as `private`.
- Readability is more important than keeping everything local.

## Practical Rule

- File-level declarations are for things reused across multiple functions or needed as stable screen-level helpers.
- Nested declarations are preferred when the logic is tightly bound to one screen, one container, or one local workflow.

## Event Callback Defaults

- Require callers to pass callbacks that handle user events, navigation, data changes, operation completion, results, or errors. Do not give these callbacks default values.
- Do not use no-op defaults such as `= {}` or `{ _ -> }` for event callbacks.
- If a callback is truly optional, allow a nullable callback with a `null` default only when its absence is a designed state. Handle that absence explicitly by hiding or disabling the action, or by intentionally skipping an optional follow-up action.
- Do not make a callback nullable merely to avoid requiring it at the call site.
- This rule does not apply to `@Composable` parameters that provide optional UI content, such as `actions: @Composable () -> Unit = {}`. These are UI slots, not event handlers.
- During refactors, treat existing event callback defaults as cleanup candidates unless the user asked to preserve them for API compatibility.

## Lazy Compose Containers

- Do not use `LazyColumn`, `LazyRow`, lazy grids, or other lazy containers by default merely because content must scroll.
- For bounded or paginated screen content, prefer a regular layout such as `Column` with `verticalScroll`.
- Be especially conservative with lazy containers as the top-level content container of a screen.
- Keep one clear scroll owner for each axis. Avoid nesting scroll containers that use the same axis unless the interaction has been explicitly designed and tested.
- Before introducing a top-level lazy container, verify that virtualization is materially needed because the collection is large, unbounded, or cannot reasonably be paginated.
- Consider lazy-item lifecycle behavior: items may leave composition and be recreated. Do not keep important screen or item state only inside a lazy item unless it has a stable key and an appropriate external or saveable owner.
- For screens containing chess boards, expandable sections, gesture-sensitive content, or nested scrolling components, prefer a regular scrolling container unless a lazy layout has a clear demonstrated benefit.
- If a lazy container is selected, explain the reason in the implementation plan and include tests for the relevant scrolling and state-preservation behavior.

## Compose UI Test Assertion Imports

- Call `assertExists()` and `assertDoesNotExist()` directly on `SemanticsNodeInteraction`.
- Do not add `import androidx.compose.ui.test.assertExists` or `import androidx.compose.ui.test.assertDoesNotExist`. These import symbols do not exist in this project's Compose test API and are not required for the member assertions.

## New File Header

- Every newly created source file must start with a file-level comment immediately after the `package` line.
- The header comment is a local contract for the file and must stay concise, specific, and useful during future edits.
- The header comment must explain:
  - why the file exists
  - what kinds of code belong in the file
  - what kinds of code should not be added to the file
- Include a validation date in the header comment.
- Apply this rule to all new project files created during implementation unless the user says otherwise.
