package com.shyam.autotypex1.data.repository

import com.shyam.autotypex1.data.local.datastore.FakeDataStore
import com.shyam.autotypex1.data.local.datastore.KnownDevicesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeviceRepositoryTest {

    private lateinit var deviceRepository: DeviceRepositoryImpl
    private lateinit var knownDevicesDataStore: KnownDevicesDataStore

    @Before
    fun setUp() {
        val fakeDataStore = FakeDataStore()
        knownDevicesDataStore = KnownDevicesDataStore(fakeDataStore)
        deviceRepository = DeviceRepositoryImpl(knownDevicesDataStore)
    }

    @Test
    fun getKnownDevices_initialEmpty() = runTest {
        val devices = deviceRepository.getKnownDevices()
        val lastConnected = deviceRepository.observeLastConnectedAddress().first()

        assertTrue(devices.isEmpty())
        assertNull(lastConnected)
    }

    @Test
    fun saveDevice_andObserve() = runTest {
        deviceRepository.saveDevice("Home PC", "12:34:56:78:90:AB")

        val devices = deviceRepository.observeKnownDevices().first()
        val lastConnected = deviceRepository.observeLastConnectedAddress().first()

        assertEquals(1, devices.size)
        assertEquals("Home PC", devices[0].name)
        assertEquals("12:34:56:78:90:AB", devices[0].address)
        assertEquals("12:34:56:78:90:AB", lastConnected)
    }

    @Test
    fun removeDevice_removesDeviceAndClearsLastConnected() = runTest {
        deviceRepository.saveDevice("Device 1", "00:11:22:33:44:55")
        deviceRepository.removeDevice("00:11:22:33:44:55")

        val devices = deviceRepository.getKnownDevices()
        val lastConnected = deviceRepository.observeLastConnectedAddress().first()

        assertTrue(devices.isEmpty())
        assertNull(lastConnected)
    }

    @Test
    fun updateLastConnected_updatesTimestamp() = runTest {
        deviceRepository.saveDevice("Device 1", "00:11:22:33:44:55")
        val initialTimestamp = deviceRepository.getKnownDevices()[0].lastConnectedAt

        Thread.sleep(10)
        deviceRepository.updateLastConnected("00:11:22:33:44:55")

        val devices = deviceRepository.getKnownDevices()
        assertTrue(devices[0].lastConnectedAt > initialTimestamp)
    }
}
