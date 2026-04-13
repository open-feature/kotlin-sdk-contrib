package dev.openfeature.kotlin.contrib.providers.launchdarkly

import dev.openfeature.kotlin.sdk.Value
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Verifies [Value] <-> LaunchDarkly Android [com.launchdarkly.sdk.LDValue] mapping via round-trip.
 */
@OptIn(ExperimentalTime::class)
class LaunchDarklyValueMappingTest {
    @Test
    fun null_roundTrips() {
        assertRoundTrip(Value.Null)
    }

    @Test
    fun booleans_roundTrip() {
        assertRoundTrip(Value.Boolean(false))
        assertRoundTrip(Value.Boolean(true))
    }

    @Test
    fun strings_roundTrip() {
        assertRoundTrip(Value.String(""))
        assertRoundTrip(Value.String("hello"))
    }

    @Test
    fun integers_roundTrip() {
        assertRoundTrip(Value.Integer(0))
        assertRoundTrip(Value.Integer(-42))
        assertRoundTrip(Value.Integer(Int.MAX_VALUE))
    }

    @Test
    fun doubles_roundTrip() {
        assertRoundTrip(Value.Double(0.0))
        assertRoundTrip(Value.Double(3.141592653589793))
        assertRoundTrip(Value.Double(-1.0e-9))
    }

    @Test
    fun instants_encodeAsLdStrings_andRoundTripToValueString() {
        val i = Instant.parse("2024-06-15T12:30:00.123Z")
        val v = Value.Instant(i)
        val ld = v.toLDValue()
        val back = ld.toValue()
        assertEquals(Value.String(i.toString()), back)
    }

    @Test
    fun lists_roundTrip() {
        assertRoundTrip(Value.List(emptyList()))
        assertRoundTrip(
            Value.List(
                listOf(
                    Value.Boolean(true),
                    Value.String("x"),
                    Value.List(listOf(Value.Integer(1))),
                ),
            ),
        )
    }

    @Test
    fun structures_roundTrip() {
        assertRoundTrip(Value.Structure(emptyMap()))
        assertRoundTrip(
            Value.Structure(
                mapOf(
                    "a" to Value.Integer(1),
                    "b" to Value.Structure(mapOf("nested" to Value.Null)),
                ),
            ),
        )
    }

    private fun assertRoundTrip(original: Value) {
        val ld = original.toLDValue()
        val back = ld.toValue()
        when (original) {
            is Value.Double -> assertDoubleRoundTrip(original, back)
            else -> assertEquals(original, back)
        }
    }

    /**
     * LD may store whole-number doubles as JSON integers, so [LDValue.toValue] can yield [Value.Integer].
     */
    private fun assertDoubleRoundTrip(original: Value.Double, back: Value) {
        when (back) {
            is Value.Double ->
                assertEquals(original.double, back.double, absoluteTolerance = 1e-12)
            is Value.Integer ->
                assertEquals(original.double, back.integer.toDouble(), absoluteTolerance = 1e-12)
            else -> assertEquals(original, back)
        }
    }
}
