# API жетонов и настроек

- Контракт входа — TokensMediator и TokensEntry. Граф подключает app, идентификаторы ресурсов остаются в impl.
- Не добавлять зависимости на impl, DI, ViewModel или DTO хранения.
- BOARD и SETTINGS — существующие экраны одной фичи. Не разделять их на дополнительные модули без отдельной задачи.
- Типы Navigation допустимы до согласованного перехода на Compose Navigation.
- Проверка контракта: `:feature:tokens_feature:api:compileDebugKotlin` и компиляция потребителей.
