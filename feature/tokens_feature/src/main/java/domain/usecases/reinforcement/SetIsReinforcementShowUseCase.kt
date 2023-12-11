package domain.usecases.reinforcement

import domain.repository.ReinforcementSettingsRepository

class SetIsReinforcementShowUseCase(private val reinforcementSettingsRepository: ReinforcementSettingsRepository) {

    fun execute(isShown: Boolean) = reinforcementSettingsRepository.setIsReinforcementShown(isShown)
}