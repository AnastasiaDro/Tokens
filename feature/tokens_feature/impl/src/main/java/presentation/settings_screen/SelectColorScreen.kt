package presentation.settings_screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cerebus.tokens.core.ui.theme.TokensDimensions
import com.cerebus.tokens.feature.tokens_feature.R
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.ColorPickerController
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import presentation.state.SaveState
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Canvas as GraphicsCanvas
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
    // NavigationDialog consumes system bars; this adds only the remaining safe insets (e.g. cutout).
    BoxWithConstraints(modifier.safeDrawingPadding().fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.widthIn(max = COLOR_DIALOG_MAX_WIDTH_DP.dp).fillMaxWidth(DIALOG_SIZE_FRACTION)
                .heightIn(max = minOf(maxHeight, COLOR_DIALOG_MAX_HEIGHT_DP.dp) * DIALOG_SIZE_FRACTION)
                .testTag(COLOR_DIALOG_TAG),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(Modifier.padding(horizontal = TokensDimensions.ContentPadding, vertical = TokensDimensions.SmallSpacing)) {
                ColorDialogHeader(state.cancellable, onCancel)
                if (state.color != null && !state.loading && !state.readError) {
                    ColorPicker(state, onColorChange, onConfirm, onCancel, Modifier.weight(CONTENT_WEIGHT))
                } else {
                    Column(Modifier.weight(CONTENT_WEIGHT).verticalScroll(rememberScrollState())) {
                        if (state.readError) {
                            TextButton(onClick = onRetryRead, enabled = state.cancellable) {
                                Text(stringResource(CoreR.string.storage_read_error), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    ColorDialogActions(state.editable, state.cancellable, onConfirm, onCancel)
                }
            }
        }
    }
}

@Composable
private fun ColorDialogHeader(cancellable: Boolean, onCancel: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.select_color),
            modifier = Modifier.weight(CONTENT_WEIGHT).testTag(COLOR_TITLE_TAG),
            style = MaterialTheme.typography.titleLarge,
        )
        val closeDescription = stringResource(R.string.close_color_dialog)
        val iconColor = MaterialTheme.colorScheme.onSurfaceVariant
        IconButton(
            onClick = onCancel,
            enabled = cancellable,
            modifier = Modifier.size(HEADER_ACTION_SIZE_DP.dp).testTag(COLOR_CLOSE_TAG),
        ) {
            Canvas(Modifier.size(CLOSE_ICON_SIZE_DP.dp).semantics { contentDescription = closeDescription }) {
                val start = size.width * CLOSE_ICON_INSET
                val end = size.width - start
                val stroke = CLOSE_ICON_STROKE_DP.dp.toPx()
                drawLine(iconColor, Offset(start, start), Offset(end, end), stroke, StrokeCap.Round)
                drawLine(iconColor, Offset(start, end), Offset(end, start), stroke, StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun ColorPicker(
    state: ColorUiState,
    onColorChange: (Int) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val availableWidth = maxWidth
        val useTwoColumns = availableWidth >= TWO_COLUMN_MIN_WIDTH_DP.dp
        // The library reinitializes when moved between layouts. Start it from the current draft,
        // not the color with which the dialog was originally opened.
        key(useTwoColumns) {
            val controller = rememberColorPickerController()
            val initialColor = remember { Color(requireNotNull(state.color)) }
            val currentOnColorChange by rememberUpdatedState(onColorChange)
            val editable by rememberUpdatedState(state.editable)
            val palette: @Composable (Modifier) -> Unit = { paletteModifier ->
                val description = stringResource(R.string.color_palette)
                BlockPickerInput(state.editable, paletteModifier) {
                    HsvColorPicker(
                        modifier = Modifier.fillMaxSize().testTag(COLOR_PICKER_TAG).semantics {
                            contentDescription = description
                            if (!state.editable) disabled()
                        },
                        controller = controller,
                        initialColor = initialColor,
                        drawDefaultWheelIndicator = false,
                        onColorChanged = { envelope ->
                            // Controller initialization must not edit the draft.
                            if (editable && envelope.fromUser) currentOnColorChange(envelope.color.toArgb())
                        },
                    )
                    SelectedColorIndicator(requireNotNull(state.color), controller)
                }
            }
            val controls: @Composable () -> Unit = { ColorControls(state, controller, initialColor) }
            val actions: @Composable () -> Unit = {
                ColorDialogActions(state.editable, state.cancellable, onConfirm, onCancel)
            }
            val compactPaletteSize = minOf(availableWidth, COMPACT_PALETTE_MAX_SIZE_DP.dp)
            if (useTwoColumns) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(TokensDimensions.ContentPadding)) {
                    BoxWithConstraints(
                        Modifier.weight(PALETTE_WEIGHT).fillMaxHeight(),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        palette(Modifier.size(minOf(maxWidth, maxHeight)))
                    }
                    Column(Modifier.weight(CONTROLS_WEIGHT).fillMaxHeight()) {
                        Column(Modifier.weight(CONTENT_WEIGHT).verticalScroll(rememberScrollState())) { controls() }
                        actions()
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.weight(CONTENT_WEIGHT).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(TokensDimensions.SmallSpacing),
                    ) {
                        palette(Modifier.size(compactPaletteSize))
                        controls()
                    }
                    actions()
                }
            }
        }
    }
}

@Composable
private fun ColorControls(state: ColorUiState, controller: ColorPickerController, initialColor: Color) {
    val brightnessDescription = stringResource(R.string.color_brightness)
    val recentColors = state.recentColors.distinct().take(MAX_COLOR_SWATCHES)
    Column(verticalArrangement = Arrangement.spacedBy(CONTROL_GROUP_SPACING_DP.dp)) {
        Column {
            Text(brightnessDescription, style = MaterialTheme.typography.titleSmall)
            BlockPickerInput(state.editable, Modifier.fillMaxWidth().height(COLOR_CONTROL_HEIGHT_DP.dp)) {
                BrightnessSlider(
                    modifier = Modifier.fillMaxSize().testTag(COLOR_BRIGHTNESS_TAG).semantics {
                        contentDescription = brightnessDescription
                        if (!state.editable) disabled()
                    },
                    controller = controller,
                    initialColor = initialColor,
                    borderSize = NO_SLIDER_BORDER_DP.dp,
                    borderRadius = SLIDER_CORNER_RADIUS_DP.dp,
                    wheelImageBitmap = brightnessMarker(requireNotNull(state.color)),
                )
            }
        }
        Column {
            Text(stringResource(R.string.recent_colors), style = MaterialTheme.typography.titleSmall)
            if (recentColors.isEmpty()) {
                Text(
                    stringResource(R.string.no_recent_colors),
                    Modifier.heightIn(min = COLOR_CONTROL_HEIGHT_DP.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                ColorSwatches(recentColors.map { Color(it) }, state, RECENT_COLOR_SWATCHES_TAG) {
                    controller.selectByColor(it, fromUser = true)
                }
            }
        }
        Column {
            Text(stringResource(R.string.quick_color_selection), style = MaterialTheme.typography.titleSmall)
            ColorSwatches(QUICK_COLORS, state, QUICK_COLOR_SWATCHES_TAG) {
                controller.selectByColor(it, fromUser = true)
            }
        }
        if (state.save == SaveState.ERROR) {
            Text(stringResource(CoreR.string.storage_save_error), color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun brightnessMarker(color: Int): ImageBitmap {
    val density = LocalDensity.current
    val sizePx = with(density) { MARKER_SIZE_DP.dp.roundToPx() }
    val borderPx = with(density) { MARKER_BORDER_DP.dp.toPx() }
    return remember(color, sizePx, borderPx) {
        ImageBitmap(sizePx, sizePx).also { bitmap ->
            val canvas = GraphicsCanvas(bitmap)
            val radius = sizePx / MARKER_DIAMETER_DIVISOR
            val center = Offset(radius, radius)
            canvas.drawCircle(center, radius, Paint().apply { this.color = Color.White })
            canvas.drawCircle(center, radius - borderPx, Paint().apply { this.color = Color(color) })
        }
    }
}

@Composable
private fun BlockPickerInput(enabled: Boolean, modifier: Modifier, content: @Composable () -> Unit) {
    // Semantics.disabled alone does not stop the library's pointer handlers.
    Box(modifier) {
        content()
        if (!enabled) {
            Box(Modifier.matchParentSize().pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                }
            })
        }
    }
}

@Composable
private fun SelectedColorIndicator(color: Int, controller: ColorPickerController) {
    val description = stringResource(R.string.selected_color_preview)
    Canvas(
        modifier = Modifier.absoluteOffset {
            val radius = MARKER_RADIUS_DP.dp.toPx()
            val point = controller.selectedPoint.value
            IntOffset((point.x - radius).roundToInt(), (point.y - radius).roundToInt())
        }.size(MARKER_SIZE_DP.dp).testTag(COLOR_PREVIEW_TAG).semantics {
            contentDescription = description
            stateDescription = colorHex(color)
        },
    ) {
        drawCircle(Color.White)
        drawCircle(Color(color), radius = (MARKER_RADIUS_DP - MARKER_BORDER_DP).dp.toPx())
    }
}

@Composable
private fun ColorSwatches(colors: List<Color>, state: ColorUiState, tag: String, onSelect: (Color) -> Unit) {
    Row(Modifier.fillMaxWidth().testTag(tag).selectableGroup()) {
        colors.forEachIndexed { index, color ->
            Box(
                modifier = Modifier.weight(CONTENT_WEIGHT).height(COLOR_CONTROL_HEIGHT_DP.dp)
                    .testTag(swatchTag(tag, index))
                    .selectable(
                        selected = color.toArgb() == state.color,
                        enabled = state.editable,
                        role = Role.RadioButton,
                        onClick = { onSelect(color) },
                    )
                    .semantics { contentDescription = colorHex(color.toArgb()) },
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    Modifier.size(SWATCH_SIZE_DP.dp),
                    shape = CircleShape,
                    color = color,
                    border = if (color.toArgb() == state.color) {
                        BorderStroke(SELECTED_BORDER_DP.dp, MaterialTheme.colorScheme.onSurface)
                    } else {
                        BorderStroke(UNSELECTED_BORDER_DP.dp, MaterialTheme.colorScheme.outlineVariant)
                    },
                ) {}
            }
        }
        repeat(MAX_COLOR_SWATCHES - colors.size) { Spacer(Modifier.weight(CONTENT_WEIGHT)) }
    }
}

@Composable
private fun ColorDialogActions(
    confirmEnabled: Boolean,
    cancelEnabled: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    FlowRow(
        Modifier.fillMaxWidth().testTag(COLOR_ACTIONS_TAG),
        horizontalArrangement = Arrangement.spacedBy(TokensDimensions.SmallSpacing, Alignment.End),
    ) {
        TextButton(onClick = onCancel, enabled = cancelEnabled) { Text(stringResource(CoreR.string.cancel)) }
        Button(onClick = onConfirm, enabled = confirmEnabled, modifier = Modifier.testTag(COLOR_CONFIRM_TAG)) {
            Text(stringResource(R.string.select_button_text))
        }
    }
}

internal const val COLOR_DIALOG_TAG = "color-dialog"
internal const val COLOR_TITLE_TAG = "color-title"
internal const val COLOR_ACTIONS_TAG = "color-actions"
internal const val COLOR_CLOSE_TAG = "color-close"
internal const val COLOR_PICKER_TAG = "color-picker"
internal const val COLOR_PREVIEW_TAG = "color-preview"
internal const val COLOR_CONFIRM_TAG = "color-confirm"
internal const val COLOR_BRIGHTNESS_TAG = "color-brightness"
internal const val QUICK_COLOR_SWATCHES_TAG = "quick-color-swatches"
internal const val RECENT_COLOR_SWATCHES_TAG = "recent-color-swatches"
internal fun quickColorTag(index: Int) = swatchTag(QUICK_COLOR_SWATCHES_TAG, index)
internal fun recentColorTag(index: Int) = swatchTag(RECENT_COLOR_SWATCHES_TAG, index)
private fun swatchTag(group: String, index: Int) = "$group-$index"
private fun colorHex(color: Int) = "#" + color.toUInt().toString(HEX_RADIX).uppercase()

private val QUICK_COLORS = listOf(Color.Red, Color(ORANGE_COLOR), Color.Yellow, Color.Green, Color.Blue, Color(VIOLET_COLOR))
private const val MAX_COLOR_SWATCHES = 6
private const val COLOR_DIALOG_MAX_WIDTH_DP = 720
private const val COLOR_DIALOG_MAX_HEIGHT_DP = 440
private const val DIALOG_SIZE_FRACTION = 0.95f
private const val TWO_COLUMN_MIN_WIDTH_DP = 520
private const val COMPACT_PALETTE_MAX_SIZE_DP = 220
private const val HEADER_ACTION_SIZE_DP = 40
private const val CLOSE_ICON_SIZE_DP = 24
private const val CLOSE_ICON_INSET = 0.2f
private const val CLOSE_ICON_STROKE_DP = 2
private const val MARKER_SIZE_DP = 24
private const val MARKER_RADIUS_DP = 12
private const val MARKER_BORDER_DP = 2
private const val MARKER_DIAMETER_DIVISOR = 2f
private const val SWATCH_SIZE_DP = 28
private const val COLOR_CONTROL_HEIGHT_DP = 32
private const val CONTROL_GROUP_SPACING_DP = 3
private const val SELECTED_BORDER_DP = 2
private const val UNSELECTED_BORDER_DP = 1
private const val NO_SLIDER_BORDER_DP = 0
private const val SLIDER_CORNER_RADIUS_DP = 6
private const val PALETTE_WEIGHT = 0.75f
private const val CONTROLS_WEIGHT = 1f
private const val CONTENT_WEIGHT = 1f
private const val HEX_RADIX = 16
private const val ORANGE_COLOR = 0xFFFF9800
private const val VIOLET_COLOR = 0xFF673AB7
