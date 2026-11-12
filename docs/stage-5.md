# Этап 5: фото, разрешения и Compose Navigation

## Итерации

1. **5.1. Надёжный фото-процесс.** Photo Picker без storage permissions; TakePicture во внешний camera app с FileProvider и приватным файлом. SavedStateHandle для ожидающей операции и результата записи, отсутствие одноразовых SharedFlow-команд. Сохранять прежний URI при отмене/ошибке, удалять только собственный незавершённый файл. Убрать зависимость подкрепления от камеры. Unit/Android-проверки, debug-компиляция и коммит.
2. **5.2. Compose-фото.** Заменить XML-диалог на Compose с прежними состояниями/файловым процессом. Общий безопасный асинхронный preview для фото и поля. Проверить запуск контрактов, недоступный URI, отсутствие камеры, крупный шрифт, поворот/восстановление и ошибки записи. Удалить ViewBinding/CardView без потребителей. Проверки и коммит.
3. **5.3. Compose Navigation.** Использовать navigation-compose той же версии, что текущая Navigation. Типизированные публичные входы feature-api, регистрация destinations через mediator, ViewModel с SavedStateHandle в scope back-stack entry. App собирает граф, impl других фич не импортируется. Перенести поля/настройки/диалоги, запуск звука и lifecycle; сохранить семантику отмены и подтверждения. Проверить back stack, возврат, обновления Flow, recreation и эффекты. Удалить Fragment/XML/Safe Args только после переноса всех потребителей. Проверки и коммит.

## Инварианты и риски

- Не менять версии приложения, JSON-схему, диапазон 1–20 и правила жетонов.
- Результат выбора записывается в единственный ReinforcementRepository; отдельный UiCache/nav-result не нужен.
- Прежние URI не сбрасывать. Если доступ к старому внешнему изображению утрачен (особенно старые непостоянные grants на Android 9), показать недоступность и предложить повторный выбор, не выдавать это за пустое сохранение.
- Новые выбранные изображения копировать в приватные файлы приложения: после успешного импорта они не зависят от срока URI grant. Оригиналы галереи и ранее сохранённые изображения не удалять. Снимки камеры также приватные, без публикации в общей галерее.
- При возврате результата до окончания чтения DataStore дождаться чтения, не потерять результат. Повторная доставка/пересоздание не должны запускать вторую камеру или повторную запись.
- Отмена не равна успеху. Не удалять файл, если он уже стал текущим в репозитории; ошибки записи оставляют возможность повтора.
- Реальное уничтожение процесса, внешняя камера разных производителей и Android 9 требуют отдельных проверок; recreation/SavedStateHandle-тесты не подменяют их.

## Основания выбора Android API

