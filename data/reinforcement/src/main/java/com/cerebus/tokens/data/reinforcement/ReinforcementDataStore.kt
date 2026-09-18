package com.cerebus.tokens.data.reinforcement

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.Required
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream

private const val FORMAT_VERSION = 1
private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

@Serializable
internal data class ReinforcementDocument(
    @Required val version: Int = FORMAT_VERSION,
    @Required val migrated: Boolean = false,
    @Required val enabled: Boolean = false,
    @Required val photoUri: String? = null,
)

internal object ReinforcementSerializer : Serializer<ReinforcementDocument> {
    override val defaultValue = ReinforcementDocument()
    override suspend fun readFrom(input: InputStream): ReinforcementDocument {
        val value = try {
            json.decodeFromString<ReinforcementDocument>(input.readBytes().decodeToString())
        } catch (error: SerializationException) {
            throw CorruptionException("Invalid reinforcement document", error)
        }
        if (value.version != FORMAT_VERSION) throw CorruptionException("Unsupported reinforcement format")
        return value
    }
    override suspend fun writeTo(t: ReinforcementDocument, output: OutputStream) {
        check(t.version == FORMAT_VERSION)
        output.write(json.encodeToString(ReinforcementDocument.serializer(), t).encodeToByteArray())
    }
}

internal fun reinforcementStore(file: File, legacy: () -> Map<String, *>, scope: CoroutineScope): DataStore<ReinforcementDocument> =
    DataStoreFactory.create(
        serializer = ReinforcementSerializer,
        migrations = listOf(object : DataMigration<ReinforcementDocument> {
            override suspend fun shouldMigrate(currentData: ReinforcementDocument) = !currentData.migrated
            override suspend fun migrate(currentData: ReinforcementDocument): ReinforcementDocument {
                val values = legacy()
                return currentData.copy(migrated = true,
                    enabled = values["ReinforcementShowing"] as? Boolean ?: false,
                    photoUri = values["PhotoUri"] as? String)
            }
            override suspend fun cleanUp() = Unit
        }),
        scope = scope,
        produceFile = { file },
    )
