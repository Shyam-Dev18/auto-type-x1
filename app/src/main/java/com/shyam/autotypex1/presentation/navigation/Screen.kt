package com.shyam.autotypex1.presentation.navigation

/**
 * Sealed class defining all navigation destinations in the app.
 */
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Permissions : Screen("permissions")
    data object Home : Screen("home")
    data object AddDevice : Screen("add_device")
    data object Typing : Screen("typing")
    data object Scripts : Screen("scripts")
    data object ScriptEditor : Screen("script_editor?scriptId={scriptId}") {
        const val ARG_SCRIPT_ID = "scriptId"
        fun createRoute(scriptId: Long? = null): String =
            if (scriptId != null) "script_editor?$ARG_SCRIPT_ID=$scriptId" else "script_editor"
    }
    data object Settings : Screen("settings")
    data object About : Screen("about")
    data object Privacy : Screen("privacy")
}
