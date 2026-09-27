package com.terinit.rhythmicmeditation.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.terinit.rhythmicmeditation.domain.model.MeditationMode
import com.terinit.rhythmicmeditation.domain.protocol.MeditationProtocol
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "rhythmic_meditation_preferences"
)

/**
 * DataStore-backed settings store.
 *
 * Persisted keys are stable strings; renaming a key requires a migration.
 */
class AppPreferencesStore(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.appPreferencesDataStore)

    val preferences: Flow<AppPreferences> = dataStore.data.map { prefs ->
        AppPreferences(
            sessionSoundEnabled = prefs[Keys.SESSION_SOUND_ENABLED] ?: true,
            subtleHapticsEnabled = prefs[Keys.SUBTLE_HAPTICS_ENABLED] ?: true,
            lastSelectedMeditationMode = MeditationMode.fromWire(
                prefs[Keys.LAST_SELECTED_MEDITATION_MODE]
            ) ?: MeditationMode.STILLNESS,
            onboardingComplete = prefs[Keys.ONBOARDING_COMPLETE] ?: false,
            lastOpenedTab = prefs[Keys.LAST_OPENED_TAB] ?: AppPreferences.LastTab.TODAY,
            pairedIntegrationEnabled = prefs[Keys.PAIRED_INTEGRATION_ENABLED] ?: false,
            knownPairedPackageName = prefs[Keys.KNOWN_PAIRED_PACKAGE_NAME],
            supportedProtocolVersion =
                prefs[Keys.SUPPORTED_PROTOCOL_VERSION] ?: MeditationProtocol.PROTOCOL_VERSION
        )
    }

    suspend fun setSessionSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.SESSION_SOUND_ENABLED] = enabled }
    }

    suspend fun setSubtleHapticsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.SUBTLE_HAPTICS_ENABLED] = enabled }
    }

    suspend fun setLastSelectedMeditationMode(mode: MeditationMode) {
        dataStore.edit { it[Keys.LAST_SELECTED_MEDITATION_MODE] = mode.name }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }

    suspend fun setLastOpenedTab(tab: String) {
        dataStore.edit { it[Keys.LAST_OPENED_TAB] = tab }
    }

    suspend fun setPairedIntegrationEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.PAIRED_INTEGRATION_ENABLED] = enabled }
    }

    suspend fun setKnownPairedPackageName(packageName: String?) {
        dataStore.edit { prefs ->
            if (packageName == null) {
                prefs.remove(Keys.KNOWN_PAIRED_PACKAGE_NAME)
            } else {
                prefs[Keys.KNOWN_PAIRED_PACKAGE_NAME] = packageName
            }
        }
    }

    private object Keys {
        val SESSION_SOUND_ENABLED = booleanPreferencesKey("session_sound_enabled")
        val SUBTLE_HAPTICS_ENABLED = booleanPreferencesKey("subtle_haptics_enabled")
        val LAST_SELECTED_MEDITATION_MODE = stringPreferencesKey("last_selected_meditation_mode")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val LAST_OPENED_TAB = stringPreferencesKey("last_opened_tab")
        val PAIRED_INTEGRATION_ENABLED = booleanPreferencesKey("paired_integration_enabled")
        val KNOWN_PAIRED_PACKAGE_NAME = stringPreferencesKey("known_paired_package_name")
        val SUPPORTED_PROTOCOL_VERSION = intPreferencesKey("supported_protocol_version")
    }
}
