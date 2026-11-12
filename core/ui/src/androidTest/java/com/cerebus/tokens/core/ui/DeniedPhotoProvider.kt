package com.cerebus.tokens.core.ui

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Looper
import android.os.ParcelFileDescriptor
import java.util.concurrent.atomic.AtomicBoolean

/** Simulates a grant revoked by another app while the saved URI still exists. */
class DeniedPhotoProvider : ContentProvider() {
    override fun onCreate() = true
    override fun getType(uri: Uri) = "image/jpeg"
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        readAttempted.set(true)
        readOnMain.set(Looper.myLooper() == Looper.getMainLooper())
        throw SecurityException("Test: photo access revoked")
    }
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = error("Read only")
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = error("Read only")
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = error("Read only")
    companion object {
        val readAttempted = AtomicBoolean()
        val readOnMain = AtomicBoolean()
        const val URI = "content://com.cerebus.tokens.core.ui.test.denied/photo"
    }
}
