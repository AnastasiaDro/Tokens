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

Следующая итерация — 5.2: Compose-фото и общий асинхронный preview. Compose Navigation (5.3) пока не реализована.
