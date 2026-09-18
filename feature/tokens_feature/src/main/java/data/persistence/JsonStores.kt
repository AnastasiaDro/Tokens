package data.persistence

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

internal class DocumentSerializer<T>(
    private val codec: KSerializer<T>,
    private val defaults: () -> T,
    private val validate: (T) -> Boolean,
) : Serializer<T> {
    override val defaultValue: T get() = defaults()
    override suspend fun readFrom(input: InputStream): T {
        val value = try {
            json.decodeFromString(codec, input.readBytes().decodeToString())
        } catch (error: SerializationException) {
            throw CorruptionException("Invalid saved document", error)
        }
        if (!validate(value)) throw CorruptionException("Unsupported or invalid saved document")
        return value
    }
    override suspend fun writeTo(t: T, output: OutputStream) {
        check(validate(t)) { "Invalid document" }
        output.write(json.encodeToString(codec, t).encodeToByteArray())
    }
}

internal fun tokensStore(file: File, legacy: () -> Map<String, *>, scope: CoroutineScope): DataStore<TokensDocument> =
    DataStoreFactory.create(
        serializer = DocumentSerializer(TokensDocument.serializer(), { TokensDocument() }) {
            it.version == FORMAT_VERSION && it.tokens.size in MIN_TOKENS..MAX_TOKENS &&
                it.tokens.all { token -> token.id.isNotBlank() } &&
                it.tokens.map { token -> token.id }.distinct().size == it.tokens.size &&
                it.revision >= INITIAL_REVISION
        },
        migrations = listOf(object : DataMigration<TokensDocument> {
            override suspend fun shouldMigrate(currentData: TokensDocument) = !currentData.migrated
            override suspend fun migrate(currentData: TokensDocument) = migrateTokens(currentData, legacy())
            override suspend fun cleanUp() = Unit
        }),
        scope = scope,
        produceFile = { file },
    )

internal fun effectsStore(file: File, legacy: () -> Map<String, *>, scope: CoroutineScope): DataStore<EffectsDocument> =
    DataStoreFactory.create(
        serializer = DocumentSerializer(EffectsDocument.serializer(), { EffectsDocument() }) { it.version == FORMAT_VERSION },
        migrations = listOf(object : DataMigration<EffectsDocument> {
            override suspend fun shouldMigrate(currentData: EffectsDocument) = !currentData.migrated
            override suspend fun migrate(currentData: EffectsDocument) = migrateEffects(currentData, legacy())
            override suspend fun cleanUp() = Unit
        }),
        scope = scope,
        produceFile = { file },
    )
