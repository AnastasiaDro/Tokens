package com.cerebus.tokens.data.reinforcement

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ReinforcementMigrationTest {
    @Test fun rawFlagAndPhotoSurviveMigrationConcurrentWritesAndRestart() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "reinforcement-test-" + UUID.randomUUID()
        val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        val file = File(context.cacheDir, "$name.json")
        val firstJob = SupervisorJob()
        val secondJob = SupervisorJob()
        try {
            assertTrue(prefs.edit().putBoolean("ReinforcementShowing", true).putString("PhotoUri", PHOTO_URI).commit())
            val legacy = prefs.all
            val repository = DataStoreReinforcementRepository(reinforcementStore(file, { prefs.all }, CoroutineScope(firstJob + Dispatchers.IO)))
            assertEquals(ReinforcementSettings(true, PHOTO_URI), repository.settings.first())
            coroutineScope {
                launch { repository.setEnabled(false) }
                launch { repository.setPhotoUri(NEXT_PHOTO) }
            }
            firstJob.cancelAndJoin()
            val reopened = DataStoreReinforcementRepository(reinforcementStore(file, { error("Do not reimport") },
                CoroutineScope(secondJob + Dispatchers.IO)))
            assertEquals(ReinforcementSettings(false, NEXT_PHOTO), reopened.settings.first())
            assertEquals(legacy, prefs.all)
        } finally {
            firstJob.cancelAndJoin()
            secondJob.cancelAndJoin()
            file.delete()
            context.deleteSharedPreferences(name)
        }
    }

    private companion object {
        const val PHOTO_URI = "content://test/original"
        const val NEXT_PHOTO = "content://test/next"
    }
}

