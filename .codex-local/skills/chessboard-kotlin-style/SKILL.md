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

## Keep Scope Narrow

- If a function is only needed inside another function, define it inside that function.
- If a data class or other structure is only needed inside another function, define it inside that function.
- Prefer the narrowest reasonable scope so local helpers do not leak into file-level API.

## Practical Rule

- File-level declarations are for things reused across multiple functions or needed as stable screen-level helpers.
- Nested declarations are preferred when the logic is tightly bound to one screen, one container, or one local workflow.
