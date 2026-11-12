# API фото подкрепления

- Здесь контракт ReinforcementPhotoMediator.registerGraph/open и публичный @Serializable PhotoDestination для подключения и открытия выбора фото.
- Не добавлять зависимости на impl, DI, ViewModel, ресурсы реализации или хранилище.
- Используется Compose Navigation: NavGraphBuilder/NavHostController при регистрации и NavController при открытии. Не возвращать NavInflater, XML ID или Safe Args.
- Результат выбора наблюдается через общий ReinforcementRepository; не добавлять дублирующий UiCache или nav-result.
- Проверка контракта: `:feature:reinforcement_photo:api:compileDebugKotlin` и компиляция потребителей.
