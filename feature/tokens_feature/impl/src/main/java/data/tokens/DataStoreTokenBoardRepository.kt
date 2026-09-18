package data.tokens

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStoreFile
import data.persistence.*
import domain.models.Token
import domain.models.resizeTokenProgress
import domain.repository.TokenBoard
import domain.repository.TokenBoardRepository
import domain.repository.TokenChange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class DataStoreTokenBoardRepository internal constructor(
    private val store: DataStore<TokensDocument>,
) : TokenBoardRepository {
    constructor(context: Context) : this(tokensStore(
        context.applicationContext.dataStoreFile("tokens.json"),
        { context.applicationContext.getSharedPreferences("TokensPreferences", Context.MODE_PRIVATE).all },
        CoroutineScope(SupervisorJob() + Dispatchers.IO),
    ))

    override val board = store.data.map { it.toBoard() }.distinctUntilChanged()

    override suspend fun toggle(id: String) = mark(id) { !it }
    override suspend fun setChecked(id: String, checked: Boolean) = mark(id) { checked }

    private suspend fun mark(id: String, transform: (Boolean) -> Boolean): TokenChange {
        var changed = false
        var completed = false
        val saved = store.updateData { current ->
            val next = current.tokens.map { if (it.id == id) it.copy(checked = transform(it.checked)) else it }
            changed = next != current.tokens
            completed = changed && !current.tokens.all { it.checked } && next.all { it.checked }
            if (changed) current.copy(tokens = next, revision = current.revision + REVISION_STEP) else current
        }
        return TokenChange(saved.toBoard(), changed, completed)
    }

    override suspend fun resize(count: Int) {
        require(count in MIN_TOKENS..MAX_TOKENS)
        store.updateData { current ->
            if (count == current.tokens.size) current else {
                val marks = resizeTokenProgress(current.tokens.map { it.checked }, count)
                current.copy(tokens = marks.mapIndexed { index, checked ->
                    current.tokens.getOrNull(index)?.copy(checked = checked) ?: StoredToken(checked = checked)
                }, revision = current.revision + REVISION_STEP)
            }
        }
    }

    override suspend fun setColor(color: Int) {
        store.updateData { if (it.color == color) it else it.copy(color = color, revision = it.revision + REVISION_STEP) }
    }

    override suspend fun clear() {
        store.updateData { current ->
            if (current.tokens.none { it.checked }) current else current.copy(
                tokens = current.tokens.map { it.copy(checked = false) }, revision = current.revision + REVISION_STEP,
            )
        }
    }

    private fun TokensDocument.toBoard() = TokenBoard(tokens.map { Token(it.checked, color, it.id) }, color, revision)
}