- [Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker): точечный выбор изображения; AndroidX предоставляет fallback на ACTION_OPEN_DOCUMENT.
- [Минимизация разрешений](https://developer.android.com/privacy-and-security/minimize-permission-requests): внешняя камера через ACTION_IMAGE_CAPTURE не требует CAMERA, если приложение само не обращается к камере.
- [Take photos](https://developer.android.com/media/camera/camera-deprecated/photobasics): передача собственного выходного URI через FileProvider.

## 5.1 — выполнено

- В `app/src/main/AndroidManifest.xml` удалены CAMERA, READ_MEDIA_IMAGES, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE и requestLegacyExternalStorage. Камера остаётся необязательной аппаратной возможностью. В `app/src/main/res/xml/file_paths.xml` добавлен узкий приватный путь reinforcement; прежний external-files путь сохранён для совместимости старых URI.
- В photo impl добавлен `PhotoFiles.kt`: создание приватного снимка, импорт выбранного изображения, проверка заголовка изображения без декодирования полного bitmap, безопасная очистка только собственных временных файлов. Сохранённое фото не зависит от дальнейшего доступа к оригиналу галереи.
- Переработаны `ChangePhotoViewModel.kt`, `PhotoUiState.kt`, `AskForReinforcementImageDialog.kt`, DI и строки ошибок. Activity Result contracts запускаются без запросов разрешений. SavedStateHandle хранит ожидающий источник, URI камеры, подготовленный результат и успешную запись; повторный callback игнорируется. После восстановления незавершённая запись требует явного повтора. Ранний результат дожидается загрузки настроек.
- Из `TokensScreen.kt`, `SettingsScreen.kt` и их Fragment-адаптеров убрана проверка наличия камеры: показ подкрепления определяется только сохранённой настройкой. `OtherExtensions.kt` безопасно обрабатывает отозванный доступ к старому URI, в том числе между проверкой и отображением. Асинхронный Compose preview с явным сообщением о недоступности остаётся задачей 5.2.
- Удалены больше не используемые `PermissionType.kt` и `PermissionsManager.kt`; восстановить их можно из истории Git. Обновлены модульные AGENTS.md. Версии, схема DataStore и прежние URI не менялись.
- Изменены `PhotoStateTest.kt`, `SettingsScreenTest.kt`, `TokensScreenTest.kt`; добавлены `PhotoFilesTest.kt` и тестовый FileProvider/XML path.

### Проверки 5.1

- `testDebugUnitTest :app:compileDebugKotlin` — успешно, 84 JVM-теста; из них 13 сценариев фото: отмена, ошибка чтения/записи, повтор, отсутствие камеры, поздний подписчик, ранний результат, восстановление SavedStateHandle и защита уже сохранённого файла.
- `:feature:tokens_feature:impl:connectedDebugAndroidTest` с фильтрами SettingsFlowTest, ColorPickerTest, SettingsScreenTest, TokenTest, TokensScreenTest, WinCelebrationTest, TokensFlowTest, DataStoreMigrationTest — 49/49 на эмуляторе API 36.
- `:feature:reinforcement_photo:impl:connectedDebugAndroidTest` — 7/7: четыре файловых проверки, две PhotoLayoutTest с крупным шрифтом/поворотом/recreation и один шаблонный тест. Приватная копия читается после удаления тестового оригинала; очистка отвергает чужие URI и выход за свой каталог.
- `:core:ui:connectedDebugAndroidTest` — 3/3, включая две ComposeInfrastructureTest.
- `:app:connectedDebugAndroidTest` с фильтром FeatureNavigationTest — 4/4; debug APK собран, установлен и запущен на эмуляторе. Итого 61 профильная instrumentation-проверка и два дополнительных шаблонных теста прошли.
- **Известная несвязанная ошибка:** полный запуск app instrumentation завершился неуспешно из-за существующего ExampleInstrumentedTest.useAppContext: ожидает `com.cerebus.tokens`, но фактический applicationId давно `com.cerebus.tokens_new`. Старое ожидание подтверждено в HEAD, тест не изменён. Отдельный последующий запуск четырёх рабочих сценариев навигации прошёл. Полный набор тестов app нельзя считать зелёным.
- Merged manifest не содержит четырёх удалённых разрешений или requestLegacyExternalStorage. `git diff --check` — успешно. Предупреждение JDK native access остаётся вне задачи.
- Реальная внешняя камера/Photo Picker, уничтожение процесса ОС, Android 9 и физическое устройство пока не проверены. Файловые тесты и восстановление новой ViewModel из SavedStateHandle не подменяют эти сценарии.

После 5.1 запланированы 5.2 (Compose-фото и общий асинхронный preview) и 5.3 (Compose Navigation).

### Исправление шаблонного теста перед 5.2

По запросу пользователя исправлен app/ExampleInstrumentedTest: ожидаемый package name приведён к действующему applicationId `com.cerebus.tokens_new`. Конфигурация приложения не менялась. `:app:connectedDebugAndroidTest :app:compileDebugKotlin` — успешно, полный набор app 5/5 на API 36; прежняя ошибка теста устранена.

## 5.2 — выполнено: Compose-фото

- Добавлен `PhotoScreen.kt`: PhotoRoute собирает состояние lifecycle-aware, экран получает PhotoUiState и callbacks, без отдельной копии данных. Вертикальные действия и прокрутка сохраняют доступность при крупном шрифте и низком окне. Загрузка, запись, ошибки чтения/записи/источника отображаются отдельно.
- `AskForReinforcementImageDialog.kt` теперь адаптер ComposeView со стабильным ID из `res/values/ids.xml`. Пока сохраняет Activity Result contracts и Fragment-навигацию. PhotoUiState.cancellable используется для кнопки отмены и нативного диалога; после пользовательского действия/результата isCancelable обновляется синхронно, до следующего кадра. Закрытие после успешной записи осталось состоянием, а не одноразовым событием.
- Добавлен `core/ui/PhotoPreview.kt`, подключён в фото-диалоге и `TokensScreen.kt`. ImageDecoder работает на Dispatchers.IO, длинная сторона bitmap ограничена 1024px с сохранением пропорций. Поддерживаются локальные URI; сетевые не загружаются. Смена URI создаёт новое состояние загрузки без показа прежнего фото. Ошибка чтения/отозванное разрешение показывают предложение повторного выбора, не меняя сохранение. Используется встроенный Android API, новая библиотека не понадобилась. Основание: [ImageDecoder](https://developer.android.com/reference/android/graphics/ImageDecoder).
- В photo impl включён Compose compiler и зависимости из существующего BOM/catalog. Удалены ViewBinding, CardView и viewbindingDelegate, а также их неиспользуемые catalog entries; версии оставшихся зависимостей не обновлялись.
- Удалены `dialog_ask_for_reinforcement_image.xml` и `core/ui/OtherExtensions.kt` с ImageView.setPhotoImage; восстановимы из Git. В навигационном XML удалена только ссылка tools:layout, destination/action/deep link сохранены. У поля жетонов больше нет AndroidView; геометрия и бизнес-правила не менялись.
- Обновлены строки EN/RU в core/ui и photo impl, правила трёх затронутых UI-модулей. Изменён `PhotoLayoutTest.kt`, добавлены `PhotoScreenTest.kt`, `PhotoPreviewTest.kt`, тестовый `DeniedPhotoProvider.kt` и его AndroidManifest.xml.
- PhotoLayoutTest использует настоящие Fragment, SavedStateHandle/DI и ActivityResultRegistry, но перехватывает запуск внешних Intent. Проверяет отмену с прежним URI, результат камеры после recreation без повторного запуска, запрет Back при записи, недоступную камеру с работающей галереей и повтор записи без повторного импорта. Это не тест реального приложения камеры/Photo Picker и не уничтожение процесса ОС.

### Проверки 5.2

- `testDebugUnitTest :app:compileDebugKotlin` — успешно: 84 JVM-теста и debug-компиляция приложения с зависимыми модулями.
- Полный `:feature:reinforcement_photo:impl:connectedDebugAndroidTest` — 13/13: шесть PhotoLayoutTest, две PhotoScreenTest, четыре PhotoFilesTest, один шаблонный тест. После усиления проверки отмены с существующим фото весь набор повторён успешно.
- Полный `:core:ui:connectedDebugAndroidTest` — 7/7: четыре PhotoPreviewTest, две ComposeInfrastructureTest, один шаблонный тест. Проверены уменьшение изображения с сохранением пропорций, отсутствующий/повреждённый/сетевой URI, смена URI и сообщение об ошибке. Тестовый provider имитирует SecurityException при отозванном доступе и подтверждает, что чтение выполняется не на main thread.
- Полный `:app:connectedDebugAndroidTest` — 5/5, включая исправленный шаблонный тест. Debug APK собран, установлен и запущен на эмуляторе.
- `:feature:tokens_feature:impl:connectedDebugAndroidTest` с фильтрами SettingsFlowTest, ColorPickerTest, SettingsScreenTest, TokenTest, TokensScreenTest, WinCelebrationTest, TokensFlowTest и DataStoreMigrationTest — 49/49. Проверена регрессия настроек, эффектов и адаптивного размещения жетонов с фото.
- Итого 74 Android-теста на эмуляторе API 36, без ошибок и пропусков. Это перечисленные наборы, а не полный instrumentation-прогон всех модулей проекта.
- В debug APK отсутствуют старый layout фото, его binding-класс, CardView и viewbinding delegate; activity_main.xml сохранён. В исходниках нет оставшихся вызовов setPhotoImage, AndroidView или использования удалённых зависимостей. Merged manifest не возвращает разрешения камеры/хранилища. `git diff --check` — успешно; новые числовые значения production-кода вынесены в константы.
- IDE Sync отдельно не проверялся. Внешние камера/Photo Picker, уничтожение процесса ОС, физическое устройство и Android 9 требуют отдельных проверок. Предупреждения JDK native access и strip debug symbols остаются прежними.

После 5.2 запланирована итерация 5.3: Compose Navigation.

## 5.3 — выполнено: Compose Navigation

- MainActivity переведена на ComponentActivity/setContent, TokensTheme и rememberNavController. App собирает NavHost через registerGraph двух mediators, не импортируя ViewModel или внутренние маршруты impl. Повторный стартовый переход при recreation не выполняется. Автоматические анимации переходов отключены, как в прежнем графе без transition-анимаций.
- В api жетонов опубликованы @Serializable TokensGraph, TokensBoard и TokensSettings; в api фото — PhotoDestination. TokensEntry/open остаются типизированным входом. Внутренние CountDestination(count) и ColorDestination принадлежат impl; передаётся число, а не Parcelable-модель хранения. Прежние URI внутренних ссылок настроек/фото зарегистрированы в новом графе.
- Mediator теперь регистрирует destinations через NavGraphBuilder, вместо возврата NavInflater/NavGraph. ViewModel создаются существующими Koin-определениями через общую navigationViewModel: AndroidX ViewModelProvider использует store и CreationExtras конкретного NavBackStackEntry; SavedStateHandle создаётся для этого entry и передаётся параметром Koin. Koin не обновлялся, внутренние Koin API не используются.
- Для трёх диалогов используется NavigationDialog. Автоматическое закрытие DialogNavigator выключено; полноэкранная прозрачная область обрабатывает нажатия снаружи, BackHandler — Back. Callback проверяет текущее состояние ViewModel, поэтому запись нельзя прервать в промежутке до следующего кадра. Позднее/повторное завершение закрывает только своё текущее entry через popEntryIfCurrent, а не произвольный следующий экран.
- PhotoDestinationContent использует стабильные rememberLauncherForActivityResult и STARTED-подписку. Подготовленные URI, ожидающий источник и успешная запись по-прежнему принадлежат SavedStateHandle/ViewModel. Результат, пришедший до регистрации нового launcher после recreation, доставляется через ActivityResultRegistry.
- TokensLifecycle переносит ON_START/ON_STOP, обработку нового celebrationId и остановку звука из Fragment в lifecycle back-stack entry. При уходе с поля/уничтожении композиции эффекты прекращаются; переход в настройки сразу помечает посещение завершённым, не позволяя отложенной записи запустить победу. Эффекты не запускаются из render.
- В SettingsLinks перенесено прежнее открытие ссылок. DataStore, редьюсеры, бизнес-правила, версии приложения и состояние жетонов не менялись.
- Удалены пять Fragment-адаптеров, три XML-графа, activity_main.xml, FragmentExtensions и неиспользуемые View ID. Safe Args, Parcelize compiler/runtime и прямые зависимости Navigation Fragment/Fragment KTX больше не нужны и убраны вместе с catalog entries. Файлы восстановимы из истории Git. AppCompat сохранён для существующих тем; наличие транзитивного Fragment через AppCompat/Koin не означает использование Fragment-навигации.
- В version catalog добавлены navigation-compose с прежней версией Navigation 2.9.6 и activity-compose с прежним version.ref activity. В api включена serialization, в app — Compose. Gradle/AGP/Kotlin/Koin/Compose BOM не обновлялись. Изменены build.gradle.kts app, core/ui, обеих api/impl фич и root; правила соответствующих модулей актуализированы.
- FeatureNavigationTest, SettingsTestActivity/SettingsFlowTest, TokensFlowTest и PhotoLayoutTest переведены на настоящие Compose-host. Проверки отмены, черновика, ошибок, поворота и повторной победы сохранены. Проверка тесного окна фото перенесена в PhotoScreenTest. Добавлены проверки раннего результата фото, обычного Back, внешнего нажатия при сохранении и повторного закрытия устаревшего entry.

### Основания и границы

- Типизированные маршруты построены на официальных [type-safe Navigation API](https://developer.android.com/guide/navigation/design/type-safety), имеющихся в используемой версии Navigation.
- Новый стек восстанавливается при recreation. Реальное уничтожение процесса ОС, переход между старой APK с Fragment-графом и новой с Compose-графом, внешние приложения камеры/Photo Picker и Android 9 требуют отдельных проверок. Сохранения DataStore не переписывались.

### Проверки 5.3

- `testDebugUnitTest :app:compileDebugKotlin` — успешно, 84 JVM-теста. Скомпилированы все затронутые production-модули и Android-тесты app/tokens/photo.
- `:app:connectedDebugAndroidTest` — 6/6: публичные входы между фичами, обе ориентации, recreation/back stack, отсутствие дублирующего входа в фото и защита от закрытия чужого entry.
- `:feature:reinforcement_photo:impl:connectedDebugAndroidTest` — 16/16: сохранены файловые/UI-проверки, добавлены ранний результат после recreation, Back в обычном состоянии и внешнее нажатие с защитой записи.
- `:core:ui:connectedDebugAndroidTest` — 7/7, включая предпросмотр и тему.
- `:feature:tokens_feature:impl:connectedDebugAndroidTest` с фильтрами SettingsFlowTest, ColorPickerTest, SettingsScreenTest, TokenTest, TokensScreenTest, WinCelebrationTest, TokensFlowTest и DataStoreMigrationTest — 49/49. Набор повторён после последней правки защиты закрытия; проверены отмена/новый черновик, восстановление при сохранении, повтор ошибки, количество/цвет через Flow и звук без повтора.
- Итого 78 Android-проверок на API 36 без ошибок и пропусков в перечисленных наборах. Debug APK приложения собран, установлен и запущен на эмуляторе.
- В ходе адаптации один тест проверял результат камеры раньше повторной регистрации Compose launcher. Синхронная проверка блокировки перенесена после готовности композиции; отдельно добавлен и прошёл сценарий действительно ранней доставки без ожидания регистрации. Проверка потери результата не удалена и не заменена ожиданием без результата.
- Поиск исходников не находит NavInflater/NavHostFragment/Safe Args/Parcelize и Fragment-переходов. В APK отсутствуют удалённые Fragment-экраны, activity_main.xml и три старых XML-графа. `git diff --check` — успешно.
- Разрешённый Lifecycle остаётся 2.9.0, Compose UI — 1.9.0, Material3 — 1.3.2; явных обновлений версий не делалось. IDE Sync отдельно не проверялся; физическое устройство не использовалось. Прежние предупреждения native access JVM и strip debug symbols остались вне задачи.

Этап 5 завершён в пределах реализованных сценариев. Финальная эмуляторная проверка приложения и системных сценариев выполнена в [этапе 6](stage-6.md): Android 9/14/16, внешние камера и выбор изображения, завершение процесса и обновление с предыдущей debug APK. Ограничения физических устройств и опубликованной версии перечислены там же.
