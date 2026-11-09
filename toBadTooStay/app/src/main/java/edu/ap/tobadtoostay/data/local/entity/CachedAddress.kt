package edu.ap.tobadtoostay.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

/** Cached Nominatim reverse geocoding result, so we don't ask the same location twice. */
@Entity(tableName = "cached_addresses")
data class CachedAddress(
    @PrimaryKey val key: String,
    val displayName: String,
    val city: String,
    val fetchedAt: Long = System.currentTimeMillis()
) {
    companion object {
        /** Rounds to 4 decimals (about 10 m), so nearby points share one cache entry. */
        fun keyFor(lat: Double, lon: Double): String =
            String.format(Locale.US, "%.4f,%.4f", lat, lon)
    }
}
