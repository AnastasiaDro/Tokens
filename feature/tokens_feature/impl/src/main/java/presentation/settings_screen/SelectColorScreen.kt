package presentation.settings_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cerebus.tokens.core.ui.theme.TokensDimensions
import com.cerebus.tokens.feature.tokens_feature.R
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import presentation.state.SaveState
import com.cerebus.tokens.core.ui.R as CoreR

@Composable
internal fun SelectColorRoute(viewModel: SelectColorViewModel, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SelectColorScreen(state, viewModel::selectColor, onConfirm, onCancel, viewModel::retryRead)
}

@Composable
internal fun SelectColorScreen(
    state: ColorUiState,
    onColorChange: (Int) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onRetryRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.widthIn(max = DIALOG_MAX_WIDTH_DP.dp), shape = MaterialTheme.shapes.medium) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(TokensDimensions.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(TokensDimensions.SmallSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.select_color), style = MaterialTheme.typography.titleMedium)
            state.color?.let { color ->
                val previewDescription = stringResource(R.string.selected_color_preview)
                Box(Modifier.size(PREVIEW_SIZE_DP.dp).background(Color(color), CircleShape)
                    .testTag(COLOR_PREVIEW_TAG).semantics { contentDescription = previewDescription })
            }
            when {
                state.readError -> TextButton(onClick = onRetryRead, enabled = state.cancellable) {
                    Text(stringResource(CoreR.string.storage_read_error), color = MaterialTheme.colorScheme.error)
                }
                state.loading -> Text(stringResource(CoreR.string.storage_loading))
                state.save == SaveState.SAVING -> Text(stringResource(R.string.settings_saving))
                state.save == SaveState.ERROR ->
                    Text(stringResource(CoreR.string.storage_save_error), color = MaterialTheme.colorScheme.error)
            }
            // Mount only a ready, interactive picker. A disabled controller cannot initialize its coordinates.
            if (state.editable) {
                ColorPicker(state.color!!, onColorChange)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCancel, enabled = state.cancellable) { Text(stringResource(CoreR.string.cancel)) }
                TextButton(onClick = onConfirm, enabled = state.editable, modifier = Modifier.testTag(COLOR_CONFIRM_TAG)) {
                    Text(stringResource(R.string.select_button_text))
                }
            }
        }
    }
}

@Composable
private fun ColorPicker(color: Int, onColorChange: (Int) -> Unit) {
    val controller = rememberColorPickerController()
    val initialColor = remember { Color(color) }
    val currentOnColorChange by rememberUpdatedState(onColorChange)
    val paletteDescription = stringResource(R.string.color_palette)
    val brightnessDescription = stringResource(R.string.color_brightness)
    HsvColorPicker(
        modifier = Modifier.size(PICKER_SIZE_DP.dp).testTag(COLOR_PICKER_TAG)
            .semantics { contentDescription = paletteDescription },
        controller = controller,
        initialColor = initialColor,
        onColorChanged = { envelope ->
            // Initialization and brightness restoration are not edits to the draft.
            if (envelope.fromUser) currentOnColorChange(envelope.color.toArgb())
        },
    )
    Text(brightnessDescription)
    BrightnessSlider(
        modifier = Modifier.fillMaxWidth().height(TokensDimensions.MinimumTouchTarget)
            .testTag(COLOR_BRIGHTNESS_TAG).semantics { contentDescription = brightnessDescription },
        controller = controller,
        initialColor = initialColor,
    )
}

internal const val COLOR_PICKER_TAG = "color-picker"
internal const val COLOR_PREVIEW_TAG = "color-preview"
internal const val COLOR_CONFIRM_TAG = "color-confirm"
internal const val COLOR_BRIGHTNESS_TAG = "color-brightness"
private const val DIALOG_MAX_WIDTH_DP = 360
private const val PICKER_SIZE_DP = 200
private const val PREVIEW_SIZE_DP = 32
