package com.example.ui.viewmodel

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.util.Log

class CallSoundPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    fun startRinging() {
        try {
            stop()
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                isLooping = true
                prepare()
                start()
            }
            Log.d("CallSoundPlayer", "Started loop playing incoming ringtone")
        } catch (e: Exception) {
            Log.e("CallSoundPlayer", "Error playing incoming ringtone", e)
        }
    }

    fun startCallingSound() {
        try {
            stop()
            // Using system default notification sound loop to emulate the outbound calling tone ringback nicely
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                isLooping = true
                prepare()
                start()
            }
            Log.d("CallSoundPlayer", "Started loop playing outgoing dialback calling tone")
        } catch (e: Exception) {
            Log.e("CallSoundPlayer", "Error playing outgoing dial tone", e)
        }
    }

    fun stop() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
            Log.d("CallSoundPlayer", "Stopped sound player")
        } catch (e: Exception) {
            Log.e("CallSoundPlayer", "Error stopping MediaPlayer", e)
        }
    }
}
