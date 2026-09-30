package com.teco.ventago.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.teco.ventago.features.business.domain.model.BusinessAddress
import org.koin.java.KoinJavaComponent

object AutocompleteLauncher {
    private val session = AddressAutocompleteSession { error ->
        Log.w("PlacesAutocomplete", "Autocomplete failed (${error.javaClass.simpleName})")
    }

    fun attach(activity: ComponentActivity, launcher: ActivityResultLauncher<Intent>) {
        session.attach(activity) {
            check(!activity.isFinishing && !activity.isDestroyed &&
                activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                "Autocomplete requires a resumed activity"
            }
            check(ensurePlacesInitialized(activity)) { "Places is unavailable" }
            val fields = listOf(Place.Field.ID, Place.Field.FORMATTED_ADDRESS, Place.Field.LOCATION)
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                .build(activity)
            launcher.launch(intent)
        }
    }

    fun detach(activity: ComponentActivity) =
        session.detach(activity, keepPendingResult = activity.isChangingConfigurations)

    fun complete(address: BusinessAddress?) = session.complete(address)

    fun launch(onResult: (BusinessAddress?) -> Unit) = session.launch(onResult)
}

private const val PLACES_API_KEY = "AIzaSyArIiQadvA4yny2iITHvIVPHSzbaQY_Kz0"

@Synchronized
fun ensurePlacesInitialized(context: Context): Boolean {
    return try {
        if (!Places.isInitialized()) {
            Places.initialize(context.applicationContext, PLACES_API_KEY)
        }
        Places.isInitialized()
    } catch (error: RuntimeException) {
        Log.w("PlacesAutocomplete", "Places initialization failed (${error.javaClass.simpleName})")
        false
    }
}

actual fun launchAutocompleteWidget(
    onAddressSelected: (formattedAddress: BusinessAddress) -> Unit,
    onCancelled: () -> Unit
) {
    AutocompleteLauncher.launch { result ->
        if (result != null) onAddressSelected(result) else onCancelled()
    }
}

actual fun openMapUrl(placeId: String?) {
    val context: Context = KoinJavaComponent.getKoin().get()
    val encodedPlaceId = Uri.encode(placeId ?: "")
    val uri =
        "https://www.google.com/maps/search/?api=1&query=Google&query_place_id=$encodedPlaceId".toUri()

    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps") // Optional: force Google Maps
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)    // If using ApplicationContext
    }

    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // fallback to any browser if Google Maps is not available
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}
