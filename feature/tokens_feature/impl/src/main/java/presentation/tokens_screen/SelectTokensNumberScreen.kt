package presentation.tokens_screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cerebus.tokens.core.ui.theme.TokensDimensions
import com.cerebus.tokens.feature.tokens_feature.R
import presentation.state.SaveState
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
    val decrease = stringResource(R.string.decrease_tokens_count)
    val increase = stringResource(R.string.increase_tokens_count)
    Surface(modifier.widthIn(max = DIALOG_MAX_WIDTH_DP.dp), shape = MaterialTheme.shapes.medium) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(TokensDimensions.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(TokensDimensions.SmallSpacing),
        ) {
            Text(stringResource(R.string.changeChips), style = MaterialTheme.typography.titleMedium)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = { state.count?.let { onCountChange(it - COUNT_STEP) } },
                    enabled = state.editable && state.count != state.minimum,
                    modifier = Modifier.semantics { contentDescription = decrease },
                ) { Text("−") }
                Text(state.count?.toString().orEmpty(), modifier = Modifier.testTag(COUNT_VALUE_TAG),
                    style = MaterialTheme.typography.headlineMedium)
                Button(
                    onClick = { state.count?.let { onCountChange(it + COUNT_STEP) } },
                    enabled = state.editable && state.count != state.maximum,
                    modifier = Modifier.semantics { contentDescription = increase },
                ) { Text("+") }
            }
            if (state.save == SaveState.SAVING) Text(stringResource(R.string.settings_saving))
            if (state.save == SaveState.ERROR) {
                Text(stringResource(CoreR.string.storage_save_error), color = MaterialTheme.colorScheme.error)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCancel, enabled = state.editable) { Text(stringResource(CoreR.string.cancel)) }
                TextButton(onClick = onConfirm, enabled = state.editable) { Text(stringResource(CoreR.string.OK)) }
            }
        }
    }
}

internal const val COUNT_VALUE_TAG = "tokens-count-value"
private const val COUNT_STEP = 1
private const val DIALOG_MAX_WIDTH_DP = 360
