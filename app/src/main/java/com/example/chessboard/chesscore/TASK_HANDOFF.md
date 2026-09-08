# Реализация парсинга UCI/SAN

Реализация парсинга UCI/SAN в независимой библиотеке `chesscore`, начиная с выделения существующего общего парсера линий из приложения ChessBoard.

Проект: `/home/coder/ChessBoard`. Папка `/home/coder/Documents/ChatGPT/ChessBoard plus training opening` относится к задаче в чате, не является проектом.

Описание состояния на 2026-09-08. Перед продолжением проверить актуальные исходники и Git-статус.

## Цель

Постепенно выделить общий парсинг:

```text
Текст PGN/SAN с вариантами + начальная позиция
    → список линий в UCI
```

Он используется при добавлении вариантов дебюта и продолжений FEN-позиций. Пока не переносим разделение импорта на главы, заполнение метаданных, сравнение с сохранёнными продолжениями, SAN-предпросмотр и другие сценарии приложения.

## Согласованная архитектура

- `chesscore` содержит собственные типы и интерфейсы, затем будет содержать общий парсер.
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
└── PositionFactory.kt

chesscorechesslib/
├── ChesslibPosition.kt
└── ChesslibPositionFactory.kt
```

## Реализованный и согласованный контракт

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
- Фабрика требует FEN из ровно шести полей. В комментариях обязательно сохранён пример полного FEN.
- Некорректное число полей или ошибка загрузки — `IllegalArgumentException`.
- Каждый вызов фабрики создаёт независимую доску.
- `getLegalMoves()` возвращает снимок из наших типов.
- `applyMove()` изменяет текущую позицию; недопустимый ход возвращает `false` без изменения позиции.
- Адаптер находит соответствующий нашему ходу объект среди легальных ходов chesslib и применяет его.
- `Square` проверяет диапазоны `a..h` и `1..8`.
- `Move` содержит `from`, `to`, необязательное превращение; цвет определяется позицией.

Типы, интерфейсы и адаптер пользователь просмотрел и одобрил. Добавлены `SquareTest` и `ChesslibPositionTest`: 13 тестов прошли, debug-сборка была успешной. Затем типы перенесены в `model`, комментарии переведены на английский; после этих изменений проверки агентом не повторялись. Пользователь сообщил, что сделал коммит.

## Как сейчас устроен парсер

Основной файл:

`/home/coder/ChessBoard/app/src/main/java/com/example/chessboard/service/PgnImportService.kt`

Общие функции:

- `parsePgnToUciLinesFromStart` — координирует разбор.
- `resolvePgnImportStartPosition` — определяет исходную позицию.
- `extractSanLines` — разворачивает варианты в полные SAN-линии.
- `inferVariationStartPly` — при отсутствии номера хода определяет начало варианта по допустимости хода.
- `parseSanLineToUci` — последовательно преобразует SAN в UCI и применяет ходы.
- `sanToUci` — сопоставляет SAN с легальными ходами.

Дебютный импорт использует стандартную позицию и удаляет одинаковые линии. FEN-продолжения передают начальный FEN и сохраняют дубли для последующего подсчёта вне парсера.

`toLoadablePgnStartFen` добавляет `0 1` к четырёхпольному FEN. Новая фабрика отклоняет остальные количества полей, кроме шести. Пользователь согласовал это усиление контракта. Пустой или отсутствующий начальный FEN в общем парсере означает стандартную позицию.

Парсер использует chesslib для создания/загрузки доски, получения FEN и стороны, чтения фигуры на поле, получения легальных ходов и их применения. Отмена, история, Zobrist-хеш и отдельная проверка шаха/мата этому парсеру не нужны.

## Последнее выполненное изменение — ещё без проверки сборкой

Пользователь разрешил только первый связанный шаг перехода парсера на интерфейсы:

- `resolvePgnImportStartPosition` теперь принимает `PositionFactory`, создаёт позицию через неё, получает FEN и сторону через наш интерфейс.
- `PgnImportStartPosition.sideToMove` теперь использует `chesscore.model.Side`.
- Обязательная фабрика проведена через вызывающие функции парсинга.
- Конкретная фабрика создаётся у потребителей дебюта и FEN-продолжений.
- Поскольку создание начальной позиции общее также для импорта партий в анализ, этот путь тоже обновлён.
- В существующих тестах обновлены аргументы вызовов.
- Остальные операции с ходами всё ещё используют chesslib. Полный перенос парсера не выполнен.

Изменено 10 файлов. Пути ниже относительно `app/src/main/java/com/example/chessboard/` и `app/src/test/java/com/example/chessboard/` соответственно:

```text
main:
service/PgnImportService.kt
runtimecontext/GameOpeningAnalysisImport.kt
ui/screen/createOpening/CreateOpeningImport.kt
ui/screen/createOpening/CreateOpeningScreenContainer.kt
ui/screen/fenpositions/continuations/FenPositionContinuationTextProcessing.kt
ui/screen/fenpositions/continuations/AddFenPositionContinuationsTextContainer.kt

test:
service/PgnServiceTest.kt
service/PgnFenImportServiceTest.kt
service/AnalysisPgnBuilderTest.kt
ui/screen/fenpositions/continuations/FenPositionContinuationTextProcessingTest.kt
```

Последний `git diff --check` прошёл. Сборка и тесты после этого шага не запускались, коммит агент не создавал. Проверить актуальный Git-статус перед продолжением. Ранее присутствовал посторонний untracked-файл `gradle/gradle-daemon-jvm.properties`; не включать его в изменения.

## Порядок дальнейшей работы

Сначала пользователь просматривает текущий шаг. Не переходить автоматически к полному переносу парсера или расширению интерфейсов. Следующие небольшие изменения согласовывать отдельно.

Предпочтения пользователя:

- Обсуждать код небольшими частями; он просматривает и корректирует решения.
- Не расширять задачу до универсальной модели позиции, собственной реализации FEN или большой архитектурной перестройки.
- Все комментарии в коде — на английском; объяснения пользователю — на русском.
- Сборки, тесты и lint запускать с разрешения пользователя.
- Обнаруженные проблемы сначала объяснять; исправления вне согласованного шага не делать автоматически.
- Коммиты — только по просьбе.

Перед работой прочитать применимые навыки:

- `/home/coder/.codex/skills/AI_COLLABORATION/SKILL.md`
- `/home/coder/.codex/skills/chessboard-kotlin-style/SKILL.md`
- `/home/coder/.codex/skills/project-directory-description/SKILL.md`

Для ранее успешного запуска Gradle использовался JDK:

```text
JAVA_HOME=/home/coder/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2
```

При ограничениях среды запись в `/home/coder/ChessBoard` и Gradle-кэш требовала разрешения.
