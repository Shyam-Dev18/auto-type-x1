package com.shyam.autotypex1.data.local.datastore

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KnownDevicesDataStoreTest {

    private lateinit var fakeDataStore: FakeDataStore
    private lateinit var knownDevicesDataStore: KnownDevicesDataStore

    @Before
    fun setUp() {
        fakeDataStore = FakeDataStore()
        knownDevicesDataStore = KnownDevicesDataStore(fakeDataStore)
    }

    @Test
    fun initialKnownDevices_returnsEmptyListAndNullLastConnected() = runTest {
        val devices = knownDevicesDataStore.getKnownDevices()
        val lastConnected = knownDevicesDataStore.observeLastConnectedAddress().first()

        assertTrue(devices.isEmpty())
        assertNull(lastConnected)
    }

    @Test
    fun saveDevice_savesNewDeviceAndUpdatesLastConnected() = runTest {
        knownDevicesDataStore.saveDevice(name = "Windows Desktop", address = "AA:BB:CC:DD:EE:01")

        val devices = knownDevicesDataStore.getKnownDevices()
        val lastConnected = knownDevicesDataStore.observeLastConnectedAddress().first()

        assertEquals(1, devices.size)
        assertEquals("AA:BB:CC:DD:EE:01", devices[0].address)
        assertEquals("Windows Desktop", devices[0].name)
        assertEquals("AA:BB:CC:DD:EE:01", lastConnected)
    }

    @Test
    fun saveDevice_multipleDevices_ordersByMostRecent() = runTest {
        knownDevicesDataStore.saveDevice(name = "Device 1", address = "AA:00:00:00:00:01")
        Thread.sleep(10) // Ensure distinct timestamps
        knownDevicesDataStore.saveDevice(name = "Device 2", address = "AA:00:00:00:00:02")

        val devices = knownDevicesDataStore.getKnownDevices()
        assertEquals(2, devices.size)
        assertEquals("AA:00:00:00:00:02", devices[0].address)
        assertEquals("AA:00:00:00:00:01", devices[1].address)
    }

    @Test
    fun saveDevice_duplicateAddress_updatesExistingEntryWithoutDuplication() = runTest {
        knownDevicesDataStore.saveDevice(name = "Old Name", address = "AA:BB:CC:DD:EE:FF")
        knownDevicesDataStore.saveDevice(name = "New Name", address = "AA:BB:CC:DD:EE:FF")

        val devices = knownDevicesDataStore.getKnownDevices()
        assertEquals(1, devices.size)
        assertEquals("New Name", devices[0].name)
        assertEquals("AA:BB:CC:DD:EE:FF", devices[0].address)
    }

    @Test
    fun removeDevice_removesDeviceAndClearsLastConnectedIfMatching() = runTest {
        knownDevicesDataStore.saveDevice(name = "PC 1", address = "11:22:33:44:55:66")
        knownDevicesDataStore.saveDevice(name = "PC 2", address = "77:88:99:AA:BB:CC")

        knownDevicesDataStore.removeDevice("77:88:99:AA:BB:CC")

        val devices = knownDevicesDataStore.getKnownDevices()
        val lastConnected = knownDevicesDataStore.observeLastConnectedAddress().first()

        assertEquals(1, devices.size)
        assertEquals("11:22:33:44:55:66", devices[0].address)
        assertNull(lastConnected)
    }

    @Test
    fun updateLastConnected_updatesTimestampAndSetsAddress() = runTest {
        knownDevicesDataStore.saveDevice(name = "PC 1", address = "11:22:33:44:55:66")
        val initialTimestamp = knownDevicesDataStore.getKnownDevices()[0].lastConnectedAt

        Thread.sleep(10)
        knownDevicesDataStore.updateLastConnected("11:22:33:44:55:66")

        val devices = knownDevicesDataStore.getKnownDevices()
        val lastConnected = knownDevicesDataStore.observeLastConnectedAddress().first()

        assertEquals(1, devices.size)
        assertTrue(devices[0].lastConnectedAt > initialTimestamp)
        assertEquals("11:22:33:44:55:66", lastConnected)
    }

    @Test
    fun saveDevice_blankAddress_ignored() = runTest {
        knownDevicesDataStore.saveDevice(name = "Bad Device", address = "   ")
        assertTrue(knownDevicesDataStore.getKnownDevices().isEmpty())
    }
}
