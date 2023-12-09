package domain.usecases.reinforcement

import domain.repository.ReinforcementRepository

class SetIsReinforcementShowUseCase(private val reinforcementRepository: ReinforcementRepository) {

    fun execute(isShown: Boolean) = reinforcementRepository.setIsReinforcementShown(isShown)
}