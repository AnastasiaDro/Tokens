package data.tokens

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import data.persistence.DocumentSerializer
import data.persistence.TokensDocument
import data.persistence.tokensStore
import domain.models.MAX_RECENT_COLORS
import domain.repository.TokenBoardRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.io.OutputStream

class RecentColorsRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    private suspend fun withRepository(file: File, block: suspend (TokenBoardRepository) -> Unit) {
        val job = SupervisorJob()
        try {
            block(DataStoreTokenBoardRepository(tokensStore(file, { emptyMap<String, Any>() }, CoroutineScope(job + Dispatchers.IO))))
        } finally { job.cancelAndJoin() }
    }

    @Test fun confirmationKeepsSixUniqueColorsNewestFirst() = runBlocking {
        withRepository(folder.newFolder().resolve(FILE_NAME)) { repository ->
            assertTrue(repository.colorSettings.first().recentColors.isEmpty())
            COLORS.forEach { repository.setColor(it) }
            val expected = COLORS.reversed().take(MAX_RECENT_COLORS)
            assertEquals(expected, repository.colorSettings.first().recentColors)
            val repeated = COLORS[REPEATED_INDEX]
            repository.setColor(repeated)
            val reordered = listOf(repeated) + expected.filterNot { it == repeated }
            assertEquals(reordered, repository.colorSettings.first().recentColors)
            val savedBoard = repository.board.first()
            repository.setColor(repeated)
            assertEquals(reordered, repository.colorSettings.first().recentColors)
            assertEquals(savedBoard, repository.board.first())
        }
    }

    @Test fun confirmingUnchangedColorAddsHistoryWithoutChangingGameBoard() = runBlocking {
        withRepository(folder.newFolder().resolve(FILE_NAME)) { repository ->
            val original = repository.board.first()
            repository.setColor(original.color)
            assertEquals(listOf(original.color), repository.colorSettings.first().recentColors)
            assertEquals(original, repository.board.first())
        }
    }

    @Test fun oldJsonWithoutHistoryKeepsColorProgressAndIds() = runBlocking {
        val file = folder.newFolder().resolve(FILE_NAME)
        file.writeText("""{"version":1,"migrated":true,"tokens":[{"id":"$TOKEN_ID","checked":true}],"color":$RED,"revision":$SAVED_REVISION}""")
        withRepository(file) { repository ->
            val original = repository.board.first()
            assertEquals(RED, original.color)
            assertEquals(SAVED_REVISION, original.revision)
            assertEquals(TOKEN_ID, original.tokens.single().id)
            assertTrue(original.tokens.single().isChecked)
            assertTrue(repository.colorSettings.first().recentColors.isEmpty())
            repository.setColor(RED)
            assertEquals(original, repository.board.first())
            assertEquals(listOf(RED), repository.colorSettings.first().recentColors)
        }
    }

    @Test fun historySurvivesReopeningWithColorAndProgress() = runBlocking {
        val file = folder.newFolder().resolve(FILE_NAME)
        withRepository(file) { repository ->
            COLORS.forEach { repository.setColor(it) }
            repository.setChecked(repository.board.first().tokens.first().id, true)
        }
        withRepository(file) { repository ->
            assertEquals(COLORS.reversed().take(MAX_RECENT_COLORS), repository.colorSettings.first().recentColors)
            assertEquals(COLORS.last(), repository.board.first().color)
            assertTrue(repository.board.first().tokens.first().isChecked)
        }
    }

    @Test fun concurrentConfirmationsDoNotLoseColorsOrDisagreeWithCurrentColor() = runBlocking {
        withRepository(folder.newFolder().resolve(FILE_NAME)) { repository ->
            val colors = COLORS.take(MAX_RECENT_COLORS)
            coroutineScope { colors.forEach { color -> launch { repository.setColor(color) } } }
            val settings = repository.colorSettings.first()
            assertEquals(colors.toSet(), settings.recentColors.toSet())
            assertEquals(MAX_RECENT_COLORS, settings.recentColors.size)
            assertEquals(repository.board.first().color, settings.recentColors.first())
        }
    }

    @Test fun failedWriteKeepsBothColorAndHistoryInMemoryAndOnDisk() = runBlocking {
        val file = folder.newFolder().resolve(FILE_NAME)
        var failWrite = false
        val serializer = DocumentSerializer(TokensDocument.serializer(), { TokensDocument(migrated = true) }) { true }
        val job = SupervisorJob()
        try {
            val store = DataStoreFactory.create(
                serializer = object : Serializer<TokensDocument> by serializer {
                    override suspend fun writeTo(t: TokensDocument, output: OutputStream) {
                        if (failWrite) throw IOException("Expected write failure")
                        serializer.writeTo(t, output)
                    }
                },
                scope = CoroutineScope(job + Dispatchers.IO), produceFile = { file },
            )
            val repository = DataStoreTokenBoardRepository(store)
            repository.setColor(RED)
            val saved = store.data.first()
            failWrite = true
            try { repository.setColor(GREEN); fail("Write must fail") } catch (_: IOException) { }
            assertEquals(saved, store.data.first())
        } finally { job.cancelAndJoin() }
        withRepository(file) { repository ->
            assertEquals(RED, repository.colorSettings.first().color)
            assertEquals(listOf(RED), repository.colorSettings.first().recentColors)
        }
    }

    private companion object {
        const val FILE_NAME = "tokens.json"
        const val TOKEN_ID = "preserved-token"
        const val SAVED_REVISION = 7L
        const val RED = -65536
        const val GREEN = -16711936
        const val REPEATED_INDEX = 2
        val COLORS = listOf(RED, GREEN, -16776961, -256, -65281, -16711681, -26624)
    }
}
