package domain.repository

interface ReinforcementRepository {

    fun getIsReinforcementShown(): Boolean

    fun setIsReinforcementShown(isShow: Boolean)
}