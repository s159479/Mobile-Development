package edu.ap.tobadtoostay.data.location

import edu.ap.tobadtoostay.data.model.MapPoint
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Distance calculations in plain Kotlin, so they can be tested without Android. */
object GeoMath {

    private const val EARTH_RADIUS_METERS = 6_371_000.0
    // Same earth radius as the distance, otherwise the range is a bit too small: 2 * PI * R / 360
    private const val METERS_PER_DEGREE_LATITUDE = 2 * Math.PI * EARTH_RADIUS_METERS / 360

    /** Distance in meters between two points over the surface of the earth (haversine formula). */
    fun distanceMeters(from: MapPoint, to: MapPoint): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(to.longitude - from.longitude)

        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    /**
     * Latitudes between which every point within [radiusMeters] of [center] lies.
     * Firestore sorts a GeoPoint on latitude first, so this range can be used in a query.
     */
    fun latitudeRange(center: MapPoint, radiusMeters: Double): ClosedFloatingPointRange<Double> {
        val delta = radiusMeters / METERS_PER_DEGREE_LATITUDE
        return (center.latitude - delta).coerceAtLeast(-90.0)..(center.latitude + delta).coerceAtMost(90.0)
    }
}
