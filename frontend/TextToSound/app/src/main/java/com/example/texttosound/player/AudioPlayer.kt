package com.example.texttosound.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.util.Log
import java.io.File

class AudioPlayer(private val context: Context) {
    private var exoPlayer: ExoPlayer? = null
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying
    
    fun playAudioFile(file: File, onCompletion: () -> Unit = {}) {
        stop()
        
        exoPlayer = ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(file.toURI().toString())
            setMediaItem(mediaItem)
            
            addListener(object : androidx.media3.common.Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }
                
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                        _isPlaying.value = false
                        onCompletion()
                    }
                }
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    Log.e("AudioPlayer", "Player error: ${error.message}")
                    _isPlaying.value = false
                    onCompletion() // Skip broken block
                }
            })
            
            prepare()
            play()
        }
    }
    
    fun stop() {
        exoPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        exoPlayer = null
        _isPlaying.value = false
    }
}
