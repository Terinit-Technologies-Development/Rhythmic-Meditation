package com.terinit.rhythmicmeditation.app

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.terinit.rhythmicmeditation.data.AppContainer
import kotlinx.coroutines.launch

/**
 * Application entry point.
 *
 * Owns the manual dependency container ([AppContainer]) and translates
 * process-level Android signals into the meditation runtime:
 *
 * - process start -> conservative session recovery (never invents time)
 * - app foreground/background -> interactive backgrounding pauses,
 *   screen-off meditation keeps qualifying
 * - screen off/on -> safe-point checkpoints (screen-off keeps qualifying)
 *
 * Activity lifecycle is deliberately NOT used for pause decisions: a
 * configuration change recreates the Activity without the user leaving the
 * app, and screen-off can stop the Activity too.
 */
class RhythmicMeditationApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        registerProcessLifecycleObserver()
        registerScreenReceiver()

        // The narrow active-session foreground service exists ONLY while a
        // qualifying session is ACTIVE (evidence-mandated: the platform killed
        // the process mid-session during screen-off). Timing logic itself
        // stays in the runtime — the service is keep-alive only.
        container.applicationScope.launch {
            container.runtimeController.state.collect { state ->
                val active =
                    state.session?.status == com.terinit.rhythmicmeditation.domain.model.MeditationSessionStatus.ACTIVE
                if (active) {
                    com.terinit.rhythmicmeditation.runtime.ActiveSessionForegroundService.start(
                        this@RhythmicMeditationApp
                    )
                } else {
                    com.terinit.rhythmicmeditation.runtime.ActiveSessionForegroundService.stop(
                        this@RhythmicMeditationApp
                    )
                }
            }
        }

        // Sessions left ACTIVE by a previous process are restored
        // conservatively: only checkpointed time survives.
        container.applicationScope.launch {
            container.runtimeController.initializeAfterProcessStart()
            // Routine owns the Evening Wind-Down trigger: sync the narrow
            // evening projection on process start (fail-safe — an absent or
            // unreadable Routine simply means "not due").
            container.eveningMeditationController.syncFromSignal()
        }
    }

    private fun registerProcessLifecycleObserver() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                container.runtimeController.onAppForeground()
            }

            override fun onStop(owner: LifecycleOwner) {
                container.runtimeController.onAppBackground()
            }
        })
    }

    private fun registerScreenReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> container.runtimeController.onScreenOff()
                    Intent.ACTION_SCREEN_ON -> container.runtimeController.onScreenOn()
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        ContextCompat.registerReceiver(
            this,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }
}
