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
- Оба кандидата проверялись вне приложения и пока НЕ добавлены в его зависимости. Компиляция не доказывает корректность поведения picker/анимаций на устройстве и не является аудитом безопасности. Начальное состояние, рекомпозиции, восстановление и анимационные ресурсы проверять при соответствующей интеграции.

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

## Следующие итерации — пока не реализованы

1. **4.3. Настройки и количество.** Route подключает ViewModel, Screen получает состояние/callbacks. Диалог имеет восстанавливаемый черновик, пишет только по подтверждению и закрывается только после успеха. Пока максимум 10.
2. **4.4. Цвет.** colorpicker-compose, восстанавливаемый черновик без сброса при рекомпозиции, ошибки и повтор сохранения. Старую View-зависимость удалять после проверки потребителей.
3. **4.5. Поле и эффекты.** Перенести экран, сохранив меню, жест очистки, фото и ошибки. Звук/анимация привязаны к celebrationId и lifecycle, не к render; единственный таймер остаётся во ViewModel. Проверить повороты и уход/возврат. Пока максимум 10.
4. **4.6. Адаптивность и 20.** Одновременно изменить UI, рабочие ограничения и валидацию сохранений. Историческую нормализацию SharedPreferences не расширять автоматически. Снять ограничение landscape только после проверки всех доступных экранов, включая старое фото.
5. **4.7. Очистка.** Удалить заменённый UI и зависимости без потребителей, обновить правила, провести финальные JVM/instrumentation/debug-проверки.

### Обязательное уточнение раскладки от пользователя

Все жетоны должны одновременно помещаться на экране, БЕЗ прокрутки поля. Размер всех жетонов одинаковый и рассчитывается по доступным ширине И высоте с учётом меню, системных отступов и подкрепления. Сначала сокращать отступы и область фото; изображение не должно вытеснять жетоны. Цели при 20 жетонах — четыре ряда по пять на телефоне и два по десять в широком окне планшета. В тесном окне разрешено адаптировать число рядов: видимость всех жетонов приоритетнее фиксированной сетки. Проверить обе ориентации, маленькое/разделённое окно и увеличенный шрифт; не обещать минимальный touch target в окне, где физически нет места.

После каждой важной итерации — релевантные тесты, debug-компиляция и отдельный стабильный коммит. Этот документ не разрешает начинать следующую итерацию без запроса пользователя.
