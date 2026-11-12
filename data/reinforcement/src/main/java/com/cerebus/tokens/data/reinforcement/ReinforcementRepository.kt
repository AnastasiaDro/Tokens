package com.cerebus.tokens.data.reinforcement

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

data class ReinforcementSettings(val enabled: Boolean = false, val photoUri: String? = null)

interface ReinforcementRepository {
    val settings: Flow<ReinforcementSettings>
    suspend fun setEnabled(enabled: Boolean)
    suspend fun setPhotoUri(uri: String)
}

/** One application-scoped instance, supplied by DI. Camera availability is a UI concern. */
class DataStoreReinforcementRepository internal constructor(
    private val store: DataStore<ReinforcementDocument>,
) : ReinforcementRepository {
    constructor(context: Context) : this(reinforcementStore(
        context.applicationContext.dataStoreFile("reinforcement.json"),
        { context.applicationContext.getSharedPreferences("ReinforcementPreferences", Context.MODE_PRIVATE).all },
        CoroutineScope(SupervisorJob() + Dispatchers.IO),
    ))

    override val settings = store.data.map { ReinforcementSettings(it.enabled, it.photoUri) }.distinctUntilChanged()
    override suspend fun setEnabled(enabled: Boolean) { store.updateData { it.copy(enabled = enabled) } }
    override suspend fun setPhotoUri(uri: String) { store.updateData { it.copy(photoUri = uri) } }
}

