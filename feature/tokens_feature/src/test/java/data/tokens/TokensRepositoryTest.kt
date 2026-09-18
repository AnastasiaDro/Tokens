package data.tokens

import domain.usecases.tokens.ChangeTokensNumberUseCase
import domain.usecases.tokens.CheckTokensAreGrappedUseCase
import data.tokens.FakeTokensStorage.Companion.DEFAULT_TOKENS_NUMBER
import data.tokens.FakeTokensStorage.Companion.NO_CHECKED_TOKENS
import org.junit.Assert.*
import org.junit.Test

class TokensRepositoryTest {
    @Test fun `resize follows agreed partial and completed board rules`() {
        val cases = listOf(
            Triple(MAX_BOARD_SIZE, HALF_MAX_PROGRESS, SMALL_BOARD_SIZE) to PAIR_CHECKED_TOKENS,
            Triple(MAX_BOARD_SIZE, NEARLY_COMPLETE_MAX_PROGRESS, MEDIUM_BOARD_SIZE) to PARTIAL_PROGRESS,
            Triple(LARGE_BOARD_SIZE, PARTIAL_PROGRESS, MEDIUM_BOARD_SIZE) to PARTIAL_PROGRESS,
            Triple(MAX_BOARD_SIZE, MAX_BOARD_SIZE, MEDIUM_BOARD_SIZE) to MEDIUM_BOARD_SIZE,
            Triple(LARGE_BOARD_SIZE, PARTIAL_PROGRESS, MIN_BOARD_SIZE) to NO_CHECKED_TOKENS,
            Triple(LARGE_BOARD_SIZE, LARGE_BOARD_SIZE, MIN_BOARD_SIZE) to MIN_BOARD_SIZE,
        )
        for ((input, expected) in cases) {
            val storage = FakeTokensStorage(input.first, input.second)
            val repository = TokensRepositoryImpl(storage)
            ChangeTokensNumberUseCase(repository).execute(input.third)
            assertEquals("$input", expected, repository.getCheckedTokensNumber())
            assertEquals(expected, storage.checkedCount)
            assertEquals(input.third, storage.count)
            assertEquals(input.third, repository.getTokensList().size)
        }
    }

    @Test fun `shrink then grow does not produce a false win`() {
        val storage = FakeTokensStorage(DEFAULT_TOKENS_NUMBER, NEARLY_COMPLETE_DEFAULT_PROGRESS)
        val repository = TokensRepositoryImpl(storage)
        val resize = ChangeTokensNumberUseCase(repository)
        resize.execute(PAIR_BOARD_SIZE)
        assertEquals(SINGLE_CHECKED_TOKEN, repository.getCheckedTokensNumber())
        resize.execute(DEFAULT_TOKENS_NUMBER)
        repository.checkToken(THIRD_TOKEN_INDEX)
        assertEquals(PAIR_CHECKED_TOKENS, repository.getCheckedTokensNumber())
        assertFalse(CheckTokensAreGrappedUseCase(repository).execute())
        assertEquals(storage.checkedCount, TokensRepositoryImpl(storage).getCheckedTokensNumber())
    }

    @Test fun `marks move into surviving free slots and positions survive restart`() {
        val storage = FakeTokensStorage(LARGE_BOARD_SIZE, PARTIAL_PROGRESS,
            setOf(SECOND_TOKEN_INDEX, FIRST_REMOVED_MARK_INDEX, LAST_REMOVED_MARK_INDEX))
        val repository = TokensRepositoryImpl(storage)
        repository.resizeTokens(MEDIUM_BOARD_SIZE)
        assertEquals(setOf(FIRST_TOKEN_INDEX, SECOND_TOKEN_INDEX, THIRD_TOKEN_INDEX), storage.indices)
        repository.uncheckToken(SECOND_TOKEN_INDEX)
        repository.checkToken(FOURTH_TOKEN_INDEX)
        val restored = TokensRepositoryImpl(storage)
        assertEquals(listOf(true, false, true, true), restored.getTokensList().map { it.isChecked })
        restored.resizeTokens(EXPANDED_BOARD_SIZE)
        assertEquals(listOf(true, false, true, true, false, false), restored.getTokensList().map { it.isChecked })
    }

    @Test fun `legacy counters are clamped and migrated`() {
        for ((count, checked) in listOf(PAIR_BOARD_SIZE to OVERFLOW_CHECKED_COUNT,
            DEFAULT_TOKENS_NUMBER to NEGATIVE_CHECKED_COUNT,
            EMPTY_BOARD_SIZE to PARTIAL_PROGRESS, OVERSIZED_BOARD_SIZE to OVERSIZED_CHECKED_COUNT)) {
            val storage = FakeTokensStorage(count, checked)
            val repository = TokensRepositoryImpl(storage)
            val expectedSize = count.coerceIn(MIN_BOARD_SIZE, MAX_BOARD_SIZE)
            assertEquals(expectedSize, repository.getTokensNumber())
            assertEquals(checked.coerceIn(NO_CHECKED_TOKENS, expectedSize), repository.getCheckedTokensNumber())
            assertEquals(repository.getCheckedTokensNumber(), storage.checkedCount)
            assertNotNull(storage.indices)
        }
    }

