package dev.openfeature.kotlin.contrib.providers.launchdarkly

import dev.openfeature.kotlin.sdk.EvaluationContext

internal const val DEFAULT_TARGETING_KEY = "anonymous"

internal val EvaluationContext.targetingKeyOrAnonymous: String
    get() = getTargetingKey().takeIf { it.isNotBlank() } ?: DEFAULT_TARGETING_KEY
