package com.terinit.rhythmicmeditation.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.terinit.rhythmicmeditation.ui.navigation.RhythmicMeditationRoot
import com.terinit.rhythmicmeditation.ui.theme.RhythmicMeditationTheme

/**
 * Single-activity host for the Compose navigation shell.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RhythmicMeditationTheme {
                RhythmicMeditationRoot()
            }
        }
    }
}
