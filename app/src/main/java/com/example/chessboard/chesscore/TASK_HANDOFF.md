# Реализация парсинга UCI/SAN

Постепенное выделение обработки SAN, UCI и FEN в независимый слой `chesscore`.

Проект: `/home/coder/ChessBoard`. Папка `/home/coder/Documents/ChatGPT/ChessBoard plus training opening` относится к задаче в чате, не является проектом.

Описание состояния на 2026-09-08. Перед продолжением проверить актуальные исходники и Git-статус.

## Цель

Постепенно выделить общий парсинг:

```text
Текст PGN/SAN с вариантами + переданная стартовая позиция
    → список линий в UCI
```

Он используется при добавлении вариантов дебюта и продолжений FEN-позиций. Пока не переносим разделение импорта на главы, заполнение метаданных, сравнение с сохранёнными продолжениями, SAN-предпросмотр и другие сценарии приложения.

## Согласованная архитектура

- `chesscore` содержит собственные типы, интерфейсы и постепенно переносимый общий парсер.
- `chesscorechesslib` реализует интерфейсы ядра через chesslib. Зависит от `chesscore`.
- `chesscore` не должен зависеть от адаптера, chesslib, Android, Compose, Room или моделей приложения.
- Приложение создаёт `ChesslibPositionFactory` и передаёт парсеру через интерфейс. Конкретный адаптер внутри общего парсера не создаём.
- Пока это пакеты внутри `app`, отдельные библиотечные модули будут позднее.

Пакеты находятся под `/home/coder/ChessBoard/app/src/main/java/com/example/chessboard/`:

```text
chesscore/
├── model/
│   ├── Side.kt
│   ├── PieceType.kt
│   ├── Piece.kt
│   ├── Square.kt
│   ├── PromotionPiece.kt
│   └── Move.kt
├── Position.kt
├── PositionFactory.kt
├── SanLineParser.kt
└── TASK_HANDOFF.md

chesscorechesslib/
├── ChesslibPosition.kt
└── ChesslibPositionFactory.kt
```

## Реализованный контракт позиции

```kotlin
interface PositionFactory {
    fun create(fen: String? = null): Position
}

interface Position {
    fun getFen(): String
    fun getSideToMove(): Side
    fun getPiece(square: Square): Piece?
    fun getLegalMoves(): List<Move>
    fun applyMove(move: Move): Boolean
}
```

- `null` при создании означает стандартную позицию.
- Фабрика требует FEN из ровно шести полей. В комментариях сохранён пример полного FEN.
- Некорректное число полей или ошибка загрузки — `IllegalArgumentException`.
- Каждый вызов фабрики создаёт независимую доску.
- `getLegalMoves()` возвращает снимок из наших типов.
- `applyMove()` изменяет текущую позицию; недопустимый ход возвращает `false` без изменения позиции.
- Адаптер находит соответствующий нашему ходу объект среди легальных ходов chesslib и применяет его.
- `Square` проверяет диапазоны `a..h` и `1..8`.
- `Move` содержит `from`, `to`, необязательное превращение; цвет определяется позицией.

## Реализованный контракт SAN-линии

Основной библиотечный файл:

`/home/coder/ChessBoard/app/src/main/java/com/example/chessboard/chesscore/SanLineParser.kt`

Публичный вход:

```kotlin
fun parseSanLineToUci(
    positionFactory: PositionFactory,
    sanTokens: List<String>,
    startFen: String? = null,
): List<String>
```

- `sanTokens` — уже очищенный список SAN-ходов без PGN-нумерации, например `["e4", "e5", "Nf3", "Nc6", "Bb5", "a6"]`.
- PGN-токены `"1."`, `"1..."`, результаты, NAG и скобки вариантов не входят в этот вход.
- `startFen = null` означает стандартную позицию.
- Непустой `startFen` должен быть шестипольным FEN, который принимает `PositionFactory`.
- Возвращает UCI-ходы в том же порядке, например `["e2e4", "e7e5", "g1f3", "b8c6", "f1b5", "a7a6"]`.
- Ошибка отдельного SAN-токена — `SanLineParseException`.
- Ошибка создания стартовой позиции, например плохой `startFen`, пробрасывается как `IllegalArgumentException` от фабрики.

Техническая ошибка:

```kotlin
enum class SanLineParseErrorReason {
    UNRECOGNIZED_NOTATION,
    ILLEGAL_MOVE,
}

class SanLineParseException(
    val token: String,
    val localMoveNumber: Int,
    val sideToMove: Side,
    val reason: SanLineParseErrorReason,
) : IllegalArgumentException()
```

- `localMoveNumber` — локальный номер хода внутри переданной SAN-линии.
- Он не читается из PGN-нумерации и не зависит от fullmove number в FEN.

## Как сейчас устроен парсер в приложении

Основной app-facing файл:

