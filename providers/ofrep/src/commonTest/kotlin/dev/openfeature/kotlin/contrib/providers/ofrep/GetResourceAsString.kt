package dev.openfeature.kotlin.contrib.providers.ofrep

import okio.FileSystem
import okio.Path.Companion.toPath

fun getResourceAsString(resourceName: String): String =
    FileSystem.SYSTEM.read("src/commonTest/resources".toPath() / resourceName) {
        readUtf8()
    }
