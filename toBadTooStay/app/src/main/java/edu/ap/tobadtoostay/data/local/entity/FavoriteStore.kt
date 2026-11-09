package edu.ap.tobadtoostay.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A store the customer marked as favorite (K12). Only stored on this device. */
@Entity(tableName = "favorite_stores")
data class FavoriteStore(
    @PrimaryKey val storeId: String,
    val name: String,
    val address: String,
    val addedAt: Long = System.currentTimeMillis()
)
