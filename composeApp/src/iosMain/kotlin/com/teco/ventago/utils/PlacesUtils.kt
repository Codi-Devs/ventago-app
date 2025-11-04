package com.teco.ventago.utils

import com.teco.ventago.features.business.domain.model.BusinessAddress
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun launchAutocompleteWidget(
    onAddressSelected: (formattedAddress: BusinessAddress) -> Unit,
    onCancelled: () -> Unit
) {
//    val controller = GMSAutocompleteViewController().apply {
//        delegate = object : NSObject(), GMSAutocompleteViewControllerDelegateProtocol {
//            override fun didAutocompleteWith(place: GMSPlace) {
//                getRootViewController()?.dismissViewControllerAnimated(true, completion = null)
//                onAddressSelected(place.formattedAddress ?: "Unknown")
//            }
//
//            override fun didFailAutocompleteWithError(error: NSError) {
//                getRootViewController()?.dismissViewControllerAnimated(true, completion = null)
//                onCancelled()
//            }
//
//            override fun wasCancelled() {
//                getRootViewController()?.dismissViewControllerAnimated(true, completion = null)
//                onCancelled()
//            }
//        }
//    }
//
//    getRootViewController()?.presentViewController(controller, true, completion = null)
}

actual fun openMapUrl(placeId: String?) {
    val encodedPlaceId = placeId ?: ""
    val url = "https://www.google.com/maps/search/?api=1&query=Google&query_place_id=$encodedPlaceId"
    val nsUrl = NSURL.URLWithString(url)

    if (nsUrl != null) {
        UIApplication.sharedApplication.openURL(nsUrl)
    }
}