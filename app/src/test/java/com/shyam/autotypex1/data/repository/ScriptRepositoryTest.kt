package com.shyam.autotypex1.data.repository

import com.shyam.autotypex1.data.local.room.FakeScriptDao
import com.shyam.autotypex1.domain.model.Script
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ScriptRepositoryTest {

    private lateinit var fakeScriptDao: FakeScriptDao
    private lateinit var repository: ScriptRepositoryImpl

    @Before
    fun setUp() {
        fakeScriptDao = FakeScriptDao()
        repository = ScriptRepositoryImpl(fakeScriptDao)
    }

    @Test
    fun initialState_emptyScripts() = runTest {
        val scripts = repository.getAllScripts().first()
        val selected = repository.getSelectedScript().first()

        assertTrue(scripts.isEmpty())
        assertNull(selected)
    }

    @Test
    fun insertScript_generatesIdAndCanBeRetrieved() = runTest {
        val script = Script(
            name = "Test Script",
            content = "Hello, world!"
        )

        val id = repository.insertScript(script)
        assertTrue(id > 0)

        val retrieved = repository.getScriptById(id)
        assertNotNull(retrieved)
        assertEquals("Test Script", retrieved?.name)
        assertEquals("Hello, world!", retrieved?.content)
    }

    @Test
    fun updateScript_updatesContentAndMetadata() = runTest {
        val id = repository.insertScript(
            Script(name = "Original Name", content = "Original Content")
        )

        val existing = repository.getScriptById(id)!!
        val updated = existing.copy(
            name = "Updated Name",
            content = "Updated Content\nLine 2",
            updatedAt = System.currentTimeMillis() + 1000
        )

        repository.updateScript(updated)

        val retrieved = repository.getScriptById(id)
        assertEquals("Updated Name", retrieved?.name)
        assertEquals("Updated Content\nLine 2", retrieved?.content)
        assertEquals(22, retrieved?.characterCount)
        assertEquals(2, retrieved?.lineCount)
    }

    @Test
    fun deleteScript_removesScriptById() = runTest {
        val id = repository.insertScript(
            Script(name = "To Delete", content = "Delete me")
        )

        assertNotNull(repository.getScriptById(id))
        repository.deleteScript(id)
        assertNull(repository.getScriptById(id))
    }

    @Test
    fun selectScript_atomicallySelectsOnlyOneScript() = runTest {
        val id1 = repository.insertScript(Script(name = "Script 1", content = "Content 1"))
        val id2 = repository.insertScript(Script(name = "Script 2", content = "Content 2"))

        repository.selectScript(id1)
        val selected1 = repository.getSelectedScript().first()
        assertEquals(id1, selected1?.id)
        assertTrue(selected1?.isSelected == true)

        repository.selectScript(id2)
        val selected2 = repository.getSelectedScript().first()
        assertEquals(id2, selected2?.id)

        val retrievedScript1 = repository.getScriptById(id1)
        assertFalse(retrievedScript1?.isSelected == true)
    }

    @Test
    fun clearSelection_unselectsAll() = runTest {
        val id = repository.insertScript(Script(name = "Script", content = "Content"))
        repository.selectScript(id)
        assertNotNull(repository.getSelectedScript().first())

        repository.clearSelection()
        assertNull(repository.getSelectedScript().first())
    }

    @Test
    fun getAllScripts_ordersByUpdatedAtDescending() = runTest {
        val id1 = repository.insertScript(
            Script(name = "Old", content = "Old", updatedAt = 1000L)
        )
        val id2 = repository.insertScript(
            Script(name = "Newer", content = "Newer", updatedAt = 2000L)
        )
        val id3 = repository.insertScript(
            Script(name = "Newest", content = "Newest", updatedAt = 3000L)
        )

        val all = repository.getAllScripts().first()
        assertEquals(3, all.size)
        assertEquals(id3, all[0].id)
        assertEquals(id2, all[1].id)
        assertEquals(id1, all[2].id)
    }

    @Test
    fun specialUnicodeAndLongContent_persistedAccurately() = runTest {
        val specialText = "Special chars: ~!@#\$%^&*()_+{}[]|\\:;\"'<>,.?/ 🚀 日本語 \n\t Tab"
        val longContent = (1..1000).joinToString("\n") { "Line $it: The quick brown fox jumps over the lazy dog." }

        val id1 = repository.insertScript(Script(name = "Special", content = specialText))
        val id2 = repository.insertScript(Script(name = "Long", content = longContent))

        val retrievedSpecial = repository.getScriptById(id1)
        val retrievedLong = repository.getScriptById(id2)

        assertEquals(specialText, retrievedSpecial?.content)
        assertEquals(longContent, retrievedLong?.content)
        assertEquals(1000, retrievedLong?.lineCount)
    }
}
