package edu.ap.tobadtoostay.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import edu.ap.tobadtoostay.data.local.entity.CachedAddress

@Dao
interface CachedAddressDao {

    @Query("SELECT * FROM cached_addresses WHERE `key` = :key")
    suspend fun getByKey(key: String): CachedAddress?

    @Upsert
    suspend fun upsert(address: CachedAddress)
}
