package com.prod.singles_date.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppCityTest {
    @Test
    fun isValid_acceptsLaunchCities() {
        assertTrue(AppCity.isValid(AppCity.BANGALORE))
        assertTrue(AppCity.isValid(AppCity.DELHI))
    }

    @Test
    fun isValid_rejectsUnknownCity() {
        assertFalse(AppCity.isValid("london"))
        assertFalse(AppCity.isValid(""))
    }

    @Test
    fun isExpansionUnlocked_allLaunchCities() {
        AppCity.ALL.forEach { city ->
            assertTrue(AppCity.isExpansionUnlocked(city))
        }
    }
}
