package data.effects

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStoreFile
import data.persistence.EffectsDocument
import data.persistence.effectsStore
import domain.repository.EffectsSettings
import domain.repository.WinEffectsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class DataStoreWinEffectsRepository internal constructor(
    private val store: DataStore<EffectsDocument>,
) : WinEffectsRepository {
    constructor(context: Context) : this(effectsStore(
        context.applicationContext.dataStoreFile("effects.json"),
        { context.applicationContext.getSharedPreferences("WinEffectsPreferences", Context.MODE_PRIVATE).all },
        CoroutineScope(SupervisorJob() + Dispatchers.IO),
    ))

    override val settings = store.data.map { EffectsSettings(it.animation, it.sound) }.distinctUntilChanged()
    override suspend fun setAnimation(enabled: Boolean) { store.updateData { it.copy(animation = enabled) } }
    override suspend fun setSound(enabled: Boolean) { store.updateData { it.copy(sound = enabled) } }
}

