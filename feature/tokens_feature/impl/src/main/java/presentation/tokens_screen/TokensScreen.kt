package presentation.tokens_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cerebus.tokens.core.ui.PhotoPreview
import com.cerebus.tokens.core.ui.theme.TokensColors
import com.cerebus.tokens.core.ui.theme.TokensComponentDefaults
import com.cerebus.tokens.core.ui.theme.TokensDimensions
import com.cerebus.tokens.core.ui.theme.menuItem
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
    safeDrawingInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    val ready = !state.loading && state.board != null && state.error != StorageFailure.READ
    val showPhoto = state.reinforcement?.enabled == true
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TokensContent(state, ready, showPhoto, onTokenClick, onClear, onRetry, onPhoto, safeDrawingInsets)
        BoardMenu(ready, { state.board?.let { onSelectCount(it.count) } }, onClear, onSettings, showPhoto, onPhoto,
            Modifier.align(Alignment.TopEnd).windowInsetsPadding(safeDrawingInsets))
    }
}

@Composable
private fun TokensContent(
    state: TokensUiState,
    ready: Boolean,
    showPhoto: Boolean,
    onTokenClick: (String) -> Unit,
    onClear: () -> Unit,
    onRetry: () -> Unit,
    onPhoto: () -> Unit,
    safeDrawingInsets: WindowInsets,
) {
    BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(safeDrawingInsets)) {
        val layoutWidth = maxWidth
        val landscape = maxWidth > maxHeight
        val statusMaxHeight = maxHeight / STATUS_HEIGHT_DIVISOR
        // union takes the maximum per edge; the parent's consumed safe insets are not added again.
        val designInsets = WindowInsets(
            left = TokensDimensions.MediumSpacing, right = TokensDimensions.MediumSpacing)
        // Match the visible dots, not the edge of their larger icon or touch target.
        val menuEndInset = TokensDimensions.MinimumTouchTarget / MENU_CENTERING_DIVISOR - MENU_DOT_RADIUS_DP.dp
        val menuInsets = if (LocalLayoutDirection.current == LayoutDirection.Ltr) WindowInsets(right = menuEndInset)
        else WindowInsets(left = menuEndInset)
        val contentInsets = if (landscape && showPhoto) {
            safeDrawingInsets.add(menuInsets).union(designInsets)
        } else safeDrawingInsets.union(designInsets)
        Column(Modifier.fillMaxSize().windowInsetsPadding(contentInsets)) {
            if (state.loading || state.error != null) {
                TextButton(onClick = onRetry, enabled = state.error != null && !state.saving,
                    modifier = Modifier.padding(end = TokensDimensions.MinimumTouchTarget + TokensDimensions.SmallSpacing)
                        .heightIn(max = statusMaxHeight).verticalScroll(rememberScrollState())) {
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
                // Keep a centered side photo below the menu instead of shifting its right edge.
                val preferredPhotoSize = if (landscape) minOf(PHOTO_MAX_SIZE_DP.dp,
                    (maxHeight - TokensDimensions.MinimumTouchTarget * CENTER_DIVISOR).coerceAtLeast(NO_SIZE.dp))
                else PHOTO_MAX_SIZE_DP.dp
                fun planForWidth(width: Int) = with(density) {
                    boardContentGeometry(width, maxHeight.roundToPx(), state.board?.count ?: NO_SIZE,
                        preferredDiameter.roundToPx(), TokensDimensions.SmallSpacing.roundToPx(),
                        TokensDimensions.MinimumTouchTarget.roundToPx(),
                        layoutWidth >= WIDE_WINDOW_MIN_WIDTH_DP.dp && maxHeight >= WIDE_WINDOW_MIN_HEIGHT_DP.dp,
                        showPhoto, preferredPhotoSize.roundToPx(),
                        largePortraitLayout = layoutWidth >= TABLET_MIN_WIDTH_DP.dp && maxHeight > layoutWidth)
                }
                val widthPx = with(density) { maxWidth.roundToPx() }
                val heightPx = with(density) { maxHeight.roundToPx() }
                val fullPlan = planForWidth(widthPx)
                val controlSpace = TokensDimensions.MinimumTouchTarget + TokensDimensions.SmallSpacing
                val menuSizePx = with(density) { TokensDimensions.MinimumTouchTarget.roundToPx() }
                // Overlay the menu in free space; in a short window reserve a side strip, not a top row.
                val reserveMenuSide = fullPlan.overlapsTopEndControl(widthPx, heightPx, menuSizePx)
                val contentWidth = if (reserveMenuSide) (maxWidth - controlSpace).coerceAtLeast(NO_SIZE.dp) else maxWidth
                val plan = if (reserveMenuSide) planForWidth(with(density) { contentWidth.roundToPx() }) else fullPlan
                Box(Modifier.width(contentWidth).fillMaxHeight()) {
                    TokenBoard(
                        tokens = state.board?.tokens.orEmpty(),
                        enabled = ready,
                        onTokenClick = onTokenClick,
                        onClear = onClear,
                        modifier = with(density) { Modifier.size(plan.boardWidth.toDp(), plan.boardHeight.toDp()) },
                        plannedGeometry = plan.tokens,
                    )
                    if (plan.photoSize > NO_SIZE) {
                        val photoPosition = plan.photoTop?.let { top ->
                            Modifier.align(Alignment.TopCenter).offset(y = with(density) { top.toDp() })
                        } ?: Modifier.align(if (plan.photoBelow) Alignment.BottomCenter else Alignment.CenterEnd)
                        ReinforcementPhoto(state.reinforcement?.photoUri, onPhoto,
                            photoPosition.size(with(density) { plan.photoSize.toDp() }))
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
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val menuDescription = stringResource(R.string.tokens_menu)
    val menuVisualOffset =
        (TokensDimensions.MinimumTouchTarget - MENU_ICON_SIZE_DP.dp) / MENU_CENTERING_DIVISOR
    Box(modifier, contentAlignment = Alignment.CenterEnd) {
        Box(Modifier.offset(y = -menuVisualOffset)) {
            IconButton(onClick = { expanded = true }, modifier = Modifier
                .size(TokensDimensions.MinimumTouchTarget).semantics { contentDescription = menuDescription }) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(MENU_ICON_SIZE_DP.dp).testTag(TOKEN_MENU_ICON_TAG),
                )
            }
            CompactDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                BoardMenuItem(stringResource(R.string.changeChips), enabled) {
                    expanded = false
                    onSelectCount()
                }
                BoardMenuItem(stringResource(R.string.clearChecked), enabled) {
                    expanded = false
                    onClear()
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = TokensDimensions.MenuHorizontalPadding)
                        .testTag(TOKEN_MENU_DIVIDER_TAG),
                    thickness = TokensDimensions.MenuDividerThickness,
                    color = TokensColors.Divider,
                )
                BoardMenuItem(stringResource(R.string.settings)) {
                    expanded = false
                    onSettings()
                }
                if (showPhoto) BoardMenuItem(stringResource(R.string.reinforcement_image)) {
                    expanded = false
                    onPhoto()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!expanded) return
    val density = LocalDensity.current
    val positionProvider = remember(density) {
        TokenMenuPositionProvider(with(density) { TokensDimensions.SmallSpacing.roundToPx() })
    }
    CompositionLocalProvider(
        LocalRippleConfiguration provides RippleConfiguration(color = TokensColors.MenuPressed),
    ) {
        Popup(
            popupPositionProvider = positionProvider,
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true),
        ) {
            Surface(
                modifier = Modifier.width(TokensDimensions.MenuWidth).testTag(TOKEN_MENU_POPUP_TAG),
                shape = RoundedCornerShape(TokensDimensions.MenuCornerRadius),
                color = TokensColors.MenuSurface,
                shadowElevation = TokensDimensions.MenuElevation,
            ) {
                Column(
                    modifier = Modifier.padding(vertical = TokensDimensions.MenuVerticalPadding),
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun BoardMenuItem(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(text, style = MaterialTheme.typography.menuItem) },
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.heightIn(min = TokensDimensions.MenuItemMinHeight),
        colors = TokensComponentDefaults.menuItemColors(),
        contentPadding = PaddingValues(horizontal = TokensDimensions.MenuHorizontalPadding),
    )
}

private class TokenMenuPositionProvider(
    private val windowMargin: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val desiredX = when (layoutDirection) {
            LayoutDirection.Ltr -> anchorBounds.right - popupContentSize.width
            LayoutDirection.Rtl -> anchorBounds.left
        }
        return IntOffset(
            x = constrainedPopupPosition(desiredX, popupContentSize.width, windowSize.width, windowMargin),
            y = constrainedPopupPosition(anchorBounds.top, popupContentSize.height, windowSize.height, windowMargin),
        )
    }
}

private fun constrainedPopupPosition(
    desired: Int,
    contentSize: Int,
    windowSize: Int,
    windowMargin: Int,
): Int {
    val maxPosition = (windowSize - contentSize - windowMargin).coerceAtLeast(NO_SIZE)
    val minPosition = windowMargin.coerceAtMost(maxPosition)
    return desired.coerceIn(minPosition, maxPosition)
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
                    column * (geometry.diameter + geometry.horizontalGap)
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
internal const val TOKEN_MENU_ICON_TAG = "board-menu-icon"
internal const val TOKEN_MENU_POPUP_TAG = "board-menu-popup"
internal const val TOKEN_MENU_DIVIDER_TAG = "board-menu-divider"
private const val MENU_ICON_SIZE_DP = 24
// Visible circles in ic_more_vert.xml; their center matches the touch target's center.
private const val MENU_DOT_RADIUS_DP = 2
private const val MENU_CENTERING_DIVISOR = 2
private const val CONTENT_WEIGHT = 1f
private const val PHOTO_MAX_SIZE_DP = 150
private const val TABLET_MIN_WIDTH_DP = 600
private const val WIDE_WINDOW_MIN_WIDTH_DP = 840
private const val WIDE_WINDOW_MIN_HEIGHT_DP = 480
private const val NO_SIZE = 0
private const val NO_DRAG = 0f
private const val CLEAR_SWIPE_DIVISOR = 3f
private const val CENTER_DIVISOR = 2
private const val STATUS_HEIGHT_DIVISOR = 4
