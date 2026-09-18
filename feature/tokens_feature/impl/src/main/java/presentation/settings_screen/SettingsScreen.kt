package presentation.settings_screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cerebus.tokens.core.ui.theme.TokensColors
import com.cerebus.tokens.core.ui.theme.TokensDimensions
import com.cerebus.tokens.feature.tokens_feature.R
import presentation.state.StorageFailure
import com.cerebus.tokens.core.ui.R as CoreR

@Composable
internal fun SettingsRoute(
    viewModel: SettingsViewModel,
    hasCamera: Boolean,
    onSelectCount: (Int) -> Unit,
    onSelectColor: () -> Unit,
    onYoutube: () -> Unit,
    onDonate: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        hasCamera = hasCamera,
        onSelectCount = { state.tokens?.let { onSelectCount(it.count) } },
        onSelectColor = onSelectColor,
        onAnimationChanged = viewModel::changeAnimation,
        onSoundChanged = viewModel::changeSound,
        onReinforcementChanged = viewModel::changeReinforcement,
        onRetry = viewModel::retry,
        onYoutube = onYoutube,
        onDonate = onDonate,
    )
}

@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    hasCamera: Boolean,
    onSelectCount: () -> Unit,
    onSelectColor: () -> Unit,
    onAnimationChanged: (Boolean) -> Unit,
    onSoundChanged: (Boolean) -> Unit,
    onReinforcementChanged: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onYoutube: () -> Unit,
    onDonate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val controls: @Composable () -> Unit = {
        SettingsControls(state, hasCamera, onSelectCount, onSelectColor,
            onAnimationChanged, onSoundChanged, onReinforcementChanged, onRetry)
    }
    BoxWithConstraints(
        modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(TokensDimensions.ContentPadding),
    ) {
        if (maxWidth >= TWO_PANE_MIN_WIDTH_DP.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(TokensDimensions.ContentPadding)) {
                Column(Modifier.weight(PANE_WEIGHT).verticalScroll(rememberScrollState())) { controls() }
                Column(Modifier.weight(PANE_WEIGHT).verticalScroll(rememberScrollState())) { AboutApp(onYoutube, onDonate) }
            }
        } else {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(TokensDimensions.ContentPadding),
            ) {
                controls()
                AboutApp(onYoutube, onDonate)
            }
        }
    }
}

@Composable
private fun SettingsControls(
    state: SettingsUiState,
    hasCamera: Boolean,
    onSelectCount: () -> Unit,
    onSelectColor: () -> Unit,
    onAnimationChanged: (Boolean) -> Unit,
    onSoundChanged: (Boolean) -> Unit,
    onReinforcementChanged: (Boolean) -> Unit,
    onRetry: () -> Unit,
) {
    val enabled = !state.loading && !state.saving && state.tokens != null && state.error != StorageFailure.READ
    Column(verticalArrangement = Arrangement.spacedBy(TokensDimensions.SmallSpacing)) {
        SectionTitle(stringResource(R.string.settings))
        when {
            state.error != null -> Button(onClick = onRetry, enabled = !state.saving && !state.loading) {
                Text(stringResource(if (state.error == StorageFailure.READ) CoreR.string.storage_read_error else CoreR.string.storage_write_error))
            }
            state.loading -> Text(stringResource(CoreR.string.storage_loading))
            state.saving -> Text(stringResource(R.string.settings_saving))
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TokensDimensions.SmallSpacing)) {
            Text(stringResource(R.string.changeChips), modifier = Modifier.weight(LABEL_WEIGHT))
            Text(state.tokens?.count?.toString().orEmpty(), Modifier.testTag(SETTINGS_COUNT_TAG))
            TextButton(onClick = onSelectCount, enabled = enabled) { Text(stringResource(CoreR.string.change)) }
        }
        HorizontalDivider()
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TokensDimensions.SmallSpacing)) {
            Text(stringResource(R.string.settings_tokens_color), modifier = Modifier.weight(LABEL_WEIGHT))
            state.tokens?.let {
                Box(Modifier.size(COLOR_PREVIEW_SIZE_DP.dp).background(Color(it.color), CircleShape))
            }
            TextButton(onClick = onSelectColor, enabled = enabled) { Text(stringResource(R.string.select_button_text)) }
        }
        HorizontalDivider()
        SettingsSwitch(stringResource(R.string.settings_animation), state.effects?.animation == true, enabled, onAnimationChanged)
        SettingsSwitch(stringResource(R.string.settings_sound), state.effects?.sound == true, enabled, onSoundChanged)
        // Preserve the existing gate until the separate photo/permissions migration.
        SettingsSwitch(stringResource(R.string.reinforcement_image), state.reinforcement?.enabled == true && hasCamera,
            enabled, onReinforcementChanged)
    }
}

@Composable
private fun SettingsSwitch(label: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = TokensDimensions.SmallSpacing),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TokensDimensions.SmallSpacing),
    ) {
        Text(label, Modifier.weight(LABEL_WEIGHT))
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colorResource(R.color.switchThumbColor),
                checkedTrackColor = colorResource(R.color.switchTrackColor),
                uncheckedTrackColor = colorResource(R.color.switchTrackColorOff),
            ),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Surface(shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Text(text, Modifier.padding(TokensDimensions.SmallSpacing), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun AboutApp(onYoutube: () -> Unit, onDonate: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(TokensDimensions.SmallSpacing)) {
        SectionTitle(stringResource(R.string.aboutAppTitle))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.me1), contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.size(AUTHOR_PHOTO_SIZE_DP.dp).clip(CircleShape))
            Column {
                TextButton(onClick = onYoutube) { Text(stringResource(R.string.youtube_link)) }
                TextButton(onClick = onDonate) { Text(stringResource(R.string.donate_link)) }
            }
        }
        Text(stringResource(R.string.aboutAppText), color = TokensColors.Text)
    }
}

internal const val SETTINGS_COUNT_TAG = "settings-token-count"
private const val TWO_PANE_MIN_WIDTH_DP = 600
private const val PANE_WEIGHT = 1f
private const val LABEL_WEIGHT = 1f
private const val COLOR_PREVIEW_SIZE_DP = 20
private const val AUTHOR_PHOTO_SIZE_DP = 80
