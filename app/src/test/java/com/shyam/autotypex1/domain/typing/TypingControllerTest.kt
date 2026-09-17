package com.shyam.autotypex1.domain.typing

import com.shyam.autotypex1.domain.model.BluetoothAdapterState
import com.shyam.autotypex1.domain.model.ConnectionState
import com.shyam.autotypex1.domain.model.ScannedDevice
import com.shyam.autotypex1.domain.repository.BluetoothRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [TypingController].
 */
class TypingControllerTest {

    private lateinit var fakeBluetoothRepository: FakeBluetoothRepository
    private lateinit var engine: HumanTypingEngine
    private lateinit var controller: TypingController

    @Before
    fun setUp() {
        fakeBluetoothRepository = FakeBluetoothRepository()
        engine = DefaultHumanTypingEngine()
        controller = TypingController(
            engine = engine,
            bluetoothRepository = fakeBluetoothRepository,
            dispatcher = Dispatchers.Default
        )
    }

    @Test
    fun `successful typing transitions from Idle to Preparing to Typing to Completed`() = runBlocking {
        fakeBluetoothRepository.connected = true
        val script = "Hello"
        val profile = TypingProfile.SPEED_DEMON.copy(typoProbability = 0.0)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val result = controller.startTyping(
            script = script,
            profile = profile,
            seed = 12345L,
            scope = scope
        )

        assertTrue(result.isSuccess)

        // Poll until completed
        var waitedMs = 0L
        while (controller.typingState.value !is TypingState.Completed && waitedMs < 2000L) {
            delay(20)
            waitedMs += 20
        }

        val state = controller.typingState.value
        assertTrue("Expected Completed state, got $state", state is TypingState.Completed)
        assertEquals(5, (state as TypingState.Completed).totalChars)
        assertEquals(100f, controller.progress.value.percentage, 0.01f)
        assertTrue("releaseAllKeys must be called in finally", fakeBluetoothRepository.releaseAllKeysCalled)

        scope.cancel()
    }

    @Test
    fun `startTyping fails if Bluetooth is not connected`() = runBlocking {
        fakeBluetoothRepository.connected = false
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val result = controller.startTyping("Hello", TypingProfile.NORMAL, 123L, scope)

        assertFalse(result.isSuccess)
        val state = controller.typingState.value
        assertTrue(state is TypingState.Failed)
        assertEquals(TypingFailure.NoHidConnection, (state as TypingState.Failed).failure)

        scope.cancel()
    }

    @Test
    fun `startTyping fails if script contains only unsupported characters`() = runBlocking {
        fakeBluetoothRepository.connected = true
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val result = controller.startTyping("€€€", TypingProfile.NORMAL, 123L, scope)

        assertFalse(result.isSuccess)
        val state = controller.typingState.value
        assertTrue(state is TypingState.Failed)
        assertEquals(TypingFailure.ScriptEmpty, (state as TypingState.Failed).failure)

        scope.cancel()
    }

    @Test
    fun `concurrent typing sessions are rejected`() = runBlocking {
        fakeBluetoothRepository.connected = true
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val result1 = controller.startTyping("A long script to type...", TypingProfile.SLOW, 123L, scope)
        assertTrue(result1.isSuccess)

        val result2 = controller.startTyping("Second script", TypingProfile.SLOW, 456L, scope)
        assertFalse(result2.isSuccess)
        assertTrue(result2.exceptionOrNull() is IllegalStateException)

        controller.stopTyping()
        scope.cancel()
    }

    @Test
    fun `stopTyping transitions state to Idle and invokes releaseAllKeys`() = runBlocking {
        fakeBluetoothRepository.connected = true
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        controller.startTyping("Very long script that will be cancelled...", TypingProfile.SLOW, 123L, scope)
        delay(50)
        controller.stopTyping()

        val state = controller.typingState.value
        assertTrue("Expected Idle state, got $state", state is TypingState.Idle)
        assertTrue(fakeBluetoothRepository.releaseAllKeysCalled)

        scope.cancel()
    }

    @Test
    fun `HID send failure aborts typing and triggers releaseAllKeys`() = runBlocking {
        fakeBluetoothRepository.connected = true
        fakeBluetoothRepository.failOnKeyDown = true
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val result = controller.startTyping("Hello", TypingProfile.SPEED_DEMON, 123L, scope)
        assertTrue(result.isSuccess)

        var waitedMs = 0L
        while (controller.typingState.value !is TypingState.Failed && waitedMs < 2000L) {
            delay(20)
            waitedMs += 20
        }

        val state = controller.typingState.value
        assertTrue("Expected Failed state, got $state", state is TypingState.Failed)
        assertEquals(TypingFailure.HidSendFailed, (state as TypingState.Failed).failure)
        assertTrue(fakeBluetoothRepository.releaseAllKeysCalled)

        scope.cancel()
    }

    @Test
    fun `disconnect mid-session aborts typing cleanly`() = runBlocking {
        fakeBluetoothRepository.connected = true
        fakeBluetoothRepository.disconnectOnFirstKey = true
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val result = controller.startTyping("Hello world", TypingProfile.SPEED_DEMON, 123L, scope)
        assertTrue(result.isSuccess)

        var waitedMs = 0L
        while (controller.typingState.value !is TypingState.Failed && waitedMs < 2000L) {
            delay(20)
            waitedMs += 20
        }

        val state = controller.typingState.value
        assertTrue("Expected Failed state with BluetoothDisconnected, got $state", state is TypingState.Failed)
        assertEquals(TypingFailure.BluetoothDisconnected, (state as TypingState.Failed).failure)
        assertTrue(fakeBluetoothRepository.releaseAllKeysCalled)

        scope.cancel()
    }

