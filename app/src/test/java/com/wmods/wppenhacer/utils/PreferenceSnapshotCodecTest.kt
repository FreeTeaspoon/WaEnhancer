package com.wmods.wppenhacer.utils

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferenceSnapshotCodecTest {
    @Test
    fun roundTripsSupportedPreferenceTypes() {
        val values = linkedMapOf<String, Any>(
            "string" to "value",
            "boolean" to true,
            "integer" to 42,
            "long" to 4_200_000_000L,
            "float" to 1.5f,
            "set" to linkedSetOf("first", "second")
        )

        val decoded = PreferenceSnapshotCodec.decode(PreferenceSnapshotCodec.encode(values))

        assertEquals(values["string"], decoded["string"])
        assertEquals(values["boolean"], decoded["boolean"])
        assertEquals(values["integer"], decoded["integer"])
        assertEquals(values["long"], decoded["long"])
        assertEquals(values["float"], decoded["float"])
        assertEquals(values["set"], decoded["set"])
    }

    @Test
    fun decodesLegacySetType() {
        val values = JSONObject().put(
            "legacy_set",
            JSONObject()
                .put("type", "HashSet")
                .put("value", org.json.JSONArray().put("one").put("two"))
        )

        val decoded = PreferenceSnapshotCodec.decode(values)

        assertEquals(setOf("one", "two"), decoded["legacy_set"])
    }

    @Test
    fun decodesLegacyDoubleAndSkipsMalformedValues() {
        val values = JSONObject()
            .put("legacy_double", JSONObject().put("type", "Double").put("value", 1.0))
            .put(
                "malformed_set",
                JSONObject()
                    .put("type", "JSONArray")
                    .put("value", org.json.JSONArray().put("valid").put(7))
            )

        val decoded = PreferenceSnapshotCodec.decode(values)

        assertEquals(1.0f, decoded["legacy_double"])
        assertTrue(!decoded.containsKey("malformed_set"))
    }
}
