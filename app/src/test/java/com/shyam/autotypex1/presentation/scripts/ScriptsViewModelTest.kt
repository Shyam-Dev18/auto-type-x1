package com.shyam.autotypex1.presentation.scripts

import com.shyam.autotypex1.domain.model.Script
import com.shyam.autotypex1.presentation.fakes.FakeScriptRepository
import com.shyam.autotypex1.presentation.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScriptsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var scriptRepo: FakeScriptRepository
    private lateinit var viewModel: ScriptsViewModel

    @Before
    fun setUp() {
        scriptRepo = FakeScriptRepository()
        viewModel = ScriptsViewModel(scriptRepository = scriptRepo)
    }

    @Test
    fun `initial state collects scripts from repository`() = runTest {
        scriptRepo.insertScript(
            Script(id = 1L, name = "Script 1", content = "text 1", createdAt = 100L, updatedAt = 100L)
        )
        scriptRepo.insertScript(
            Script(id = 2L, name = "Script 2", content = "text 2", createdAt = 200L, updatedAt = 200L)
        )

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.scripts.size)
    }

    @Test
    fun `select script updates selection in repository`() = runTest {
        val id1 = scriptRepo.insertScript(
            Script(id = 1L, name = "Script 1", content = "text 1", createdAt = 100L, updatedAt = 100L)
        )
        val id2 = scriptRepo.insertScript(
            Script(id = 2L, name = "Script 2", content = "text 2", createdAt = 200L, updatedAt = 200L)
        )

        viewModel.onEvent(ScriptsUiEvent.OnSelect(id2))

        val selected = scriptRepo.getSelectedScript().first()
        assertEquals(id2, selected?.id)
    }

    @Test
    fun `delete script removes script from repository`() = runTest {
        val id = scriptRepo.insertScript(
            Script(id = 1L, name = "Script 1", content = "text 1", createdAt = 100L, updatedAt = 100L)
        )

        viewModel.onEvent(ScriptsUiEvent.OnDelete(id))

        val scripts = scriptRepo.getAllScripts().first()
        assertTrue(scripts.isEmpty())
    }
}
