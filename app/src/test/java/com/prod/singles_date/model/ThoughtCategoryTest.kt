package com.prod.singles_date.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThoughtCategoryTest {
    @Test
    fun isValid_acceptsBlankAndKnownCategories() {
        assertTrue(ThoughtCategory.isValid(""))
        assertTrue(ThoughtCategory.isValid(ThoughtCategory.TRAFFIC))
        assertTrue(ThoughtCategory.isValid(ThoughtCategory.STARTUP))
    }

    @Test
    fun isValid_rejectsUnknownCategory() {
        assertFalse(ThoughtCategory.isValid("politics"))
    }

    @Test
    fun displayName_mapsKnownIds() {
        assertTrue(ThoughtCategory.displayName(ThoughtCategory.WORK).contains("Work"))
    }
}
