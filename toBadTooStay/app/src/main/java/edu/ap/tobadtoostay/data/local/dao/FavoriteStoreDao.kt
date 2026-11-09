package edu.ap.tobadtoostay.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import edu.ap.tobadtoostay.data.local.entity.FavoriteStore
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteStoreDao {

    @Query("SELECT * FROM favorite_stores ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FavoriteStore>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_stores WHERE storeId = :storeId)")
    fun observeIsFavorite(storeId: String): Flow<Boolean>

    @Upsert
    suspend fun upsert(store: FavoriteStore)

    @Query("DELETE FROM favorite_stores WHERE storeId = :storeId")
    suspend fun deleteById(storeId: String)
}
