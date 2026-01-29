package com.podify.app.util

import android.content.Context
import android.media.AudioManager
import android.util.Log
import android.view.KeyEvent

/**
 * Helper to control media playback based on AirPods wearing state.
 * Simulates media button presses.
 */
class MediaControlHelper(private val context: Context) {

    companion object {
        private const val TAG = "MediaControlHelper"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var wasPlayingBeforeRemove = false
    private var lastWearingState = AirPodsWearingState.UNKNOWN

    /**
     * Handle state updates to trigger play/pause
     */
    fun onStateChanged(status: AirPodsStatus) {
        val newState = status.wearingState
        
        // Skip if state hasn't changed
        if (newState == lastWearingState) return
        
        Log.d(TAG, "State changed: $lastWearingState -> $newState")
        
        // Skip logic if this is the first state update (from UNKNOWN)
        // to avoid auto-playing when the service just starts
        if (lastWearingState == AirPodsWearingState.UNKNOWN) {
            lastWearingState = newState
            return
        }
        
        // Logic for Auto Play/Pause
        // 1. If we went from NOT wearing to WEARING -> Play
        // 2. If we went from WEARING to NOT wearing -> Pause
        
        val wasWearing = isWearing(lastWearingState)
        val isWearing = isWearing(newState)
        
        if (!wasWearing && isWearing) {
            // Put in ear -> Play (if previously paused by us, or just try to resume)
            Log.d(TAG, "AirPods put in ear -> Resuming playback")
            resumePlayback()
        } else if (wasWearing && !isWearing) {
            // Took out of ear -> Pause
            Log.d(TAG, "AirPods taken out -> Pausing playback")
            if (audioManager.isMusicActive) {
                wasPlayingBeforeRemove = true
                pausePlayback()
            }
        }
        
        lastWearingState = newState
    }

    private fun isWearing(state: AirPodsWearingState): Boolean {
        return state == AirPodsWearingState.BOTH_IN_EAR ||
               state == AirPodsWearingState.LEFT_IN_EAR ||
               state == AirPodsWearingState.RIGHT_IN_EAR
    }

    private fun resumePlayback() {
        sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY)
    }

    private fun pausePlayback() {
        sendMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE)
    }

    private fun sendMediaKey(keyCode: Int) {
        try {
            val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            val eventUp = KeyEvent(KeyEvent.ACTION_UP, keyCode)
            
            audioManager.dispatchMediaKeyEvent(eventDown)
            audioManager.dispatchMediaKeyEvent(eventUp)
            Log.d(TAG, "Sent media key: $keyCode")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send media key", e)
        }
    }
}
