package com.shyam.autotypex1.presentation.scripteditor

import androidx.lifecycle.SavedStateHandle
import com.shyam.autotypex1.domain.model.Script
import com.shyam.autotypex1.presentation.fakes.FakeScriptRepository
import com.shyam.autotypex1.presentation.navigation.Screen
import com.shyam.autotypex1.presentation.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScriptEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var scriptRepo: FakeScriptRepository

    @Before
    fun setUp() {
        scriptRepo = FakeScriptRepository()
    }

    @Test
    fun `new script - initial state is empty`() = runTest {
        val viewModel = ScriptEditorViewModel(
            savedStateHandle = SavedStateHandle(),
            scriptRepository = scriptRepo
        )

        val state = viewModel.uiState.value
        assertEquals("", state.name)
        assertEquals("", state.content)
        assertEquals(0, state.characterCount)
        assertEquals(0, state.lineCount)
        assertFalse(state.saved)
    }

    @Test
    fun `existing script - loads data from repository`() = runTest {
        val id = scriptRepo.insertScript(
            Script(
                id = 42L,
                name = "Existing Script",
                content = "Line 1\nLine 2",
                createdAt = 100L,
                updatedAt = 100L
            )
        )

        val handle = SavedStateHandle(mapOf(Screen.ScriptEditor.ARG_SCRIPT_ID to id.toString()))
        val viewModel = ScriptEditorViewModel(
            savedStateHandle = handle,
            scriptRepository = scriptRepo
        )

        val state = viewModel.uiState.value
        assertEquals("Existing Script", state.name)
        assertEquals("Line 1\nLine 2", state.content)
        assertEquals(13, state.characterCount)
        assertEquals(2, state.lineCount)
    }

    @Test
    fun `content change updates count and detects unsupported characters`() = runTest {
        val viewModel = ScriptEditorViewModel(
            savedStateHandle = SavedStateHandle(),
            scriptRepository = scriptRepo
        )

        viewModel.onEvent(ScriptEditorUiEvent.OnContentChange("Hello \u00A9 World"))

        val state = viewModel.uiState.value
        assertEquals("Hello \u00A9 World", state.content)
        assertTrue(state.unsupportedChars.contains('\u00A9'))
    }

    @Test
    fun `save with blank name displays error`() = runTest {
        val viewModel = ScriptEditorViewModel(
            savedStateHandle = SavedStateHandle(),
            scriptRepository = scriptRepo
        )
        viewModel.onEvent(ScriptEditorUiEvent.OnContentChange("Valid Content"))
        viewModel.onEvent(ScriptEditorUiEvent.OnSave)

        assertEquals("Script name cannot be blank", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.saved)
    }

    @Test
    fun `save with blank content displays error`() = runTest {
        val viewModel = ScriptEditorViewModel(
            savedStateHandle = SavedStateHandle(),
            scriptRepository = scriptRepo
        )
        viewModel.onEvent(ScriptEditorUiEvent.OnNameChange("Valid Name"))
        viewModel.onEvent(ScriptEditorUiEvent.OnSave)

        assertEquals("Script content cannot be blank", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.saved)
    }

    @Test
    fun `save new script inserts into repo and marks saved`() = runTest {
        val viewModel = ScriptEditorViewModel(
            savedStateHandle = SavedStateHandle(),
            scriptRepository = scriptRepo
        )
        viewModel.onEvent(ScriptEditorUiEvent.OnNameChange("New Script"))
        viewModel.onEvent(ScriptEditorUiEvent.OnContentChange("print('hello')"))
        viewModel.onEvent(ScriptEditorUiEvent.OnSave)

        assertTrue(viewModel.uiState.value.saved)
        val scripts = scriptRepo.getAllScripts().first()
        assertEquals(1, scripts.size)
        assertEquals("New Script", scripts[0].name)
        assertEquals("print('hello')", scripts[0].content)
        assertTrue(scripts[0].isSelected)
    }

    @Test
    fun `save existing script updates repository`() = runTest {
        val id = scriptRepo.insertScript(
            Script(
                id = 1L,
                name = "Old Name",
                content = "Old Content",
                createdAt = 100L,
                updatedAt = 100L
            )
        )

        val handle = SavedStateHandle(mapOf(Screen.ScriptEditor.ARG_SCRIPT_ID to id.toString()))
        val viewModel = ScriptEditorViewModel(
            savedStateHandle = handle,
            scriptRepository = scriptRepo
        )

        viewModel.onEvent(ScriptEditorUiEvent.OnNameChange("Updated Name"))
        viewModel.onEvent(ScriptEditorUiEvent.OnContentChange("Updated Content"))
        viewModel.onEvent(ScriptEditorUiEvent.OnSave)

        assertTrue(viewModel.uiState.value.saved)
        val updated = scriptRepo.getScriptById(id)
        assertNotNull(updated)
        assertEquals("Updated Name", updated?.name)
        assertEquals("Updated Content", updated?.content)
    }

    @Test
    fun `delete script removes from repository and marks saved`() = runTest {
        val id = scriptRepo.insertScript(
            Script(
                id = 1L,
                name = "To Delete",
                content = "Content",
                createdAt = 100L,
                updatedAt = 100L
            )
        )

        val handle = SavedStateHandle(mapOf(Screen.ScriptEditor.ARG_SCRIPT_ID to id.toString()))
        val viewModel = ScriptEditorViewModel(
            savedStateHandle = handle,
            scriptRepository = scriptRepo
        )

        viewModel.onEvent(ScriptEditorUiEvent.OnDelete)

        assertTrue(viewModel.uiState.value.saved)
        val script = scriptRepo.getScriptById(id)
        assertNull(script)
    }
}
