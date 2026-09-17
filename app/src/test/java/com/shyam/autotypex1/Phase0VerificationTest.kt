package com.shyam.autotypex1

import com.shyam.autotypex1.presentation.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Test

class Phase0VerificationTest {

    @Test
    fun testScreenRoutesAreCorrect() {
        assertEquals("splash", Screen.Splash.route)
        assertEquals("permissions", Screen.Permissions.route)
        assertEquals("home", Screen.Home.route)
        assertEquals("typing", Screen.Typing.route)
        assertEquals("scripts", Screen.Scripts.route)
        assertEquals("settings", Screen.Settings.route)
    }
}