    @Test fun `new position format is authoritative and invalid positions are discarded`() {
        val storage = FakeTokensStorage(SMALL_BOARD_SIZE, MISMATCHED_LEGACY_CHECKED_COUNT,
            setOf(NEGATIVE_TOKEN_INDEX, SECOND_TOKEN_INDEX, OUT_OF_RANGE_TOKEN_INDEX))
        val repository = TokensRepositoryImpl(storage)
        assertEquals(listOf(false, true, false), repository.getTokensList().map { it.isChecked })
        assertEquals(setOf(SECOND_TOKEN_INDEX), storage.indices)
        assertEquals(SINGLE_CHECKED_TOKEN, storage.checkedCount)
    }

    @Test fun `repeated and stale actions are harmless`() {
        val storage = FakeTokensStorage()
        val repository = TokensRepositoryImpl(storage)
        assertTrue(repository.checkToken(THIRD_TOKEN_INDEX))
        assertFalse(repository.checkToken(THIRD_TOKEN_INDEX))
        assertEquals(SINGLE_CHECKED_TOKEN, repository.getCheckedTokensNumber())
        assertTrue(repository.uncheckToken(THIRD_TOKEN_INDEX))
        assertFalse(repository.uncheckToken(THIRD_TOKEN_INDEX))
        assertFalse(repository.checkToken(DEFAULT_TOKENS_NUMBER))
        assertFalse(repository.uncheckToken(NEGATIVE_TOKEN_INDEX))
        val writes = storage.writes
        listOf(NEGATIVE_BOARD_SIZE, EMPTY_BOARD_SIZE, ABOVE_MAX_BOARD_SIZE, DEFAULT_TOKENS_NUMBER)
            .forEach { assertFalse(repository.resizeTokens(it)) }
        assertEquals(writes, storage.writes)
        assertEquals(NO_CHECKED_TOKENS, repository.getCheckedTokensNumber())
    }

    @Test fun `returned tokens do not expose mutable repository state`() {
        val repository = TokensRepositoryImpl(FakeTokensStorage())
        val snapshot = repository.getTokensList()
        val changedCopy = repository.getTokenById(SECOND_TOKEN_INDEX).copy(isChecked = true)
        assertTrue(changedCopy.isChecked)
        assertEquals(NO_CHECKED_TOKENS, repository.getCheckedTokensNumber())
        repository.checkToken(FIRST_TOKEN_INDEX)
        assertFalse(snapshot[FIRST_TOKEN_INDEX].isChecked)
    }

    @Test fun `resize preserves surviving identities and creates fresh appended identities`() {
        val repository = TokensRepositoryImpl(FakeTokensStorage())
        val original = repository.getTokensList().map { it.id }
        repository.resizeTokens(PAIR_BOARD_SIZE)
        assertEquals(original.take(PAIR_BOARD_SIZE), repository.getTokensList().map { it.id })
        repository.resizeTokens(DEFAULT_TOKENS_NUMBER)
        val expanded = repository.getTokensList().map { it.id }
        assertEquals(original.take(PAIR_BOARD_SIZE), expanded.take(PAIR_BOARD_SIZE))
        assertTrue(expanded.drop(PAIR_BOARD_SIZE).none { it in original })
    }

    @Test fun `clearing a board persists no marks`() {
        val storage = FakeTokensStorage(DEFAULT_TOKENS_NUMBER, DEFAULT_TOKENS_NUMBER)
        val repository = TokensRepositoryImpl(storage)
        repository.uncheckAllTokens()
        assertEquals(NO_CHECKED_TOKENS, TokensRepositoryImpl(storage).getCheckedTokensNumber())
        assertEquals(emptySet<Int>(), storage.indices)
    }

    private companion object {
        const val MIN_BOARD_SIZE = 1
        const val MAX_BOARD_SIZE = 10
        const val LARGE_BOARD_SIZE = 9
        const val MEDIUM_BOARD_SIZE = 4
        const val SMALL_BOARD_SIZE = 3
        const val PAIR_BOARD_SIZE = 2
        const val EXPANDED_BOARD_SIZE = 6
        const val EMPTY_BOARD_SIZE = 0
        const val NEGATIVE_BOARD_SIZE = -1
        const val HALF_MAX_PROGRESS = 5
        const val NEARLY_COMPLETE_MAX_PROGRESS = 9
        const val NEARLY_COMPLETE_DEFAULT_PROGRESS = 4
        const val PARTIAL_PROGRESS = 3
        const val PAIR_CHECKED_TOKENS = 2
        const val SINGLE_CHECKED_TOKEN = 1
        const val FIRST_TOKEN_INDEX = 0
        const val SECOND_TOKEN_INDEX = 1
        const val THIRD_TOKEN_INDEX = 2
        const val FOURTH_TOKEN_INDEX = 3
        const val FIRST_REMOVED_MARK_INDEX = 6
        const val LAST_REMOVED_MARK_INDEX = 8
        const val NEGATIVE_TOKEN_INDEX = -1
        const val OUT_OF_RANGE_TOKEN_INDEX = 15
        const val OVERFLOW_CHECKED_COUNT = 7
        const val NEGATIVE_CHECKED_COUNT = -3
        const val OVERSIZED_BOARD_SIZE = 100
        const val OVERSIZED_CHECKED_COUNT = 90
        const val MISMATCHED_LEGACY_CHECKED_COUNT = 99
        const val ABOVE_MAX_BOARD_SIZE = 11
    }
}
