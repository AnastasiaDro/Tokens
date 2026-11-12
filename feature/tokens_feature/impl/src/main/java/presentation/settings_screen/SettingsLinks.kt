package presentation.settings_screen

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.text.Spanned
import android.text.style.URLSpan
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.net.toUri
import com.cerebus.tokens.feature.tokens_feature.R

internal fun Context.openSettingsLink(@StringRes resource: Int) {
    val text = resources.getText(resource) as? Spanned ?: return
    val url = text.getSpans(TEXT_START, text.length, URLSpan::class.java).firstOrNull()?.url ?: return
    try { startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
    catch (_: ActivityNotFoundException) { Toast.makeText(this, R.string.link_open_error, Toast.LENGTH_LONG).show() }
}
private const val TEXT_START = 0
