package com.cerebus.tokens.core.ui

import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

object PermissionsManager {

    fun Fragment.createPermissionLauncher(successAction: () -> Unit, unsuccessAction: () -> Unit) =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) successAction.invoke() else unsuccessAction.invoke()
        }

    fun Fragment.isPermissionGranted(permission: String) =
        ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
}