package com.cerebus.tokens.reinforcement_photo.presentation

/**
 * [PermissionType] - a class for permission types.
 * Contains used in this module permission types and error messages for it
 * if a user doesn't give an asked permission
 *
 * @see AskForReinforcementImageDialog
 * @see ChangePhotoViewModel
 *
 * @author Anastasia Drogunova
 * @since 25.12.2023
 */
enum class PermissionType(val errorMessage: String) {
    WRITE_STORAGE_PERMISSION("Can't open camera without permission"),
    READ_STORAGE_PERMISSION("Can't open a gallery without permission"),
    CAMERA_PERMISSION("Can't open a camera without permission")
}