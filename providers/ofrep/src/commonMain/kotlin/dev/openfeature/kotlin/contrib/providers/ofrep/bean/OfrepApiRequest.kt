package dev.openfeature.kotlin.contrib.providers.ofrep.bean

import dev.openfeature.kotlin.contrib.providers.ofrep.serialization.EvaluationContextSerializer
import dev.openfeature.sdk.EvaluationContext
import dev.openfeature.sdk.ImmutableContext
import kotlinx.serialization.Serializable

@Serializable
internal data class OfrepApiRequest(
    @Serializable(with = EvaluationContextSerializer::class)
    val ctx: EvaluationContext = ImmutableContext(),
)
