# chesscore

## TODO: вычитка документации

- Вычитать и очистить Readme как руководство самостоятельной библиотеки.
- Убрать привязки к `chessboardui`, приложению ChessBoard и chesslib. Инструкции по сборке конкретных потребителей, сведения об их адаптерах и интеграционных тестах перенести в документацию этих проектов.
- Оставить описание назначения, публичного API, общих контрактов адаптера, подключения и выпуска библиотеки; сверить примеры, ссылки и дерево файлов после очистки.

## Назначение

`chesscore` — Kotlin/JVM-библиотека с шахматными моделями, интерфейсами позиции и парсерами PGN, SAN, UCI и формы FEN. Она позволяет преобразовать текст партии или дебютного варианта в последовательности UCI-ходов:

```text
PGN/SAN + необязательный стартовый FEN + PositionFactory
    → одна UCI-линия или список линий с вариантами
```

Библиотека подходит для импорта и обработки шахматных линий независимо от интерфейса пользователя и способа хранения данных. Она зависит только от стандартной библиотеки Kotlin. Android, Compose, Room и chesslib для сборки самого `chesscore` не нужны. Текущая конфигурация: Kotlin JVM 2.2.10, JVM target 17; тесты используют JUnit 4.13.2.

## Возможности и границы

Библиотека предоставляет:

- собственные типы стороны, фигуры, клетки и хода;
- интерфейсы `Position` и `PositionFactory` для подключения реализации шахматных правил;
- преобразование основной PGN-линии или всех её вариантов в UCI;
- преобразование списка SAN-ходов в UCI;
- токенизацию ходовой части PGN и извлечение SAN-линий без проигрывания;
- синтаксический разбор UCI-ходов и нормализацию формы FEN;
- технические исключения с контекстом ошибки.

Проверку допустимости ходов, изменение позиции и загрузку FEN выполняет переданный адаптер. В библиотеку не входит готовая реализация шахматных правил. Приложение также отвечает за удаление дублей, метаданные партии, сохранение данных и локализованные сообщения об ошибках.

Парсеры обрабатывают ходовую часть одной PGN-записи. Заголовки на отдельных строках и комментарии пропускаются; метаданные не возвращаются. Начальную позицию из заголовка `[FEN "..."]` нужно передать явно через `startFen`. Разделение файла с несколькими партиями выполняет вызывающий код. Библиотека не является полным валидатором формата PGN.

## Подключение готовой библиотеки

Исходники находятся в каталоге `chesscore/` репозитория ChessBoard и имеют самостоятельную Gradle-сборку. Приложение и `chessboardui` используют опубликованный JAR `com.example.chessboard:chesscore:0.1.0` из файлового Maven-репозитория `ChessBoard/build/maven-repository/`. Подключений `includeBuild` для ядра в настройках потребителей нет.

### Первая сборка после клонирования

**До первой синхронизации Gradle или сборки приложения и `chessboardui` сформируйте локальный Maven-репозиторий.** Его содержимое исключено из Git и отсутствует после клонирования на новый компьютер.

Из корня репозитория:

```shell
# Независимая сборка ядра и его unit-тесты.
./gradlew -p chesscore build

# Создание локального Maven-репозитория с версией 0.1.0.
./gradlew -p chesscore publishMavenJavaPublicationToLocalRepository

# Проверка потребителей с опубликованным JAR.
./gradlew -p chessboardui testDebugUnitTest assembleDebug
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Повторите публикацию, если каталог `build/maven-repository/` был удалён или в нём отсутствует требуемая версия. Команда `-p chesscore` использует собственные настройки ядра, поэтому публикация не требует предварительной сборки потребителей и не создаёт зависимости по кругу. Простое создание JAR в `chesscore/build/libs/` не заменяет публикацию: потребителям нужны файлы по Maven-координатам и метаданные зависимостей.

### Настройки потребителя

В корневом `settings.gradle.kts` репозиторий объявлен в `dependencyResolutionManagement`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        exclusiveContent {
            forRepository {
                maven {
                    name = "localChesscore"
                    url = uri("build/maven-repository")
                }
            }
            filter {
                includeModule("com.example.chessboard", "chesscore")
            }
        }
    }
}
```

В `chessboardui/settings.gradle.kts` используется тот же блок с путём `../build/maven-repository`. Фильтр направляет модуль `com.example.chessboard:chesscore` исключительно в этот файловый репозиторий; остальные зависимости разрешаются через Google и Maven Central.

