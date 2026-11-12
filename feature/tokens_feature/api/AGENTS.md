# API жетонов и настроек

- Контракт входа — TokensMediator.registerGraph/open и TokensEntry. Публичные @Serializable-маршруты TokensGraph, TokensBoard, TokensSettings нужны для типизированного подключения/старта; количество и цвет остаются внутренними destinations impl. Граф подключает app.
- Не добавлять зависимости на impl, DI, ViewModel или DTO хранения.
- BOARD и SETTINGS — существующие экраны одной фичи. Не разделять их на дополнительные модули без отдельной задачи.
- Используется Compose Navigation: NavGraphBuilder/NavHostController при регистрации и NavController при открытии. Не возвращать NavInflater, XML ID или Safe Args.
- Проверка контракта: `:feature:tokens_feature:api:compileDebugKotlin` и компиляция потребителей.
