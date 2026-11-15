package edu.ap.tobadtoostay.data.repository

import edu.ap.tobadtoostay.data.local.dao.CachedAddressDao
import edu.ap.tobadtoostay.data.local.entity.CachedAddress
import edu.ap.tobadtoostay.data.remote.NominatimApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Reverse geocoding with Nominatim.
 * First looks in the Room cache, and never sends more than 1 request per second.
 */
class GeocodingRepository(
    private val api: NominatimApi,
    private val cache: CachedAddressDao
) {

    suspend fun reverse(lat: Double, lon: Double): CachedAddress {
        val key = CachedAddress.keyFor(lat, lon)
        cache.getByKey(key)?.let { return it }

        val response = rateLimited { api.reverse(lat, lon, zoom = 18) }
        val address = response.address
        val result = CachedAddress(
            key = key,
            displayName = response.displayName.orEmpty(),
            city = address?.cityName.orEmpty(),
            road = address?.road.orEmpty(),
            houseNumber = address?.houseNumber.orEmpty(),
            postcode = address?.postcode.orEmpty()
        )
        cache.upsert(result)
        return result
    }

    private suspend fun <T> rateLimited(block: suspend () -> T): T = mutex.withLock {
        val wait = lastRequestAt + MIN_INTERVAL_MS - System.currentTimeMillis()
        if (wait > 0) delay(wait)
        try {
            block()
        } finally {
            lastRequestAt = System.currentTimeMillis()
        }
    }

    companion object {
        private const val MIN_INTERVAL_MS = 1_000L

        // Shared by every instance, so two screens together still stay under 1 request per second
        private val mutex = Mutex()
        private var lastRequestAt = 0L
    }
}