    @Test
    fun `regression test — stuck key protection guarantees releaseAllKeys on any exit`() = runBlocking {
        fakeBluetoothRepository.connected = true
        fakeBluetoothRepository.throwExceptionOnKeyDown = true
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val result = controller.startTyping("Testing stuck keys", TypingProfile.SPEED_DEMON, 123L, scope)
        assertTrue(result.isSuccess)

        var waitedMs = 0L
        while (controller.typingState.value !is TypingState.Failed && waitedMs < 2000L) {
            delay(20)
            waitedMs += 20
        }

        val state = controller.typingState.value
        assertTrue(state is TypingState.Failed)
        assertTrue("releaseAllKeys must be called even on unexpected crash", fakeBluetoothRepository.releaseAllKeysCalled)

        scope.cancel()
    }

    @Test
    fun `pauseTyping halts execution and releases keys, resumeTyping completes successfully`() = runBlocking {
        fakeBluetoothRepository.connected = true
        val script = "Hello"
        val profile = TypingProfile.FAST.copy(typoProbability = 0.0)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        controller.startTyping(
            script = script,
            profile = profile,
            seed = 42L,
            scope = scope
        )

        // Wait for typing to start
        var waited = 0L
        while (controller.typingState.value !is TypingState.Typing && waited < 1000L) {
            delay(10)
            waited += 10
        }
        assertTrue(controller.typingState.value is TypingState.Typing)

        // Pause
        fakeBluetoothRepository.releaseAllKeysCalled = false
        controller.pauseTyping()

        val pausedState = controller.typingState.value
        assertTrue("Expected Paused state, got $pausedState", pausedState is TypingState.Paused)
        assertTrue("Keys must be released on pause", fakeBluetoothRepository.releaseAllKeysCalled)

        val pausedEventIndex = controller.progress.value.currentEvent
        delay(100)
        // Verify progress hasn't advanced while paused
        assertEquals(pausedEventIndex, controller.progress.value.currentEvent)

        // Resume
        controller.resumeTyping()
        assertTrue(controller.typingState.value is TypingState.Typing)

        // Wait for completion
        waited = 0L
        while (controller.typingState.value !is TypingState.Completed && waited < 5000L) {
            delay(20)
            waited += 20
        }

        val completedState = controller.typingState.value
        assertTrue("Expected Completed state, got $completedState", completedState is TypingState.Completed)
        assertTrue(controller.progress.value.percentage >= 100f)

        scope.cancel()
    }

    @Test
    fun `stopTyping while paused cleanly cancels and releases keys`() = runBlocking {
        fakeBluetoothRepository.connected = true
        val script = "Test String Long Enough"
        val profile = TypingProfile.SLOW
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        controller.startTyping(script = script, profile = profile, scope = scope)
        var waited = 0L
        while (controller.typingState.value !is TypingState.Typing && waited < 1000L) {
            delay(10)
            waited += 10
        }
        assertTrue(controller.typingState.value is TypingState.Typing)

        controller.pauseTyping()
        assertTrue(controller.typingState.value is TypingState.Paused)

        fakeBluetoothRepository.releaseAllKeysCalled = false
        controller.stopTyping()

        val state = controller.typingState.value
        assertTrue("Expected Idle state, got $state", state is TypingState.Idle)
        assertTrue(fakeBluetoothRepository.releaseAllKeysCalled)

        scope.cancel()
    }
}

/**
 * Fake implementation of [BluetoothRepository] for unit testing [TypingController].
 */
private class FakeBluetoothRepository : BluetoothRepository {
    var connected = false
    var failOnKeyDown = false
    var throwExceptionOnKeyDown = false
    var disconnectOnFirstKey = false
    var releaseAllKeysCalled = false

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

    override fun observeAdapterState(): Flow<BluetoothAdapterState> =
        MutableStateFlow(BluetoothAdapterState.Enabled).asStateFlow()

    override fun observeConnectionState(): Flow<ConnectionState> = _connectionState.asStateFlow()

    override fun observeScannedDevices(): Flow<List<ScannedDevice>> =
        MutableStateFlow(emptyList<ScannedDevice>()).asStateFlow()

    override fun observeIsDiscovering(): Flow<Boolean> =
        MutableStateFlow(false).asStateFlow()

    override suspend fun startDiscovery(): Result<Unit> = Result.success(Unit)

    override suspend fun stopDiscovery() {}

    override suspend fun connect(address: String): Result<Unit> {
        connected = true
        _connectionState.value = ConnectionState.Connected("Test Host", address)
        return Result.success(Unit)
    }

    override suspend fun disconnect() {
        connected = false
        _connectionState.value = ConnectionState.Disconnected
    }

    override suspend fun sendKeyDown(char: Char): Result<Boolean> {
        if (throwExceptionOnKeyDown) {
            throw RuntimeException("Simulated unexpected transport crash")
        }
        if (disconnectOnFirstKey) {
            connected = false
            _connectionState.value = ConnectionState.Disconnected
            return Result.failure(Exception("Disconnected"))
        }
        if (failOnKeyDown) {
            return Result.failure(Exception("Send failed"))
        }
        return Result.success(true)
    }

    override suspend fun sendKeyUp(): Result<Boolean> = Result.success(true)

    override suspend fun releaseAllKeys(): Result<Boolean> {
        releaseAllKeysCalled = true
        return Result.success(true)
    }

    override fun isConnected(): Boolean = connected

    override fun release() {}
}
