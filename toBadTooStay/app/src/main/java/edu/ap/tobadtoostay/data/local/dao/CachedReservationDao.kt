package edu.ap.tobadtoostay.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import edu.ap.tobadtoostay.data.local.entity.CachedReservation
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedReservationDao {

    @Query("SELECT * FROM cached_reservations ORDER BY pickupStart DESC")
    fun observeAll(): Flow<List<CachedReservation>>

    @Upsert
    suspend fun upsertAll(reservations: List<CachedReservation>)

    @Query("DELETE FROM cached_reservations")
    suspend fun deleteAll()

    /** Replaces the cache with the latest Firestore snapshot in one transaction. */
    @Transaction
    suspend fun replaceAll(reservations: List<CachedReservation>) {
        deleteAll()
        upsertAll(reservations)
    }
}
