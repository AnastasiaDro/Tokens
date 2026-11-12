package com.cerebus.tokens.reinforcement_photo.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.tokens.data.reinforcement.ReinforcementRepository
import com.cerebus.tokens.reinforcement_photo.PhotoFiles
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChangePhotoViewModel(
    private val repository: ReinforcementRepository,
    private val files: PhotoFiles,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val mutableState = MutableStateFlow(PhotoUiState(
        saved = savedState[SAVED] ?: false,
        writeFailure = savedState.get<String>(SELECTION) != null,
        awaiting = savedState.get<String>(AWAITING)?.let(ImageSource::valueOf),
        launch = savedState.get<String>(LAUNCH)?.let(ImageSource::valueOf),
        captureUri = savedState[CAPTURE],
    ))
    val state = mutableState.asStateFlow()
    private var observation: Job? = null
    private var saveWhenReady = false

    init { observe() }

    private fun observe(retryPending: Boolean = false) {
        observation?.cancel()
        mutableState.update { it.copy(loading = true) }
        observation = viewModelScope.launch {
            var retry = retryPending
            try {
                repository.settings.collect { value ->
                    mutableState.update { it.copy(photoUri = value.photoUri, loading = false, readFailure = false) }
                    if (saveWhenReady || retry) {
                        saveWhenReady = false
                        retry = false
                        savePending()
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.update { it.copy(loading = false, readFailure = true) } }
        }
    }

    fun retry() {
        when {
            state.value.saving -> Unit
            state.value.readFailure -> observe(state.value.writeFailure)
            state.value.writeFailure -> savePending()
            else -> mutableState.update { it.copy(sourceFailure = false) }
        }
    }

    fun chooseGallery() {
        if (!state.value.canSelect) return
        waitFor(ImageSource.GALLERY)
    }

    fun takePhoto() {
        if (!state.value.canSelect) return
        mutableState.update { it.copy(saving = true, sourceFailure = false) }
        viewModelScope.launch {
            try {
                val uri = files.createCapture()
                savedState[CAPTURE] = uri
                mutableState.update { it.copy(captureUri = uri, saving = false) }
                waitFor(ImageSource.CAMERA)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.update { it.copy(saving = false, sourceFailure = true) } }
        }
    }

    private fun waitFor(source: ImageSource) {
        savedState[AWAITING] = source.name
        savedState[LAUNCH] = source.name
        mutableState.update { it.copy(awaiting = source, launch = source, sourceFailure = false) }
    }

    /** Acknowledge immediately before launcher.launch: recomposition/recreation must not relaunch. */
    fun consumeLaunch(): ImageSource? {
        val source = state.value.launch ?: return null
        savedState[LAUNCH] = null
        mutableState.update { it.copy(launch = null) }
        return source
    }

    fun sourceUnavailable() {
        state.value.captureUri?.let(files::discard)
        clearWaiting()
        mutableState.update { it.copy(sourceFailure = true) }
    }

    fun onCameraResult(success: Boolean) {
        if (state.value.awaiting != ImageSource.CAMERA) return
        val uri = state.value.captureUri
        clearWaiting()
        if (success && uri != null) select(uri, ImageSource.CAMERA)
        else uri?.let(files::discard)
    }

    fun onGalleryResult(uri: String?) {
        if (state.value.awaiting != ImageSource.GALLERY) return
        clearWaiting()
        if (uri != null) select(uri, ImageSource.GALLERY)
    }

    private fun clearWaiting() {
        savedState[AWAITING] = null
        savedState[LAUNCH] = null
        savedState[CAPTURE] = null
        mutableState.update { it.copy(awaiting = null, launch = null, captureUri = null) }
    }

    private fun select(uri: String, source: ImageSource) {
        savedState[SELECTION] = uri
        savedState[SOURCE] = source.name
        savedState[PREPARED] = null
        if (state.value.loading || state.value.readFailure) {
            saveWhenReady = true
            mutableState.update { it.copy(writeFailure = true) }
        } else savePending()
    }

    private fun savePending() {
        val selected = savedState.get<String>(SELECTION) ?: return
        if (state.value.loading || state.value.readFailure || state.value.saving || state.value.saved) return
        mutableState.update { it.copy(saving = true, writeFailure = false, sourceFailure = false) }
        viewModelScope.launch {
            try {
                val prepared = savedState.get<String>(PREPARED) ?: when (savedState.get<String>(SOURCE)) {
                    ImageSource.CAMERA.name -> selected.also { files.validateCapture(it) }
                    else -> files.importGallery(selected)
                }.also { savedState[PREPARED] = it }
                repository.setPhotoUri(prepared)
                if (savedState.get<String>(SOURCE) == ImageSource.GALLERY.name) files.releaseSource(selected)
                savedState[SAVED] = true
                savedState[SELECTION] = null
                savedState[PREPARED] = null
                mutableState.update { it.copy(saving = false, saved = true, writeFailure = false) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.update { it.copy(saving = false, writeFailure = true) } }
        }
    }

    fun cancel(): Boolean {
        if (state.value.loading || state.value.saving) return false
        // A restored operation may already have committed; never delete the current saved image.
        if (!state.value.readFailure && !state.value.saved) {
            val pending = state.value.captureUri ?: savedState.get<String>(PREPARED)
                ?: savedState.get<String>(SELECTION)?.takeIf { savedState.get<String>(SOURCE) == ImageSource.CAMERA.name }
            if (pending != null && pending != state.value.photoUri) files.discard(pending)
            val selected = savedState.get<String>(SELECTION)
            if (selected != null && selected != state.value.photoUri && savedState.get<String>(SOURCE) == ImageSource.GALLERY.name) {
                files.releaseSource(selected)
            }
        }
        clearWaiting()
        savedState[SELECTION] = null
        savedState[PREPARED] = null
        saveWhenReady = false
        return true
    }

    private companion object {
        const val CAPTURE = "photo.capture"
        const val AWAITING = "photo.awaiting"
        const val LAUNCH = "photo.launch"
        const val SELECTION = "photo.selection"
        const val SOURCE = "photo.source"
        const val PREPARED = "photo.prepared"
        const val SAVED = "photo.saved"
    }
}
