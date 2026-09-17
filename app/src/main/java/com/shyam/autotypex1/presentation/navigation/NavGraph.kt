package com.shyam.autotypex1.presentation.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shyam.autotypex1.presentation.about.AboutScreen
import com.shyam.autotypex1.presentation.adddevice.AddDeviceScreen
import com.shyam.autotypex1.presentation.home.HomeScreen
import com.shyam.autotypex1.presentation.permissions.PermissionsScreen
import com.shyam.autotypex1.presentation.privacy.PrivacyScreen
import com.shyam.autotypex1.presentation.scripteditor.ScriptEditorScreen
import com.shyam.autotypex1.presentation.scripts.ScriptsScreen
import com.shyam.autotypex1.presentation.settings.SettingsScreen
import com.shyam.autotypex1.presentation.splash.SplashScreen
import com.shyam.autotypex1.presentation.typing.TypingScreen

/**
 * Main navigation graph for AutoType X1.
 * Uses smooth, premium slide and fade transitions for a fluid UX.
 */
@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = { fadeIn(animationSpec = tween(320, easing = FastOutSlowInEasing)) },
        exitTransition = { fadeOut(animationSpec = tween(320, easing = FastOutSlowInEasing)) },
        popEnterTransition = { fadeIn(animationSpec = tween(320, easing = FastOutSlowInEasing)) },
        popExitTransition = { fadeOut(animationSpec = tween(320, easing = FastOutSlowInEasing)) }
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onAllPermissionsGranted = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNeedsPermissions = {
                    navController.navigate(Screen.Permissions.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Permissions.route) {
            PermissionsScreen(
                onAllPermissionsGranted = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Permissions.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToScripts = { navController.navigateSafely(Screen.Scripts.route) },
                onNavigateToSettings = { navController.navigateSafely(Screen.Settings.route) },
                onNavigateToTyping = { navController.navigateSafely(Screen.Typing.route) },
                onNavigateToAddDevice = { navController.navigateSafely(Screen.AddDevice.route) },
                onNavigateToAbout = { navController.navigateSafely(Screen.About.route) }
            )
        }

        composable(Screen.AddDevice.route) {
            AddDeviceScreen(
                onBack = { navController.popBackStackSafely() }
            )
        }

        composable(Screen.Typing.route) {
            TypingScreen(
                onBack = { navController.popBackStackSafely() }
            )
        }

        composable(Screen.Scripts.route) {
            ScriptsScreen(
                onNavigateToEditor = { scriptId ->
                    navController.navigateSafely(Screen.ScriptEditor.createRoute(scriptId))
                },
                onBack = { navController.popBackStackSafely() }
            )
        }

        composable(
            route = Screen.ScriptEditor.route,
            arguments = listOf(
                navArgument(Screen.ScriptEditor.ARG_SCRIPT_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            ScriptEditorScreen(
                onBack = { navController.popBackStackSafely() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStackSafely() }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(
                onNavigateToPrivacy = { navController.navigateSafely(Screen.Privacy.route) },
                onBack = { navController.popBackStackSafely() }
            )
        }

        composable(Screen.Privacy.route) {
            PrivacyScreen(
                onBack = { navController.popBackStackSafely() }
            )
        }
    }
}

/**
 * Guards against rapid duplicate back presses popping past the start destination or causing a blank screen.
 */
fun NavHostController.popBackStackSafely(): Boolean {
    val current = currentBackStackEntry
    if (current?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        if (currentDestination?.route != Screen.Home.route) {
            return popBackStack()
        }
    }
    return false
}

/**
 * Guards against rapid repeated clicks initiating multiple transitions simultaneously.
 */
fun NavHostController.navigateSafely(route: String) {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        navigate(route)
    }
}
