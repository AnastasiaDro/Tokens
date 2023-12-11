package domain.repository

interface ReinforcementSettingsRepository {

    fun getIsReinforcementShown(): Boolean

    fun setIsReinforcementShown(isShow: Boolean)
}