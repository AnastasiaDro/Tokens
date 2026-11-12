package presentation.settings_screen

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cerebus.tokens.core.ui.theme.TokensComponentDefaults
import com.cerebus.tokens.core.ui.theme.TokensDimensions
import com.cerebus.tokens.core.ui.theme.link
import com.cerebus.tokens.feature.tokens_feature.R
import presentation.state.StorageFailure
import com.cerebus.tokens.core.ui.R as CoreR

@Composable
internal fun SettingsRoute(
    viewModel: SettingsViewModel,
    onNavigate: (SettingsNavigator.Destination) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnNavigate by rememberUpdatedState(onNavigate)
    DisposableEffect(viewModel.navigator) {
        val binding = viewModel.navigator.bind { currentOnNavigate(it) }
        onDispose { binding.close() }
    }
    SettingsScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val verticalSpacing = if (isLandscape) SETTINGS_COMPACT_SPACING else TokensDimensions.SmallSpacing
    val verticalContentPadding = if (isLandscape) SETTINGS_COMPACT_SPACING else TokensDimensions.ContentPadding
    val controls: @Composable () -> Unit = {
        SettingsControls(state, onAction, verticalSpacing)
    }
    BoxWithConstraints(
        modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = TokensDimensions.ContentPadding, vertical = verticalContentPadding),
    ) {
        if (maxWidth >= TWO_PANE_MIN_WIDTH_DP.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(TokensDimensions.ContentPadding)) {
                Column(Modifier.weight(PANE_WEIGHT).verticalScroll(rememberScrollState())) { controls() }
                Column(Modifier.weight(PANE_WEIGHT).verticalScroll(rememberScrollState())) {
                    AboutApp(onAction, verticalSpacing)
                }
            }
        } else {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            ) {
                controls()
                AboutApp(onAction, verticalSpacing)
            }
        }
    }
}

@Composable
private fun SettingsControls(
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    verticalSpacing: Dp,
) {
    val enabled = state.editable
    Column(verticalArrangement = Arrangement.spacedBy(verticalSpacing)) {
        SectionTitle(stringResource(R.string.settings), verticalSpacing)
        when {
            state.error != null -> Button(onClick = { onAction(SettingsAction.RetryClicked) }, enabled = !state.saving && !state.loading) {
                Text(stringResource(if (state.error == StorageFailure.READ) CoreR.string.storage_read_error else CoreR.string.storage_write_error))
            }
            state.loading -> Text(stringResource(CoreR.string.storage_loading))
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TokensDimensions.SmallSpacing)) {
            Text(stringResource(R.string.changeChips), modifier = Modifier.weight(LABEL_WEIGHT))
            Text(state.tokens?.count?.toString().orEmpty(), Modifier.testTag(SETTINGS_COUNT_TAG))
            SettingsActionButton(
                text = stringResource(CoreR.string.change),
                enabled = enabled,
                onClick = { onAction(SettingsAction.SelectCountClicked) },
            )
        }
        HorizontalDivider()
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TokensDimensions.SmallSpacing)) {
            Text(stringResource(R.string.settings_tokens_color), modifier = Modifier.weight(LABEL_WEIGHT))
            state.tokens?.let {
                Box(Modifier.size(COLOR_PREVIEW_SIZE_DP.dp).background(Color(it.color), CircleShape)
                    .testTag(SETTINGS_COLOR_TAG))
            }
            SettingsActionButton(
                text = stringResource(R.string.select_button_text),
                enabled = enabled,
                onClick = { onAction(SettingsAction.SelectColorClicked) },
            )
        }
        HorizontalDivider()
        SettingsSwitch(stringResource(R.string.settings_animation), state.effects?.animation, enabled,
            { onAction(SettingsAction.AnimationChanged(it)) }, verticalSpacing)
        SettingsSwitch(stringResource(R.string.settings_sound), state.effects?.sound, enabled,
            { onAction(SettingsAction.SoundChanged(it)) }, verticalSpacing)
        SettingsSwitch(stringResource(R.string.reinforcement_image), state.reinforcement?.enabled,
            enabled, { onAction(SettingsAction.ReinforcementChanged(it)) }, verticalSpacing)
    }
}

@Composable
private fun SettingsActionButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val actionColor = MaterialTheme.colorScheme.primary
    TextButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = actionColor,
            disabledContentColor = actionColor,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.link)
    }
}

@Composable
private fun SettingsSwitch(
    label: String,
    checked: Boolean?,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
    verticalSpacing: Dp,
) {
    Row(
        Modifier.fillMaxWidth().then(
            if (checked != null) Modifier.toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            else Modifier,
        )
            .padding(vertical = verticalSpacing),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TokensDimensions.SmallSpacing),
    ) {
        Text(label, Modifier.weight(LABEL_WEIGHT))
        if (checked == null) {
            Spacer(Modifier.size(width = SWITCH_WIDTH_DP.dp, height = SWITCH_HEIGHT_DP.dp))
        } else Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = TokensComponentDefaults.switchColors(),
        )
    }
}

@Composable
private fun SectionTitle(text: String, verticalPadding: Dp) {
    Surface(shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Text(text, Modifier.padding(horizontal = TokensDimensions.SmallSpacing, vertical = verticalPadding),
            style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun AboutApp(onAction: (SettingsAction) -> Unit, verticalSpacing: Dp) {
    Column(verticalArrangement = Arrangement.spacedBy(verticalSpacing)) {
        SectionTitle(stringResource(R.string.aboutAppTitle), verticalSpacing)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.me1), contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.size(AUTHOR_PHOTO_SIZE_DP.dp).clip(CircleShape))
            Column {
                TextButton(onClick = { onAction(SettingsAction.YoutubeClicked) }, contentPadding = LINK_CONTENT_PADDING) {
                    Text(stringResource(R.string.youtube_link),
                        style = MaterialTheme.typography.link,
                        modifier = Modifier.offset(y = LINK_TEXT_OFFSET))
                }
                TextButton(onClick = { onAction(SettingsAction.OtherAppsClicked) }, contentPadding = LINK_CONTENT_PADDING) {
                    Text(stringResource(R.string.other_apps),
                        style = MaterialTheme.typography.link,
                        modifier = Modifier.offset(y = -LINK_TEXT_OFFSET))
                }
            }
        }
        Text(stringResource(R.string.aboutAppText))
    }
}

internal const val SETTINGS_COUNT_TAG = "settings-token-count"
internal const val SETTINGS_COLOR_TAG = "settings-token-color"
private const val TWO_PANE_MIN_WIDTH_DP = 600
private const val PANE_WEIGHT = 1f
private const val LABEL_WEIGHT = 1f
private const val COLOR_PREVIEW_SIZE_DP = 20
private const val SWITCH_WIDTH_DP = 52
private const val SWITCH_HEIGHT_DP = 32
private const val AUTHOR_PHOTO_SIZE_DP = 80
private const val COMPACT_SPACING_DIVISOR = 2
private val SETTINGS_COMPACT_SPACING = TokensDimensions.SmallSpacing / COMPACT_SPACING_DIVISOR
private val LINK_CONTENT_PADDING = PaddingValues(
    horizontal = TokensDimensions.SmallSpacing,
    vertical = SETTINGS_COMPACT_SPACING,
)
private val LINK_TEXT_OFFSET = SETTINGS_COMPACT_SPACING
