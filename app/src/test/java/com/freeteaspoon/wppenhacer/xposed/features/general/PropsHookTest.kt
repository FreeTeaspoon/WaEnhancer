package com.freeteaspoon.wppenhacer.xposed.features.general

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PropsHookTest {
    @Test
    fun overridesKeepTheHostsValueType() {
        val layer = PropsHook.PropLayer().apply {
            booleans[1] = false
            integers[2] = 42
            floats[3] = 1.5f
            strings[4] = "custom"
        }

        assertEquals(false, layer.find(1, true))
        assertEquals(42, layer.find(2, 0))
        assertEquals(1.5f, layer.find(3, 0f))
        assertEquals("custom", layer.find(4, "original"))
        assertNull(layer.find(1, 0))
        assertNull(layer.find(2, true))
        assertNull(layer.find(3, 0.0))
        assertNull(layer.find(4, emptyList<String>()))
        assertNull(layer.find(999, true))
    }

    @Test
    fun nullHostValueCanUseAnyConfiguredType() {
        val layer = PropsHook.PropLayer().apply {
            booleans[1] = false
            integers[2] = 42
            floats[3] = 1.5f
            strings[4] = "custom"
        }

        assertEquals(false, layer.find(1, null))
        assertEquals(42, layer.find(2, null))
        assertEquals(1.5f, layer.find(3, null))
        assertEquals("custom", layer.find(4, null))
        assertNull(layer.find(999, null))
    }
}
