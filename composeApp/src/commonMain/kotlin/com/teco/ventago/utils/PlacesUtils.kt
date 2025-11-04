package com.teco.ventago.utils

import com.teco.ventago.features.business.domain.model.BusinessAddress

expect fun launchAutocompleteWidget(
    onAddressSelected: (formattedAddress: BusinessAddress) -> Unit,
    onCancelled: () -> Unit
)

expect fun openMapUrl(placeId: String?)