package dev.juanpe.habittracker.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.juanpe.habittracker.HabitApp
import dev.juanpe.habittracker.R
import dev.juanpe.habittracker.domain.model.Area
import dev.juanpe.habittracker.ui.AppViewModelProvider
import dev.juanpe.habittracker.ui.achievements.AchievementsScreen
import dev.juanpe.habittracker.ui.celebration.CelebrationHost
import dev.juanpe.habittracker.ui.detail.HabitDetailScreen
import dev.juanpe.habittracker.ui.habits.EditHabitScreen
import dev.juanpe.habittracker.ui.habits.HabitsScreen
import dev.juanpe.habittracker.ui.progress.ProgressScreen
import dev.juanpe.habittracker.ui.settings.SettingsScreen
import dev.juanpe.habittracker.ui.today.TodayScreen

/** Rutas de navegación y sus argumentos. */
object Routes {
    const val TODAY = "today"
    const val HABITS = "habits"
    const val PROGRESS = "progress"
    const val ACHIEVEMENTS = "achievements"
    const val SETTINGS = "settings"

    const val ARG_HABIT_ID = "habitId"
    const val ARG_AREA = "area"

    /** Valor de [ARG_HABIT_ID] que significa "hábito nuevo". */
    const val NO_HABIT = -1L

    const val HABIT_DETAIL = "habit/{$ARG_HABIT_ID}"
    const val HABIT_EDIT = "habit_edit?$ARG_HABIT_ID={$ARG_HABIT_ID}&$ARG_AREA={$ARG_AREA}"

    fun habitDetail(habitId: Long) = "habit/$habitId"

    /** Sin argumentos crea un hábito nuevo; con [area], ya preseleccionada. */
    fun habitEdit(habitId: Long = NO_HABIT, area: Area? = null): String {
        val areaPart = if (area != null) "&$ARG_AREA=${area.name}" else ""
        return "habit_edit?$ARG_HABIT_ID=$habitId$areaPart"
    }
}

/** Las cuatro pestañas de la barra inferior. */
private enum class TopLevelDestination(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    TODAY(Routes.TODAY, R.string.nav_today, Icons.Rounded.Today),
    HABITS(Routes.HABITS, R.string.nav_habits, Icons.AutoMirrored.Rounded.FormatListBulleted),
    PROGRESS(Routes.PROGRESS, R.string.nav_progress, Icons.Rounded.Insights),
    ACHIEVEMENTS(Routes.ACHIEVEMENTS, R.string.nav_achievements, Icons.Rounded.EmojiEvents),
}

private const val TRANSITION_MILLIS = 280

/**
 * Raíz de la interfaz: barra inferior, grafo de navegación y, por encima de
 * todo, las celebraciones a pantalla completa.
 */
@Composable
fun HabitTrackerApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = TopLevelDestination.entries.any { it.route == currentRoute }

    val container = (LocalContext.current.applicationContext as HabitApp).container
    val celebrations by container.celebrations.pending.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                // La barra se esconde en las pantallas secundarias (detalle, formulario, ajustes).
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = expandVertically(tween(TRANSITION_MILLIS)) + fadeIn(),
                    exit = shrinkVertically(tween(TRANSITION_MILLIS)) + fadeOut(),
                ) {
                    BottomBar(currentRoute = currentRoute, onNavigate = navController::navigateToTopLevel)
                }
            },
        ) { padding ->
            AppNavHost(
                navController = navController,
                // consumeWindowInsets avisa a las pantallas hijas de que las barras del sistema ya están resueltas.
                modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            )
        }

        CelebrationHost(celebration = celebrations.firstOrNull(), onDismiss = container.celebrations::dismiss)
    }
}

@Composable
private fun BottomBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { onNavigate(destination.route) },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(stringResource(destination.label), maxLines = 1) },
            )
        }
    }
}

