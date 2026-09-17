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
        prefs.edit().putInt("TokensNumber", 3).putInt("CheckedTokensNumber", 9)
            .putInt("CheckedTokensColor", -65536).commit()
        val repository = TokensRepositoryImpl(storage())
        assertEquals(3, repository.getTokensNumber())
        assertEquals(3, repository.getCheckedTokensNumber())
        assertEquals(-65536, repository.getCheckedColor())
        assertEquals(setOf("0", "1", "2"), prefs.getStringSet("CheckedTokenIndices", null))
        assertEquals(3, prefs.getInt("CheckedTokensNumber", -1))
    }

    @Test fun exactPositionsSurviveRepositoryRecreationAndResize() {
        val repository = TokensRepositoryImpl(storage())
        repository.checkToken(1)
        repository.checkToken(4)
        assertEquals(listOf(false, true, false, false, true),
            TokensRepositoryImpl(storage()).getTokensList().map { it.isChecked })
        repository.resizeTokens(2)
        repository.resizeTokens(5)
        val restored = TokensRepositoryImpl(storage())
        assertEquals(listOf(false, true, false, false, false), restored.getTokensList().map { it.isChecked })
        assertEquals(5, prefs.getInt("TokensNumber", -1))
        assertEquals(1, prefs.getInt("CheckedTokensNumber", -1))
    }

    @Test fun normalizesInvalidPersistedPositions() {
        prefs.edit().putInt("TokensNumber", 3).putInt("CheckedTokensNumber", 9)
            .putStringSet("CheckedTokenIndices", setOf("-1", "1", "9", "invalid")).commit()
        val restored = TokensRepositoryImpl(storage())
        assertEquals(listOf(false, true, false), restored.getTokensList().map { it.isChecked })
        assertEquals(setOf("1"), prefs.getStringSet("CheckedTokenIndices", null))
        assertEquals(1, prefs.getInt("CheckedTokensNumber", -1))
    }
}
