package data.tokens

import data.persistence.tokensStore
import domain.repository.TokenBoardRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreTokenBoardRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    private fun testBoard(legacy: Map<String, *> = emptyMap<String, Any>(), block: suspend (TokenBoardRepository) -> Unit) = runBlocking {
        val job = SupervisorJob()
        try {
            val repository = DataStoreTokenBoardRepository(tokensStore(folder.newFolder().resolve("tokens.json"),
                { legacy }, CoroutineScope(job + Dispatchers.IO)))
            block(repository)
        } finally { job.cancelAndJoin() }
    }

    @Test fun concurrentTogglesUseSavedStateAndKeepColor() = testBoard {
        val original = it.board.first()
        coroutineScope {
            repeat(EVEN_CLICKS) { _ -> launch { it.toggle(original.tokens.first().id) } }
            launch { it.setColor(COLOR) }
        }
        val result = it.board.first()
        assertFalse(result.tokens.first().isChecked)
        assertEquals(COLOR, result.color)
        assertEquals(original.tokens.map { token -> token.id }, result.tokens.map { token -> token.id })
    }

    @Test fun resizePreservesPositionsAndSurvivingIds() = testBoard(mapOf(
        "TokensNumber" to ORIGINAL_SIZE, "CheckedTokenIndices" to setOf("1", "6", "8"),
    )) {
        val original = it.board.first()
        it.resize(SHRUNK_SIZE)
        val shrunk = it.board.first()
        assertEquals(listOf(true, true, true, false), shrunk.tokens.map { token -> token.isChecked })
        assertEquals(original.tokens.take(SHRUNK_SIZE).map { token -> token.id }, shrunk.tokens.map { token -> token.id })
        it.resize(ORIGINAL_SIZE)
        val expanded = it.board.first()
        assertEquals(shrunk.tokens, expanded.tokens.take(SHRUNK_SIZE))
        assertTrue(expanded.tokens.drop(SHRUNK_SIZE).none { token -> token.isChecked || token.id in original.tokens.map { old -> old.id } })
        assertFalse(it.toggle(original.tokens.last().id).changed)
    }

    @Test fun completedAndPartialBoardsShrinkDifferently() = testBoard {
        val original = it.board.first()
        original.tokens.dropLast(SINGLE_TOKEN).forEach { token -> it.setChecked(token.id, true) }
        it.resize(SINGLE_TOKEN)
        assertFalse(it.board.first().completed)
        assertTrue(it.toggle(it.board.first().tokens.single().id).completedByUser)
        it.resize(SHRUNK_SIZE)
        it.board.first().tokens.forEach { token -> it.setChecked(token.id, true) }
        it.resize(SINGLE_TOKEN)
        assertTrue(it.board.first().completed)
    }

    @Test fun repeatMarksAndNoOpResizeDoNotChangeRevisionOrRepeatWin() = testBoard(mapOf("TokensNumber" to SINGLE_TOKEN)) {
        val id = it.board.first().tokens.single().id
        assertTrue(it.setChecked(id, true).completedByUser)
        val saved = it.board.first()
        assertFalse(it.setChecked(id, true).changed)
        assertFalse(it.setChecked(id, true).completedByUser)
        it.resize(SINGLE_TOKEN)
        assertEquals(saved, it.board.first())
        it.clear()
        assertFalse(it.board.first().completed)
        assertTrue(saved.completed)
    }

    @Test fun lateSubscribersSeeSameCommittedBoard() = testBoard {
        val id = it.board.first().tokens.last().id
        it.toggle(id)
        coroutineScope {
            val first = async { it.board.first() }
            val second = async { it.board.first() }
            assertEquals(first.await(), second.await())
            assertTrue(first.await().tokens.last().isChecked)
        }
    }

    @Test fun invalidResizeLeavesBoardIntact() = testBoard {
        val original = it.board.first()
        try { it.resize(11); fail("Invalid size must fail") } catch (_: IllegalArgumentException) { }
        assertEquals(original, it.board.first())
    }

    private companion object {
        const val EVEN_CLICKS = 100
        const val COLOR = -65536
        const val ORIGINAL_SIZE = 9
        const val SHRUNK_SIZE = 4
        const val SINGLE_TOKEN = 1
    }
}
