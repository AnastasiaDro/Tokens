package presentation

import com.cerebus.tokens.data.reinforcement.*
import domain.models.Token
import domain.models.resizeTokenProgress
import domain.repository.*
import kotlinx.coroutines.flow.*
import java.io.IOException

class FakeBoardRepository : TokenBoardRepository {
    val values = MutableStateFlow(TokenBoard(List(BOARD_SIZE) { Token(false, COLOR, "token-$it") }, COLOR, INITIAL_BOARD_REVISION))
    var failRead = false
    var failWrite = false
    var failReadAfterWrite = false
    var beforeWrite: suspend () -> Unit = {}
    override val board = flow {
        if (failRead) throw IOException("Read failed")
        emitAll(values)
    }
    override suspend fun toggle(id: String) = change(id) { !it }
    override suspend fun setChecked(id: String, checked: Boolean) = change(id) { checked }
    private suspend fun change(id: String, transform: (Boolean) -> Boolean): TokenChange {
        beforeWrite()
        if (failWrite) throw IOException("Write failed")
        val current = values.value
        val tokens = current.tokens.map { if (it.id == id) it.copy(isChecked = transform(it.isChecked)) else it }
        val changed = tokens != current.tokens
        val result = if (changed) current.copy(tokens = tokens, revision = current.revision + REVISION_STEP) else current
        values.value = result
        if (failReadAfterWrite) failRead = true
        return TokenChange(result, changed, changed && !current.completed && result.completed)
    }
    override suspend fun resize(count: Int) {
        beforeWrite()
        if (failWrite) throw IOException("Write failed")
        val current = values.value
        val marks = resizeTokenProgress(current.tokens.map { it.isChecked }, count)
        values.value = current.copy(tokens = marks.mapIndexed { index, checked ->
            current.tokens.getOrNull(index)?.copy(isChecked = checked) ?: Token(checked, current.color)
        }, revision = current.revision + REVISION_STEP)
    }
    override suspend fun setColor(color: Int) {
        beforeWrite()
        if (failWrite) throw IOException("Write failed")
        values.update { it.copy(color = color, tokens = it.tokens.map { token -> token.copy(checkedColor = color) },
            revision = it.revision + REVISION_STEP) }
    }
    override suspend fun clear() {
        beforeWrite()
        if (failWrite) throw IOException("Write failed")
        values.update { it.copy(tokens = it.tokens.map { token -> token.copy(isChecked = false) }, revision = it.revision + REVISION_STEP) }
    }

    companion object {
        const val BOARD_SIZE = 2
        const val COLOR = -12517557
        const val REVISION_STEP = 1L
    }
}

class FakeEffectsRepository : WinEffectsRepository {
    override val settings = MutableStateFlow(EffectsSettings())
    var failWrite = false
    override suspend fun setAnimation(enabled: Boolean) {
        if (failWrite) throw IOException("Write failed")
        settings.update { it.copy(animation = enabled) }
    }
    override suspend fun setSound(enabled: Boolean) {
        if (failWrite) throw IOException("Write failed")
        settings.update { it.copy(sound = enabled) }
    }
}

class FakeReinforcementRepository : ReinforcementRepository {
    override val settings = MutableStateFlow(ReinforcementSettings())
    override suspend fun setEnabled(enabled: Boolean) { settings.update { it.copy(enabled = enabled) } }
    override suspend fun setPhotoUri(uri: String) { settings.update { it.copy(photoUri = uri) } }
}
