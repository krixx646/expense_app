package com.chelumore.xpenze.core.localization

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguagesTest {

    @Test
    fun `normalizes Latvian language tag`() {
        assertEquals("lv", AppLanguages.normalizeTag("lv"))
        assertEquals("lv", AppLanguages.normalizeTag("lv-LV"))
    }

    @Test
    fun `uses English for unsupported language tags`() {
        assertEquals("en", AppLanguages.normalizeTag("et-EE"))
    }

    @Test
    fun `shows Latvian native language name`() {
        assertEquals("Latviešu", AppLanguages.displayName("lv-LV"))
    }
}
