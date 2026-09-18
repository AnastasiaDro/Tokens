package com.cerebus.tokens.reinforcement_photo.presentation

enum class PhotoError { READ, WRITE, SOURCE }

data class PhotoUiState(
    val photoUri: String? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val readFailure: Boolean = false,
    val writeFailure: Boolean = false,
    val sourceFailure: Boolean = false,
    val awaiting: ImageSource? = null,
    val launch: ImageSource? = null,
    val captureUri: String? = null,
) {
    val cancellable get() = !loading && !saving
    val canSelect get() = !loading && !saving && !saved && !readFailure && !writeFailure && awaiting == null
    val error: PhotoError?
        get() = when {
            readFailure -> PhotoError.READ
            writeFailure -> PhotoError.WRITE
            sourceFailure -> PhotoError.SOURCE
            else -> null
        }
}
