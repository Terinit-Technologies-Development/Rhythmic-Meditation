package com.terinit.rhythmicmeditation.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.terinit.rhythmicmeditation.app.MainActivity
import com.terinit.rhythmicmeditation.app.RhythmicMeditationApp
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI/instrumentation coverage for the morning ritual flows
 * (run with: ./gradlew connectedAndroidTest).
 *
 * Covered here: Today -> Begin -> Active, Active -> Pause -> Resume,
 * End early -> confirmation, Active -> Complete -> Completion.
 *
 * Screen-off and process-death behavior cannot be proven faithfully by
 * instrumentation alone — see the physical-device QA checklist in the README.
 */
@RunWith(AndroidJUnit4::class)
class MorningFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val app: RhythmicMeditationApp
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun resetSessions() {
        // Deterministic starting point: no live sessions -> Today shows
        // "Morning Meditation Required".
        runBlocking {
            app.container.sessionRepository.observeSessions().first()
                .filter { it.isActiveLifecycle }
                .forEach { app.container.sessionService.cancelSession(it.sessionId) }
        }
    }

    @Test
    fun todayBeginSessionOpensActiveSession() {
        composeRule.onNodeWithText("Morning Meditation Required").assertIsDisplayed()
        composeRule.onNodeWithText("Begin Session").assertIsDisplayed()

        composeRule.onNodeWithText("Begin Session").performClick()

        composeRule.onNodeWithText("MORNING SESSION").assertIsDisplayed()
        composeRule.onNodeWithText("Pause").assertIsDisplayed()
        composeRule.onNodeWithText("End Session").assertIsDisplayed()
    }

    @Test
    fun activeSessionPauseAndResume() {
        composeRule.onNodeWithText("Begin Session").performClick()
        composeRule.onNodeWithText("Pause").assertIsDisplayed()

        composeRule.onNodeWithText("Pause").performClick()
        composeRule.onNodeWithText("Resume").assertIsDisplayed()

        composeRule.onNodeWithText("Resume").performClick()
        composeRule.onNodeWithText("Pause").assertIsDisplayed()
    }

    @Test
    fun endEarlyShowsCalmConfirmation() {
        composeRule.onNodeWithText("Begin Session").performClick()

        composeRule.onNodeWithText("End Session").performClick()

        // Calm confirmation — no shaming, no rewards language.
        composeRule.onNodeWithText("End this session?").assertIsDisplayed()
        composeRule.onNodeWithText("Your current meditation requirement will remain incomplete.")
            .assertIsDisplayed()

        // "Keep meditating" dismisses without ending.
        composeRule.onNodeWithText("Keep meditating").performClick()
        composeRule.onNodeWithText("Pause").assertIsDisplayed()

        // Confirming ends the session and returns to Today.
        composeRule.onNodeWithText("End Session").performClick()
        composeRule.onNodeWithText("End session").performClick()

        composeRule.onNodeWithText("Morning Meditation Required").assertIsDisplayed()
    }

    @Test
    fun completedSessionShowsCompletionScreen() {
        // A short REAL session (10s of monotonic time) so the engine completes
        // it on-device without waiting 30 minutes. 10s is long enough that the
        // live session is still open while the test navigates.
        runBlocking {
            app.container.runtimeController.startSession(
                kind = MeditationSessionKind.STANDALONE,
                requiredSeconds = 10
            )
        }

        composeRule.onNodeWithText("Sessions").performClick()
        composeRule.onNodeWithText("Open session").performClick()
        composeRule.onNodeWithText("OPEN SESSION").assertIsDisplayed()

        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodesWithText("Session complete")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Session complete").assertIsDisplayed()
        composeRule.onNodeWithText("Return to Today").assertIsDisplayed()
    }
}
