package data.reinforcement

import com.cerebus.tokens.data.reinforcement.storage.ReinforcementStorage
import domain.repository.ReinforcementSettingsRepository

class ReinforcementRepositoryImpl(private val reinforcementStorage: ReinforcementStorage) : ReinforcementSettingsRepository {
    override fun getIsReinforcementShown() = reinforcementStorage.isReinforcementShow()

    override fun setIsReinforcementShown(isShow: Boolean) {
        reinforcementStorage.setReinforcementShow(isShow)
    }

    override fun getReinforcementPhotoPathString() = reinforcementStorage.getPhotoUri()
}
