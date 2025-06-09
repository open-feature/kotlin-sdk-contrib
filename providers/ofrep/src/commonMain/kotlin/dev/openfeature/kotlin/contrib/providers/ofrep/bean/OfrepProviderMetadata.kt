package dev.openfeature.kotlin.contrib.providers.ofrep.bean

import dev.openfeature.sdk.ProviderMetadata

internal class OfrepProviderMetadata : ProviderMetadata {
    override val name: String
        get() = "OFREP Provider"
}
