package com.cerebus.tokens.reinforcement_photo.presentation

enum class PermissionsEnum(val errorMessage: String) {
    WRITE_STORAGE_PERMISSION("Can't open camera without permission"),
    READ_STORAGE_PERMISSION("Can't open a gallery without permission"),
    CAMERA_PERMISSION("Can't open a camera without permission")
}