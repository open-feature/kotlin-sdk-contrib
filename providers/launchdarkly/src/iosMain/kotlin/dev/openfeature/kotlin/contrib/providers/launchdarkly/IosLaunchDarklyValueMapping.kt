@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.openfeature.kotlin.contrib.providers.launchdarkly

import dev.openfeature.kotlin.sdk.Value
import platform.Foundation.NSNumber
import swiftPMImport.dev.openfeature.kotlin.contrib.providers.launchdarkly.LDValue
import swiftPMImport.dev.openfeature.kotlin.contrib.providers.launchdarkly.LDValueTypeArray
import swiftPMImport.dev.openfeature.kotlin.contrib.providers.launchdarkly.LDValueTypeBool
import swiftPMImport.dev.openfeature.kotlin.contrib.providers.launchdarkly.LDValueTypeNull
import swiftPMImport.dev.openfeature.kotlin.contrib.providers.launchdarkly.LDValueTypeNumber
import swiftPMImport.dev.openfeature.kotlin.contrib.providers.launchdarkly.LDValueTypeObject
import swiftPMImport.dev.openfeature.kotlin.contrib.providers.launchdarkly.LDValueTypeString

/**
 * Maps OpenFeature [Value] to LaunchDarkly iOS (SwiftPM) [LDValue].
 */
internal fun Value.toLDValue(): LDValue = when (this) {
    is Value.Boolean -> LDValue.ofBool(boolean)
    is Value.String -> LDValue.ofString(string)
    is Value.Integer -> LDValue.ofNumber(NSNumber(int = integer))
    is Value.Double -> LDValue.ofNumber(NSNumber(double = double))
    is Value.Instant -> LDValue.ofString(instant.toString())
    is Value.List -> {
        val a = mutableListOf<LDValue>()
        for (item in list) {
            a.add(item.toLDValue())
        }
        LDValue.ofArray(a)
    }

    is Value.Structure -> {
        val m = mutableMapOf<Any?, Any?>()
        for ((key, item) in structure) {
            m[key] = item.toLDValue()
        }
        LDValue.ofDict(m)
    }

    is Value.Null -> LDValue.ofNull()
}

/**
 * Maps LaunchDarkly iOS (SwiftPM) [LDValue] to OpenFeature [Value].
 *
 * Discrimination uses [LDValue.getType] against the bridged `LDValueType` constants ([LDValueTypeNull],
 * [LDValueTypeBool], …) from `ObjcLDValueType` / `@objc(LDValueType)` in the LaunchDarkly SDK, not raw
 * integers.
 *
 * Numbers: the ObjC bridge only exposes [doubleValue]; whole numbers are mapped to [Value.Integer]
 * when `n == n.toInt().toDouble()`.
 */
internal fun LDValue.toValue(): Value =
    when (getType()) {
        LDValueTypeNull -> Value.Null
        LDValueTypeBool -> Value.Boolean(boolValue())
        LDValueTypeString -> Value.String(stringValue())
        LDValueTypeNumber -> {
            val n = doubleValue()
            val i = n.toInt()
            if (n == i.toDouble()) Value.Integer(i) else Value.Double(n)
        }
        LDValueTypeArray -> {
            val list = mutableListOf<Value>()
            for (item in arrayValue()) {
                list.add((item as LDValue).toValue())
            }
            Value.List(list)
        }
        LDValueTypeObject -> {
            val map = mutableMapOf<String, Value>()
            for ((k, v) in dictValue()) {
                map[k.toString()] = (v as LDValue).toValue()
            }
            Value.Structure(map)
        }
        else -> Value.Null
    }
