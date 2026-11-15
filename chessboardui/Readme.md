# chessboardui

`chessboardui` — Android-библиотека отрисовки шахматной доски на Jetpack Compose. Она принимает расстановку из типов `chesscore`, ориентацию, стиль и визуальное состояние фигур и рисует готовую сцену в переданном `DrawScope`.

Библиотека предоставляет:

- рисование клеток, подписей координат, фигур, заливок и рамок;
- отображение доски с белой или чёрной стороны;
- размещение неподвижных, анимируемых и перетаскиваемой фигур;
- преобразование клеток в пиксельные координаты и поиск клетки по координатам;
- расчёт центра фигуры на прямом отрезке между двумя клетками.

Библиотека принимает произвольную расстановку, включая пустую доску. Она не проверяет шахматную легальность, не применяет ходы и не требует `Position` или конкретного шахматного движка. Обработку жестов, изменение расстановки, очередь и время анимаций задаёт вызывающий код. Готового интерактивного composable-компонента в текущем API нет: вызывающий код создаёт Canvas и вызывает renderer.

## Требования и зависимости

Библиотека предназначена только для Android: renderer использует `android.graphics.Paint` и Compose `nativeCanvas`. Пакеты API — `com.example.chessboard.chessboardui.model`, `.geometry`, `.animation` и `.render`.

| Параметр текущей сборки | Значение |
|---|---|
| Минимальная версия Android | API 24 |
| Android SDK для компиляции | API 36 |
| JVM target | 17 |
| Android Gradle Plugin | 9.4.1 |
| Kotlin/Compose compiler plugin | 2.2.10 |
| Compose BOM | 2026.03.01 |
| `chesscore` | 0.1.0 |

Для работы с исходниками нужны JDK для запуска Gradle и настроенный Android SDK: через `ANDROID_HOME` либо `sdk.dir` в локальном `chessboardui/local.properties`. Локальные пути SDK не входят в Git.

Типы `chesscore`, Compose UI и Graphics входят в публичные контракты, поэтому соответствующие зависимости объявлены через `api`. Зависимости и Compose BOM включаются в метаданные публикации; их классы не упаковываются внутрь AAR.

### Настройка SDK для сборки и тестов из IDE

`chessboardui` имеет собственный корень Gradle-сборки. Файл `local.properties` из корня репозитория ChessBoard задаёт SDK для приложения; для библиотеки нужен отдельный `chessboardui/local.properties` либо доступный процессу Gradle `ANDROID_HOME`.

Создайте `chessboardui/local.properties` и укажите абсолютный путь к установленному Android SDK:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
```

Замените пример реальным путём. Можно использовать то же значение `sdk.dir`, что и в корневом `local.properties`: отдельная установка SDK не требуется. Файл локальный, исключён из Git и настраивается на каждом компьютере.

При ошибке `SDK location not found` во время запуска unit-тестов из Android Studio сначала проверьте этот файл. Сборка из терминала может работать благодаря `ANDROID_HOME`, который отсутствует в окружении запущенной IDE. Для использования переменной она должна быть доступна при запуске Android Studio. После настройки SDK выполните синхронизацию Gradle и повторите запуск теста.

Android SDK нужен и для компиляции unit-тестов этой Android-библиотеки. Успешные отчёты зависимостей и просмотр графа через `--dry-run` не подтверждают готовность SDK к компиляции.

## Подключение опубликованной версии

Координаты выпуска:

```text
com.example.chessboard:chessboardui:0.1.0
```

Публикация настроена в файловый Maven-репозиторий. Постоянный удалённый репозиторий пока не настроен. Наличие файлов выпуска требуется до разрешения зависимости потребителем.

Пример `settings.gradle.kts` потребителя, если Maven-репозиторий расположен в его каталоге `build/maven-repository/`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        exclusiveContent {
            forRepository {
                maven {
                    name = "localChessLibraries"
                    url = uri("build/maven-repository")
                }
            }
            filter {
                includeModule("com.example.chessboard", "chesscore")
                includeModule("com.example.chessboard", "chessboardui")
            }
        }
    }
}
```

Путь задаётся относительно `settings.gradle.kts` потребителя. Зависимость модуля:

```kotlin
implementation("com.example.chessboard:chessboardui:0.1.0")
```

