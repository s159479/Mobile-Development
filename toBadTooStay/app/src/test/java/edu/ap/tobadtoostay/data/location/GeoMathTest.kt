package edu.ap.tobadtoostay.data.location

import edu.ap.tobadtoostay.data.model.MapPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoMathTest {

    private val antwerpCentral = MapPoint(51.2172, 4.4211)
    private val groenplaats = MapPoint(51.2190, 4.4010)
    private val brusselsCentral = MapPoint(50.8455, 4.3571)

    @Test
    fun distanceToSamePointIsZero() {
        assertEquals(0.0, GeoMath.distanceMeters(antwerpCentral, antwerpCentral), 0.001)
    }

    @Test
    fun distanceInsideAntwerp() {
        // About 1.4 km between Antwerp Central and the Groenplaats
        assertEquals(1_420.0, GeoMath.distanceMeters(antwerpCentral, groenplaats), 50.0)
    }

    @Test
    fun distanceAntwerpToBrussels() {
        // About 41.6 km in a straight line
        assertEquals(41_600.0, GeoMath.distanceMeters(antwerpCentral, brusselsCentral), 500.0)
    }

    @Test
    fun distanceIsTheSameInBothDirections() {
        assertEquals(
            GeoMath.distanceMeters(antwerpCentral, brusselsCentral),
            GeoMath.distanceMeters(brusselsCentral, antwerpCentral),
            0.001
        )
    }

    @Test
    fun latitudeRangeContainsEveryPointWithinTheRadius() {
        val radius = 5_000.0
        val range = GeoMath.latitudeRange(antwerpCentral, radius)

        // Straight north and south, the furthest latitude a point within the radius can have
        val north = MapPoint(range.endInclusive, antwerpCentral.longitude)
        val south = MapPoint(range.start, antwerpCentral.longitude)
        assertTrue(GeoMath.distanceMeters(antwerpCentral, north) >= radius - 1)
        assertTrue(GeoMath.distanceMeters(antwerpCentral, south) >= radius - 1)
    }

    @Test
    fun latitudeRangeStaysOnTheGlobe() {
        val range = GeoMath.latitudeRange(MapPoint(89.99, 0.0), 50_000.0)
        assertEquals(90.0, range.endInclusive, 0.0)
    }
}
