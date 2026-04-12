package dev.openfeature.kotlin.contrib.providers.launchdarkly

import com.launchdarkly.sdk.LDValue
import com.launchdarkly.sdk.LDValueType
import dev.openfeature.kotlin.sdk.Value

/**
 * Maps OpenFeature [Value] to LaunchDarkly Android [LDValue].
 */
internal fun Value.toLDValue(): LDValue =
    when (this) {
        is Value.Boolean -> LDValue.of(boolean)
        is Value.String -> LDValue.of(string)
        is Value.Integer -> LDValue.of(integer)
        is Value.Double -> LDValue.of(double)
        is Value.Instant -> LDValue.of(instant.toString())
        is Value.List -> {
            val a = LDValue.buildArray()
            for (item in list) {
                a.add(item.toLDValue())
            }
            a.build()
        }
        is Value.Structure -> {
            val o = LDValue.buildObject()
            for ((key, item) in structure) {
                o.put(key, item.toLDValue())
            }
            o.build()
        }
        is Value.Null -> LDValue.ofNull()
    }

/**
 * Maps LaunchDarkly Android [LDValue] to OpenFeature [Value].
 */
internal fun LDValue.toValue(): Value =
    when (type) {
        LDValueType.NULL -> Value.Null
        LDValueType.BOOLEAN -> Value.Boolean(booleanValue())
        LDValueType.STRING -> Value.String(stringValue())
        LDValueType.NUMBER -> if (isInt) Value.Integer(intValue()) else Value.Double(doubleValue())
        LDValueType.ARRAY -> {
            val list = mutableListOf<Value>()
            for (i in 0 until size()) {
                get(i)?.let { list.add(it.toValue()) }
            }
            Value.List(list)
        }
        LDValueType.OBJECT -> {
            val map = mutableMapOf<String, Value>()
            for (k in keys()) {
                get(k)?.let { map[k] = it.toValue() }
            }
            Value.Structure(map)
        }
    }
