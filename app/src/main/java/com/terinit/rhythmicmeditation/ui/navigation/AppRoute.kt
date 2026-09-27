package com.terinit.rhythmicmeditation.ui.navigation

/**
 * App destinations.
 *
 * Top-level tabs (Today / Sessions / Insights / Settings) show the bottom
 * navigation bar; the remaining routes are focused full-screen flows.
 */
sealed class AppRoute(val route: String) {
    data object Today : AppRoute("today")
    data object Sessions : AppRoute("sessions")
    data object ActiveSession : AppRoute("active_session")
    data object Completion : AppRoute("completion")
    data object Evening : AppRoute("evening")
    data object Insights : AppRoute("insights")
    data object Settings : AppRoute("settings")
    data object RestorativeChoice : AppRoute("restorative_choice")

    companion object {
        /** Routes that keep the bottom navigation bar visible. */
        val topLevelRoutes = setOf(Today.route, Sessions.route, Insights.route, Settings.route)
    }
}