Зависимость приложения:

```kotlin
implementation("com.example.chessboard:chesscore:0.1.0")
```

Зависимость `chessboardui`, публичный API которой использует типы ядра:

```kotlin
api("com.example.chessboard:chesscore:0.1.0")
```

`chessboardui` пока подключён к приложению через `includeBuild("chessboardui")`, но сам получает ядро из Maven-репозитория. Изменения исходников `chesscore` не попадают к потребителям автоматически: для следующего выпуска нужно опубликовать новую версию и обновить зависимость.

Пакеты API сохранены: `com.example.chessboard.chesscore` и `com.example.chessboard.chesscore.model`.

## Выпуск JAR и локальная публикация

Сборка настроена на выпуск `com.example.chessboard:chesscore:0.1.0`. Плагин `maven-publish` публикует стандартный компонент `java`: основной JAR, архив исходников и метаданные зависимостей.

Из корня репозитория:

```shell
# Собрать JAR и архив исходников, запустить unit-тесты.
./gradlew -p chesscore build

# Опубликовать подготовленные артефакты в файловый Maven-репозиторий.
./gradlew -p chesscore publishMavenJavaPublicationToLocalRepository
```

Команда публикации сама собирает необходимые артефакты, но не запускает unit-тесты. Для проверки выпуска сначала выполните `build`.

В `chesscore/build/libs/` формируются:

```text
chesscore-0.1.0.jar
chesscore-0.1.0-sources.jar
```

Основной JAR содержит скомпилированные production-классы и Kotlin-метаданные. Архив исходников содержит исходники `src/main`. Тесты, тестовый сценарный адаптер, chesslib и код приложения в эти архивы не входят. Стандартная библиотека Kotlin указывается как внешняя зависимость в метаданных публикации; её классы в JAR не упаковываются. JUnit остаётся только тестовой зависимостью.

Репозиторий публикации находится в `ChessBoard/build/maven-repository/`. Файлы этой версии располагаются по Maven-координатам:

```text
build/maven-repository/com/example/chessboard/chesscore/0.1.0/
├── chesscore-0.1.0.jar
├── chesscore-0.1.0-sources.jar
├── chesscore-0.1.0.pom
└── chesscore-0.1.0.module
```

Gradle также создаёт файлы контрольных сумм и метаданные репозитория. POM описывает координаты и зависимости для Maven-совместимых потребителей; Gradle Module Metadata дополнительно содержит варианты библиотеки и сведения о JVM target.

При проверке выпуска убедитесь, что основной JAR содержит классы `chesscore` и Kotlin-метаданные, архив исходников — Kotlin-файлы `src/main`, а POM и `.module` указывают версию `0.1.0` и зависимость от стандартной библиотеки Kotlin без JUnit, chesslib или моделей приложения. Например, содержимое архивов можно посмотреть командами:

```shell
jar tf chesscore/build/libs/chesscore-0.1.0.jar
jar tf chesscore/build/libs/chesscore-0.1.0-sources.jar
```

Настройка публикации ещё не означает, что артефакты уже созданы. Они появляются после запуска соответствующих команд. Каталог репозитория находится в `build/`, исключён из Git и предназначен для локальной проверки. Для сборки на другом компьютере его нужно сформировать заново; постоянное место распространения выпусков будет выбрано отдельно.

Содержимое окончательно выпущенной версии сохраняйте неизменным; дальнейшие изменения получают новый номер версии. Файловый репозиторий сам не запрещает повторную публикацию по тем же координатам.

Оба потребителя используют опубликованную версию `0.1.0`. Подмены ядра исходниками через `includeBuild` отключены в корневой сборке и в сборке `chessboardui`.

## Начало работы

Для PGN/SAN → UCI передайте реализацию `PositionFactory`, создающую независимые изменяемые позиции. В ChessBoard это `ChesslibPositionFactory`, которая находится в приложении, а не в артефакте `chesscore`. В другом проекте нужен собственный адаптер.

Пример функции, использующей переданную фабрику:

```kotlin
import com.example.chessboard.chesscore.PositionFactory
import com.example.chessboard.chesscore.parsePgnMainLineToUci

fun readOpening(positionFactory: PositionFactory): List<String> {
    return parsePgnMainLineToUci(
        positionFactory = positionFactory,
        pgnText = "1. Nf3 Nf6 2. g3 d5 3. Bg2 c6",
    )
}
```

