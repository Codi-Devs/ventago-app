package com.teco.ventago.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.common.util.CollectionUtils.listOf
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.teco.ventago.features.business.domain.model.BusinessAddress
import org.koin.java.KoinJavaComponent
import androidx.core.net.toUri

object AutocompleteLauncher {
    lateinit var launcher: ActivityResultLauncher<Intent>
    var onResult: ((BusinessAddress?) -> Unit)? = null
}

actual fun launchAutocompleteWidget(
    onAddressSelected: (formattedAddress: BusinessAddress) -> Unit,
    onCancelled: () -> Unit
) {
    val context: Context = KoinJavaComponent.getKoin().get()
    val apiKey = "AIzaSyArIiQadvA4yny2iITHvIVPHSzbaQY_Kz0"

    if (!Places.isInitialized()) {
        Places.initialize(context.applicationContext, apiKey)
    }

    val fields: List<Place.Field> =
        listOf(Place.Field.ID, Place.Field.ADDRESS, Place.Field.LAT_LNG)
    val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
        .build(context)

    AutocompleteLauncher.onResult = { result ->
        if (result != null) onAddressSelected(result) else onCancelled()
    }

    AutocompleteLauncher.launcher.launch(intent)
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