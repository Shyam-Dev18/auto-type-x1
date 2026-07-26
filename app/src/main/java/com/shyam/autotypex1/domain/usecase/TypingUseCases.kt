package com.shyam.autotypex1.domain.usecase

import com.shyam.autotypex1.domain.model.TypingProfileSpec
import com.shyam.autotypex1.domain.typing.HumanTypingEngine
import com.shyam.autotypex1.domain.typing.TimedKeyEvent
import javax.inject.Inject

class GenerateTypingPlanUseCase @Inject constructor(
    private val engine: HumanTypingEngine
) {
    operator fun invoke(script: String, spec: TypingProfileSpec, seed: Long): List<TimedKeyEvent> =
        engine.generate(script, spec, seed)
}
