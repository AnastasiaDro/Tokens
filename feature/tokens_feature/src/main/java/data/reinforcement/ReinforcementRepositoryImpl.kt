package data.reinforcement

import data.reinforcement.storage.ReinforcementStorage
import domain.repository.ReinforcementRepository

class ReinforcementRepositoryImpl(private val reinforcementStorage: ReinforcementStorage) : ReinforcementRepository {
    override fun getIsReinforcementShown() = reinforcementStorage.isReinforcementShow()

    override fun setIsReinforcementShown(isShow: Boolean) {
        reinforcementStorage.setReinforcementShow(isShow)
    }
}