package presentation.tokens_screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cerebus.tokens.core.ui.theme.TokensColors
import com.cerebus.tokens.core.ui.theme.TokensDimensions
import com.cerebus.tokens.feature.tokens_feature.R
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import presentation.state.SaveState
import kotlin.math.roundToInt
import com.cerebus.tokens.core.ui.R as CoreR

@Composable
internal fun SelectTokensNumberRoute(
    viewModel: SelectTokensNumberViewModel,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SelectTokensNumberScreen(state, viewModel::selectCount, onConfirm, onCancel)
}

@Composable
internal fun SelectTokensNumberScreen(
    state: SelectTokensNumberUiState,
    onCountChange: (Int) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedCount = state.count ?: state.minimum
    Surface(
        modifier = modifier.widthIn(max = DIALOG_MAX_WIDTH_DP.dp).fillMaxWidth(),
        shape = RoundedCornerShape(DIALOG_CORNER_RADIUS_DP.dp),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = DIALOG_ELEVATION_DP.dp,
    ) {
        Column(
            modifier = Modifier.padding(DIALOG_CONTENT_PADDING_DP.dp),
            verticalArrangement = Arrangement.spacedBy(TokensDimensions.SmallSpacing),
        ) {
            Text(
                text = stringResource(R.string.changeChips),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TokensDimensions.MediumSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TokenCountWheel(
                    selectedCount = selectedCount,
                    minimum = state.minimum,
                    maximum = state.maximum,
                    enabled = state.editable,
                    onCountChange = onCountChange,
                )
                QuickCountSelection(
                    selectedCount = selectedCount,
                    enabled = state.editable,
                    onCountChange = onCountChange,
                    modifier = Modifier.weight(DIALOG_CONTENT_WEIGHT),
                )
            }
            if (state.save == SaveState.SAVING) {
                Text(
                    text = stringResource(R.string.settings_saving),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (state.save == SaveState.ERROR) {
                Text(
                    text = stringResource(CoreR.string.storage_save_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                DialogAction(
                    text = stringResource(CoreR.string.cancel),
                    enabled = state.editable,
                    onClick = onCancel,
                )
                DialogAction(
                    text = stringResource(CoreR.string.OK),
                    enabled = state.editable,
                    onClick = onConfirm,
                )
            }
        }
    }
}

@Composable
private fun TokenCountWheel(
    selectedCount: Int,
    minimum: Int,
    maximum: Int,
    enabled: Boolean,
    onCountChange: (Int) -> Unit,
) {
    val pageCount = maximum - minimum + RANGE_INCLUSIVE_ITEM_COUNT
    val initialPage = (selectedCount - minimum).coerceIn(FIRST_PAGE, pageCount - RANGE_INCLUSIVE_ITEM_COUNT)
    val pagerState = rememberPagerState(initialPage = initialPage) { pageCount }
    val scope = rememberCoroutineScope()
    var synchronizedCount by remember { mutableIntStateOf(selectedCount) }

    LaunchedEffect(selectedCount) {
        synchronizedCount = selectedCount
        val targetPage = selectedCount - minimum
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }
    LaunchedEffect(pagerState, enabled) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                val count = page + minimum
                if (enabled && count != synchronizedCount) {
                    synchronizedCount = count
                    onCountChange(count)
                }
            }
    }

    VerticalPager(
        state = pagerState,
        modifier = Modifier
            .size(width = WHEEL_WIDTH_DP.dp, height = WHEEL_HEIGHT_DP.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = selectedCount.toFloat(),
                    range = minimum.toFloat()..maximum.toFloat(),
                    steps = (maximum - minimum - RANGE_INCLUSIVE_ITEM_COUNT).coerceAtLeast(NO_STEPS),
                )
                if (!enabled) disabled()
                setProgress { requestedValue ->
                    if (!enabled) {
                        false
                    } else {
                        val requestedCount = requestedValue.roundToInt().coerceIn(minimum, maximum)
                        synchronizedCount = requestedCount
                        onCountChange(requestedCount)
                        scope.launch { pagerState.animateScrollToPage(requestedCount - minimum) }
                        true
                    }
                }
            }
            .testTag(COUNT_WHEEL_TAG),
        pageSize = PageSize.Fixed(WHEEL_ITEM_HEIGHT_DP.dp),
        contentPadding = PaddingValues(vertical = WHEEL_ITEM_HEIGHT_DP.dp),
        flingBehavior = PagerDefaults.flingBehavior(
            state = pagerState,
            pagerSnapDistance = PagerSnapDistance.atMost(MAX_PAGES_PER_FLING),
        ),
        userScrollEnabled = enabled,
    ) { page ->
        val count = page + minimum
        val selected = page == pagerState.currentPage
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(WHEEL_ITEM_HEIGHT_DP.dp)
                .padding(horizontal = WHEEL_ITEM_HORIZONTAL_PADDING_DP.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = MaterialTheme.shapes.medium,
                color = if (selected) TokensColors.WarmActionContainer else Color.Transparent,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = count.toString(),
                        modifier = if (selected) Modifier.testTag(COUNT_VALUE_TAG) else Modifier,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontSize = if (selected) SELECTED_COUNT_TEXT_SIZE_SP.sp else NEIGHBOR_COUNT_TEXT_SIZE_SP.sp,
                        lineHeight = if (selected) SELECTED_COUNT_LINE_HEIGHT_SP.sp else NEIGHBOR_COUNT_LINE_HEIGHT_SP.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickCountSelection(
    selectedCount: Int,
    enabled: Boolean,
    onCountChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.quick_count_selection),
            style = MaterialTheme.typography.titleSmall,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = QUICK_SELECTION_TOP_PADDING_DP.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TOKEN_COUNT_PRESETS.forEach { count ->
                CountPreset(
                    count = count,
                    selected = selectedCount == count,
                    enabled = enabled,
                    onClick = { onCountChange(count) },
                )
            }
        }
        Text(
            text = stringResource(R.string.token_count_range, MINIMUM_TOKEN_COUNT, MAXIMUM_TOKEN_COUNT),
            modifier = Modifier.padding(top = QUICK_SELECTION_CAPTION_PADDING_DP.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun CountPreset(
    count: Int,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .height(PRESET_HEIGHT_DP.dp)
            .widthIn(min = PRESET_MIN_WIDTH_DP.dp)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .testTag(countPresetTag(count)),
        shape = RoundedCornerShape(PRESET_CORNER_RADIUS_DP.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else TokensColors.WarmActionContainer,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = PRESET_HORIZONTAL_PADDING_DP.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = count.toString(),
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun DialogAction(text: String, enabled: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled) {
        Text(
            text = text,
            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                alpha = DISABLED_ACTION_ALPHA,
            ),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

internal const val COUNT_VALUE_TAG = "tokens-count-value"
internal const val COUNT_WHEEL_TAG = "tokens-count-wheel"
internal fun countPresetTag(count: Int) = "tokens-count-preset-$count"

private val TOKEN_COUNT_PRESETS = listOf(
    PRESET_ONE,
    PRESET_FIVE,
    PRESET_TEN,
    PRESET_FIFTEEN,
    PRESET_TWENTY,
)

private const val PRESET_ONE = 1
private const val PRESET_FIVE = 5
private const val PRESET_TEN = 10
private const val PRESET_FIFTEEN = 15
private const val PRESET_TWENTY = 20
private const val MINIMUM_TOKEN_COUNT = PRESET_ONE
private const val MAXIMUM_TOKEN_COUNT = PRESET_TWENTY
private const val DIALOG_MAX_WIDTH_DP = 364
private const val DIALOG_CONTENT_PADDING_DP = 16
private const val DIALOG_ELEVATION_DP = 8
private const val DIALOG_CORNER_RADIUS_DP = 24
private const val DIALOG_CONTENT_WEIGHT = 1f
private const val WHEEL_WIDTH_DP = 96
private const val WHEEL_HEIGHT_DP = 132
private const val WHEEL_ITEM_HEIGHT_DP = 44
private const val WHEEL_ITEM_HORIZONTAL_PADDING_DP = 4
private const val SELECTED_COUNT_TEXT_SIZE_SP = 34
private const val SELECTED_COUNT_LINE_HEIGHT_SP = 40
private const val NEIGHBOR_COUNT_TEXT_SIZE_SP = 20
private const val NEIGHBOR_COUNT_LINE_HEIGHT_SP = 24
private const val QUICK_SELECTION_TOP_PADDING_DP = 8
private const val QUICK_SELECTION_CAPTION_PADDING_DP = 8
private const val PRESET_HEIGHT_DP = 38
private const val PRESET_MIN_WIDTH_DP = 38
private const val PRESET_HORIZONTAL_PADDING_DP = 12
private const val PRESET_CORNER_RADIUS_DP = 12
private const val FIRST_PAGE = 0
private const val NO_STEPS = 0
private const val RANGE_INCLUSIVE_ITEM_COUNT = 1
private const val MAX_PAGES_PER_FLING = 1
private const val DISABLED_ACTION_ALPHA = 0.38f
