# Этап 4: Compose

## Границы

Перевести жетоны, настройки и диалоги количества/цвета на Compose. Сохранить существующие StateFlow, редьюсеры, репозитории, DI и контракты api/impl. Fragment-навигация и createGraph временно остаются: Compose Navigation, фото и разрешения — следующий отдельный этап.

## 4.1. Инфраструктура — выполнено

- В core:ui включён Compose compiler plugin из существующего version catalog.
- В core:ui и tokens_feature:impl подключены Material3, previews, debug tooling и Compose UI-тестирование через общий BOM 2025.08.00. В tokens_feature:impl явно добавлен lifecycle-runtime-compose для последующих collectAsStateWithLifecycle-подписок.
- Версии AGP, Kotlin, Gradle, BOM и уже существующих библиотек не менялись. Material3 разрешается в 1.3.2. Lifecycle использует существующую общую version reference; реально выбранная транзитивными зависимостями версия уже до изменений была 2.9.0.
- TokensTheme, TokensColors и TokensDimensions находятся в core:ui. Используются общие цвета существующего оформления, sans-serif типографика и именованные размеры. Тема светлая, как действующий AppTheme, без dynamic colors и изменения системных панелей. Feature-ресурсы не стали зависимостью core.
- ComposeView.setTokensContent устанавливает тему и DisposeOnViewTreeLifecycleDestroyed. Вызывается до подключения View; вызывающая сторона обязана назначить стабильный resource ID для восстановления rememberSaveable. ViewModel и навигацией адаптер не управляет.
- ComposeInfrastructureTest проверяет палитру/обновление текста и вызов onDispose при уничтожении View lifecycle, даже пока View ещё прикреплена к окну.
- Существующие экраны, XML, плеер, данные, предел 10 и ориентация не менялись. Koin остаётся прежним.

### Проверка библиотек для следующих итераций

Проверены опубликованные Maven Central POM и выполнена отдельная debug-компиляция минимального Android-модуля с AGP 9.0.1, Kotlin 2.2.10, Compose BOM 2025.08.00, Java target 17, compileSdk 36 и minSdk 28. В исходнике вызываются HsvColorPicker/rememberColorPickerController и LottieAnimation.

- Кандидат для 4.4: `com.github.skydoves:colorpicker-compose:1.1.2`. POM Android-артефакта использует Kotlin 2.0.0 и Compose 1.6.7; с текущим стеком проекта компилируется. Импорты начинаются с `com.github.skydoves.colorpicker.compose`. Версия 1.1.3 подтягивает Kotlin 2.2.21 и Compose Multiplatform 1.9.3, 1.1.4 — Kotlin 2.3.21 и Compose Multiplatform 1.10.3: их сейчас не вводим, чтобы не менять стек транзитивно.
- Кандидат для 4.5: `com.airbnb.android:lottie-compose:6.6.7`. POM использует Kotlin 1.9.22 и Compose BOM 2024.02.01; с текущим стеком компилируется. Подключение потребует согласованно перевести и основной Lottie с 3.4.0 на 6.6.7 и проверить имеющиеся JSON-анимации.
- На момент 4.1 оба кандидата проверялись вне приложения и не добавлялись в его зависимости; colorpicker подключён позднее в 4.4. Компиляция не доказывает корректность поведения picker/анимаций на устройстве и не является аудитом безопасности. Начальное состояние, рекомпозиции, восстановление и анимационные ресурсы проверять при соответствующей интеграции.

