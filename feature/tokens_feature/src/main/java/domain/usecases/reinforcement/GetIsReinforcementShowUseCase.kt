package domain.usecases.reinforcement

import domain.repository.ReinforcementRepository

class GetIsReinforcementShowUseCase(private val reinforcementRepository: ReinforcementRepository) {

    fun execute() = reinforcementRepository.getIsReinforcementShown()
}