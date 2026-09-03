package dev.openradio.android

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocaleManagerTest {

    @Test
    fun `supportsLanguage returns true for en`() {
        assertTrue(LocaleManager.supportsLanguage("en"))
    }

    @Test
    fun `supportsLanguage returns true for te`() {
        assertTrue(LocaleManager.supportsLanguage("te"))
    }

    @Test
    fun `supportsLanguage returns true for hi`() {
        assertTrue(LocaleManager.supportsLanguage("hi"))
    }

    @Test
    fun `supportsLanguage returns false for unsupported languages`() {
        assertFalse(LocaleManager.supportsLanguage("fr"))
        assertFalse(LocaleManager.supportsLanguage("es"))
        assertFalse(LocaleManager.supportsLanguage(""))
        assertFalse(LocaleManager.supportsLanguage("EN"))
    }
}
