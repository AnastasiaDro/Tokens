package presentation.tokens_screen

import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import com.cerebus.tokens.core.ui.theme.TokensDimensions
import com.cerebus.tokens.core.ui.theme.TokensTheme
import com.cerebus.tokens.feature.tokens_feature.R
import presentation.state.TokenShape
import presentation.state.TokenState

/**
 * Stateless token. The board owns key(state.id) and dispatches clicks by that ID.
 * Parent constraints take precedence over the preferred size, including in a small window.
 */
@Composable
internal fun Token(
    state: TokenState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState().value
    val scale = animateFloatAsState(
        targetValue = if (isPressed) PRESSED_SCALE else DEFAULT_SCALE,
        animationSpec = tween(PRESS_ANIMATION_DURATION_MILLIS),
        label = "tokenPressScale",
    ).value
    val description = stringResource(R.string.token_description)
    val checkedDescription = stringResource(
        if (state.checked) R.string.token_checked else R.string.token_unchecked,
    )
    val fillColor = if (state.checked) Color(state.color) else colorResource(R.color.baseColor)

    Box(
        modifier = modifier
            .sizeIn(
                minWidth = TokensDimensions.MinimumTouchTarget,
                minHeight = TokensDimensions.MinimumTouchTarget,
            )
            .semantics {
                contentDescription = description
                stateDescription = checkedDescription
            }
            .toggleable(
                value = state.checked,
                enabled = enabled,
                role = Role.Checkbox,
                interactionSource = interactionSource,
                indication = null,
                onValueChange = { onClick() },
            ),
        propagateMinConstraints = true,
    ) {
        Canvas(
            Modifier
                .size(dimensionResource(R.dimen.token_width))
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        ) {
            when (state.shape) {
                TokenShape.CIRCLE -> drawCircle(color = fillColor)
            }
        }
    }
}

private const val DEFAULT_SCALE = 1f
private const val PRESSED_SCALE = 1.08f
private const val PRESS_ANIMATION_DURATION_MILLIS = 300

@Preview(showBackground = true)
@Composable
private fun TokenPreview() {
    TokensTheme {
        Surface {
            Row(
                modifier = Modifier.padding(TokensDimensions.ContentPadding),
                horizontalArrangement = Arrangement.spacedBy(TokensDimensions.SmallSpacing),
            ) {
                val state = TokenState(
                    id = "preview-token",
                    shape = TokenShape.CIRCLE,
                    color = colorResource(R.color.checkedColor).toArgb(),
                    checked = false,
                )
                Token(state = state, onClick = {})
                Token(state = state.copy(id = "preview-checked-token", checked = true), onClick = {})
            }
        }
    }
}
