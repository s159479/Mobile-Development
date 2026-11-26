package edu.ap.tobadtoostay.data.local.entity

import androidx.room.Entity

/**
 * A store the customer marked as favorite (K12). Only stored on this device.
 * [customerId] keeps the favorites of each account apart, so they can stay after logout.
 */
@Entity(tableName = "favorite_stores", primaryKeys = ["customerId", "storeId"])
data class FavoriteStore(
    val customerId: String,
    val storeId: String,
    val name: String,
    val address: String,
    val addedAt: Long = System.currentTimeMillis()
)
