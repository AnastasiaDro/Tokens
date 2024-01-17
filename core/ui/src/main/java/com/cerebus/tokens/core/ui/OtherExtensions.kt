package com.cerebus.tokens.core.ui

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import androidx.annotation.DrawableRes
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException


fun ImageView.setPhotoImage(imageUri: Uri?, @DrawableRes defaultImage: Int) {
    this.setImageURI(null)

    if (imageUri != null && isFileExists(imageUri, this.context))
        this.setImageURI(imageUri)
    else
        setImageResource(defaultImage)
}

private fun isFileExists(uri: Uri, context: Context): Boolean {
    val contentResolver = context.contentResolver
    try {
        contentResolver.openInputStream(uri)?.use {
            return  true
        }
    } catch (e: FileNotFoundException) {
        Log.e("OtherExtensions", "File not found: ${e.message}")
    } catch (e: IOException) {
        Log.e("OtherUtils", "Error reading file: ${e.message}")
    }
    return false
}