Источники: [colorpicker 1.1.2 POM](https://repo.maven.apache.org/maven2/com/github/skydoves/colorpicker-compose-android/1.1.2/colorpicker-compose-android-1.1.2.pom), [1.1.3 POM](https://repo.maven.apache.org/maven2/com/github/skydoves/colorpicker-compose-android/1.1.3/colorpicker-compose-android-1.1.3.pom), [1.1.4 POM](https://repo.maven.apache.org/maven2/com/github/skydoves/colorpicker-compose-android/1.1.4/colorpicker-compose-android-1.1.4.pom), [Lottie POM](https://repo.maven.apache.org/maven2/com/airbnb/android/lottie-compose/6.6.7/lottie-compose-6.6.7.pom), [ComposeView lifecycle](https://developer.android.com/develop/ui/compose/migrate/interoperability-apis/compose-in-views).

### Проверки 4.1

- `testDebugUnitTest :app:compileDebugKotlin :feature:tokens_feature:impl:compileDebugAndroidTestKotlin` — успешно, 46 JVM-тестов, debug-компиляция приложения и instrumentation-кода фичи.
- `:core:ui:connectedDebugAndroidTest` с фильтром `com.cerebus.tokens.core.ui.ComposeInfrastructureTest` — 2/2 на API 36.
- `:app:connectedDebugAndroidTest` с фильтром `com.cerebus.tokens.FeatureNavigationTest` — 3/3 на API 36; при этом собраны и установлены debug APK приложения и тестов.
- Изолированная `compileDebugKotlin` двух библиотек — успешно.
- `git diff --check` — успешно. Предупреждения JDK о native access и упаковка native-библиотек без stripping остаются вне задачи.
- IDE Sync и ручная визуальная проверка экранов не выполнялись. Release-сборки не запускались.

## 4.2. Compose-жетон — выполнено

- Добавлен internal-компонент `presentation/tokens_screen/Token.kt` в tokens_feature:impl. Получает существующий TokenState, callback, Modifier и флаг enabled для блокировки взаимодействия. Не хранит копии checked/color, не обращается к ViewModel, хранилищу или навигации.
- Форма пока CIRCLE. Отмеченный жетон использует state.color, неотмеченный — существующий baseColor. Предпочтительный размер берётся из token_width; фактическая окружность рассчитывается Canvas по ограничениям родителя. Компонент не навязывает сетку и не выходит за маленький контейнер.
- Задана минимальная область 48dp, когда позволяют ограничения родителя. Это не жёсткий requiredSize: тесное окно может ограничить размер. Увеличенная Compose-область касания у маленьких элементов не заменяет проверку расстояний между жетонами при реализации поля в 4.6.
- Добавлены локализованные описания жетона и отметки, toggleable semantics с ролью Checkbox. Нажатие само по себе не меняет отображение: новое состояние приходит от родителя. enabled не является признаком отмеченного жетона.
- Ключи списка принадлежат будущему полю: `key(state.id)`, обработчик замыкает этот же ID. Сценарий перестановки проверен тестом с контейнером; само рабочее поле пока не переводилось.
- Добавлен preview обоих состояний под общей темой. Старый TokenView, XML, логика сохранений, эффекты, максимум 10 и ориентация сохранены.

### Проверки 4.2

- `:feature:tokens_feature:impl:testDebugUnitTest :app:compileDebugKotlin :feature:tokens_feature:impl:compileDebugAndroidTestKotlin` — успешно, 34 JVM-теста фичи и debug-компиляция.
- `:feature:tokens_feature:impl:connectedDebugAndroidTest` с фильтром `presentation.tokens_screen.TokenTest` — 7/7 на API 36. Проверены изменения отметки/цвета, отсутствие локального toggle после клика, ключи и обработчики после перестановки, блокировка нажатий, круг и его масштабирование, ограничения тесного контейнера. Цвета/форма дополнительно проверяются по пикселям captureToImage.
- `git diff --check` — успешно. IDE Sync и ручная проверка TalkBack/preview не выполнялись. Release-сборок не было.

## 4.3. Настройки и количество — выполнено

- SettingsFragment теперь создаёт ComposeView со стабильным resource ID. SettingsRoute подписывается на существующий SettingsViewModel через collectAsStateWithLifecycle; SettingsScreen получает состояние и callbacks без DI/навигации. Фрагмент открывает прежние destinations и внешние ссылки из существующих строковых ресурсов.
- По уточнению пользователя градиент НЕ переносился: используется нейтральный background общей темы. Сохранены фото автора, текст, YouTube/донат, цветовое превью, настройки звука/анимации/подкрепления. При достаточной ширине — две колонки, на узком экране — одна прокручиваемая. Запрет прокрутки относится к будущему полю жетонов, а не настройкам.
- Сохранены отдельные ошибки чтения/записи, приоритет ошибки чтения, загрузка и блокировка изменения настроек во время записи. Switch не хранит собственное значение и не отправляет запись при отрисовке. Зависимость отображения флага подкрепления от камеры пока сохранена до этапа фото.
- SelectTokenNumberAlert остаётся DialogFragment в прежнем графе с прежними Safe Args, но содержимое — Compose. Количество выбирается кнопками уменьшения/увеличения в старом диапазоне 1–10. Цветовой диалог остаётся View-based до 4.4.
- В SelectTokensNumberViewModel добавлены SavedStateHandle из Koin, единый SelectTokensNumberUiState и чистый reduceTokensNumber. Черновик сохраняется отдельно от DataStore; повторная инициализация не сбрасывает выбор. Запись выполняется только по подтверждению, повторные подтверждения блокируются синхронно.
- Во время записи запрещены изменение числа, отмена, native Back и закрытие касанием вне диалога. После ошибки черновик сохраняется и доступен повтор; закрытие по успешной записи выполняется только в состоянии SAVED.
- При пересоздании Activity текущая запись продолжает работу во ViewModel. В SavedStateHandle сохраняются только число и факт завершённого успеха, не состояние SAVING: после пересоздания процесса незавершённая операция не возобновляется автоматически и диалог не зависает; пользователь может подтвердить черновик снова.
- Старые XML и CustomStyledSwitch пока оставлены до очистки 4.7. Версии, формат хранения, лимит, ориентация и API mediators не менялись.

### Проверки 4.3

- `testDebugUnitTest :app:compileDebugKotlin` — успешно, 54 JVM-теста (42 в tokens impl, включая 8 новых тестов диалога).
- `:feature:tokens_feature:impl:connectedDebugAndroidTest` с фильтрами `presentation.SettingsFlowTest,presentation.SettingsScreenTest,presentation.tokens_screen.TokenTest` — 16/16 на API 36: 4 теста реальных Fragment/DI/навигации, 5 тестов Compose-настроек/диалога и 7 регрессионных тестов жетона.
- Интеграционные тесты используют отдельную Activity только в androidTest и production Koin-модуль с подменой репозиториев. Проверены сохранение черновика при Activity recreation, отмена, блокировка Back во время записи после recreation, ошибка/повтор, обновление количества через Flow и переход в старый цветовой диалог. Пользовательские данные не меняются.
- Проверены доступность ссылок в узком окне с увеличенным шрифтом, граничные значения количества, отключённые действия и отсутствие записей от отрисовки. Восстановление новой ViewModel из снимка SavedStateHandle проверено unit-тестом; реальное уничтожение процесса не имитировалось.
- `:app:connectedDebugAndroidTest` с фильтром `com.cerebus.tokens.FeatureNavigationTest` — 3/3 на API 36; debug APK собран и запущен в рамках instrumentation.
- `git diff --check` — успешно. IDE Sync, ручная визуальная оценка и TalkBack не проверялись. Release-сборок не было.

## 4.4. Цвет — выполнено

- SelectColorDialogFragment остаётся прежним destination, но его содержимое — ComposeView со стабильным ID. SelectColorRoute собирает StateFlow с lifecycle; SelectColorScreen получает только состояние/callbacks.
- Подключён colorpicker-compose 1.1.2 только в tokens impl. Старый colorpickerview удалён из impl, app и version catalog. Удалён неиспользуемый dialog_select_color.xml с классом/атрибутами старой библиотеки и его tools:layout в nav graph; остальные XML ждут 4.7. Удалённый файл восстанавливается из Git.
- Используются HSV-палитра, отдельное превью из состояния и ползунок яркости: HsvColorPicker сам восстанавливает лишь тон/насыщенность, BrightnessSlider сохраняет возможность выбора тёмных оттенков. Прозрачность не добавлялась. Подтверждение без редактирования сохраняет исходный Int цвета точно.
- ColorUiState и чистый reduceColor разделяют загрузку, ошибку чтения и результат записи. Первый успешный снимок репозитория инициализирует отсутствующий черновик; дальнейшие эмиссии, включая обновление прогресса, его не перезаписывают.
- SavedStateHandle сохраняет черновик и завершённый успех. Незавершённое SAVING после восстановления процесса не воспроизводится автоматически; до успешного чтения подтверждение недоступно даже при восстановленном цвете. Ошибка записи оставляет диалог открытым и сохраняет выбор для повтора.
- Только события библиотеки с fromUser изменяют черновик; служебная инициализация игнорируется. initialColor стабилен на время жизни picker. На загрузке/ошибке чтения/сохранении picker не создаётся, чтобы не инициализировать отключённый controller с некорректными координатами. После ошибки записи он создаётся заново из черновика.
- Отмена и обычный Back не записывают цвет. Во время записи блокируются редактирование, повторное подтверждение, отмена, Back и касание вне диалога; закрытие выполняется после SAVED.
- Обновлены DI, локализованные строки/ID, unit- и UI-тесты, локальные правила. SettingsScreen получил testTag превью для проверки обновления через Flow. Тестовый SettingsTestActivity теперь восстанавливает программный граф после recreation по той же схеме, что MainActivity.
- Стек Gradle/AGP/Kotlin, Compose BOM, формат сохранений, лимит жетонов, ориентация, медиаторы, фото и эффекты не менялись. Проверка debugRuntimeClasspath показала colorpicker-compose/Android 1.1.2, Kotlin stdlib 2.2.10, Compose UI 1.9.0; старого colorpickerview нет.

### Проверки 4.4

- Финальный запуск `testDebugUnitTest :app:compileDebugKotlin :app:connectedDebugAndroidTest` с фильтром `com.cerebus.tokens.FeatureNavigationTest` — успешно: 63 JVM-теста (51 в tokens impl), debug-компиляция, упаковка/запуск debug APK и 3/3 теста навигации приложения на API 36.
- JVM-проверки включают 9 новых SelectColorViewModelTest: загрузка, независимые ошибки, повтор, сохранение черновика, внешние эмиссии, защита от двойной записи, восстановление состояния и чистота редьюсера.
- 22/22 instrumentation-теста tokens impl на API 36: 7 SettingsFlowTest, 3 ColorPickerTest, 5 SettingsScreenTest и 7 TokenTest. Проверены реальные клики по picker/яркости, отсутствие событий от инициализации/рекомпозиции, восстановление тёмного цвета, отмена, повторное открытие, recreation, ошибка/повтор записи и блокировка Back.
- Цвета проверяются по пикселям, обновление настроек после сохранения — через реальный SettingsViewModel/Flow. Для DialogFragment используется снимок всего экрана после завершения нативной анимации: стандартный captureToImage для встроенного ComposeView захватывал Activity под диалогом.
- Тесты используют изолированные репозитории, не пользовательские сохранения. Восстановление новой ViewModel из SavedStateHandle проверено unit-тестом; реальное уничтожение процесса не имитировалось.
- `git diff --check` — успешно. Существующие предупреждения native access JVM и strip debug symbols не исправлялись как несвязанные с миграцией цвета.
- IDE Sync, физическое устройство и ручной TalkBack не проверялись. Release-сборок не было.

## 4.5. Поле и эффекты — выполнено

- TokensFragment создаёт ComposeView со стабильным ID; навигация остаётся прежней через Safe Args/mediator. TokensRoute подписывается на ViewModel через collectAsStateWithLifecycle, TokensScreen получает состояние и callbacks.
- TokenBoard использует готовый stateless Token, ключи и клики по стабильному ID. Сохраняются до пяти колонок и двух рядов при текущем лимите 10. Чистая tokenBoardGeometry ограничивает одинаковый диаметр жетонов по обеим доступным сторонам, без прокрутки поля; пустое поле и тесные ограничения обрабатываются без отрицательных размеров.
- Перенесены меню количества/очистки/настроек, загрузка, приоритет ошибки чтения, повтор после ошибки записи и горизонтальный жест очистки справа налево. Длинный drag не становится кликом по жетону; меню даёт альтернативный способ очистки без жеста.
- Фото занимает ограниченную долю ширины и высоты рядом с полем и не вытесняет жетоны. На этом этапе сохранён AndroidView/ImageView с существующим загрузчиком: View пересоздаётся только при изменении URI, при потере разрешения показывается placeholder. Зависимость видимости от камеры пока не убиралась — это согласованный этап фото.
- Lottie и lottie-compose согласованно используют 6.6.7 из version catalog. Gradle/AGP/Kotlin/Compose BOM и остальные декларации версий не менялись. debugRuntimeClasspath подтвердил единую версию обеих Lottie-зависимостей.
- WinCelebration рисует прежние три JSON через Compose. Новая победа ключуется celebrationId; старт слева/справа/по центру — через 0/500/800 мс. Эти задержки только упорядочивают отображение; единственный таймер завершения победы остаётся во ViewModel. ON_STOP снимает анимации и помечает ID завершённым, чтобы устаревший Compose-снимок не запустил его повторно при возврате.
- Используется overload LottieAnimation с автоматическим однократным воспроизведением; для пиксельной проверки ресурсов — overload с заданным progress. API сверено с [исходником Lottie 6.6.7](https://github.com/airbnb/lottie-android/blob/v6.6.7/lottie-compose/src/main/java/com/airbnb/lottie/compose/LottieAnimation.kt).
- Звук не перенесён в render: Fragment в STARTED-подписке запускает прежний Android-плеер один раз на новый celebrationId и останавливает его по состоянию, onStop и onDestroyView. Введён локальный WinSoundOutput и factory в impl для подмены I/O в интеграционных тестах; логика audio focus/MediaPlayer не менялась.
- Репозитории, редьюсер/таймер победы, формат сохранений, публичные API и manifest ориентации не менялись. Старые TokenView/XML/SwipeParser оставлены для отдельной очистки 4.7, рабочий экран их больше не использует. Максимум 20 и общая адаптивная раскладка не включались.

### Проверки 4.5

- `testDebugUnitTest :app:compileDebugKotlin` — успешно, 67 JVM-тестов (55 в tokens impl) и debug-компиляция приложения.
- `:feature:tokens_feature:impl:connectedDebugAndroidTest` с фильтрами SettingsFlowTest, ColorPickerTest, SettingsScreenTest, TokenTest, TokensScreenTest, WinCelebrationTest и TokensFlowTest — 40/40 на API 36.
- `:app:connectedDebugAndroidTest` с фильтром FeatureNavigationTest — 3/3 на API 36; debug APK упакован, установлен и запущен.
- `git diff --check` — успешно. Существующие предупреждения native access JVM и strip debug symbols оставлены вне задачи.
- TokenBoardGeometryTest: все количества 1–10, обычные/тесные/нулевые ограничения, ограничение высотой, пять колонок/два ряда и пустое поле.
- TokensScreenTest: одинаковые размеры и видимость всех жетонов, перестановка ID, меню, ошибки/блокировки, направление/длина свайпа, отсутствие toggle при drag и ограничение области фото.
- WinCelebrationTest: парсинг всех трёх JSON новой Lottie и проверка реальных отрисованных пикселей, независимость анимации от звука, снятие на ON_STOP без повтора старого ID, отмена задержанных фейерверков и новая последовательность для нового ID.
- TokensFlowTest: реальные Fragment/DI/ViewModel с изолированными репозиториями; повторные эмиссии, recreation, background/возврат, незавершённая запись при уходе, ошибка/повтор, очистка и новая победа, изменение количества через диалог, независимые эффекты.
- Проверен настоящий поворот тестовой Activity в portrait и обратно: прогресс сохранён, эффекты остановлены и не переигрываются. Это не снимает landscape-ограничение рабочего приложения.
- Отдельный Android smoke-тест проверил успешную подготовку и start MediaPlayer, двукратное освобождение и повторный start в foreground на API 36. Проверки громкости на слух/физического устройства не было.
- IDE Sync, ручной TalkBack и проверка на физическом устройстве не выполнялись. Release-сборок не было.

## 4.6. Адаптивность и 20 — выполнено

- Рабочий диапазон 1–20 задан в TokenBoardRepository; валидация JSON и resize используют те же границы. Оба входа в диалог количества уже используют общий максимум, отдельные копии лимита не добавлялись.
- Формат tokens.json и версия схемы не изменились. Существующие JSON с 1–10 жетонами сохраняют ID, позиции, цвет и revision; новые документы с 20 читаются после повторного открытия DataStore. SharedPreferences нормализуются по историческому LEGACY_MAX_TOKENS=10. Повреждённый JSON сверх нового лимита не заменяется пустым полем. Обратная совместимость с установкой старой версии приложения, читающей максимум 10, не обещается.
- TokenBoardGeometry рассчитывает одинаковый диаметр по обеим сторонам. При 20 обычная сетка — 4×5; широкая — 2×10. TokensScreen выбирает широкую сетку при доступной области поля не меньше 840×480 dp (после системных панелей, меню и статуса). Это характеристика окна, а не определение физического устройства.
- Сначала уменьшаются промежутки до сохранения целевого размера нажатия 48 dp. Если привычная сетка уже не позволяет этот размер, перебираются допустимые числа колонок и выбирается наибольший одинаковый диаметр. Предпочтительный максимум остаётся прежним — token_width. В физически тесном окне жетоны могут быть меньше 48 dp; все должны оставаться видимыми, без прокрутки поля.
- boardContentGeometry сначала резервирует место жетонам без фото. Превью размещается снизу в высокой области, сбоку в широкой; уменьшается за счёт свободного пространства и промежутков, но не диаметра жетонов. При невозможности показать превью хотя бы 48 dp оно скрывается, а действие фото остаётся в меню. Прежний камерный gate пока сохранён до этапа фото.
- Высота сообщения загрузки/ошибки ограничена четвертью доступной высоты; длинное сообщение прокручивается отдельно, не вытесняя поле. Меню остаётся над полем.
- Снята фиксация landscape у MainActivity. До этого проверены реальные повороты поля, настроек, диалога количества, цвета и старого фото. Фото-диалог получил ScrollView и растягиваемую строку кнопок; разрешения, съёмка/выбор и URI-логика не менялись.
- Новые layout-тесты Compose перебирают 1–20, фото вкл./выкл., четыре размера области и fontScale=2; проверяют границы, положительный одинаковый размер и отсутствие пересечений. Для искусственных размеров используются тестовая плотность и уже учтённые системные отступы; это не физический планшет или запуск Android split-screen. Отдельные тесты реальной Activity проверяют поворот, 20 в диалоге, сохранение отметок, recreation и отсутствие ложной победы.
- PhotoLayoutTest использует настоящий DialogFragment с production DI и изолированным репозиторием: обе ориентации, fontScale=2, окно 320×200 dp, отмена и recreation. Камера и галерея не запускаются; данные пользователя не меняются.
- Изменены TokenBoardRepository, TokenDocuments, TokenBoardGeometry, TokensScreen, manifest приложения, XML фото; добавлены/расширены JVM- и instrumentation-тесты, актуализированы AGENTS.md. API mediators, зависимости, эффекты, версии приложения и навигационная технология не менялись. Очистка старого UI не начата.
- Сессионный кэш компилятора `.kotlin/`, появившийся во время проверок, добавлен в `.gitignore` рядом с `.gradle`.

### Проверки 4.6

- `testDebugUnitTest :app:compileDebugKotlin` — успешно: 75 JVM-тестов (63 в tokens impl) и debug-компиляция приложения.
- `:feature:tokens_feature:impl:connectedDebugAndroidTest` с фильтрами SettingsFlowTest, ColorPickerTest, SettingsScreenTest, TokenTest, TokensScreenTest, WinCelebrationTest и TokensFlowTest — 45/45 на API 36.
- `:feature:reinforcement_photo:impl:connectedDebugAndroidTest` с фильтром PhotoLayoutTest — 2/2 на API 36.
- `:app:connectedDebugAndroidTest` с фильтром FeatureNavigationTest — 4/4 на API 36; debug APK упакован, установлен и запущен. Дополнительно повторно скомпилированы androidTest tokens impl после выноса повторяющихся размеров тестового окна в константы.
- `git diff --check` — успешно. Проверен merged manifest: ограничение ориентации отсутствует.
- Первый UI-прогон: 44/45. Тест целевой сетки накладывал реальные системные отступы эмулятора на искусственно уменьшенную область, поэтому проверял тесную сетку вместо обычной. В тестовом контейнере эти отступы отмечены уже учтёнными; полный повторный прогон 45/45. Реальные повороты проверяются отдельно без этой подмены.
- Проверки выполнены на эмуляторе телефона API 36. Физический планшет, настоящий split-screen, TalkBack и уничтожение процесса не проверялись; крупные/тесные области проверены ограничениями Compose, восстановление — recreation и повторным открытием DataStore. IDE Sync отдельно не выполнялся. Существующие предупреждения native access JVM и strip debug symbols остались вне задачи.

## 4.7. Очистка — выполнено

- Проверены ссылки из Kotlin, XML, nav graph и тестов. Удалены 25 файлов старого UI: TokenView, CustomStyledSwitch, интерфейс/реализация SwipeParser, девять layout XML жетонов/настроек/количества, старое меню, два selector цвета, семь drawable XML, attrs.xml и неиспользуемая View Material3-тема themes.xml. Файлы отслеживались Git и могут быть восстановлены из предыдущего коммита.
- В tokens_nav_graph.xml удалены только tools:layout и неиспользуемый tools namespace. Destination ID, actions, deep link, Safe Args и имена Fragment остались прежними.
- В styles.xml сохранён используемый приложением AppTheme без изменения значений. В dimen.xml остался token_width; в colors.xml — цвета, используемые Compose и AppTheme. Портрет, значки приложения, MP3 и JSON анимаций сохранены. Действующие строки и ComposeView-ID не менялись.
- ViewBinding и viewbindingDelegate убраны из app и tokens impl. Фото-диалог остаётся View-based: его ViewBinding и делегат сохранены, CardView подключён прямо в photo impl, вместо получения через чужие модули/Material Views.
- Удалены декларации Material Views, ConstraintLayout, Navigation UI и Navigation Dynamic Features без потребителей. Также убраны неиспользуемый navigation-testing, прямые CardView/Lottie из app, CardView из tokens impl и дубли зависимостей JUnit/Espresso/lifecycle в app/tokens impl. Material Views и AppCompat удалены из data/reinforcement и core/logger, где нет UI.
- В version catalog удалены только осиротевшие aliases/version entries. Значения оставшихся версий не обновлялись. Compose Material3 не затронут; AppCompat, Fragment/Safe Args, Lottie/Compose, colorpicker, фото и permission-утилиты сохранены у действующих потребителей.
- Сравнение debugRuntimeClasspath до/после подтвердило отсутствие Material Views, ConstraintLayout, Navigation UI/Dynamic Features и Google Play Feature Delivery. Сохранились Kotlin 2.2.10, Compose UI 1.9.0, Compose Material3 1.3.2, Lifecycle 2.9.0, AppCompat 1.7.1, Lottie 6.6.7, colorpicker 1.1.2 и CardView 1.0.0. После удаления Material Views транзитивные DrawerLayout/Transition разрешаются в штатные версии оставшихся потребителей (1.0.0/1.4.1 вместо 1.1.1/1.5.0); новые прямые зависимости ради их удержания не вводились.
- Изменены шесть build.gradle.kts (app, tokens impl, photo impl, core/ui, core/logger, data/reinforcement), gradle/libs.versions.toml, ресурсы tokens impl, модульные AGENTS.md и этот документ. Код состояний, хранения, эффектов и расчёта сетки не менялся.

### Проверки 4.7

- `testDebugUnitTest :app:compileDebugKotlin` — успешно: 75 JVM-тестов, все затронутые модули и app скомпилированы в debug.
- `:feature:tokens_feature:impl:connectedDebugAndroidTest` с фильтрами SettingsFlowTest, ColorPickerTest, SettingsScreenTest, TokenTest, TokensScreenTest, WinCelebrationTest, TokensFlowTest и DataStoreMigrationTest — 48/48 на API 36. Это весь набор UI-проверок 4.6 плюс три проверки настоящих SharedPreferences/DataStore на Android.
- `:core:ui:connectedDebugAndroidTest` с фильтром ComposeInfrastructureTest — 2/2; `:feature:reinforcement_photo:impl:connectedDebugAndroidTest` с PhotoLayoutTest — 2/2; `:app:connectedDebugAndroidTest` с FeatureNavigationTest — 4/4. Все выполнены последовательно на работающем эмуляторе API 36, итого 56 instrumentation-проверок без ошибок/пропусков.
- В рамках проверки app debug APK заново упакован, установлен и запущен. Проверка содержимого APK подтвердила отсутствие TokenView, CustomStyledSwitch, SwipeParser и старых экранных layout/menu XML; activity_main.xml и dialog_ask_for_reinforcement_image.xml сохранены.
- Поиск в исходниках Kotlin/XML/ProGuard не нашёл ссылок на удалённые UI-классы и экранные layout/menu. `git diff --check` — успешно.
- Существующие предупреждения native access JVM и strip debug symbols остались вне задачи. Проверки выполнялись на эмуляторе API 36, а не на физическом устройстве.

## Далее — отдельный этап

Этап 4 завершает перенос жетонов, настроек, выбора количества/цвета и эффектов на Compose. Фото, разрешения и Compose Navigation остаются отдельной работой; в 4.7 их реализация не начинается.

### Обязательное уточнение раскладки от пользователя

Все жетоны должны одновременно помещаться на экране, БЕЗ прокрутки поля. Размер всех жетонов одинаковый и рассчитывается по доступным ширине И высоте с учётом меню, системных отступов и подкрепления. Сначала сокращать отступы и область фото; изображение не должно вытеснять жетоны. Цели при 20 жетонах — четыре ряда по пять на телефоне и два по десять в широком окне планшета. В тесном окне разрешено адаптировать число рядов: видимость всех жетонов приоритетнее фиксированной сетки. Проверить обе ориентации, маленькое/разделённое окно и увеличенный шрифт; не обещать минимальный touch target в окне, где физически нет места.

После каждой важной итерации — релевантные тесты, debug-компиляция и отдельный стабильный коммит. Этот документ не разрешает начинать следующую итерацию без запроса пользователя.
