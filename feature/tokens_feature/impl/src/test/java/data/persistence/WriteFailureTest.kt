package data.persistence

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import data.tokens.DataStoreTokenBoardRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.io.OutputStream

class WriteFailureTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun failedWriteDoesNotPublishOrReplaceSavedBoardAndRetryPersists() = runBlocking {
        val file = folder.newFolder().resolve("tokens.json")
        val codec = DocumentSerializer(TokensDocument.serializer(), { TokensDocument(migrated = true) }) { true }
        var failWrites = false
        val serializer = object : Serializer<TokensDocument> by codec {
            override suspend fun writeTo(t: TokensDocument, output: OutputStream) {
                if (failWrites) throw IOException("Disk unavailable")
                codec.writeTo(t, output)
            }
        }
        val job = SupervisorJob()
        val retryJob = SupervisorJob()
        try {
            val store = DataStoreFactory.create(serializer, scope = CoroutineScope(job + Dispatchers.IO), produceFile = { file })
            val repo = DataStoreTokenBoardRepository(store)
            repo.setColor(COLOR)
            val saved = repo.board.first()
            val content = file.readText()
            failWrites = true
            try { repo.toggle(saved.tokens.first().id); fail("Write must fail") } catch (_: IOException) { }
            assertEquals(saved, repo.board.first())
            assertEquals(content, file.readText())
            failWrites = false
            repo.toggle(saved.tokens.last().id)
            val marked = repo.board.first()
            assertTrue(marked.tokens.last().isChecked)
            repo.clear()
            val cleared = repo.board.first()
            job.cancelAndJoin()
            val reopened = DataStoreTokenBoardRepository(tokensStore(file, { error("No reimport") }, CoroutineScope(retryJob + Dispatchers.IO)))
            assertEquals(cleared, reopened.board.first())
            assertTrue(reopened.board.first().tokens.none { it.isChecked })
        } finally { job.cancelAndJoin(); retryJob.cancelAndJoin() }
    }

    private companion object { const val COLOR = -65536 }
}

