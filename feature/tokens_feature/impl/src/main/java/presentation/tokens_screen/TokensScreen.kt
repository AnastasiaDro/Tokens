package presentation.tokens_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cerebus.tokens.core.ui.PhotoPreview
import com.cerebus.tokens.core.ui.theme.TokensDimensions
import com.cerebus.tokens.feature.tokens_feature.R
import presentation.state.StorageFailure
import presentation.state.TokenState
import com.cerebus.tokens.core.ui.R as CoreR

@Composable
internal fun TokensRoute(
    viewModel: TokensViewModel,
    onSelectCount: (Int) -> Unit,
    onSettings: () -> Unit,
    onPhoto: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
        TokensScreen(state, viewModel::onTokenClicked, viewModel::clearTokens,
            viewModel::retry, onSelectCount, onSettings, onPhoto)
        WinCelebration(state.effects, Modifier.matchParentSize())
    }
}

@Composable
internal fun TokensScreen(
    state: TokensUiState,
    onTokenClick: (String) -> Unit,
    onClear: () -> Unit,
    onRetry: () -> Unit,
    onSelectCount: (Int) -> Unit,
    onSettings: () -> Unit,
    onPhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ready = !state.loading && state.board != null && state.error != StorageFailure.READ
    val showPhoto = state.reinforcement?.enabled == true
    BoxWithConstraints(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()) {
        val statusMaxHeight = maxHeight / STATUS_HEIGHT_DIVISOR
        Column(Modifier.fillMaxSize()) {
            BoardMenu(ready, { state.board?.let { onSelectCount(it.count) } }, onClear, onSettings, showPhoto, onPhoto)
            if (state.loading || state.error != null) {
                TextButton(onClick = onRetry, enabled = state.error != null && !state.saving,
                    modifier = Modifier.heightIn(max = statusMaxHeight).verticalScroll(rememberScrollState())) {
                    Text(stringResource(when (state.error) {
                        StorageFailure.READ -> CoreR.string.storage_read_error
                        StorageFailure.WRITE -> CoreR.string.storage_write_error
                        null -> CoreR.string.storage_loading
                    }))
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth().weight(CONTENT_WEIGHT)) {
                val density = LocalDensity.current
                val preferredDiameter = dimensionResource(R.dimen.token_width)
                val plan = with(density) {
                    boardContentGeometry(maxWidth.roundToPx(), maxHeight.roundToPx(), state.board?.count ?: NO_SIZE,
                        preferredDiameter.roundToPx(), TokensDimensions.SmallSpacing.roundToPx(),
                        TokensDimensions.MinimumTouchTarget.roundToPx(),
                        maxWidth >= WIDE_WINDOW_MIN_WIDTH_DP.dp && maxHeight >= WIDE_WINDOW_MIN_HEIGHT_DP.dp,
                        showPhoto, PHOTO_MAX_SIZE_DP.dp.roundToPx())
                }
                Box(Modifier.fillMaxSize()) {
                    TokenBoard(
                        tokens = state.board?.tokens.orEmpty(),
                        enabled = ready,
                        onTokenClick = onTokenClick,
                        onClear = onClear,
                        modifier = with(density) { Modifier.size(plan.boardWidth.toDp(), plan.boardHeight.toDp()) },
                        plannedGeometry = plan.tokens,
                    )
                    if (plan.photoSize > NO_SIZE) {
                        ReinforcementPhoto(state.reinforcement?.photoUri, onPhoto,
                            Modifier.align(if (plan.photoBelow) Alignment.BottomCenter else Alignment.CenterEnd)
                                .size(with(density) { plan.photoSize.toDp() }))
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardMenu(
    enabled: Boolean, onSelectCount: () -> Unit, onClear: () -> Unit,
    onSettings: () -> Unit, showPhoto: Boolean, onPhoto: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val menuDescription = stringResource(R.string.tokens_menu)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Box {
            IconButton(onClick = { expanded = true }, modifier = Modifier.semantics { contentDescription = menuDescription }) {
                Text("⋮", style = MaterialTheme.typography.headlineMedium)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.changeChips)) }, enabled = enabled,
                    onClick = { expanded = false; onSelectCount() })
                DropdownMenuItem(text = { Text(stringResource(R.string.clearChecked)) }, enabled = enabled,
                    onClick = { expanded = false; onClear() })
                DropdownMenuItem(text = { Text(stringResource(R.string.settings)) },
                    onClick = { expanded = false; onSettings() })
                if (showPhoto) DropdownMenuItem(text = { Text(stringResource(R.string.reinforcement_image)) },
                    onClick = { expanded = false; onPhoto() })
            }
        }
    }
}

@Composable
internal fun TokenBoard(
    tokens: List<TokenState>,
    enabled: Boolean,
    onTokenClick: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    plannedGeometry: TokenBoardGeometry? = null,
) {
    val currentClear by rememberUpdatedState(onClear)
    val preferredDiameter = dimensionResource(R.dimen.token_width)
    Layout(
        modifier = modifier.clipToBounds().testTag(TOKEN_BOARD_TAG).pointerInput(enabled) {
            if (!enabled) return@pointerInput
            var drag = NO_DRAG
            detectHorizontalDragGestures(
                onDragStart = { drag = NO_DRAG },
                onHorizontalDrag = { _, amount -> drag += amount },
                onDragEnd = { if (drag < -size.width / CLEAR_SWIPE_DIVISOR) currentClear() },
                onDragCancel = { drag = NO_DRAG },
            )
        },
        content = {
            tokens.forEach { token ->
                key(token.id) {
                    Token(token, { onTokenClick(token.id) }, Modifier.testTag(tokenTag(token.id)), enabled)
                }
            }
        },
    ) { measurables, constraints ->
        val geometry = plannedGeometry ?: tokenBoardGeometry(constraints.maxWidth, constraints.maxHeight, tokens.size,
            preferredDiameter.roundToPx(), TokensDimensions.SmallSpacing.roundToPx(),
            TokensDimensions.MinimumTouchTarget.roundToPx())
        val children = measurables.map { it.measure(Constraints.fixed(geometry.diameter, geometry.diameter)) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            children.forEachIndexed { index, child ->
                val row = index / geometry.columns
                val column = index % geometry.columns
                val itemsInRow = minOf(geometry.columns, children.size - row * geometry.columns)
                val x = (constraints.maxWidth - geometry.rowWidth(itemsInRow)) / CENTER_DIVISOR +
                    column * (geometry.diameter + geometry.gap)
                val y = (constraints.maxHeight - geometry.height) / CENTER_DIVISOR +
                    row * (geometry.diameter + geometry.gap)
                child.placeRelative(x, y)
            }
        }
    }
}

@Composable
private fun ReinforcementPhoto(uri: String?, onClick: () -> Unit, modifier: Modifier) {
    val description = stringResource(R.string.reinforcement_image)
    Card(onClick = onClick, modifier = modifier.testTag(REINFORCEMENT_TAG).semantics { contentDescription = description }) {
        PhotoPreview(uri, description, Modifier.fillMaxSize())
    }
}

internal fun tokenTag(id: String) = "board-token-$id"
internal const val TOKEN_BOARD_TAG = "token-board"
internal const val REINFORCEMENT_TAG = "board-reinforcement"
private const val CONTENT_WEIGHT = 1f
private const val PHOTO_MAX_SIZE_DP = 150
private const val WIDE_WINDOW_MIN_WIDTH_DP = 840
private const val WIDE_WINDOW_MIN_HEIGHT_DP = 480
private const val NO_SIZE = 0
private const val NO_DRAG = 0f
private const val CLEAR_SWIPE_DIVISOR = 3f
private const val CENTER_DIVISOR = 2
private const val STATUS_HEIGHT_DIVISOR = 4
