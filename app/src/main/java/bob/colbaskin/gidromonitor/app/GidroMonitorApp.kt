package bob.colbaskin.gidromonitor.app

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.hilt.navigation.compose.hiltViewModel
import bob.colbaskin.gidromonitor.design_system.theme.GidroMonitorTheme
import bob.colbaskin.gidromonitor.common.ui.GidroLoader
import bob.colbaskin.gidromonitor.features.analysis.presentation.NewAnalysisRoute
import bob.colbaskin.gidromonitor.features.comparison.presentation.ComparisonRoute
import bob.colbaskin.gidromonitor.features.events.presentation.EventsRoute
import bob.colbaskin.gidromonitor.features.onboarding.presentation.OnboardingRoute
import bob.colbaskin.gidromonitor.features.onboarding.presentation.OnboardingViewModel
import bob.colbaskin.gidromonitor.features.report.presentation.AnalyticsDetailsRoute
import bob.colbaskin.gidromonitor.navigation.Screens
import kotlinx.coroutines.delay

@Composable
fun GidroMonitorApp() {
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val onboardingState = onboardingViewModel.state
    var isLaunchIntroVisible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(1_200)
        isLaunchIntroVisible = false
    }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route.orEmpty()
    val showBottomBar = !route.contains("Onboarding") && !route.contains("Comparison")
    GidroMonitorTheme {
        if (onboardingState.isLoading || isLaunchIntroVisible) {
            LaunchIntro()
            return@GidroMonitorTheme
        }
        Scaffold(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically(animationSpec = tween(280)) { it / 2 } + fadeIn(tween(180)),
                    exit = slideOutVertically(animationSpec = tween(180)) { it / 2 } + fadeOut(tween(120))
                ) {
                    BottomNavigationBar(
                        analyticsSelected = route.contains("Analytics") || route.contains("AnalysisDetails"),
                        eventsSelected = route.contains("Events"),
                        onAnalyticsClick = {
                            navController.navigate(Screens.Analytics) {
                                popUpTo<Screens.Analytics> { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        onEventsClick = {
                            navController.navigate(Screens.Events) {
                                popUpTo<Screens.Analytics> { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        ) { padding ->
            NavigationGraph(
                navController = navController,
                padding = if (showBottomBar) padding else PaddingValues(),
                startDestination = if (onboardingState.isCompleted) Screens.Analytics else Screens.Onboarding
            )
        }
    }
}

@Composable
private fun LaunchIntro() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
    ) {
        GidroLoader(
            modifier = Modifier.align(Alignment.Center),
            size = 72.dp,
            showBackground = true
        )
    }
}

@Composable
private fun BottomNavigationBar(
    analyticsSelected: Boolean,
    eventsSelected: Boolean,
    onAnalyticsClick: () -> Unit,
    onEventsClick: () -> Unit
) {
    val surfaceColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = surfaceColor,
            shadowElevation = 10.dp
        ) {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = analyticsSelected,
                    onClick = onAnalyticsClick,
                    icon = { Icon(Icons.Outlined.Analytics, contentDescription = null) },
                    label = { Text("Аналитика") },
                    colors = navigationItemColors()
                )
                NavigationBarItem(
                    selected = eventsSelected,
                    onClick = onEventsClick,
                    icon = { Icon(Icons.Outlined.EventNote, contentDescription = null) },
                    label = { Text("Измерения") },
                    colors = navigationItemColors()
                )
            }
        }
    }
}

@Composable
private fun navigationItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
    selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
    unselectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
    disabledIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
    disabledTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
    indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
)

@Composable
private fun NavigationGraph(
    navController: androidx.navigation.NavHostController,
    padding: PaddingValues,
    startDestination: Screens
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.padding(padding),
        enterTransition = {
            fadeIn(animationSpec = tween(220)) +
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(220))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(160)) +
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(160))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(220)) +
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(220))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(160)) +
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(160))
        }
    ) {
        composable<Screens.Onboarding> {
            OnboardingRoute(
                onStartAnalysis = {
                    navController.navigate(Screens.Analytics) {
                        popUpTo<Screens.Onboarding> { inclusive = true }
                    }
                },
                onAlreadyCompleted = {
                    navController.navigate(Screens.Analytics) {
                        popUpTo<Screens.Onboarding> { inclusive = true }
                    }
                }
            )
        }
        composable<Screens.Analytics> {
            NewAnalysisRoute(
                onAnalysisReady = { analysisId -> navController.navigate(Screens.AnalysisDetails(analysisId)) }
            )
        }
        composable<Screens.Events> {
            EventsRoute(onOpenAnalysis = { analysisId -> navController.navigate(Screens.AnalysisDetails(analysisId)) })
        }
        composable<Screens.AnalysisDetails> { entry ->
            val analysisId = entry.toRoute<Screens.AnalysisDetails>().analysisId
            AnalyticsDetailsRoute(
                analysisId = analysisId,
                onBack = navController::popBackStack,
                onNewAnalysis = {
                    navController.navigate(Screens.Analytics) {
                        popUpTo<Screens.Analytics> { inclusive = true }
                    }
                },
                onOpenComparison = { id -> navController.navigate(Screens.Comparison(id)) }
            )
        }
        composable<Screens.Comparison> { entry ->
            ComparisonRoute(
                analysisId = entry.toRoute<Screens.Comparison>().analysisId,
                onBack = navController::popBackStack
            )
        }
    }
}
