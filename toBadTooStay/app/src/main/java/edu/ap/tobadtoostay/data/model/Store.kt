package edu.ap.tobadtoostay.data.model

/** A store in Firestore: stores/{storeId}, where storeId is the uid of the merchant. */
data class Store(
    val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val category: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val street: String = "",
    val postalCode: String = "",
    val city: String = ""
) {
    /** Full address as one line, saved in the "address" field. */
    val address: String
        get() = "$street, $postalCode $city"
}

/** Store categories. The key is saved in Firestore, the label comes from strings.xml. */
enum class StoreCategory(val key: String) {
    BAKERY("bakery"),
    SUPERMARKET("supermarket"),
    RESTAURANT("restaurant"),
    CAFE("cafe"),
    BUTCHER("butcher"),
    GREENGROCER("greengrocer"),
    OTHER("other");

    companion object {
        fun fromKey(key: String): StoreCategory? = entries.firstOrNull { it.key == key }
    }
}
