package edu.ap.tobadtoostay.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import edu.ap.tobadtoostay.data.local.entity.CachedReservation
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedReservationDao {

    // no ORDER BY: Orders.split sorts active and past orders each in their own way
    @Query("SELECT * FROM cached_reservations WHERE customerId = :customerId")
    fun observeForCustomer(customerId: String): Flow<List<CachedReservation>>

    @Upsert
    suspend fun upsertAll(reservations: List<CachedReservation>)

    @Query("DELETE FROM cached_reservations WHERE customerId = :customerId")
    suspend fun deleteForCustomer(customerId: String)

    /** Removes old orders of the other accounts that used this phone. */
    @Query("DELETE FROM cached_reservations WHERE customerId != :customerId AND pickupEnd < :before")
    suspend fun deleteOthersEndedBefore(customerId: String, before: Long)

    /** Replaces the cache of one customer with the latest Firestore snapshot in one transaction. */
    @Transaction
    suspend fun replaceForCustomer(customerId: String, reservations: List<CachedReservation>) {
        deleteForCustomer(customerId)
        upsertAll(reservations)
    }
}
