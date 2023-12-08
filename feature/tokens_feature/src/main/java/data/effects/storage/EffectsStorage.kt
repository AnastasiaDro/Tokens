package data.effects.storage

/**
 * [EffectsStorage] - an interface for storage of effects data:
 * a data about animation and sound which used here
 * is it on or off for example
 *
 * @see EffectsStorageImpl
 *
 * @since 18.11.2023
 * @author Anastasia Drogunova
 */
interface EffectsStorage {

    fun plugWinAnimationOn()

    fun plugWinAnimationOff()

    fun plugWinSoundOn()

    fun plugWinSoundOff()

    fun getIsWinAnimation(): Boolean

    fun getIsWinSound(): Boolean

    fun getEffectsDuration(): Long

    fun getAnimationRepeatTimes(): Int
}