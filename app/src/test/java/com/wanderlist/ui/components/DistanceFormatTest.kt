package com.wanderlist.ui.components

import com.wanderlist.domain.model.DistanceUnit
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class DistanceFormatTest {
    private val pl = Locale.forLanguageTag("pl-PL")

    @Test
    fun `kilometres use metres below 1 km and one decimal below 100 km`() {
        assertEquals("850 m", formatDistance(850.4, DistanceUnit.KILOMETERS, pl))
        assertEquals("12,3 km", formatDistance(12_345.0, DistanceUnit.KILOMETERS, pl))
        assertEquals("252 km", formatDistance(252_400.0, DistanceUnit.KILOMETERS, pl))
    }

    @Test
    fun `miles use feet for very short distances`() {
        assertEquals("98 ft", formatDistance(30.0, DistanceUnit.MILES, pl))
        assertEquals("6,2 mi", formatDistance(10_000.0, DistanceUnit.MILES, pl))
        assertEquals("156 mi", formatDistance(252_000.0, DistanceUnit.MILES, pl))
    }
}
