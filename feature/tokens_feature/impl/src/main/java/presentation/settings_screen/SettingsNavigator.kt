package presentation.settings_screen

/** One instance per ViewModel; only the active Route supplies the current navigation environment. */
class SettingsNavigator {
    sealed interface Destination {
        data class SelectCount(val count: Int) : Destination
        data object SelectColor : Destination
        data object Youtube : Destination
        data object OtherApps : Destination
    }

    private var binding: Binding? = null

    fun selectCount(count: Int) { navigate(Destination.SelectCount(count)) }
    fun selectColor() { navigate(Destination.SelectColor) }
    fun youtube() { navigate(Destination.Youtube) }
    fun otherApps() { navigate(Destination.OtherApps) }

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
