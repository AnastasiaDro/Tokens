package com.cerebus.tokens.data.reinforcement

import androidx.datastore.core.CorruptionException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ReinforcementDataStoreTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun migrationPreservesRawFlagAndUriAcrossReopening() = runBlocking {
        val file = folder.newFolder().resolve("reinforcement.json")
        val firstJob = SupervisorJob()
        try {
            val store = reinforcementStore(file, { mapOf("ReinforcementShowing" to true, "PhotoUri" to PHOTO_URI) },
                CoroutineScope(firstJob + Dispatchers.IO))
            assertTrue(store.data.first().enabled)
            assertEquals(PHOTO_URI, store.data.first().photoUri)
            store.updateData { it.copy(enabled = false) }
        } finally { firstJob.cancelAndJoin() }
        val secondJob = SupervisorJob()
        try {
            val store = reinforcementStore(file, { error("Unexpected reimport") }, CoroutineScope(secondJob + Dispatchers.IO))
            assertFalse(store.data.first().enabled)
            assertEquals(PHOTO_URI, store.data.first().photoUri)
        } finally { secondJob.cancelAndJoin() }
    }

    @Test fun freshInstallationHasNoPhotoAndIsDisabled() = runBlocking {
        val job = SupervisorJob()
        try {
            val store = reinforcementStore(folder.newFolder().resolve("reinforcement.json"),
                { emptyMap<String, Any>() }, CoroutineScope(job + Dispatchers.IO))
            assertNull(store.data.first().photoUri)
            assertFalse(store.data.first().enabled)
        } finally { job.cancelAndJoin() }
    }

    @Test fun corruptFileIsNotSilentlyReset() = runBlocking {
        val file = folder.newFile()
        file.writeText("invalid")
        val job = SupervisorJob()
        try {
            val store = reinforcementStore(file, { emptyMap<String, Any>() }, CoroutineScope(job + Dispatchers.IO))
            try { store.data.first(); fail("Expected corruption") }
            catch (_: CorruptionException) { assertEquals("invalid", file.readText()) }
        } finally { job.cancelAndJoin() }
    }

    private companion object { const val PHOTO_URI = "content://test/preserved-photo" }
}
