package com.cerebus.tokens.core.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = TokensColors.Action,
    onPrimary = TokensColors.SwitchThumb,
    primaryContainer = TokensColors.Background,
    onPrimaryContainer = TokensColors.PrimaryText,
    secondary = TokensColors.SecondaryText,
    onSecondary = TokensColors.SwitchThumb,
    secondaryContainer = TokensColors.Background,
    onSecondaryContainer = TokensColors.PrimaryText,
    background = TokensColors.Background,
    onBackground = TokensColors.PrimaryText,
    surface = TokensColors.Background,
    onSurface = TokensColors.PrimaryText,
    onSurfaceVariant = TokensColors.SecondaryText,
    outline = TokensColors.Divider,
    outlineVariant = TokensColors.Divider,
)

private const val BODY_SIZE_SP = 16
private const val TITLE_SIZE_SP = 20
private const val LABEL_SIZE_SP = 12
private const val BODY_LINE_HEIGHT_SP = 24
private const val LABEL_LINE_HEIGHT_SP = 16

private val TokensTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = BODY_SIZE_SP.sp,
        lineHeight = BODY_LINE_HEIGHT_SP.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = TITLE_SIZE_SP.sp,
        lineHeight = BODY_LINE_HEIGHT_SP.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = LABEL_SIZE_SP.sp,
        lineHeight = LABEL_LINE_HEIGHT_SP.sp,
    ),
)

private val TokensShapes = Shapes(medium = RoundedCornerShape(TokensDimensions.CornerRadius))

object TokensComponentDefaults {
    @Composable
    fun switchColors(): SwitchColors = SwitchDefaults.colors(
        checkedThumbColor = TokensColors.SwitchThumb,
        checkedTrackColor = TokensColors.SwitchCheckedTrack,
        uncheckedThumbColor = TokensColors.SwitchThumb,
        uncheckedTrackColor = TokensColors.SwitchUncheckedTrack,
    )
}

/** Keeps the existing AppTheme's light appearance; no dynamic colors or window mutations. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokensTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides RippleConfiguration(color = TokensColors.PressedAction),
    ) {
        MaterialTheme(
            colorScheme = LightColors,
            typography = TokensTypography,
            shapes = TokensShapes,
            content = {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onBackground,
                    content = content,
                )
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TokensThemePreview() {
    TokensTheme {
        Surface {
            Column(Modifier.padding(TokensDimensions.ContentPadding)) {
                Text("Tokens", style = MaterialTheme.typography.titleMedium)
                Text("Настройки", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
