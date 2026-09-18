package com.cerebus.tokens.reinforcement_photo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cerebus.tokens.core.ui.theme.TokensTheme
import com.cerebus.tokens.reinforcement_photo.presentation.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun shortPhotoWindowScrollsToEveryAction() {
        compose.setContent { TokensTheme {
            Box(Modifier.size(320.dp, 200.dp)) { PhotoScreen(PhotoUiState(loading = false), {}, {}, {}, {}) }
        } }
        listOf(PHOTO_CANCEL_TAG, PHOTO_CAMERA_TAG, PHOTO_GALLERY_TAG).forEach {
            compose.onNodeWithTag(it).performScrollTo().assertIsDisplayed().assertIsEnabled()
        }
    }

    @Test fun readFailureAllowsRetryAndCancellationButNotSourceSelection() {
        val state = mutableStateOf(PhotoUiState())
        var retried = false
        var cancelled = false
        compose.setContent { TokensTheme {
            PhotoScreen(state.value, {}, {}, { retried = true }, { cancelled = true })
        } }
        listOf(PHOTO_CAMERA_TAG, PHOTO_GALLERY_TAG, PHOTO_CANCEL_TAG).forEach {
            compose.onNodeWithTag(it).performScrollTo().assertIsNotEnabled()
        }
        compose.runOnIdle { state.value = PhotoUiState(loading = false, readFailure = true) }
        listOf(PHOTO_CAMERA_TAG, PHOTO_GALLERY_TAG).forEach {
            compose.onNodeWithTag(it).performScrollTo().assertIsNotEnabled()
        }
        compose.onNodeWithTag(PHOTO_RETRY_TAG).performScrollTo().performClick()
        compose.onNodeWithTag(PHOTO_CANCEL_TAG).performScrollTo().performClick()
        compose.runOnIdle { assertTrue(retried); assertTrue(cancelled) }
    }

    @Test fun selectingSourceDoesNotKeepLocalSelectionAndAwaitingResultDisablesBothButtons() {
        val state = mutableStateOf(PhotoUiState(loading = false))
        var gallerySelected = false
        compose.setContent { TokensTheme { PhotoScreen(state.value, {}, { gallerySelected = true }, {}, {}) } }
        compose.onNodeWithTag(PHOTO_GALLERY_TAG).performScrollTo().performClick()
        compose.runOnIdle {
            assertTrue(gallerySelected)
            state.value = state.value.copy(awaiting = ImageSource.GALLERY)
        }
        listOf(PHOTO_CAMERA_TAG, PHOTO_GALLERY_TAG).forEach {
            compose.onNodeWithTag(it).performScrollTo().assertIsNotEnabled()
        }
        compose.runOnIdle { state.value = PhotoUiState(loading = false, sourceFailure = true) }
        compose.onNodeWithTag(PHOTO_GALLERY_TAG).performScrollTo().assertIsEnabled()
    }
}
