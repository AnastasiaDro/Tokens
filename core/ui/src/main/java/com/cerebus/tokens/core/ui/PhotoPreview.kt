package com.cerebus.tokens.core.ui

import android.content.Context
import android.graphics.ImageDecoder
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.math.roundToInt

/** A bounded local-image preview. It never changes the saved URI or loads network resources. */
@Composable
fun PhotoPreview(uri: String?, contentDescription: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    // Reset immediately on a new URI: do not flash the previous photo while the next one loads.
    key(uri, context) {
        val state by produceState<PhotoPreviewState>(if (uri == null) PhotoPreviewState.Empty else PhotoPreviewState.Loading) {
            if (uri != null) value = loadPhotoPreview(context, uri)
        }
        Box(modifier.clipToBounds(), contentAlignment = Alignment.Center) {
            when (val photo = state) {
                is PhotoPreviewState.Ready -> Image(photo.bitmap, contentDescription,
                    Modifier.fillMaxSize().testTag(PHOTO_READY_TAG), contentScale = ContentScale.Crop)
                PhotoPreviewState.Unavailable -> Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.testTag(PHOTO_UNAVAILABLE_TAG)) {
                    Icon(painterResource(R.drawable.baseline_add_a_photo_24), contentDescription)
                    Text(stringResource(R.string.photo_unavailable), style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center,
                        maxLines = ERROR_MAX_LINES, overflow = TextOverflow.Ellipsis)
                }
                PhotoPreviewState.Empty -> Icon(painterResource(R.drawable.baseline_add_a_photo_24), contentDescription)
                PhotoPreviewState.Loading -> Text(stringResource(R.string.photo_loading), textAlign = TextAlign.Center)
            }
        }
    }
}

internal sealed interface PhotoPreviewState {
    data object Empty : PhotoPreviewState
    data object Loading : PhotoPreviewState
    data object Unavailable : PhotoPreviewState
    data class Ready(val bitmap: ImageBitmap) : PhotoPreviewState
}

internal suspend fun loadPhotoPreview(context: Context, uri: String): PhotoPreviewState = withContext(Dispatchers.IO) {
    try {
        val parsed = uri.toUri()
        if (parsed.scheme !in LOCAL_SCHEMES) return@withContext PhotoPreviewState.Unavailable
        val source = ImageDecoder.createSource(context.contentResolver, parsed)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longest = maxOf(info.size.width, info.size.height)
            if (longest > MAX_PREVIEW_EDGE_PX) {
                val scale = MAX_PREVIEW_EDGE_PX.toDouble() / longest
                decoder.setTargetSize((info.size.width * scale).roundToInt().coerceAtLeast(MIN_IMAGE_EDGE_PX),
                    (info.size.height * scale).roundToInt().coerceAtLeast(MIN_IMAGE_EDGE_PX))
            }
        }
        PhotoPreviewState.Ready(bitmap.asImageBitmap())
    } catch (_: IOException) {
        PhotoPreviewState.Unavailable
    } catch (_: SecurityException) {
        PhotoPreviewState.Unavailable
    } catch (_: IllegalArgumentException) {
        PhotoPreviewState.Unavailable
    }
}

internal const val MAX_PREVIEW_EDGE_PX = 1024
internal const val PHOTO_READY_TAG = "photo-preview-ready"
internal const val PHOTO_UNAVAILABLE_TAG = "photo-preview-unavailable"
private const val MIN_IMAGE_EDGE_PX = 1
private const val ERROR_MAX_LINES = 3
private val LOCAL_SCHEMES = setOf("content", "file", "android.resource")
