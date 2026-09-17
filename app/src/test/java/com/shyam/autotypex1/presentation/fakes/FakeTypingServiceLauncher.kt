package com.shyam.autotypex1.presentation.fakes

import com.shyam.autotypex1.domain.typing.TypingServiceLauncher

class FakeTypingServiceLauncher : TypingServiceLauncher {
    var connectionStarted = false
    var connectionStopped = false
    var lastDeviceName: String? = null

    var startCalled = false
    var pauseCalled = false
    var resumeCalled = false
    var stopCalled = false
    var lastScript: String? = null
    var lastProfileName: String? = null
    var lastSeed: Long? = null

    override fun startConnection(deviceName: String) {
        connectionStarted = true
        lastDeviceName = deviceName
    }

    override fun stopConnection() {
        connectionStopped = true
    }

    override fun startTyping(script: String, profileName: String, seed: Long?) {
        startCalled = true
        lastScript = script
        lastProfileName = profileName
        lastSeed = seed
    }

    override fun pause() {
        pauseCalled = true
    }

    override fun resume() {
        resumeCalled = true
    }

    override fun stop() {
        stopCalled = true
    }
}
