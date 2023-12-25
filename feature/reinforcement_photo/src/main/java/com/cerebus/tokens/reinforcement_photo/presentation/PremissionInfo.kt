package com.cerebus.tokens.reinforcement_photo.presentation

class PermissionInfo(
    val permissionType: PermissionsEnum,
    val onSuccess: () -> Unit,
   // val onUnSuccess: () -> Unit
)
