package com.cerebus.tokens.core.ui

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.io.File
import java.time.Duration


fun getRealPathFromURI(context: Context, uri: Uri): String? {
    val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)

    val path = if (cursor != null) {
        cursor.moveToFirst()
        val idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
        cursor.getString(idx)
    } else null
    cursor?.close()
    return (path)
}

fun getImageByFilePath(path: String): Bitmap? {
    val file = File(path)
    return if (file.exists())
        BitmapFactory.decodeFile(file.absolutePath)
    else null
}


fun Fragment.showToast(message: String, duration: ToastDuration = ToastDuration.LONG) {
    Toast.makeText(requireActivity(), message, duration.value).show()
}

enum class ToastDuration(val value: Int) {
    SHORT(Toast.LENGTH_SHORT),
    LONG(Toast.LENGTH_LONG)
}


