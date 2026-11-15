package edu.ap.tobadtoostay.ui.location

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import edu.ap.tobadtoostay.data.location.LOCATION_PERMISSIONS
import edu.ap.tobadtoostay.data.location.LocationAccess
import edu.ap.tobadtoostay.data.location.locationAccess

class LocationPermissionState(
    val access: LocationAccess,
    /**
     * Android no longer shows the dialog ("Don't ask again", or denied twice on Android 11+),
     * the user can only change it in the app settings.
     */
    val isPermanentlyDenied: Boolean,
    private val onRequest: () -> Unit,
    private val onOpenSettings: () -> Unit
) {
    /** Shows the Android dialog, or opens the app settings when the dialog is blocked. */
    fun request() = if (isPermanentlyDenied) onOpenSettings() else onRequest()
}

/**
 * Keeps track of the location permission and asks it with the Android dialog.
 * The state is checked again when the screen comes back (e.g. after the app settings).
 *
 * Android has no direct check for "the dialog is blocked". After a denied request without
 * rationale, it is blocked if the rationale was shown before, or if we already asked before
 * in this session. So a dismissed dialog (tap outside) is not seen as blocked, and after a
 * restart a blocked user needs one extra tap before the settings button appears.
 */
@Composable
fun rememberLocationPermissionState(): LocationPermissionState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var access by remember { mutableStateOf(context.locationAccess()) }
    var permanentlyDenied by rememberSaveable { mutableStateOf(false) }
    var askedBefore by rememberSaveable { mutableStateOf(false) }
    var rationaleBefore by remember { mutableStateOf(false) }

    fun refresh() {
        access = context.locationAccess()
        if (access != LocationAccess.DENIED) permanentlyDenied = false
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refresh()
        if (access == LocationAccess.DENIED) {
            val rationaleAfter = context.shouldShowRationale()
            permanentlyDenied = !rationaleAfter && (rationaleBefore || askedBefore)
        }
        askedBefore = true
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return LocationPermissionState(
        access = access,
        isPermanentlyDenied = permanentlyDenied,
        onRequest = {
            rationaleBefore = context.shouldShowRationale()
            launcher.launch(LOCATION_PERMISSIONS)
        },
        onOpenSettings = {
            context.startActivitySafely(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            )
        }
    )
}

private fun Context.shouldShowRationale(): Boolean {
    val activity = findActivity() ?: return false
    return LOCATION_PERMISSIONS.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
}

/** Some devices (Android Go, TV...) don't have every settings screen. */
internal fun Context.startActivitySafely(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Nothing to open, the explanation on the card stays visible
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
