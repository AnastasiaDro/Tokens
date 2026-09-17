package data.tokens

import domain.usecases.tokens.ChangeTokensNumberUseCase
import domain.usecases.tokens.CheckTokensAreGrappedUseCase
import org.junit.Assert.*
import org.junit.Test

class TokensRepositoryTest {
    @Test fun `resize follows agreed partial and completed board rules`() {
        val cases = listOf(
            Triple(10, 5, 3) to 2,
            Triple(10, 9, 4) to 3,
            Triple(9, 3, 4) to 3,
            Triple(10, 10, 4) to 4,
            Triple(9, 3, 1) to 0,
            Triple(9, 9, 1) to 1,
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
        val storage = FakeTokensStorage(5, 4)
        val repository = TokensRepositoryImpl(storage)
        val resize = ChangeTokensNumberUseCase(repository)
        resize.execute(2)
        assertEquals(1, repository.getCheckedTokensNumber())
        resize.execute(5)
        repository.checkToken(2)
        assertEquals(2, repository.getCheckedTokensNumber())
        assertFalse(CheckTokensAreGrappedUseCase(repository).execute())
        assertEquals(storage.checkedCount, TokensRepositoryImpl(storage).getCheckedTokensNumber())
    }

    @Test fun `marks move into surviving free slots and positions survive restart`() {
        val storage = FakeTokensStorage(9, 3, setOf(1, 6, 8))
        val repository = TokensRepositoryImpl(storage)
        repository.resizeTokens(4)
        assertEquals(setOf(0, 1, 2), storage.indices)
        repository.uncheckToken(1)
        repository.checkToken(3)
        val restored = TokensRepositoryImpl(storage)
        assertEquals(listOf(true, false, true, true), restored.getTokensList().map { it.isChecked })
        restored.resizeTokens(6)
        assertEquals(listOf(true, false, true, true, false, false), restored.getTokensList().map { it.isChecked })
    }

    @Test fun `legacy counters are clamped and migrated`() {
        for ((count, checked) in listOf(2 to 7, 5 to -3, 0 to 3, 100 to 90)) {
            val storage = FakeTokensStorage(count, checked)
            val repository = TokensRepositoryImpl(storage)
            val expectedSize = count.coerceIn(1, 10)
            assertEquals(expectedSize, repository.getTokensNumber())
            assertEquals(checked.coerceIn(0, expectedSize), repository.getCheckedTokensNumber())
            assertEquals(repository.getCheckedTokensNumber(), storage.checkedCount)
            assertNotNull(storage.indices)
        }
    }

    @Test fun `new position format is authoritative and invalid positions are discarded`() {
        val storage = FakeTokensStorage(3, 99, setOf(-1, 1, 15))
        val repository = TokensRepositoryImpl(storage)
        assertEquals(listOf(false, true, false), repository.getTokensList().map { it.isChecked })
        assertEquals(setOf(1), storage.indices)
        assertEquals(1, storage.checkedCount)
    }

    @Test fun `repeated and stale actions are harmless`() {
        val storage = FakeTokensStorage()
        val repository = TokensRepositoryImpl(storage)
        assertTrue(repository.checkToken(2))
        assertFalse(repository.checkToken(2))
        assertEquals(1, repository.getCheckedTokensNumber())
        assertTrue(repository.uncheckToken(2))
        assertFalse(repository.uncheckToken(2))
        assertFalse(repository.checkToken(5))
        assertFalse(repository.uncheckToken(-1))
        val writes = storage.writes
        listOf(-1, 0, 11, 5).forEach { assertFalse(repository.resizeTokens(it)) }
        assertEquals(writes, storage.writes)
        assertEquals(0, repository.getCheckedTokensNumber())
    }

    @Test fun `returned tokens do not expose mutable repository state`() {
        val repository = TokensRepositoryImpl(FakeTokensStorage())
        repository.getTokensList()[0].isChecked = true
        repository.getTokenById(1).isChecked = true
        assertEquals(0, repository.getCheckedTokensNumber())
    }

    @Test fun `clearing a board persists no marks`() {
        val storage = FakeTokensStorage(5, 5)
        val repository = TokensRepositoryImpl(storage)
        repository.uncheckAllTokens()
        assertEquals(0, TokensRepositoryImpl(storage).getCheckedTokensNumber())
        assertEquals(emptySet<Int>(), storage.indices)
    }
}
