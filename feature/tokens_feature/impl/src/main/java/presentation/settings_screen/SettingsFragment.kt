package presentation.settings_screen

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Spanned
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cerebus.tokens.core.ui.setTokensContent
import com.cerebus.tokens.core.ui.showToast
import com.cerebus.tokens.feature.tokens_feature.R
import domain.repository.MAX_TOKEN_COUNT
import domain.repository.MIN_TOKEN_COUNT
import org.koin.androidx.viewmodel.ext.android.viewModel
import presentation.SelectTokensNumberAlertData

class SettingsFragment : Fragment() {
    private val viewModel: SettingsViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            id = R.id.settings_compose_view
            setTokensContent {
                SettingsRoute(
                    viewModel = viewModel,
                    onSelectCount = { count ->
                        findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToSelectTokenNumberAlert(
                            SelectTokensNumberAlertData(MIN_TOKEN_COUNT, MAX_TOKEN_COUNT, count)))
                    },
                    onSelectColor = { findNavController().navigate(R.id.action_settingsFragment_to_selectColorDialogFragment) },
                    onYoutube = { openLink(R.string.youtube_link) },
                    onDonate = { openLink(R.string.donate_link) },
                )
            }
        }

    private fun openLink(@StringRes resource: Int) {
        val text = resources.getText(resource) as? Spanned ?: return
        val url = text.getSpans(TEXT_START, text.length, URLSpan::class.java).firstOrNull()?.url ?: return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            showToast(getString(R.string.link_open_error))
        }
    }

    private companion object {
        const val TEXT_START = 0
    }
}
