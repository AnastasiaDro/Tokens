# API фото подкрепления

- Здесь только контракт ReinforcementPhotoMediator для подключения графа и открытия выбора фото.
- Не добавлять зависимости на impl, DI, ViewModel, ресурсы реализации или хранилище.
- Типы Navigation допустимы до согласованного перехода на Compose Navigation.
- Результат выбора наблюдается через общий ReinforcementRepository; не добавлять дублирующий UiCache или nav-result.
- Проверка контракта: `:feature:reinforcement_photo:api:compileDebugKotlin` и компиляция потребителей.
