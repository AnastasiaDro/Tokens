package data.tokens

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import data.persistence.*
import data.effects.DataStoreWinEffectsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DataStoreMigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun exactPositionsMigrateAndResizeSurvivesRestartWithoutReimport() = runBlocking {
        val name = "migration-test-" + UUID.randomUUID()
        val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        val file = File(context.cacheDir, "$name.json")
        val firstJob = SupervisorJob()
        val secondJob = SupervisorJob()
        try {
            assertTrue(prefs.edit().putInt("TokensNumber", BOARD_SIZE)
                .putInt("CheckedTokensNumber", CORRUPT_COUNT)
                .putStringSet("CheckedTokenIndices", setOf("1", "4", "-1", "invalid"))
                .putInt("CheckedTokensColor", COLOR).commit())
            val legacy = prefs.all
            val repo = DataStoreTokenBoardRepository(tokensStore(file, { prefs.all }, CoroutineScope(firstJob + Dispatchers.IO)))
            val original = repo.board.first()
            assertEquals(listOf(false, true, false, false, true), original.tokens.map { it.isChecked })
            assertEquals(COLOR, original.color)
            repo.resize(SHRUNK_SIZE)
            repo.resize(BOARD_SIZE)
            val saved = repo.board.first()
            assertEquals(listOf(false, true, false, false, false), saved.tokens.map { it.isChecked })
            assertEquals(legacy, prefs.all)
            firstJob.cancelAndJoin()
            val reopened = DataStoreTokenBoardRepository(tokensStore(file, { error("Do not reimport") },
                CoroutineScope(secondJob + Dispatchers.IO)))
            assertEquals(saved, reopened.board.first())
        } finally {
            firstJob.cancelAndJoin()
            secondJob.cancelAndJoin()
            file.delete()
            context.deleteSharedPreferences(name)
        }
    }

    @Test fun corruptLegacyCounterIsClampedWithoutChangingPreferences() = runBlocking {
        val name = "counter-test-" + UUID.randomUUID()
        val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        val file = File(context.cacheDir, "$name.json")
        val job = SupervisorJob()
        try {
            assertTrue(prefs.edit().putInt("TokensNumber", SHRUNK_SIZE).putInt("CheckedTokensNumber", CORRUPT_COUNT).commit())
            val repo = DataStoreTokenBoardRepository(tokensStore(file, { prefs.all }, CoroutineScope(job + Dispatchers.IO)))
            assertEquals(SHRUNK_SIZE, repo.board.first().count)
            assertTrue(repo.board.first().completed)
            assertEquals(CORRUPT_COUNT, prefs.getInt("CheckedTokensNumber", MISSING))
        } finally {
            job.cancelAndJoin()
            file.delete()
            context.deleteSharedPreferences(name)
        }
    }

    @Test fun effectsMigrateBothFlagsAndPersistIndependentUpdates() = runBlocking {
        val name = "effects-test-" + UUID.randomUUID()
        val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        val file = File(context.cacheDir, "$name.json")
        val firstJob = SupervisorJob()
        val secondJob = SupervisorJob()
        try {
            assertTrue(prefs.edit().putBoolean("SoundPlaying", false).putBoolean("AnimationShowing", true).commit())
            val repo = DataStoreWinEffectsRepository(effectsStore(file, { prefs.all }, CoroutineScope(firstJob + Dispatchers.IO)))
            assertFalse(repo.settings.first().sound)
            assertTrue(repo.settings.first().animation)
            coroutineScope {
                launch { repo.setSound(true) }
                launch { repo.setAnimation(false) }
            }
            val saved = repo.settings.first()
            firstJob.cancelAndJoin()
            val reopened = DataStoreWinEffectsRepository(effectsStore(file, { error("Do not reimport") },
                CoroutineScope(secondJob + Dispatchers.IO)))
            assertEquals(saved, reopened.settings.first())
            assertTrue(saved.sound)
            assertFalse(saved.animation)
        } finally {
            firstJob.cancelAndJoin()
            secondJob.cancelAndJoin()
            file.delete()
            context.deleteSharedPreferences(name)
        }
    }

    private companion object {
        const val BOARD_SIZE = 5
        const val SHRUNK_SIZE = 2
        const val CORRUPT_COUNT = 99
        const val COLOR = -65536
        const val MISSING = -1
    }
}

