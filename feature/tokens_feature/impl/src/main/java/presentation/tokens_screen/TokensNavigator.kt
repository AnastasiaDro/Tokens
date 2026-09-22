package presentation.tokens_screen

/** One instance per ViewModel; only the active Route supplies the current navigation environment. */
class TokensNavigator {
    sealed interface Destination {
        data class SelectCount(val count: Int) : Destination
        data object Settings : Destination
        data object Photo : Destination
    }

    private var binding: Binding? = null

    fun selectCount(count: Int) { navigate(Destination.SelectCount(count)) }
    fun settings() { navigate(Destination.Settings) }
    fun photo() { navigate(Destination.Photo) }

    internal fun bind(onNavigate: (Destination) -> Unit): AutoCloseable {
        val current = Binding(onNavigate)
        binding = current
        return AutoCloseable {
            // Disposing an old composition must not detach a newer navigation environment.
            if (binding === current) binding = null
        }
    }

    private fun navigate(destination: Destination) {
        binding?.onNavigate?.invoke(destination)
    }

    private class Binding(val onNavigate: (Destination) -> Unit)
}
