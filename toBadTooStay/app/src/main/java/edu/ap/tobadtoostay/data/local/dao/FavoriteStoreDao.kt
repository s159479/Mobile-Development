package edu.ap.tobadtoostay.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import edu.ap.tobadtoostay.data.local.entity.FavoriteStore
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteStoreDao {

    @Query("SELECT * FROM favorite_stores WHERE customerId = :customerId ORDER BY addedAt DESC")
    fun observeForCustomer(customerId: String): Flow<List<FavoriteStore>>

    @Upsert
    suspend fun upsert(store: FavoriteStore)

    @Query("DELETE FROM favorite_stores WHERE customerId = :customerId AND storeId = :storeId")
    suspend fun delete(customerId: String, storeId: String)
}