Ожидаемый результат:

```text
[g1f3, g8f6, g2g3, d7d5, f1g2, c7c6]
```

В следующих примерах `positionFactory` также обозначает переданную реализацию `PositionFactory`.

### Продолжение из FEN

```kotlin
import com.example.chessboard.chesscore.parsePgnMainLineToUci

val moves = parsePgnMainLineToUci(
    positionFactory = positionFactory,
    pgnText = "2... d5 3. Bg2 c6",
    startFen = "rnbqkb1r/pppppppp/5n2/8/8/5NP1/PPPPPP1P/RNBQKB1R b KQkq - 0 2",
)
// [d7d5, f1g2, c7c6]
```

### Разбор всех вариантов

```kotlin
import com.example.chessboard.chesscore.parsePgnToUciLines

val lines = parsePgnToUciLines(
    positionFactory = positionFactory,
    pgnText = "1. e4 e5 (c5) 2. d4",
)
// [[e2e4, e7e5, d2d4], [e2e4, c7c5]]
```

Основная линия возвращается первой. Далее идут варианты в обратном порядке закрытия скобок. Каждая линия содержит полный путь от стартовой позиции; одинаковые линии сохраняются. Ошибка любой линии завершает разбор без частичного результата.

Вариант без номера заменяет непосредственно предшествующий ход содержащей его линии. В примере `c5` проигрывается после `e4`. При невозможном ходе другую стартовую позицию для варианта библиотека не ищет.

Вариант с явным номером использует номер и сторону относительно первого номера фрагмента. Перед открывающей скобкой должен быть собственный ход содержащей линии. Вариант должен содержать хотя бы один собственный ход; NAG до него запрещён. Например, `(d4) e4`, `e4 e5 ()` и `e4 e5 ($1 c5)` отклоняются.

`parsePgnMainLineToUci` проигрывает только основную линию: ходы вариантов игнорируются, их структура этим входом не проверяется. Например, `1. e4 e5 (1... Qa5) 2. Nf3` возвращает `[e2e4, e7e5, g1f3]`.

Оба PGN-входа возвращают пустой список для пустой ходовой части. При этом стартовая позиция создаётся и проверяется даже для пустого текста.

### Готовая SAN-линия

```kotlin
import com.example.chessboard.chesscore.parseSanLineToUci

val moves = parseSanLineToUci(
    positionFactory = positionFactory,
    sanTokens = listOf("Nf3", "Nf6", "g3", "d5", "Bg2", "c6"),
)
// [g1f3, g8f6, g2g3, d7d5, f1g2, c7c6]
```

Этот вход принимает только SAN-ходы, без номеров PGN, NAG, результатов и скобок. Для произвольного текста PGN используйте один из двух PGN-входов.

## Интерфейсы позиции

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

При реализации адаптера соблюдайте следующие условия:

- Каждый вызов `create` создаёт независимую позицию. `null` означает стандартную начальную позицию; ненулевой FEN передаётся в шестипольной форме. При отклонении FEN фабрика сообщает `IllegalArgumentException`.
- `getFen` возвращает текущий шестипольный FEN, по которому фабрика может восстановить позицию для проигрывания линии.
- `getSideToMove` возвращает сторону очередного хода; `getPiece` — фигуру на клетке либо `null` для пустой клетки.
- `getLegalMoves` возвращает снимок допустимых ходов. Последующие изменения позиции не должны менять уже возвращённый список.
- `applyMove` изменяет позицию при допустимом ходе и возвращает `true`. При недопустимом ходе возвращает `false`, сохраняя позицию.

Парсеры используют эти методы для сопоставления SAN с допустимыми ходами и последовательного проигрывания. Типы конкретного шахматного движка должны оставаться внутри адаптера.

Пример прямой работы с позицией:

```kotlin
import com.example.chessboard.chesscore.model.Move
import com.example.chessboard.chesscore.model.Square

val position = positionFactory.create()
val move = Move(from = Square('g', 1), to = Square('f', 3))
val applied = position.applyMove(move)
val fenAfterMove = position.getFen()
```

Модели находятся в пакете `model`: `Side`, `PieceType`, `Piece`, `Square`, `PromotionPiece`, `Move`. Клетка задаётся как `Square(file: Char, rank: Int)` с координатами `a..h` и `1..8`; некорректные координаты отклоняются. `Piece` содержит сторону и тип фигуры. `Move` содержит `from`, `to` и необязательное `promotion`; сторону хода определяет позиция.

