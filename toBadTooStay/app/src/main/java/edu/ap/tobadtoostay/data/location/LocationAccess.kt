package edu.ap.tobadtoostay.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/** What the user allowed for the location. */
enum class LocationAccess {
    /** Precise location (GPS). */
    PRECISE,

    /** Only the approximate location, the user picked "Approximate" in the dialog. */
    APPROXIMATE,

    /** Not allowed (yet). */
    DENIED
}

/** Asked together, so the user can choose between precise and approximate (Android 12+). */
val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

/** The only place where the location permission is checked. */
fun Context.locationAccess(): LocationAccess = when {
    isGranted(Manifest.permission.ACCESS_FINE_LOCATION) -> LocationAccess.PRECISE
    isGranted(Manifest.permission.ACCESS_COARSE_LOCATION) -> LocationAccess.APPROXIMATE
    else -> LocationAccess.DENIED
}

private fun Context.isGranted(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
