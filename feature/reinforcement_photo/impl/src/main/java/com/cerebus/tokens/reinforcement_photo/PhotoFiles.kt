package com.cerebus.tokens.reinforcement_photo

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

interface PhotoFiles {
    suspend fun createCapture(): String
    suspend fun importGallery(uri: String): String
    suspend fun validateCapture(uri: String)
    fun discard(uri: String)
    fun releaseSource(uri: String)
}

/** Own files only. Never deletes MediaStore/SAF content or the user's gallery original. */
class AndroidPhotoFiles(context: Context, private val authority: String = PHOTO_AUTHORITY) : PhotoFiles {
    private val context = context.applicationContext
    private val directory get() = File(context.filesDir, PHOTO_DIRECTORY)

    override suspend fun createCapture(): String = withContext(Dispatchers.IO) {
        val file = newFile(CAMERA_SUFFIX)
        try { photoUri(file) } catch (failure: Exception) { file.delete(); throw failure }
    }

    override suspend fun importGallery(uri: String): String = withContext(Dispatchers.IO) {
        val source = uri.toUri()
        // Best effort protects an interrupted import; the completed copy needs no external grant.
        try { context.contentResolver.takePersistableUriPermission(source, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        catch (_: SecurityException) { /* Some picker providers offer temporary access only. */ }
        catch (_: IllegalArgumentException) { /* The provider may not support persistent grants. */ }
        val file = newFile(IMPORT_SUFFIX)
        try {
            val input = context.contentResolver.openInputStream(source) ?: throw IOException("Image unavailable")
            input.use { stream -> file.outputStream().use { stream.copyTo(it) } }
            val result = photoUri(file)
            validateCapture(result)
            result
        } catch (failure: Exception) {
            file.delete()
            throw failure
        }
    }

    override suspend fun validateCapture(uri: String) = withContext(Dispatchers.IO) {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val input = context.contentResolver.openInputStream(uri.toUri()) ?: throw IOException("Image unavailable")
        input.use { BitmapFactory.decodeStream(it, null, options) }
        if (options.outWidth <= EMPTY_SIZE || options.outHeight <= EMPTY_SIZE) throw IOException("Invalid image")
    }

    override fun discard(uri: String) {
        val value = uri.toUri()
        if (value.scheme != "content" || value.authority != authority) return
        if (value.pathSegments.size != OWNED_PATH_SEGMENTS || value.pathSegments.first() != PHOTO_PATH_NAME) return
        val name = value.lastPathSegment ?: return
        if (!OWNED_FILE.matches(name)) return
        val file = File(directory, name)
        try { if (file.canonicalFile.parentFile == directory.canonicalFile) file.delete() }
        catch (_: IOException) { /* Best effort cleanup must not turn cancellation into a crash. */ }
        catch (_: SecurityException) { /* Only our private file was considered. */ }
    }

    override fun releaseSource(uri: String) {
        val source = uri.toUri()
        if (source.scheme != "content" || source.authority == authority) return
        try { context.contentResolver.releasePersistableUriPermission(source, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        catch (_: SecurityException) { /* Temporary grants have nothing to release. */ }
        catch (_: IllegalArgumentException) { /* Cleanup is optional after saving the private copy. */ }
    }

    private fun newFile(suffix: String): File {
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Cannot create image directory")
        return File.createTempFile("photo-${UUID.randomUUID()}-", suffix, directory)
    }
    private fun photoUri(file: File) = FileProvider.getUriForFile(context, authority, file).toString()

    private companion object {
        const val PHOTO_AUTHORITY = "com.cerebus.tokens.provider"
        const val PHOTO_DIRECTORY = "reinforcement"
        const val PHOTO_PATH_NAME = "reinforcement_photos"
        const val CAMERA_SUFFIX = ".jpg"
        const val IMPORT_SUFFIX = ".img"
        const val EMPTY_SIZE = 0
        const val OWNED_PATH_SEGMENTS = 2
        val OWNED_FILE = Regex("photo-[A-Za-z0-9-]+\\.(jpg|img)")
    }
}
