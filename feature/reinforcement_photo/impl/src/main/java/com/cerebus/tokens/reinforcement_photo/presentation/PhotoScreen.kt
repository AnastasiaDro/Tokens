package com.cerebus.tokens.reinforcement_photo.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cerebus.tokens.core.ui.PhotoPreview
import com.cerebus.tokens.core.ui.theme.TokensDimensions
import com.cerebus.tokens.reinforcement_photo.R
import com.cerebus.tokens.core.ui.R as CoreR

@Composable
internal fun PhotoRoute(viewModel: ChangePhotoViewModel, onCamera: () -> Unit, onGallery: () -> Unit,
    onRetry: () -> Unit, onCancel: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PhotoScreen(state, onCamera, onGallery, onRetry, onCancel)
}

@Composable
internal fun PhotoScreen(state: PhotoUiState, onCamera: () -> Unit, onGallery: () -> Unit,
    onRetry: () -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.widthIn(max = DIALOG_MAX_WIDTH_DP.dp), shape = MaterialTheme.shapes.medium) {
        Column {
            Box(Modifier.fillMaxWidth().heightIn(min = TokensDimensions.MinimumTouchTarget)
                .padding(horizontal = TokensDimensions.SmallSpacing)) {
                Text(stringResource(R.string.photo_title), style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                        .padding(horizontal = TokensDimensions.MinimumTouchTarget))
                IconButton(onClick = onCancel, enabled = state.cancellable,
                    modifier = Modifier.align(Alignment.CenterEnd).size(TokensDimensions.MinimumTouchTarget)
                        .testTag(PHOTO_CLOSE_TAG)) {
                    Icon(painterResource(R.drawable.ic_close), stringResource(R.string.close_photo_dialog),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(Modifier.weight(WEIGHT_FULL, fill = false).verticalScroll(rememberScrollState())
                .padding(start = TokensDimensions.ContentPadding, top = TokensDimensions.SmallSpacing,
                    end = TokensDimensions.ContentPadding, bottom = TokensDimensions.ContentPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TokensDimensions.SmallSpacing)) {
                PhotoPreview(state.photoUri, stringResource(R.string.photo_title), Modifier.size(PREVIEW_SIZE_DP.dp))
                when {
                    state.loading -> Text(stringResource(CoreR.string.storage_loading))
                    state.saving -> Text(stringResource(R.string.photo_saving))
                    state.error != null -> TextButton(onClick = onRetry, modifier = Modifier.testTag(PHOTO_RETRY_TAG)) {
                        Text(stringResource(when (state.error) {
                            PhotoError.READ -> CoreR.string.storage_read_error
                            PhotoError.WRITE -> CoreR.string.storage_write_error
                            else -> R.string.photo_source_unavailable
                        }))
                    }
                }
                Button(onClick = onGallery, enabled = state.canSelect,
                    modifier = Modifier.fillMaxWidth().testTag(PHOTO_GALLERY_TAG)) {
                    Text(stringResource(R.string.from_gallery))
                }
                Button(onClick = onCamera, enabled = state.canSelect,
                    modifier = Modifier.fillMaxWidth().testTag(PHOTO_CAMERA_TAG)) {
                    Text(stringResource(R.string.make_a_photo))
                }
            }
        }
    }
}

internal const val PHOTO_CAMERA_TAG = "photo-camera"
internal const val PHOTO_GALLERY_TAG = "photo-gallery"
internal const val PHOTO_CLOSE_TAG = "photo-close"
internal const val PHOTO_RETRY_TAG = "photo-retry"
private const val DIALOG_MAX_WIDTH_DP = 360
private const val PREVIEW_SIZE_DP = 150
private const val WEIGHT_FULL = 1f
