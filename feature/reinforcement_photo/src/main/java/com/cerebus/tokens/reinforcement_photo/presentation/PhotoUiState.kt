package com.cerebus.tokens.reinforcement_photo.presentation

enum class PhotoError { READ, WRITE }

data class PhotoUiState(
    val photoUri: String? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: PhotoError? = null,
)

