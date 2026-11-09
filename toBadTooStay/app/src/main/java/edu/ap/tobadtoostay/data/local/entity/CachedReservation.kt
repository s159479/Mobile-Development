package edu.ap.tobadtoostay.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local copy of a Firestore reservation, so the orders screen also works offline (K10).
 * Times are epoch millis, status is "reserved", "collected" or "cancelled".
 */
@Entity(tableName = "cached_reservations")
data class CachedReservation(
    @PrimaryKey val id: String,
    val bagTitle: String,
    val storeName: String,
    val pickupCode: String,
    val pickupStart: Long,
    val pickupEnd: Long,
    val status: String
)
