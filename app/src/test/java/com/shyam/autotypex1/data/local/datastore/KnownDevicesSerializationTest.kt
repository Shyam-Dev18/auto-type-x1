package com.shyam.autotypex1.data.local.datastore

import com.shyam.autotypex1.domain.model.KnownDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KnownDevicesSerializationTest {

    private val dataStore = KnownDevicesDataStore(
        dataStore = FakeDataStore()
    )

    @Test
    fun parseDevices_emptyOrNull_returnsEmptyList() {
        assertTrue(dataStore.parseDevices(null).isEmpty())
        assertTrue(dataStore.parseDevices("").isEmpty())
        assertTrue(dataStore.parseDevices("   ").isEmpty())
    }

    @Test
    fun parseDevices_malformedJson_failsSafelyWithEmptyList() {
        assertTrue(dataStore.parseDevices("not valid json at all").isEmpty())
        assertTrue(dataStore.parseDevices("{ invalid: json }").isEmpty())
        assertTrue(dataStore.parseDevices("[{ malformed").isEmpty())
    }

    @Test
    fun encodeAndParse_roundTrip_preservesAllFields() {
        val devices = listOf(
            KnownDevice(
                address = "AA:BB:CC:DD:EE:FF",
                name = "Office PC",
                lastConnectedAt = 1700000000000L
            ),
            KnownDevice(
                address = "11:22:33:44:55:66",
                name = "MacBook Pro 16\"",
                lastConnectedAt = 1700000050000L
            )
        )

        val encoded = dataStore.encodeDevices(devices)
        val parsed = dataStore.parseDevices(encoded)

        assertEquals(2, parsed.size)
        // Ordered by lastConnectedAt descending
        assertEquals("11:22:33:44:55:66", parsed[0].address)
        assertEquals("MacBook Pro 16\"", parsed[0].name)
        assertEquals(1700000050000L, parsed[0].lastConnectedAt)

        assertEquals("AA:BB:CC:DD:EE:FF", parsed[1].address)
        assertEquals("Office PC", parsed[1].name)
        assertEquals(1700000000000L, parsed[1].lastConnectedAt)
    }

    @Test
    fun namesWithSpecialCharacters_tabsNewlinesQuotesEmojis_handledSafely() {
        val specialDevices = listOf(
            KnownDevice(
                address = "00:11:22:33:44:55",
                name = "Device\tWith\tTabs\nAnd Newlines\r\"Quotes\" & 🚀 Emojis",
                lastConnectedAt = 1000L
            )
        )

        val encoded = dataStore.encodeDevices(specialDevices)
        val parsed = dataStore.parseDevices(encoded)

        assertEquals(1, parsed.size)
        assertEquals("Device\tWith\tTabs\nAnd Newlines\r\"Quotes\" & 🚀 Emojis", parsed[0].name)
        assertEquals("00:11:22:33:44:55", parsed[0].address)
    }

    @Test
    fun missingOrBlankName_fallsBackToUnknownDevice() {
        val json = """[{"address":"AA:BB:CC:11:22:33","name":"","lastConnectedAt":5000}]"""
        val parsed = dataStore.parseDevices(json)

        assertEquals(1, parsed.size)
        assertEquals("AA:BB:CC:11:22:33", parsed[0].address)
        assertEquals("Unknown Device", parsed[0].name)
    }

    @Test
    fun nullNameField_fallsBackToUnknownDevice() {
        val json = """[{"address":"AA:BB:CC:11:22:33","lastConnectedAt":5000}]"""
        val parsed = dataStore.parseDevices(json)

        assertEquals(1, parsed.size)
        assertEquals("AA:BB:CC:11:22:33", parsed[0].address)
        assertEquals("Unknown Device", parsed[0].name)
    }

    @Test
    fun duplicatesInStoredData_deduplicatedByAddress() {
        val json = """
            [
                {"address":"AA:BB:CC:DD:EE:FF","name":"Device 1","lastConnectedAt":1000},
                {"address":"AA:BB:CC:DD:EE:FF","name":"Device 1 Dup","lastConnectedAt":2000},
                {"address":"aa:bb:cc:dd:ee:ff","name":"Device 1 Case Dup","lastConnectedAt":3000}
            ]
        """.trimIndent()

        val parsed = dataStore.parseDevices(json)
        assertEquals(1, parsed.size)
        assertEquals("AA:BB:CC:DD:EE:FF", parsed[0].address)
    }

    @Test
    fun futureUnknownFields_ignoredWithoutCrashing() {
        val json = """
            [
                {"address":"AA:BB:CC:DD:EE:FF","name":"Future Device","lastConnectedAt":1000,"futureField1":123,"extraArray":[1,2,3]}
            ]
        """.trimIndent()

        val parsed = dataStore.parseDevices(json)
        assertEquals(1, parsed.size)
        assertEquals("Future Device", parsed[0].name)
    }
}
