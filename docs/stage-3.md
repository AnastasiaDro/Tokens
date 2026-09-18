# Этап 3: feature api/impl и mediators

## Границы модулей

- `feature:tokens_feature:api` — TokensMediator и TokensEntry (BOARD, SETTINGS).
- `feature:tokens_feature:impl` — жетоны, настройки, диалоги количества/цвета, состояния, репозитории tokens/effects, эффекты победы, XML и Koin-модуль.
- `feature:reinforcement_photo:api` — ReinforcementPhotoMediator.
- `feature:reinforcement_photo:impl` — выбор/съёмка фото, PhotoUiState, ViewModel, XML и Koin-модуль.
- `data:reinforcement` — существующий общий контракт Flow и DataStore-реализация. Единственный singleton создаётся в DI приложения; дополнительного UiCache нет.
- `core:logger` и `core:ui` остаются прежними Gradle-модулями. В logger api/impl пока являются пакетами, а не отдельными модулями.

Настройки и жетоны остаются одной фичей с двумя экранами. Их repository-контракты не экспортируются в feature-api: внешним потребителям они сейчас не нужны.

## Зависимости и навигация

App подключает api для навигационных контрактов и impl для композиции Koin.
Tokens impl зависит от photo api, но не от photo impl. Оба api содержат только контракты на существующих типах Android Navigation, без DI, ViewModel и DTO хранения.

Каждый mediator создаёт собственный NavGraph через createGraph(NavInflater). MainActivity добавляет оба графа в корневой граф и задаёт граф жетонов стартовым. Повторная сборка графа после пересоздания Activity позволяет NavController восстановить стек.

ReinforcementPhotoMediator.open открывает диалог; сохранённый результат получают подписчики ReinforcementRepository. TokensMediator.open принимает TokensEntry. SETTINGS разрешается через внутренний deep link, поскольку прямой destination ID не находится из соседнего графа фото. Строка маршрута не выходит за границы tokens impl.

Fragment/XML, Safe Args для количества, имена классов, namespace, ID существующих графов и формат трёх JSON-файлов сохранены. Перенос исходников и тестов выполнен физически в impl, старых Android-модулей по прежним путям нет.

## Проверки 2026-09-18

- `./gradlew testDebugUnitTest :app:compileDebugKotlin :app:assembleDebug` — успешно, 46 JVM-тестов во всех модулях.
- `:app:connectedDebugAndroidTest` с фильтром `com.cerebus.tokens.FeatureNavigationTest` — 3/3 на API 36: фото и возврат, восстановление фото/настроек после пересоздания Activity, оба входа в жетоны из графа фото.
- `:feature:tokens_feature:impl:connectedDebugAndroidTest` с фильтром `data.tokens.DataStoreMigrationTest` — 3/3 на API 36: миграция, ID, пересчёт и повторное открытие сохранений, настройки эффектов.
- Проверено отсутствие зависимостей feature-api на impl и обращений app к ViewModel/репозиториям жетонов.
- `git diff --check` — успешно. IDE Sync отдельно не проверялся; instrumentation проверяет Activity recreation, не полноценное уничтожение процесса.

## Следующий этап

Compose, colorpicker-compose, адаптивное поле на 20 жетонов, переработка permissions/камеры и Compose Navigation выполняются отдельно. Версии библиотек и метаданные приложения этим этапом не менялись.
