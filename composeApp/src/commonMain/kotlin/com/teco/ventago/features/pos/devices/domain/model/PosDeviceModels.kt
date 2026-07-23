package com.teco.ventago.features.pos.devices.domain.model

import com.teco.ventago.features.pos.provisioning.domain.model.PosDevicePermissions
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PosDeviceLocation(
    @SerialName("latitude") val latitude: String? = null,
    @SerialName("longitude") val longitude: String? = null,
    @SerialName("location_accuracy_m") val locationAccuracyM: String? = null,
) {
    fun hasCoordinates(): Boolean = !latitude.isNullOrBlank() && !longitude.isNullOrBlank()
}

@Serializable
data class PosDevice(
    @SerialName("device_id") val deviceId: String,
    @SerialName("business_id") val businessId: Int? = null,
    @SerialName("name") val name: String = "",
    @SerialName("model") val model: String? = null,
    @SerialName("serial_number") val serialNumber: String = "",
    @SerialName("activation_date") val activationDate: String? = null,
    @SerialName("status") val status: String = "",
    @SerialName("battery_level") val batteryLevel: Int? = null,
    @SerialName("location") val location: PosDeviceLocation? = null,
    @SerialName("branch_code") val branchCode: String = "",
    @SerialName("billing_point_code") val billingPointCode: String = "",
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
    @SerialName("pos_app_version") val posAppVersion: String? = null,
    @SerialName("network_type") val networkType: String? = null,
    @SerialName("connectivity_status") val connectivityStatus: String? = null,
    @SerialName("last_reboot_at") val lastRebootAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("permissions") val permissions: PosDevicePermissions? = null,
) {
    val displayModel: String
        get() = model?.takeIf { it.isNotBlank() } ?: "H10P"

    val displayName: String
        get() = name.takeIf { it.isNotBlank() } ?: displayModel
}

@Serializable
data class PosDevicesPage(
    @SerialName("items") val items: List<PosDevice> = emptyList(),
    @SerialName("total") val total: Int = 0,
    @SerialName("page") val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 20,
    @SerialName("total_pages") val totalPages: Int = 1,
)

@Serializable
data class PosDeviceConfig(
    @SerialName("device_id") val deviceId: String,
    @SerialName("business_id") val businessId: Int? = null,
    @SerialName("branch_code") val branchCode: String = "",
    @SerialName("billing_point_code") val billingPointCode: String = "",
    @SerialName("status") val status: String = "",
    @SerialName("permissions") val permissions: PosDevicePermissions? = null,
)

data class PosDevicesListRequest(
    val page: Int = 1,
    val pageSize: Int = 20,
    val status: String? = "active",
)

@Serializable
data class UpdatePosDeviceRequest(
    @SerialName("name") val name: String,
    @SerialName("branch_code") val branchCode: String,
    @SerialName("billing_point_code") val billingPointCode: String,
)

@Serializable
data class UpdatePosDevicePermissionsRequest(
    @SerialName("expenses_view") val expensesView: Boolean = false,
    @SerialName("expenses_create") val expensesCreate: Boolean = false,
    @SerialName("products_view") val productsView: Boolean = false,
    @SerialName("products_create") val productsCreate: Boolean = false,
    @SerialName("clients_view") val clientsView: Boolean = false,
    @SerialName("clients_create") val clientsCreate: Boolean = false,
    @SerialName("quotes_view") val quotesView: Boolean = false,
    @SerialName("quotes_create") val quotesCreate: Boolean = false,
    @SerialName("payment_methods_configure") val paymentMethodsConfigure: Boolean = false,
    @SerialName("payment_yappy_onsite") val paymentYappyOnsite: Boolean = false,
    @SerialName("payment_link") val paymentLink: Boolean = false,
    @SerialName("payment_manual_methods") val paymentManualMethods: Boolean = false,
    @SerialName("reports_view") val reportsView: Boolean = false,
) {
    companion object {
        fun fromPermissions(value: PosDevicePermissions): UpdatePosDevicePermissionsRequest =
            UpdatePosDevicePermissionsRequest(
                expensesView = value.expensesView,
                expensesCreate = value.expensesCreate,
                productsView = value.productsView,
                productsCreate = value.productsCreate,
                clientsView = value.clientsView,
                clientsCreate = value.clientsCreate,
                quotesView = value.quotesView,
                quotesCreate = value.quotesCreate,
                paymentMethodsConfigure = value.paymentMethodsConfigure,
                paymentYappyOnsite = value.paymentYappyOnsite,
                paymentLink = value.paymentLink,
                paymentManualMethods = value.paymentManualMethods,
                reportsView = value.reportsView,
            )
    }
}

fun isValidPosDeviceBranchCode(value: String): Boolean =
    value.length == 4 && value.all { it.isDigit() }

fun isValidPosDeviceBillingPointCode(value: String): Boolean =
    value.length == 3 && value.all { it.isDigit() } && value != "000"
