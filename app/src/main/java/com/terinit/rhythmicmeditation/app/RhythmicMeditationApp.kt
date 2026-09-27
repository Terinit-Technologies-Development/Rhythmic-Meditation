package com.terinit.rhythmicmeditation.app

import android.app.Application
import com.terinit.rhythmicmeditation.data.AppContainer

/**
 * Application entry point.
 *
 * Owns the manual dependency container ([AppContainer]). No DI framework is
 * introduced on purpose: the graph is small and explicit wiring keeps the
 * architecture narrow and testable.
 */
class RhythmicMeditationApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