`chesscore:0.1.0` и необходимые зависимости Compose будут получены из метаданных. Поэтому передавайте потребителю весь Maven-каталог выпуска, включая POM и `.module`. Простое копирование AAR не описывает его зависимости.

Для примера с Compose `Canvas` ниже потребителю также нужна Foundation:

```kotlin
implementation(platform("androidx.compose:compose-bom:2026.03.01"))
implementation("androidx.compose.foundation:foundation")
```

В модуле, содержащем composable-функции, должна быть настроена поддержка Compose. При подключении библиотеки через `includeBuild` Gradle по умолчанию подменяет опубликованную зависимость локальным проектом. Для использования опубликованного AAR потребитель должен отключить автоматическую подмену в своих конфигурациях.

В текущем репозитории корневой `settings.gradle.kts` включает обе библиотеки для совместной работы с исходниками и тестами в одном окне IDE. В `app/build.gradle.kts` и `chessboardui/build.gradle.kts` задано:

```kotlin
configurations.configureEach {
    resolutionStrategy.useGlobalDependencySubstitutionRules.set(false)
}
```

Поэтому приложение использует опубликованный AAR этой библиотеки, а сама библиотека использует опубликованный JAR `chesscore`, включая зависимости unit- и Android-тестов. Изменения исходников становятся доступны потребителям после публикации новой версии и обновления зависимости. Самостоятельные сборка и публикация через `-p chessboardui` сохраняются. Порядок совместной работы и проверки зависимостей описан в [корневом README](../README.md#working-in-android-studio).

## Первичная сборка и локальная публикация

В текущем репозитории библиотека получает `chesscore:0.1.0` из `build/maven-repository/` родительского каталога. После клонирования сначала сформируйте этот репозиторий с ядром: он исключён из Git. Сборка одной библиотеки не публикует другую автоматически.

Команды из корня репозитория, использующие общий Gradle Wrapper:

```shell
# Сначала ядро и его метаданные, необходимые для сборки chessboardui.
./gradlew -p chesscore build
./gradlew -p chesscore publishMavenJavaPublicationToLocalRepository

# Unit-тесты release-варианта и сборка AAR.
./gradlew -p chessboardui testReleaseUnitTest assembleRelease

# Публикация AAR, исходников и метаданных в файловый репозиторий.
./gradlew -p chessboardui publishReleasePublicationToLocalRepository
```

Публикация собирает необходимые артефакты, но сама не запускает unit- или Android-тесты. Перед выпуском выполните нужные проверки отдельно. Если `build/maven-repository/` удалён или требуемой версии нет, сначала повторите соответствующую публикацию.

Публикуется только вариант `release`. Основной AAR создаётся в `chessboardui/build/outputs/aar/`. Файлы публикации располагаются здесь:

```text
build/maven-repository/com/example/chessboard/chessboardui/0.1.0/
├── chessboardui-0.1.0.aar
├── chessboardui-0.1.0-sources.jar
├── chessboardui-0.1.0.pom
└── chessboardui-0.1.0.module
```

Gradle также создаёт контрольные суммы и метаданные репозитория. Файлы появляются после выполнения команд публикации; сама конфигурация сборки их не создаёт.

AAR содержит production-классы в `classes.jar`, Kotlin-метаданные и Android manifest. Архив исходников содержит Kotlin-файлы основной реализации. Тестовые классы, тестовые размеры и визуальный тестовый экран в выпуск не входят. Зависимости JUnit, AndroidX Test и `ui-test-manifest` не должны попадать в метаданные release-публикации. Последняя зависимость подключена только к debug-варианту для запуска Compose-тестов.

Проверяя выпуск, сопоставьте содержимое AAR и архива исходников с `src/main`, а также проверьте координаты, зависимости от `chesscore:0.1.0` и Compose в POM и `.module`. Выпущенную версию сохраняйте неизменной; изменения оформляйте новым номером. Файловый Maven-репозиторий сам не запрещает повторную публикацию по тем же координатам.

## Отрисовка позиции

Полный пример: два короля на квадратном Canvas. Стиль задаётся явно и не зависит от внешней темы.

```kotlin
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.chessboard.chessboardui.model.BoardOrientation
import com.example.chessboard.chessboardui.model.BoardRenderPiece
import com.example.chessboard.chessboardui.model.BoardRenderScene
import com.example.chessboard.chessboardui.model.BoardRenderStyle
import com.example.chessboard.chessboardui.render.drawBoardScene
import com.example.chessboard.chesscore.model.Piece
import com.example.chessboard.chesscore.model.PieceType
import com.example.chessboard.chesscore.model.Side
import com.example.chessboard.chesscore.model.Square

@Composable
fun TwoKingsBoard() {
    val scene = BoardRenderScene(
        pieces = listOf(
            BoardRenderPiece(Piece(Side.WHITE, PieceType.KING), Square('e', 1)),
            BoardRenderPiece(Piece(Side.BLACK, PieceType.KING), Square('e', 8)),
        ),
        orientation = BoardOrientation.WHITE,
    )
    val style = BoardRenderStyle(
        lightSquareColor = Color(0xFFF0D9B5),
        darkSquareColor = Color(0xFFB58863),
        whitePieceColor = Color.White,
        blackPieceColor = Color(0xFF212121),
        whitePieceOutlineColor = Color.Black,
        blackPieceOutlineColor = Color.White,
        coordinateColor = Color.Black,
    )

    Canvas(modifier = Modifier.size(320.dp)) {
        drawBoardScene(
            drawScope = this,
            scene = scene,
            squareSizePx = size.width / 8f,
            style = style,
        )
    }
}
```

`drawBoardScene` — обычная функция с явным параметром `DrawScope`. Она не создаёт Canvas и не управляет layout или обрезкой. Размер доски — `8 * squareSizePx` по каждой оси от текущего начала рисования. Все координаты renderer и геометрии заданы в пикселях; в примере размер клетки вычисляется из реальной пиксельной ширины Canvas.

Порядок слоёв фиксирован:

1. Фон клеток.
2. Подписи координат.
3. Оформления клеток в порядке списка.
4. Неподвижные фигуры, затем анимируемые, затем перетаскиваемая.

Оформления могут закрывать подписи, фигуры рисуются поверх оформлений. Пустой список фигур всё равно рисует доску, подписи и оформления.

Фигуры отображаются Unicode-глифами через Android `Paint`. Для обеих сторон используется одна форма глифа; цвета заливки и обводки выбираются по стороне фигуры из `BoardRenderStyle`. Вид глифа зависит от доступного на устройстве шрифта.

## Сцена, стиль и ориентация

| Тип | Назначение |
|---|---|
| `BoardRenderPiece(piece, square)` | Фигура `chesscore` на клетке `Square` |
| `BoardRenderScene` | Расстановка, ориентация, оформления и текущие координаты движущихся фигур |
| `BoardRenderStyle` | Семь обязательных цветов клеток, фигур, обводок и подписей |
| `BoardOrientation` | Сторона просмотра доски, независимо от стороны очередного хода |
| `BoardSquareDecoration` | Заливка клетки и необязательная рамка |
| `BoardSquareBorder` | Цвет и относительная толщина рамки |
| `BoardRenderAnimatedPiece` | Ссылка на фигуру по исходной клетке и её текущий центр |

В ориентации `WHITE` клетка `a8` находится слева сверху, `h1` — справа снизу. В ориентации `BLACK` слева сверху находится `h1`, справа снизу — `a8`. Сами фигуры и подписи не переворачиваются вверх ногами.

Обязательные параметры `BoardRenderScene` — `pieces` и `orientation`. Остальные имеют значения по умолчанию:

```kotlin
decorations = emptyList()
dragFromSquare = null
dragOffset = Offset.Zero
animatedPieces = emptyList()
```

Сцена копирует три входных списка и предоставляет неизменяемые представления. Изменение исходного списка не изменяет уже созданную сцену. Для нового кадра создайте новую сцену; `BoardRenderScene` является обычным классом и не предоставляет `copy` data-класса.

Фигуры должны занимать разные клетки. Исходная клетка любой движущейся фигуры должна присутствовать в `pieces`. Одну фигуру нельзя одновременно анимировать и перетаскивать или указать дважды в списке анимаций. Центры должны быть конечными; это относится и к `dragOffset`, даже если `dragFromSquare = null`.

## Оформления клеток

Пример рамки без заливки:

```kotlin
import androidx.compose.ui.graphics.Color
import com.example.chessboard.chessboardui.model.BoardSquareBorder
import com.example.chessboard.chessboardui.model.BoardSquareDecoration
import com.example.chessboard.chesscore.model.Square

val decoration = BoardSquareDecoration(
    square = Square('e', 4),
    fillColor = Color.Transparent,
    border = BoardSquareBorder(
        color = Color.Green,
        widthFraction = 0.07f,
    ),
)
```

Передайте оформления через `BoardRenderScene.decorations`. Можно оформлять пустые клетки и задавать несколько оформлений одной клетки: они рисуются последовательно без объединения. Для каждого элемента сначала рисуется заливка, затем рамка.

Толщина рамки равна `squareSizePx * widthFraction`. Рамка центрирована по границе клетки: половина толщины расположена внутри, половина снаружи. `widthFraction` должен быть положительным и конечным; верхняя граница не задана. Прозрачность цветов стиля и оформлений учитывается при рисовании.

## Геометрия и поиск клетки

Публичные функции пакета `geometry`:

| Функция | Результат |
|---|---|
| `calculateSquareTopLeftOffset(square, orientation, squareSizePx)` | Левый верхний угол клетки |
| `calculateSquareCenterOffset(square, orientation, squareSizePx)` | Центр клетки |
| `getSquareFromOffset(offset, orientation, squareSizePx)` | Клетка под пиксельной координатой или `null` за доской |

Начало координат — левый верхний угол доски, X направлен вправо, Y вниз. Например, центр `a1` при ориентации `WHITE` равен `(0.5 * s, 7.5 * s)`, где `s` — размер клетки в пикселях.

`getSquareFromOffset` включает верхнюю и левую границы доски, исключает нижнюю и правую. Внутренняя граница относится к клетке справа или снизу. Конечные координаты за доской возвращают `null`; неконечные координаты отклоняются.

## Анимация и перетаскивание

Публичная `calculateAnimatedPieceCenter` из пакета `animation` вычисляет текущий центр на прямом отрезке между центрами двух клеток. Она принимает `fromSquare`, `toSquare`, `progress`, `orientation` и `squareSizePx`.

Пример кадра в середине движения коня:

```kotlin
import com.example.chessboard.chessboardui.animation.calculateAnimatedPieceCenter
import com.example.chessboard.chessboardui.model.BoardOrientation
import com.example.chessboard.chessboardui.model.BoardRenderAnimatedPiece
import com.example.chessboard.chessboardui.model.BoardRenderPiece
import com.example.chessboard.chessboardui.model.BoardRenderScene
import com.example.chessboard.chesscore.model.Piece
import com.example.chessboard.chesscore.model.PieceType
import com.example.chessboard.chesscore.model.Side
import com.example.chessboard.chesscore.model.Square

val fromSquare = Square('g', 1)
val squareSizePx = 40f
val center = calculateAnimatedPieceCenter(
    fromSquare = fromSquare,
    toSquare = Square('f', 3),
    progress = 0.5f,
    orientation = BoardOrientation.WHITE,
    squareSizePx = squareSizePx,
)
val scene = BoardRenderScene(
    pieces = listOf(BoardRenderPiece(Piece(Side.WHITE, PieceType.KNIGHT), fromSquare)),
    orientation = BoardOrientation.WHITE,
    animatedPieces = listOf(BoardRenderAnimatedPiece(fromSquare, center)),
)
```

Передайте эту сцену в `drawBoardScene` с тем же размером клетки. Renderer нарисует коня в указанном центре без копии на исходной клетке.

Любое конечное значение `progress` ограничивается диапазоном `0..1`. При совпадающих клетках центр неподвижен. Функция не применяет easing и не управляет временем; значения `NaN` и бесконечности отклоняются. Для движения двух фигур вычислите два центра и передайте две ссылки в `animatedPieces`.

Для перетаскивания используются `dragFromSquare` и `dragOffset`: первое поле указывает фигуру в `pieces`, второе задаёт её центр в пикселях. Готовые центры анимаций и перетаскивания используются как переданы, без повторного преобразования ориентации. Они могут находиться за границами доски; обрезкой управляет вызывающий код.

Renderer не изменяет расстановку в конце движения. Новую расстановку, удаление взятой фигуры, обработку ввода и последующий кадр формирует вызывающий код.

## Проверка входов

Нарушение перечисленных условий вызывает `IllegalArgumentException`:

- `squareSizePx` должен быть положительным и конечным, в том числе для пустой сцены;
- координаты указателя, центров анимации и перетаскивания должны быть конечными;
- фигуры в сцене должны занимать разные клетки, а ссылки на движущиеся фигуры — быть существующими и неповторяющимися;
- `widthFraction` рамки должен быть положительным и конечным;
- `progress` расчёта анимации должен быть конечным, включая неподвижную фигуру и концы диапазона.

Размер клетки проверяется до рисования сцены. Ошибки компонентных функций передаются без обёрток. Шахматная корректность позиции и допустимость перемещения не проверяются.

## Тесты

Unit-тесты расположены в `src/test`, Android-тесты — в `src/androidTest`. Они входят в самостоятельную сборку библиотеки и не публикуются вместе с AAR.

AGP 9 по умолчанию создаёт unit-тесты только для `debug`. В `gradle.properties` библиотеки задано `android.onlyEnableUnitTestForTheTestedBuildType=false`, чтобы были доступны обе задачи: `testDebugUnitTest` и `testReleaseUnitTest`.

Из корня репозитория:

```shell
# JVM-тесты debug-варианта.
./gradlew -p chessboardui testDebugUnitTest

# JVM-тесты и AAR выбранного release-варианта.
./gradlew -p chessboardui testReleaseUnitTest assembleRelease

# Проверки изображений и видимые Compose-тесты.
./gradlew -p chessboardui connectedDebugAndroidTest
```

Unit-тесты проверяют модели, валидацию, геометрию, выбор глифов и интерполяцию. Android-тесты проверяют рисование компонент и полной сцены, порядок слоёв, размещение фигур и подписи. Для полного запуска, включая захват изображения в визуальных Compose-тестах, используйте устройство или эмулятор с API 26 или выше.

[BoardRenderingVisualTest.kt](src/androidTest/kotlin/com/example/chessboard/chessboardui/render/BoardRenderingVisualTest.kt) показывает начальную расстановку в обеих ориентациях и сохраняет итоговую картинку на экране на три секунды в каждом тесте. Оформление и анимационные центры также показаны в [BoardSceneRendererTest.kt](src/androidTest/kotlin/com/example/chessboard/chessboardui/render/BoardSceneRendererTest.kt). Ожидаемые координаты интерполяции в unit-тестах сравниваются с допустимой погрешностью.

## Структура исходников

```text
chessboardui/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── Readme.md
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   └── kotlin/com/example/chessboard/chessboardui/
    │       ├── model/
    │       │   ├── BoardRenderPiece.kt
    │       │   ├── BoardOrientation.kt
    │       │   ├── BoardRenderScene.kt
    │       │   ├── BoardRenderAnimatedPiece.kt
    │       │   ├── BoardSquareDecoration.kt
    │       │   └── BoardRenderStyle.kt
    │       ├── geometry/
    │       │   └── BoardGeometry.kt
    │       ├── animation/
    │       │   └── BoardAnimationGeometry.kt
    │       └── render/
    │           ├── BoardSquaresRenderer.kt
    │           ├── BoardSquareDecorationsRenderer.kt
    │           ├── BoardPieceRenderer.kt
    │           ├── BoardScenePiecesRenderer.kt
    │           ├── BoardCoordinatesRenderer.kt
    │           └── BoardSceneRenderer.kt
    ├── test/kotlin/com/example/chessboard/chessboardui/
    │   ├── BoardTestDimensions.kt
    │   ├── model/
    │   │   ├── BoardRenderSceneTest.kt
    │   │   └── BoardSquareDecorationTest.kt
    │   ├── geometry/
    │   │   └── BoardGeometryTest.kt
    │   ├── animation/
    │   │   └── BoardAnimationGeometryTest.kt
    │   └── render/
    │       └── PieceGlyphTest.kt
    └── androidTest/kotlin/com/example/chessboard/chessboardui/render/
        ├── BoardSquaresRendererTest.kt
        ├── BoardSquareDecorationsRendererTest.kt
        ├── BoardPieceRendererTest.kt
        ├── BoardScenePiecesRendererTest.kt
        ├── BoardCoordinatesRendererTest.kt
        ├── BoardSceneRendererTest.kt
        └── BoardRenderingVisualTest.kt
```
