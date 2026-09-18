package com.cerebus.tokens.reinforcement_photo.presentation

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.cerebus.tokens.core.ui.NavigationDialog

@Composable
internal fun PhotoDestinationContent(viewModel: ChangePhotoViewModel, onClose: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val close by rememberUpdatedState(onClose)
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { viewModel.onGalleryResult(it?.toString()) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture(), viewModel::onCameraResult)
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.state.collect { value ->
                if (value.saved) close()
                else try {
                    when (viewModel.consumeLaunch()) {
                        ImageSource.GALLERY -> gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        ImageSource.CAMERA -> camera.launch(requireNotNull(value.captureUri).toUri())
                        null -> Unit
                    }
                } catch (_: ActivityNotFoundException) { viewModel.sourceUnavailable() }
                catch (_: SecurityException) { viewModel.sourceUnavailable() }
            }
        }
    }
    val cancel = { if (viewModel.cancel()) close() }
    NavigationDialog(cancel) {
        PhotoScreen(state, viewModel::takePhoto, viewModel::chooseGallery, viewModel::retry, cancel)
    }
}
