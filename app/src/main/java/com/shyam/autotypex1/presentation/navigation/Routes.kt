package com.shyam.autotypex1.presentation.navigation

/** Route constants for all screens. */
object Routes {
    const val SPLASH = "splash"
    const val PERMISSIONS = "permissions"
    const val HOME = "home"
    const val ADD_DEVICE = "add_device"
    const val SCRIPTS = "scripts"
    const val SCRIPT_EDITOR = "script_editor?id={id}"
    const val TYPING = "typing"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val PRIVACY = "privacy"

    fun scriptEditor(id: Long? = null): String =
        if (id != null) "script_editor?id=$id" else "script_editor?id=-1"
}