`/home/coder/ChessBoard/app/src/main/java/com/example/chessboard/service/PgnImportService.kt`

Текущие обязанности:

- `parsePgnToUciLinesFromStart` — координирует разбор PGN/SAN-текста в линии.
- `resolvePgnImportStartPosition` — определяет исходную позицию через `PositionFactory`.
- `toLoadablePgnStartFen` — добавляет `0 1` к четырёхпольному FEN перед строгой фабрикой.
- `extractPgnMoveTokens` — режет PGN-текст на токены.
- `extractMainSanLine` — извлекает главную SAN-линию без вариантов.
- `extractSanLines` — разворачивает варианты в полные SAN-линии.
- `inferVariationStartPly` — при отсутствии номера хода определяет начало варианта по допустимости хода.
- app-specific `parseSanLineToUci` wrapper — вызывает `chesscore.parseSanLineToUci` и форматирует `SanLineParseException` через `PgnParseErrorStrings`.
- `sanToUci(san, board)` — старый chesslib-helper, пока нужен только для `inferVariationStartPly`.
- stored-PGN/UCI функции (`uciMovesToMoves`, `buildStoredPgnFromUci`, `parsePgnMoves`, `ParsedLine`) остаются app/service-логикой.

Дебютный импорт использует стандартную позицию и удаляет одинаковые линии. FEN-продолжения передают начальный FEN и сохраняют дубли для последующего подсчёта вне парсера.

## Выполненные коммиты по задаче

- `0a6fef6 Lib fow own move and position declaration and interfaces.`
  - Добавлены типы, интерфейсы и chesslib-адаптер.
  - По состоянию задачи пользователь подтверждал успешные проверки для этого этапа.
- `7a94cba Prepare parsers for using interfaces`
  - `PositionFactory` протянут через entry points PGN-парсера.
  - `resolvePgnImportStartPosition` начал создавать стартовую позицию через наш интерфейс.
- `92ae222 Use chesscore position for SAN replay`
  - Основной replay одной SAN-линии переведён на `Position`/`Move` из `chesscore`.
  - `inferVariationStartPly` специально оставлен на chesslib.
  - Пользователь сообщил, что сборка и unit tests отработали.
- `ef68e84 Extract SAN line parser to chesscore`
  - `SanLineParser.kt` перенесён в `chesscore`.
  - `PgnImportService.kt` стал тонкой app-facing обёрткой для форматирования ошибок.
  - Добавлены прямые unit-тесты `SanLineParserTest`.
  - Пользователь сообщил, что сборка и unit tests отработали.

## Что ещё не перенесено

Пока остаётся в `PgnImportService.kt`:

- PGN tokenization.
- Извлечение main line.
- Разбор вариантов.
- Duplicate-line handling.
- App-specific error wrapping через `PgnParseErrorStrings`.
- Stored-PGN/UCI функции приложения.

Особенно аккуратно:

- `inferVariationStartPly` выглядит подозрительно и оставлен без переноса намеренно.
- Не переносить эту эвристику автоматически. Сначала отдельно разобраться, какое поведение она должна иметь.
- Старый chesslib `sanToUci(san, board)` сейчас держится именно из-за `inferVariationStartPly`.

## Возможный порядок дальнейшей работы

Следующие изменения согласовывать отдельно, небольшими шагами.

Возможные следующие шаги:

1. Обсудить и проверить поведение `inferVariationStartPly`.
2. После стабилизации эвристики решить, переносить ли `extractSanLines`.
3. Отдельно рассмотреть перенос `extractPgnMoveTokens` и `extractMainSanLine` в `chesscore`.
4. После этого собрать общий facade вида `PGN/SAN text + startFen -> UCI lines`.

Не расширять задачу до собственной реализации шахматных правил, универсальной модели позиции или большой архитектурной перестройки без отдельного согласования.

## Рабочие правила

- Обсуждать код небольшими частями; пользователь просматривает и корректирует решения.
- Перед изменениями сначала объяснять, что и с какой целью планируется менять.
- Все комментарии в коде — на английском; объяснения пользователю — на русском.
- Сборки, тесты и lint запускать только с разрешения пользователя.
- Обнаруженные проблемы сначала объяснять; исправления вне согласованного шага не делать автоматически.
- Коммиты — только по просьбе.
- Перед коммитом проверять staged set и не включать unrelated файлы.

Перед работой прочитать применимые навыки:

- `/home/coder/.codex/skills/AI_COLLABORATION/SKILL.md`
- `/home/coder/.codex/skills/chessboard-kotlin-style/SKILL.md`
- `/home/coder/.codex/skills/project-directory-description/SKILL.md`

Для ранее успешного запуска Gradle использовался JDK:

```text
JAVA_HOME=/home/coder/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2
```

Ранее присутствовал посторонний untracked-файл `gradle/gradle-daemon-jvm.properties`; не включать его в изменения или коммиты, если пользователь явно не попросит.
