package domain.usecases.reinforcement

import domain.repository.ReinforcementSettingsRepository

class GetReinforcementUriStringUseCase(private val reinforcementSettingsRepository: ReinforcementSettingsRepository) {

    fun execute() = reinforcementSettingsRepository.getReinforcementPhotoPathString()
}