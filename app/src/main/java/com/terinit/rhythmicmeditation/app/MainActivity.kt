package com.terinit.rhythmicmeditation.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionKind
import com.terinit.rhythmicmeditation.domain.protocol.MeditationProtocol
import com.terinit.rhythmicmeditation.integration.contract.RecoveryOutcome
import com.terinit.rhythmicmeditation.integration.intent.MeditationIntents
import com.terinit.rhythmicmeditation.ui.navigation.AppRoute
import com.terinit.rhythmicmeditation.ui.navigation.RhythmicMeditationRoot
import com.terinit.rhythmicmeditation.ui.theme.RhythmicMeditationTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Single-activity host for the Compose navigation shell.
 *
 * Also the entry point for Routine recovery intents
 * ([com.terinit.rhythmicmeditation.domain.protocol.MeditationProtocol.ACTION_START_MEDITATION_RECOVERY]):
 * the request is parsed, protocol-validated, caller-verified (deny-by-default),
 * created/loaded idempotently, and then routed into the session UI.
 */
class MainActivity : ComponentActivity() {

    private val pendingRoute = MutableStateFlow<String?>(null)
    private val pendingRouteState = pendingRoute.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as RhythmicMeditationApp
        // Configuration changes recreate this Activity; that must never pause a
        // session — it only takes a safe-point checkpoint.
        app.container.runtimeController.onHostActivityRecreated()

        handleRecoveryIntent(intent)

        setContent {
            RhythmicMeditationTheme {
                RhythmicMeditationRoot(
                    pendingRoute = pendingRouteState,
                    onRouteConsumed = { pendingRoute.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleRecoveryIntent(intent)
    }

    private fun handleRecoveryIntent(intent: Intent?) {
        if (intent == null) return
        val app = application as RhythmicMeditationApp
        val container = app.container

        val parsed = MeditationIntents.parseStartRecoveryIntent(intent)
        if (parsed.isFailure) {
            // A recovery action with a malformed payload is rejected here —
            // nothing is created and the user gets a quiet, calm notice.
            if (intent.action == MeditationProtocol.ACTION_START_MEDITATION_RECOVERY) {
                showRejection()
            }
            return
        }
        val request = parsed.getOrThrow()

        // Caller identity where the platform attests it (e.g. startActivityForResult).
        // Unattested callers fail closed in CallerVerifier.
        val caller = container.callerIdentityResolver.identityOf(callingPackage)

        lifecycleScope.launch {
            container.recoveryRequestHandler.handle(request, caller)
                .onSuccess { outcome ->
                    when (outcome) {
                        is RecoveryOutcome.RouteToSession -> {
                            if (outcome.alreadyCompleted) {
                                pendingRoute.value = AppRoute.Completion.route
                                return@onSuccess
                            }

                            val kind = MeditationSessionKind.fromWire(request.sessionKind)
                            if (kind == null) {
                                showRejection()
                                return@onSuccess
                            }

                            // RecoveryRequestHandler persists the bound request
                            // idempotently. Adopt/start that exact session in
                            // the runtime before routing so the screen timer and
                            // evidence provider use the same session id and
                            // required duration.
                            container.runtimeController.startSession(
                                kind = kind,
                                requiredSeconds = request.requiredQualifiedSeconds,
                                rhythmicDayId = request.sourceRhythmicDayId,
                                sessionId = request.sessionId
                            ).onSuccess {
                                pendingRoute.value = AppRoute.ActiveSession.route
                            }.onFailure {
                                showRejection()
                            }
                        }
                    }
                }
                .onFailure { showRejection() }
        }
    }

    private fun showRejection() {
        Toast.makeText(
            this,
            "Meditation request could not be accepted.",
            Toast.LENGTH_SHORT
        ).show()
    }
}
