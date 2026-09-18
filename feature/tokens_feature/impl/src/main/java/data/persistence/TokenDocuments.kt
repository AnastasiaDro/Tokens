package data.persistence

import kotlinx.serialization.Serializable
import kotlinx.serialization.Required
import java.util.UUID
import domain.repository.MIN_TOKEN_COUNT
import domain.repository.MAX_TOKEN_COUNT

internal const val FORMAT_VERSION = 1
internal const val MIN_TOKENS = MIN_TOKEN_COUNT
internal const val MAX_TOKENS = MAX_TOKEN_COUNT
internal const val LEGACY_MAX_TOKENS = 10
internal const val DEFAULT_TOKENS = 5
internal const val NO_MARKS = 0
internal const val INITIAL_REVISION = 0L
internal const val REVISION_STEP = 1L
internal const val DEFAULT_COLOR = -12517557
internal const val UNSET_COLOR = 0

@Serializable
internal data class StoredToken(@Required val id: String = UUID.randomUUID().toString(), @Required val checked: Boolean = false)

@Serializable
internal data class TokensDocument(
    @Required val version: Int = FORMAT_VERSION,
    @Required val migrated: Boolean = false,
    @Required val tokens: List<StoredToken> = List(DEFAULT_TOKENS) { StoredToken() },
    @Required val color: Int = DEFAULT_COLOR,
    @Required val revision: Long = INITIAL_REVISION,
)

@Serializable
internal data class EffectsDocument(
    @Required val version: Int = FORMAT_VERSION,
    @Required val migrated: Boolean = false,
    @Required val sound: Boolean = true,
    @Required val animation: Boolean = true,
)

internal fun migrateTokens(current: TokensDocument, legacy: Map<String, *>): TokensDocument {
    val size = (legacy["TokensNumber"] as? Int ?: DEFAULT_TOKENS).coerceIn(MIN_TOKENS, LEGACY_MAX_TOKENS)
    val positions = (legacy["CheckedTokenIndices"] as? Set<*>)?.mapNotNull { (it as? String)?.toIntOrNull() }?.toSet()
        ?: List((legacy["CheckedTokensNumber"] as? Int ?: NO_MARKS).coerceIn(NO_MARKS, size)) { it }.toSet()
    val color = (legacy["CheckedTokensColor"] as? Int ?: DEFAULT_COLOR).let {
        if (it == UNSET_COLOR) DEFAULT_COLOR else it
    }
    return current.copy(migrated = true, tokens = List(size) { StoredToken(checked = it in positions) }, color = color)
}

internal fun migrateEffects(current: EffectsDocument, legacy: Map<String, *>): EffectsDocument =
    current.copy(migrated = true, sound = legacy["SoundPlaying"] as? Boolean ?: true,
        animation = legacy["AnimationShowing"] as? Boolean ?: true)
