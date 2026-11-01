package com.example.durtlesreborn.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

/**
 * Plays a pronunciation clip from the local [AudioCache], fetching it first when it has not been
 * played before. A second call interrupts the clip in progress.
 */
class AudioPlayer(
    private val scope: CoroutineScope,
    private val cache: AudioCache,
) {
    private var player: MediaPlayer? = null
    private var download: Job? = null

    fun play(url: String, level: Int) {
        download?.cancel()
        download = scope.launch {
            val file = cache.fetch(url, level) ?: return@launch
            start(file)
        }
    }

    /** Stops playback and any download in progress. */
    fun stop() {
        download?.cancel()
        download = null
        player?.release()
        player = null
    }

    private fun start(file: File) {
        player?.release()
        val media = MediaPlayer()
        player = media
        media.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build(),
        )
        media.setOnPreparedListener { it.start() }
        media.setOnCompletionListener {
            it.release()
            if (player === it) player = null
        }
        runCatching {
            media.setDataSource(file.absolutePath)
            media.prepareAsync()
        }.onFailure {
            media.release()
            if (player === media) player = null
        }
    }
}
