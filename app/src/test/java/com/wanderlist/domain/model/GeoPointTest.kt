package com.wanderlist.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoPointTest {
    private val krakow = GeoPoint(50.0617, 19.9373)
    private val warsaw = GeoPoint(52.2297, 21.0122)

    @Test
    fun `distance between Krakow and Warsaw is about 252 km`() {
        assertEquals(252_000.0, krakow.distanceMetersTo(warsaw), 2_000.0)
    }

    @Test
    fun `distance is symmetric and zero for the same point`() {
        assertEquals(krakow.distanceMetersTo(warsaw), warsaw.distanceMetersTo(krakow), 0.001)
        assertEquals(0.0, krakow.distanceMetersTo(krakow), 0.001)
    }

    @Test
    fun `coordinates outside the valid range are rejected`() {
        assertTrue(krakow.isValid)
        assertFalse(GeoPoint(91.0, 0.0).isValid)
        assertFalse(GeoPoint(0.0, -180.5).isValid)
    }
}
