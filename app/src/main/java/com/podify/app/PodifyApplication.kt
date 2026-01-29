package com.podify.app

import android.app.Application

/**
 * Main Application class for Podify
 */
class PodifyApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Simple application - no complex DI needed for this app
    }
}
