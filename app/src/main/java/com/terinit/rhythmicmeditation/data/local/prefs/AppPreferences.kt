package com.terinit.rhythmicmeditation.data.local.prefs

import com.terinit.rhythmicmeditation.domain.model.MeditationMode
import com.terinit.rhythmicmeditation.domain.protocol.MeditationProtocol

/**
 * Small app-level settings.
 *
 * Deliberately NOT the session ledger: session evidence lives in Room.
 * DataStore only holds lightweight preferences.
 */
data class AppPreferences(
    val sessionSoundEnabled: Boolean = true,
    val subtleHapticsEnabled: Boolean = true,
    val lastSelectedMeditationMode: MeditationMode = MeditationMode.STILLNESS,
    val onboardingComplete: Boolean = false,
    val lastOpenedTab: String = LastTab.TODAY,
    val pairedIntegrationEnabled: Boolean = false,
    val knownPairedPackageName: String? = null,
    val supportedProtocolVersion: Int = MeditationProtocol.PROTOCOL_VERSION
) {
    /** Stable ids for the bottom navigation tabs. */
    object LastTab {
        const val TODAY = "today"
        const val SESSIONS = "sessions"
        const val INSIGHTS = "insights"
        const val SETTINGS = "settings"
    }
}
