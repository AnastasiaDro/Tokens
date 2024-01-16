package com.cerebus.tokens.data.reinforcement.storage

interface ReinforcementStorage {

    fun isReinforcementShow(): Boolean

    fun setReinforcementShow(isShow: Boolean)

    fun getPhotoUri(): String?

    fun savePhotoUri(uriString: String)
}