package com.shyam.autotypex1.domain.usecase

import com.shyam.autotypex1.domain.model.Script
import com.shyam.autotypex1.domain.repository.ScriptRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveScriptsUseCase @Inject constructor(
    private val scriptRepository: ScriptRepository
) {
    operator fun invoke(): Flow<List<Script>> = scriptRepository.observeAll()
}

class ObserveSelectedScriptUseCase @Inject constructor(
    private val scriptRepository: ScriptRepository
) {
    operator fun invoke(): Flow<Script?> = scriptRepository.observeSelected()
}

class LoadScriptUseCase @Inject constructor(
    private val scriptRepository: ScriptRepository
) {
    suspend operator fun invoke(id: Long): Script? = scriptRepository.getById(id)
}

class SaveScriptUseCase @Inject constructor(
    private val scriptRepository: ScriptRepository
) {
    suspend operator fun invoke(name: String, content: String, existingId: Long? = null): Result<Long> =
        scriptRepository.save(name, content, existingId)
}

class DeleteScriptUseCase @Inject constructor(
    private val scriptRepository: ScriptRepository
) {
    suspend operator fun invoke(id: Long): Result<Unit> = scriptRepository.delete(id)
}

class SelectScriptUseCase @Inject constructor(
    private val scriptRepository: ScriptRepository
) {
    suspend operator fun invoke(id: Long): Result<Unit> = scriptRepository.selectScript(id)
}
