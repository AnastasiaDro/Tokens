package com.cerebus.tokens.reinforcement_photo

import android.app.Activity
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class PhotoFilesTest {
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val files = AndroidPhotoFiles(context, AUTHORITY)

    @Test fun captureUsesPrivateProviderAndTakePictureKeepsOutputUri() = runBlocking {
        val uri = files.createCapture()
        try {
            assertEquals(AUTHORITY, uri.toUri().authority)
            val intent = ActivityResultContracts.TakePicture().createIntent(context, uri.toUri())
            assertEquals(MediaStore.ACTION_IMAGE_CAPTURE, intent.action)
            @Suppress("DEPRECATION")
            assertEquals(uri.toUri(), intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT))
            assertFalse(ActivityResultContracts.TakePicture().parseResult(Activity.RESULT_CANCELED, null))
            try { files.validateCapture(uri); fail("Empty capture must fail") } catch (_: IOException) { }
            writeImage(uri)
            files.validateCapture(uri)
        } finally { files.discard(uri) }
    }

    @Test fun importedImageSurvivesOriginalRemovalAndReopening() = runBlocking {
        val source = files.createCapture()
        var imported: String? = null
        try {
            writeImage(source)
            imported = files.importGallery(source)
            assertNotEquals(source, imported)
            files.discard(source)
            AndroidPhotoFiles(context, AUTHORITY).validateCapture(imported)
        } finally {
            files.discard(source)
            imported?.let(files::discard)
        }
    }

    @Test fun cleanupRejectsForeignAuthorityAndPaths() = runBlocking {
        val uri = files.createCapture()
        try {
            writeImage(uri)
            files.discard(uri.replace(AUTHORITY, "another.provider"))
            files.discard(uri.replace("reinforcement_photos", "external_files"))
            files.discard("content://$AUTHORITY/reinforcement_photos/../photo-outside.jpg")
            files.validateCapture(uri)
        } finally { files.discard(uri) }
    }

    @Test fun invalidGalleryContentIsRejectedWithoutDeletingSource() = runBlocking {
        val source = files.createCapture()
        try {
            context.contentResolver.openOutputStream(source.toUri())!!.use { it.write(INVALID_IMAGE.toByteArray()) }
            try { files.importGallery(source); fail("Invalid image must fail") } catch (_: IOException) { }
            assertEquals(INVALID_IMAGE, context.contentResolver.openInputStream(source.toUri())!!.bufferedReader().use { it.readText() })
        } finally { files.discard(source) }
    }

    private fun writeImage(uri: String) {
        val bitmap = Bitmap.createBitmap(IMAGE_SIZE, IMAGE_SIZE, Bitmap.Config.ARGB_8888)
        try { context.contentResolver.openOutputStream(uri.toUri())!!.use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, it)) } }
        finally { bitmap.recycle() }
    }
    private companion object {
        const val AUTHORITY = "com.cerebus.tokens.reinforcement_photo.test.photos"
        const val IMAGE_SIZE = 16
        const val IMAGE_QUALITY = 90
        const val INVALID_IMAGE = "not an image"
    }
}
