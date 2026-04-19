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

## New File Header

- Every newly created source file must start with a file-level comment immediately after the `package` line.
- The header comment must explain:
  - why the file exists
  - what kinds of code belong in the file
  - what kinds of code should not be added to the file
- Keep the header concise, but explicit enough to guide future edits.
- Apply this rule to all new project files created during implementation unless the user says otherwise.