## Правила FEN

Разные входы имеют разные правила обработки FEN:

| Вход | Обработка |
|---|---|
| `parsePgnToUciLines`, `parsePgnMainLineToUci` | `null` или пробельная строка означает стандартную позицию; непустой FEN нормализуется из четырёх или шести полей |
| `normalizeFenForPositionLoad` | Четыре поля дополняются `0 1`; шесть полей сохраняют счётчики; пробелы нормализуются. Пустая строка, пять полей и другое число полей отклоняются |
| `parseSanLineToUci` | `null` означает стандартную позицию; любой ненулевой FEN передаётся фабрике без нормализации |
| `PositionFactory.create` | По контракту принимает `null` либо шестипольный FEN; проверку содержимого выполняет конкретный адаптер |

```kotlin
import com.example.chessboard.chesscore.normalizeFenForPositionLoad

val fen = normalizeFenForPositionLoad("4k3/8/8/8/8/8/8/4K3 b - -")
// "4k3/8/8/8/8/8/8/4K3 b - - 0 1"
```

Нормализатор проверяет число полей и пробельные разделители. Содержимое полей и шахматную корректность позиции проверяет адаптер.

## Функции без адаптера

Эти функции не требуют позиции или шахматного движка:

| Функция | Назначение |
|---|---|
| `tokenizePgnMoveText` | Выделяет токены ходовой части PGN, пропуская BOM, строки заголовков и комментарии |
| `extractMainSanTokens` | Из готовых PGN-токенов извлекает основную SAN-линию |
| `extractSanLines` | Раскрывает готовые PGN-токены в полные SAN-линии с учётом начальной стороны хода |
| `parsePgnMoveNumber` | Возвращает `PgnMoveNumber` с номером и стороной для `1.` или `23...`; иначе `null` |
| `isPgnMoveNumberToken` | Распознаёт номера, включая простое число без точки |
| `isPgnResultToken` | Распознаёт `1-0`, `0-1`, `1/2-1/2`, `*` |
| `normalizeFenForPositionLoad` | Нормализует форму FEN |
| `isUciMoveToken` | Проверяет синтаксис UCI-токена |
| `parseUciMove` | Преобразует UCI-токен в `Move` |
| `extractUciMoveTokens` | Извлекает отдельные UCI-токены из текста в исходном порядке |

`extractSanLines` возвращает варианты в порядке закрытия скобок, основную линию последней. Это отличается от порядка результата `parsePgnToUciLines`. Номера, NAG, результаты и скобки исключаются; дубли сохраняются. Допустимость SAN-ходов эта функция не проверяет.

```kotlin
import com.example.chessboard.chesscore.parseUciMove

val move = parseUciMove("e7e8q")
// Move(Square('e', 7), Square('e', 8), PromotionPiece.QUEEN)
```

UCI-входы проверяют только синтаксис. Они принимают четыре символа координат и необязательный суффикс превращения `q/r/b/n` в любом регистре. Допустимость хода проверяется через позицию. `extractUciMoveTokens` фильтрует разделённые пробелами токены, а не ищет ходы внутри произвольных слов.

## Ошибки

Публичные исключения парсинга наследуются от `IllegalArgumentException` и содержат технические данные:

| Исключение | Контекст |
|---|---|
| `FenParseException` | Исходный `fen`, число полей `fieldCount`, причина `reason` |
| `PgnVariationParseException` | `token`, индекс токена `tokenIndex`, причина `reason` |
| `SanLineParseException` | `token`, локальный номер хода `localMoveNumber`, сторона `sideToMove`, причина `reason` |
| `PgnLineParseException` | Индекс линии `lineIndex`, исходная SAN-ошибка `sanError`, также доступная через `cause` |
| `UciMoveParseException` | Исходный `token` и причина `reason` |

`tokenIndex` и `lineIndex` отсчитываются с нуля. Индекс токена относится к результату токенизации без заголовков и комментариев, а не к символам исходной строки. Индекс линии относится к результату полного парсера до удаления дублей: основная линия имеет индекс `0`.

Номер в `SanLineParseException` считается внутри переданной линии с учётом начальной стороны, независимо от fullmove number в FEN. Причины SAN-ошибки: `UNRECOGNIZED_NOTATION` и `ILLEGAL_MOVE`; причины ошибки вариантов: `MISSING_PRECEDING_MOVE`, `EMPTY_VARIATION`, `NAG_BEFORE_FIRST_MOVE`.

