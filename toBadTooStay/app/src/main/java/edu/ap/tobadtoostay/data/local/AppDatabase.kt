package edu.ap.tobadtoostay.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import edu.ap.tobadtoostay.data.local.dao.CachedAddressDao
import edu.ap.tobadtoostay.data.local.dao.CachedReservationDao
import edu.ap.tobadtoostay.data.local.dao.FavoriteStoreDao
import edu.ap.tobadtoostay.data.local.entity.CachedAddress
import edu.ap.tobadtoostay.data.local.entity.CachedReservation
import edu.ap.tobadtoostay.data.local.entity.FavoriteStore

@Database(
    entities = [FavoriteStore::class, CachedReservation::class, CachedAddress::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun favoriteStoreDao(): FavoriteStoreDao
    abstract fun cachedReservationDao(): CachedReservationDao
    abstract fun cachedAddressDao(): CachedAddressDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /** Returns the single database instance for the whole app. */
        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tobadtoostay.db"
                )
                    // No migrations yet: a schema change rebuilds the local database
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}
