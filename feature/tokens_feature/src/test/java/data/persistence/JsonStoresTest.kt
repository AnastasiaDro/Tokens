package data.persistence

import androidx.datastore.core.CorruptionException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class JsonStoresTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun legacyPositionsOverrideCounterAndKeepColor() = runBlocking {
        val job = SupervisorJob()
        try {
            val store = tokensStore(folder.newFolder().resolve("tokens.json"), {
                mapOf("TokensNumber" to BOARD_SIZE, "CheckedTokensNumber" to CORRUPT_COUNT,
                    "CheckedTokenIndices" to setOf("1", "-1", "99", "bad"), "CheckedTokensColor" to CUSTOM_COLOR)
            }, CoroutineScope(job + Dispatchers.IO))
            val result = store.data.first()
            assertEquals(listOf(false, true, false), result.tokens.map { it.checked })
            assertEquals(CUSTOM_COLOR, result.color)
            assertTrue(result.migrated)
        } finally { job.cancelAndJoin() }
    }

    @Test fun legacyCounterIsClamped() = runBlocking {
        val job = SupervisorJob()
        try {
            val store = tokensStore(folder.newFolder().resolve("tokens.json"),
                { mapOf("TokensNumber" to BOARD_SIZE, "CheckedTokensNumber" to CORRUPT_COUNT) },
                CoroutineScope(job + Dispatchers.IO))
            assertTrue(store.data.first().tokens.all { it.checked })
        } finally { job.cancelAndJoin() }
    }

    @Test fun reopeningPreservesIdsAndDoesNotReimportLegacy() = runBlocking {
        val file = folder.newFolder().resolve("tokens.json")
        val firstJob = SupervisorJob()
        val store = tokensStore(file, { emptyMap<String, Any>() }, CoroutineScope(firstJob + Dispatchers.IO))
        val written = try {
            store.updateData { it.copy(color = CUSTOM_COLOR) }
        } finally { firstJob.cancelAndJoin() }
        val secondJob = SupervisorJob()
        try {
            val reopened = tokensStore(file, { error("Must not read legacy again") }, CoroutineScope(secondJob + Dispatchers.IO))
            assertEquals(written, reopened.data.first())
        } finally { secondJob.cancelAndJoin() }
    }

    @Test fun effectsMigrateAndPersistIndependently() = runBlocking {
        val job = SupervisorJob()
        try {
            val store = effectsStore(folder.newFolder().resolve("effects.json"),
                { mapOf("SoundPlaying" to false, "AnimationShowing" to true) }, CoroutineScope(job + Dispatchers.IO))
            assertFalse(store.data.first().sound)
            assertTrue(store.data.first().animation)
            store.updateData { it.copy(animation = false) }
            assertFalse(store.data.first().animation)
        } finally { job.cancelAndJoin() }
    }

    @Test fun malformedAndFutureDocumentsAreNotReplaced() = runBlocking {
        val future = Json.encodeToString(TokensDocument.serializer(), TokensDocument(version = 999))
        for (content in listOf("{broken", "{}", future)) {
            val file = folder.newFile()
            file.writeText(content)
            val job = SupervisorJob()
            try {
                val store = tokensStore(file, { emptyMap<String, Any>() }, CoroutineScope(job + Dispatchers.IO))
                try {
                    store.data.first()
                    fail("Corrupt document must fail")
                } catch (_: CorruptionException) {
                    assertEquals(content, file.readText())
                }
            } finally { job.cancelAndJoin() }
        }
    }

    @Test fun cancelledMigrationLeavesLegacyUntouchedAndCanRetry() = runBlocking {
        val file = folder.newFolder().resolve("tokens.json")
        val legacy = mapOf("TokensNumber" to BOARD_SIZE)
        val failedJob = SupervisorJob()
        try {
            val store = tokensStore(file, { throw java.io.IOException("Unavailable") }, CoroutineScope(failedJob + Dispatchers.IO))
            try { store.data.first(); fail("Expected failure") } catch (_: java.io.IOException) { }
        } finally { failedJob.cancelAndJoin() }
        val retryJob = SupervisorJob()
        try {
            val store = tokensStore(file, { legacy }, CoroutineScope(retryJob + Dispatchers.IO))
            assertEquals(BOARD_SIZE, store.data.first().tokens.size)
        } finally { retryJob.cancelAndJoin() }
    }

    private companion object {
        const val BOARD_SIZE = 3
        const val CORRUPT_COUNT = 9
        const val CUSTOM_COLOR = -65536
    }
}