Полный парсер оборачивает SAN-ошибку в `PgnLineParseException`. Парсер основной линии и `parseSanLineToUci` возвращают `SanLineParseException` напрямую. Ошибки FEN, структуры вариантов и фабрики позиции передаются без обёртки линии, в том числе при создании позиций для проигрывания. Формирование сообщения для пользователя остаётся в вызывающем коде.

## Тесты и примеры

[PositionUsageExamplesTest.kt](src/test/kotlin/com/example/chessboard/chesscore/PositionUsageExamplesTest.kt) содержит пять примеров: применение ходов и проверку FEN после каждого полухода, независимость позиций, SAN → UCI, основную PGN-линию → UCI и продолжение из FEN с хода чёрных.

Для них используется [OpeningSequenceFixture.kt](src/test/kotlin/com/example/chessboard/chesscore/fixtures/OpeningSequenceFixture.kt): семь записанных FEN и шесть ходов последовательности `1. Nf3 Nf6 2. g3 d5 3. Bg2 c6`.

Сценарный адаптер находится только в `src/test` и не входит в библиотечный JAR. Он предоставляет только следующий записанный ход, не вычисляет все допустимые ходы и не является реализацией общего шахматного движка. Неизвестная позиция, незаписанный переход или запрос ходов после последней позиции завершаются `IllegalStateException`, обозначающим неполноту тестового сценария. Примеры с вариантами из этого руководства требуют адаптера с соответствующими позициями и ходами.

Интеграционные тесты парсеров с настоящим chesslib-адаптером остаются в приложении ChessBoard: [PgnLineParserTest.kt](../app/src/test/java/com/example/chessboard/chesscore/PgnLineParserTest.kt), [SanLineParserTest.kt](../app/src/test/java/com/example/chessboard/chesscore/SanLineParserTest.kt) и [ChesslibPositionTest.kt](../app/src/test/java/com/example/chessboard/chesscorechesslib/ChesslibPositionTest.kt).

Из корня репозитория:

```shell
# Unit-тесты библиотеки.
./gradlew -p chesscore test

# Только пять примеров использования.
./gradlew -p chesscore test --tests com.example.chessboard.chesscore.PositionUsageExamplesTest

# JAR библиотеки и её unit-тесты.
./gradlew -p chesscore build

# Unit-тесты приложения с настоящим адаптером.
./gradlew :app:testDebugUnitTest
```

Используется общий Gradle Wrapper репозитория. Результат сборки JAR находится в `chesscore/build/libs/`, отчёт unit-тестов — в `chesscore/build/reports/tests/test/index.html`. Запуск тестов приложения сам по себе не запускает unit-тесты самостоятельной сборки `chesscore`.

## Структура исходников

Библиотека:

```text
chesscore/
├── settings.gradle.kts
├── build.gradle.kts
├── Readme.md
└── src/
    ├── main/kotlin/com/example/chessboard/chesscore/
    │   ├── model/
    │   │   ├── Side.kt
    │   │   ├── PieceType.kt
    │   │   ├── Piece.kt
    │   │   ├── Square.kt
    │   │   ├── PromotionPiece.kt
    │   │   └── Move.kt
    │   ├── Position.kt
    │   ├── PositionFactory.kt
    │   ├── FenParser.kt
    │   ├── PgnMoveTextParser.kt
    │   ├── PgnVariationParseException.kt
    │   ├── PgnVariationParser.kt
    │   ├── PgnLineParseException.kt
    │   ├── PgnLineParser.kt
    │   ├── SanLineParser.kt
    │   └── UciMoveParser.kt
    └── test/kotlin/com/example/chessboard/chesscore/
        ├── FenParserTest.kt
        ├── PgnMoveTextParserTest.kt
        ├── PgnVariationParserTest.kt
        ├── PositionUsageExamplesTest.kt
        ├── SquareTest.kt
        ├── UciMoveParserTest.kt
        └── fixtures/
            └── OpeningSequenceFixture.kt
```

Адаптер приложения находится отдельно и не входит в сборку библиотеки:

```text
app/src/main/java/com/example/chessboard/chesscorechesslib/
├── ChesslibPosition.kt
└── ChesslibPositionFactory.kt
```