/** Cambia de pestaña sin apilar pantallas y conservando el estado (scroll, etc.) de cada una. */
private fun NavHostController.navigateToTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    val factory = AppViewModelProvider.Factory
    val openHabit: (Long) -> Unit = { navController.navigate(Routes.habitDetail(it)) }

    NavHost(
        navController = navController,
        startDestination = Routes.TODAY,
        modifier = modifier,
        // Entre pestañas: fundido rápido con un ligero aumento.
        enterTransition = { fadeIn(tween(TRANSITION_MILLIS)) + scaleIn(tween(TRANSITION_MILLIS), initialScale = 0.97f) },
        exitTransition = { fadeOut(tween(140)) },
        popEnterTransition = { fadeIn(tween(TRANSITION_MILLIS)) },
        popExitTransition = { fadeOut(tween(140)) },
    ) {
        composable(Routes.TODAY) {
            TodayScreen(
                viewModel = viewModel(factory = factory),
                onOpenHabit = openHabit,
                onCreateHabit = { navController.navigate(Routes.habitEdit()) },
            )
        }
        composable(Routes.HABITS) {
            HabitsScreen(
                viewModel = viewModel(factory = factory),
                onOpenHabit = openHabit,
                onCreateHabit = { area -> navController.navigate(Routes.habitEdit(area = area)) },
            )
        }
        composable(Routes.PROGRESS) {
            ProgressScreen(
                viewModel = viewModel(factory = factory),
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.ACHIEVEMENTS) {
            AchievementsScreen(viewModel = viewModel(factory = factory))
        }

        // Pantallas secundarias: entran deslizándose desde la derecha y salen por donde vinieron.
        composable(
            route = Routes.HABIT_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_HABIT_ID) { type = NavType.LongType }),
            enterTransition = { slideInHorizontally(tween(TRANSITION_MILLIS)) { it / 5 } + fadeIn(tween(TRANSITION_MILLIS)) },
            popExitTransition = { slideOutHorizontally(tween(TRANSITION_MILLIS)) { it / 5 } + fadeOut(tween(200)) },
        ) {
            HabitDetailScreen(
                viewModel = viewModel(factory = factory),
                // popBackStack con ruta es seguro ante dobles toques: si ya se cerró, no hace nada.
                onBack = { navController.popBackStack(Routes.HABIT_DETAIL, inclusive = true) },
                onEdit = { navController.navigate(Routes.habitEdit(habitId = it)) },
            )
        }
        composable(
            route = Routes.SETTINGS,
            enterTransition = { slideInHorizontally(tween(TRANSITION_MILLIS)) { it / 5 } + fadeIn(tween(TRANSITION_MILLIS)) },
            popExitTransition = { slideOutHorizontally(tween(TRANSITION_MILLIS)) { it / 5 } + fadeOut(tween(200)) },
        ) {
            SettingsScreen(
                viewModel = viewModel(factory = factory),
                onBack = { navController.popBackStack(Routes.SETTINGS, inclusive = true) },
            )
        }

        // El formulario sube desde abajo, como una hoja que se abre.
        composable(
            route = Routes.HABIT_EDIT,
            arguments = listOf(
                navArgument(Routes.ARG_HABIT_ID) {
                    type = NavType.LongType
                    defaultValue = Routes.NO_HABIT
                },
                navArgument(Routes.ARG_AREA) {
                    type = NavType.StringType
                    nullable = true
                },
            ),
            enterTransition = { slideInVertically(tween(TRANSITION_MILLIS)) { it / 6 } + fadeIn(tween(TRANSITION_MILLIS)) },
            popExitTransition = { slideOutVertically(tween(TRANSITION_MILLIS)) { it / 6 } + fadeOut(tween(200)) },
        ) {
            EditHabitScreen(
                viewModel = viewModel(factory = factory),
                onClose = { navController.popBackStack(Routes.HABIT_EDIT, inclusive = true) },
            )
        }
    }
}
