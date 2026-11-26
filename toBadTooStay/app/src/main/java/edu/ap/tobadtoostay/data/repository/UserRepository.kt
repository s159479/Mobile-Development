package edu.ap.tobadtoostay.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /** uid of the logged in user, or null when nobody is logged in. */
    fun currentUserId(): String? = auth.currentUser?.uid

    /** "customer" or "merchant" from users/{uid}, null when not logged in or the profile is missing. */
    suspend fun getMyRole(): String? {
        val uid = auth.currentUser?.uid ?: return null
        return db.collection("users").document(uid).get().await().getString("role")
    }

    fun signOut() {
        auth.signOut()
    }

    companion object {
        const val ROLE_CUSTOMER = "customer"
    }
}
