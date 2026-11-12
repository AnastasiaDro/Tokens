package presentation.tokens_screen

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import com.cerebus.tokens.feature.tokens_feature.R
import com.cerebus.tokens.logger.api.Logger

/** Owns one short fanfare; all calls are made on the main thread. */
internal interface WinSoundOutput {
    fun play()
    fun stop()
}

internal class WinSoundPlayer(context: Context, private val logger: Logger) : WinSoundOutput {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attributes)
        .setOnAudioFocusChangeListener({ change ->
            if (change < AudioManager.AUDIOFOCUS_NONE) stop()
        }, Handler(Looper.getMainLooper()))
        .build()
    private var player: MediaPlayer? = null
    private var hasFocus = false

    override fun play() {
        stop()
        try {
            hasFocus = audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            if (!hasFocus) {
                logger.w("Fanfare: audio focus denied")
                return
            }
            val nextPlayer = MediaPlayer.create(appContext, R.raw.fanfare, attributes, GENERATE_AUDIO_SESSION_ID)
            if (nextPlayer == null) {
                logger.e("Fanfare: MediaPlayer could not prepare the audio resource")
                stop()
                return
            }
            player = nextPlayer
            nextPlayer.setOnErrorListener { _, what, extra ->
                logger.e("Fanfare: playback error what=$what extra=$extra")
                stop()
                true
            }
            nextPlayer.setOnCompletionListener {
                logger.d("Fanfare: completed")
                stop()
            }
            nextPlayer.start()
            logger.d("Fanfare: started, mediaVolume=${audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)}")
        } catch (error: RuntimeException) {
            logger.e("Fanfare: ${error.javaClass.simpleName}: ${error.message}")
            stop()
        }
    }

    override fun stop() {
        player?.release()
        player = null
        if (hasFocus) {
            audioManager.abandonAudioFocusRequest(focusRequest)
            hasFocus = false
        }
    }

    private companion object {
        const val GENERATE_AUDIO_SESSION_ID = 0
    }
}
