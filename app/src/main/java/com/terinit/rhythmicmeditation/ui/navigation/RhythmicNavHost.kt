package com.terinit.rhythmicmeditation.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.terinit.rhythmicmeditation.ui.components.CalmScreenBackground
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.terinit.rhythmicmeditation.ui.screens.completion.CompletionScreen
import com.terinit.rhythmicmeditation.ui.screens.evening.EveningScreen
import com.terinit.rhythmicmeditation.ui.screens.insights.InsightsScreen
import com.terinit.rhythmicmeditation.ui.screens.restorative.RestorativeChoiceScreen
import com.terinit.rhythmicmeditation.ui.screens.session.ActiveSessionScreen
import com.terinit.rhythmicmeditation.ui.screens.session.SessionsScreen
import com.terinit.rhythmicmeditation.ui.screens.settings.SettingsScreen
import com.terinit.rhythmicmeditation.ui.screens.today.TodayScreen

/**
 * Navigation shell for Rhythmic Meditation.
 *
 * Pass 1 wires every screen shell into a single NavHost with a bottom nav bar
 * on the top-level tabs. Screens navigate through lambda callbacks only —
 * no screen knows about NavController directly, which keeps them previewable
 * and testable.
 *
 * [pendingRoute] lets the Activity route a verified Routine recovery request
 * into the session UI without coupling the protocol layer to Compose.
 */
@Composable
fun RhythmicMeditationRoot(
    pendingRoute: StateFlow<String?> = MutableStateFlow(null),
    onRouteConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val routeToOpen by pendingRoute.collectAsStateWithLifecycle()
    LaunchedEffect(routeToOpen) {
        val route = routeToOpen ?: return@LaunchedEffect
        onRouteConsumed()
        navController.navigate(route) {
            popUpTo(AppRoute.Today.route)
        }
    }

    CalmScreenBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (currentRoute in AppRoute.topLevelRoutes) {
                    RhythmicBottomBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = AppRoute.Today.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                composable(AppRoute.Today.route) {
                    TodayScreen(
                        onStartSession = { navController.navigate(AppRoute.ActiveSession.route) },
                        onOpenRestorativeChoice = {
                            navController.navigate(AppRoute.RestorativeChoice.route)
                        },
                        onOpenEvening = { navController.navigate(AppRoute.Evening.route) },
                        onOpenInsights = { navController.navigate(AppRoute.Insights.route) }
                    )
                }
                composable(AppRoute.Sessions.route) {
                    SessionsScreen(
                        onOpenActiveSession = {
                            navController.navigate(AppRoute.ActiveSession.route)
                        }
                    )
                }
                composable(AppRoute.ActiveSession.route) {
                    ActiveSessionScreen(
                        onClose = { navController.popBackStack() },
                        onSessionCompleted = {
                            navController.navigate(AppRoute.Completion.route) {
                                popUpTo(AppRoute.Today.route)
                            }
                        },
                        onSessionCancelled = {
                            navController.navigate(AppRoute.Today.route) {
                                popUpTo(AppRoute.Today.route) { inclusive = true }
                            }
                        }
                    )
                }
                composable(AppRoute.Completion.route) {
                    CompletionScreen(
                        onReturnToToday = {
                            navController.navigate(AppRoute.Today.route) {
                                popUpTo(AppRoute.Today.route) { inclusive = true }
                            }
                        },
                        onViewInsights = {
                            navController.navigate(AppRoute.Insights.route) {
                                popUpTo(AppRoute.Today.route)
                            }
                        }
                    )
                }
                composable(AppRoute.Evening.route) {
                    EveningScreen(
                        onStartNow = { navController.navigate(AppRoute.ActiveSession.route) },
                        onSnooze = { navController.popBackStack() },
                        onDefer = { navController.popBackStack() }
                    )
                }
                composable(AppRoute.Insights.route) {
                    InsightsScreen()
                }
                composable(AppRoute.Settings.route) {
                    SettingsScreen()
                }
                composable(AppRoute.RestorativeChoice.route) {
                    RestorativeChoiceScreen(
                        onBeginMeditation = {
                            navController.navigate(AppRoute.ActiveSession.route)
                        },
                        onOpenReader = { /* Handoff to Rhythmic Reader in a later pass */ }
                    )
                }
            }
        }
    }
}
