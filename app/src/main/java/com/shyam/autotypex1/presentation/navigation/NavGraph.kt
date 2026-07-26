package com.shyam.autotypex1.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.shyam.autotypex1.presentation.about.AboutScreen
import com.shyam.autotypex1.presentation.adddevice.AddDeviceScreen
import com.shyam.autotypex1.presentation.adddevice.AddDeviceViewModel
import com.shyam.autotypex1.presentation.home.HomeScreen
import com.shyam.autotypex1.presentation.home.HomeViewModel
import com.shyam.autotypex1.presentation.permissions.PermissionsScreen
import com.shyam.autotypex1.presentation.privacy.PrivacyScreen
import com.shyam.autotypex1.presentation.scripteditor.ScriptEditorScreen
import com.shyam.autotypex1.presentation.scripteditor.ScriptEditorViewModel
import com.shyam.autotypex1.presentation.scripts.ScriptsScreen
import com.shyam.autotypex1.presentation.scripts.ScriptsViewModel
import com.shyam.autotypex1.presentation.settings.SettingsScreen
import com.shyam.autotypex1.presentation.settings.SettingsViewModel
import com.shyam.autotypex1.presentation.splash.SplashScreen
import com.shyam.autotypex1.presentation.typing.TypingScreen
import com.shyam.autotypex1.presentation.typing.TypingViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToPermissions = {
                    navController.navigate(Routes.PERMISSIONS) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.PERMISSIONS) {
            PermissionsScreen(
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.PERMISSIONS) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            val viewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = viewModel,
                onNavigateToScripts = { navController.navigate(Routes.SCRIPTS) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToTyping = { navController.navigate(Routes.TYPING) },
                onNavigateToAddDevice = { navController.navigate(Routes.ADD_DEVICE) },
                onNavigateToAbout = { navController.navigate(Routes.ABOUT) }
            )
        }

        composable(Routes.ADD_DEVICE) {
            val viewModel: AddDeviceViewModel = hiltViewModel()
            AddDeviceScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SCRIPTS) {
            val viewModel: ScriptsViewModel = hiltViewModel()
            ScriptsScreen(
                viewModel = viewModel,
                onNavigateToEditor = { id ->
                    navController.navigate(Routes.scriptEditor(id))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.SCRIPT_EDITOR,
            arguments = listOf(
                navArgument("id") {
                    type = NavType.StringType
                    defaultValue = "-1"
                }
            )
        ) {
            val viewModel: ScriptEditorViewModel = hiltViewModel()
            ScriptEditorScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.TYPING) {
            val viewModel: TypingViewModel = hiltViewModel()
            TypingScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ABOUT) {
            AboutScreen(
                onNavigateToPrivacy = { navController.navigate(Routes.PRIVACY) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PRIVACY) {
            PrivacyScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
