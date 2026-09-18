package data.tokens

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cerebus.tokens.logger.impl.LoggerFactoryImpl
import data.tokens.storage.TokensStorageImpl
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TokensStorageTest {
    private val context = object : ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
        override fun getSharedPreferences(name: String, mode: Int) =
            super.getSharedPreferences("TokensStorageTest_$name", mode)
    }
    private val prefs get() = context.getSharedPreferences("TokensPreferences", Context.MODE_PRIVATE)
    private fun storage() = TokensStorageImpl(context, LoggerFactoryImpl())

    @Before fun setUp() { prefs.edit().clear().commit() }
    @After fun tearDown() { prefs.edit().clear().commit() }

    @Test fun migratesCorruptLegacyProgressAndPreservesColor() {
        prefs.edit().putInt("TokensNumber", SMALL_BOARD_SIZE).putInt("CheckedTokensNumber", CORRUPT_CHECKED_COUNT)
            .putInt("CheckedTokensColor", CUSTOM_COLOR).commit()
        val repository = TokensRepositoryImpl(storage())
        assertEquals(SMALL_BOARD_SIZE, repository.getTokensNumber())
        assertEquals(SMALL_BOARD_SIZE, repository.getCheckedTokensNumber())
        assertEquals(CUSTOM_COLOR, repository.getCheckedColor())
        assertEquals(setOf(FIRST_TOKEN_INDEX.toString(), SECOND_TOKEN_INDEX.toString(), THIRD_TOKEN_INDEX.toString()),
            prefs.getStringSet("CheckedTokenIndices", null))
        assertEquals(SMALL_BOARD_SIZE, prefs.getInt("CheckedTokensNumber", MISSING_PREFERENCE))
    }

    @Test fun exactPositionsSurviveRepositoryRecreationAndResize() {
        val repository = TokensRepositoryImpl(storage())
        repository.checkToken(SECOND_TOKEN_INDEX)
        repository.checkToken(LAST_TOKEN_INDEX)
        assertEquals(listOf(false, true, false, false, true),
            TokensRepositoryImpl(storage()).getTokensList().map { it.isChecked })
        repository.resizeTokens(SHRUNK_BOARD_SIZE)
        repository.resizeTokens(DEFAULT_BOARD_SIZE)
        val restored = TokensRepositoryImpl(storage())
        assertEquals(listOf(false, true, false, false, false), restored.getTokensList().map { it.isChecked })
        assertEquals(DEFAULT_BOARD_SIZE, prefs.getInt("TokensNumber", MISSING_PREFERENCE))
        assertEquals(SINGLE_CHECKED_TOKEN, prefs.getInt("CheckedTokensNumber", MISSING_PREFERENCE))
    }

    @Test fun normalizesInvalidPersistedPositions() {
        prefs.edit().putInt("TokensNumber", SMALL_BOARD_SIZE).putInt("CheckedTokensNumber", CORRUPT_CHECKED_COUNT)
            .putStringSet("CheckedTokenIndices", setOf(NEGATIVE_TOKEN_INDEX.toString(), SECOND_TOKEN_INDEX.toString(),
                OUT_OF_RANGE_TOKEN_INDEX.toString(), "invalid")).commit()
        val restored = TokensRepositoryImpl(storage())
        assertEquals(listOf(false, true, false), restored.getTokensList().map { it.isChecked })
        assertEquals(setOf(SECOND_TOKEN_INDEX.toString()), prefs.getStringSet("CheckedTokenIndices", null))
        assertEquals(SINGLE_CHECKED_TOKEN, prefs.getInt("CheckedTokensNumber", MISSING_PREFERENCE))
    }

    private companion object {
        const val SMALL_BOARD_SIZE = 3
        const val DEFAULT_BOARD_SIZE = 5
        const val SHRUNK_BOARD_SIZE = 2
        const val CORRUPT_CHECKED_COUNT = 9
        const val SINGLE_CHECKED_TOKEN = 1
        const val CUSTOM_COLOR = -65536
        const val MISSING_PREFERENCE = -1
        const val FIRST_TOKEN_INDEX = 0
        const val SECOND_TOKEN_INDEX = 1
        const val THIRD_TOKEN_INDEX = 2
        const val LAST_TOKEN_INDEX = 4
        const val NEGATIVE_TOKEN_INDEX = -1
        const val OUT_OF_RANGE_TOKEN_INDEX = 9
    }
}
