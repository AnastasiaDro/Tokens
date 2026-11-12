package com.cerebus.tokens.core.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cerebus.tokens.core.ui.theme.TokensTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PhotoPreviewTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun largeImageIsDownsampledAndKeepsAspectRatio() = runBlocking {
        val file = image()
        try {
            val result = loadPhotoPreview(compose.activity, Uri.fromFile(file).toString()) as PhotoPreviewState.Ready
            assertEquals(MAX_PREVIEW_EDGE_PX, result.bitmap.width)
            assertEquals(MAX_PREVIEW_EDGE_PX / ASPECT_RATIO, result.bitmap.height)
        } finally { file.delete() }
    }

    @Test fun invalidMissingAndNonLocalImagesAreUnavailable() = runBlocking {
        val file = File.createTempFile("invalid-preview", ".jpg", compose.activity.cacheDir)
        try {
            assertEquals(PhotoPreviewState.Unavailable, loadPhotoPreview(compose.activity, Uri.fromFile(file).toString()))
            file.delete()
            assertEquals(PhotoPreviewState.Unavailable, loadPhotoPreview(compose.activity, Uri.fromFile(file).toString()))
            assertEquals(PhotoPreviewState.Unavailable, loadPhotoPreview(compose.activity, "https://example.com/photo.jpg"))
            assertEquals(PhotoPreviewState.Unavailable, loadPhotoPreview(compose.activity, "content://missing.provider/photo"))
        } finally { file.delete() }
    }

    @Test fun uriChangesClearOldPreviewAndMissingImageShowsReselectionMessage() {
        val file = image()
        val uri = mutableStateOf<String?>(null)
        try {
            compose.setContent { TokensTheme { PhotoPreview(uri.value, DESCRIPTION, Modifier.size(PREVIEW_SIZE.dp)) } }
            compose.onNodeWithContentDescription(DESCRIPTION).assertIsDisplayed()
            compose.runOnIdle { uri.value = Uri.fromFile(file).toString() }
            compose.waitUntil(TIMEOUT_MS) { compose.onAllNodesWithTag(PHOTO_READY_TAG).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag(PHOTO_READY_TAG).assertIsDisplayed()
            compose.runOnIdle { uri.value = "content://missing.provider/another-photo" }
            compose.waitUntil(TIMEOUT_MS) { compose.onAllNodesWithTag(PHOTO_UNAVAILABLE_TAG).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag(PHOTO_READY_TAG).assertDoesNotExist()
            compose.onNodeWithText(compose.activity.getString(R.string.photo_unavailable)).assertIsDisplayed()
            compose.runOnIdle { uri.value = null }
            compose.onNodeWithTag(PHOTO_UNAVAILABLE_TAG).assertDoesNotExist()
            compose.onNodeWithContentDescription(DESCRIPTION).assertIsDisplayed()
        } finally { file.delete() }
    }

    @Test fun revokedAccessIsReadOffMainAndBecomesRecoverableUiError() {
        DeniedPhotoProvider.readAttempted.set(false)
        DeniedPhotoProvider.readOnMain.set(true)
        compose.setContent { TokensTheme {
            PhotoPreview(DeniedPhotoProvider.URI, DESCRIPTION, Modifier.size(PREVIEW_SIZE.dp))
        } }
        compose.waitUntil(TIMEOUT_MS) { compose.onAllNodesWithTag(PHOTO_UNAVAILABLE_TAG).fetchSemanticsNodes().isNotEmpty() }
        assertTrue(DeniedPhotoProvider.readAttempted.get())
        assertFalse(DeniedPhotoProvider.readOnMain.get())
        compose.onNodeWithText(compose.activity.getString(R.string.photo_unavailable)).assertIsDisplayed()
    }

    private fun image(): File {
        val file = File.createTempFile("preview", ".png", compose.activity.cacheDir)
        val bitmap = Bitmap.createBitmap(SOURCE_WIDTH, SOURCE_WIDTH / ASPECT_RATIO, Bitmap.Config.ARGB_8888)
        try { file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, IMAGE_QUALITY, it)) } }
        finally { bitmap.recycle() }
        return file
    }
    private companion object {
        const val SOURCE_WIDTH = 2048
        const val ASPECT_RATIO = 2
        const val IMAGE_QUALITY = 100
        const val PREVIEW_SIZE = 150
        const val TIMEOUT_MS = 5_000L
        const val DESCRIPTION = "Reinforcement photo"
    }
}
