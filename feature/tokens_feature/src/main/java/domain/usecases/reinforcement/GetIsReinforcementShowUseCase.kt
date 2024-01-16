package domain.usecases.reinforcement

import domain.repository.ReinforcementSettingsRepository

class GetIsReinforcementShowUseCase(private val reinforcementSettingsRepository: ReinforcementSettingsRepository) {

    fun execute() = reinforcementSettingsRepository.getIsReinforcementShown()
}