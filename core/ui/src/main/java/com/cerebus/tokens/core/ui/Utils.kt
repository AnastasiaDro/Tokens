package com.cerebus.tokens.core.ui

import android.R
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.widget.ImageView
import java.io.File


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
