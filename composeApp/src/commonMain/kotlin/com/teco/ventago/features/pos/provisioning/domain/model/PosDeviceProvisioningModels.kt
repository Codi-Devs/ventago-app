package com.teco.ventago.features.pos.provisioning.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PosAgentDeviceConfig(
    @SerialName("device_id") val deviceId: String,
    @SerialName("business_id") val businessId: Int,
    @SerialName("branch_code") val branchCode: String,
    @SerialName("billing_point_code") val billingPointCode: String,
) {
    fun isComplete(): Boolean =
        deviceId.isNotBlank() &&
            businessId > 0 &&
            branchCode.isNotBlank() &&
            billingPointCode.isNotBlank()
}

@Serializable
data class PosDeviceConfig(
    @SerialName("device_id") val deviceId: String,
    @SerialName("business_id") val businessId: Int,
    @SerialName("branch_code") val branchCode: String,
    @SerialName("billing_point_code") val billingPointCode: String,
    @SerialName("status") val status: String,
    @SerialName("permissions") val permissions: PosDevicePermissions = PosDevicePermissions(),
) {
    val active: Boolean
        get() = status.equals("active", ignoreCase = true)
}

@Serializable
data class PosDevicePermissions(
    @SerialName("device_id") val deviceId: String = "",
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
)

enum class PosLinkMode {
    NotRequired,
    Linked,
    Degraded,
    Unlinked,
}

data class PosProvisioningState(
    val required: Boolean = false,
    val agentConfig: PosAgentDeviceConfig? = null,
    val deviceConfig: PosDeviceConfig? = null,
    val valid: Boolean = !required,
    val linkMode: PosLinkMode = if (required) PosLinkMode.Unlinked else PosLinkMode.NotRequired,
) {
    val isProvisioned: Boolean
        get() = !required || valid

    val locksBranchPoint: Boolean
        get() = required &&
            valid &&
            (linkMode == PosLinkMode.Linked || linkMode == PosLinkMode.Degraded) &&
            !fixedBranchCode.isNullOrBlank() &&
            !fixedBillingPointCode.isNullOrBlank()

    val fixedBranchCode: String?
        get() = deviceConfig?.branchCode ?: agentConfig?.branchCode

    val fixedBillingPointCode: String?
        get() = deviceConfig?.billingPointCode ?: agentConfig?.billingPointCode

    val permissions: PosDevicePermissions?
        get() = deviceConfig?.permissions

    val bannerMessage: String?
        get() = when {
            !required || !valid -> null
            linkMode == PosLinkMode.Degraded -> BANNER_DEGRADED
            linkMode == PosLinkMode.Unlinked -> BANNER_UNLINKED
            else -> null
        }

    companion object {
        const val BANNER_DEGRADED =
            "VentaGo Agent no responde. Facturando con la sucursal vinculada."
        const val BANNER_UNLINKED =
            "Este equipo no está vinculado. Elige sucursal y punto para facturar."
    }
}

data class PosAgentConfigResult(
    val activated: Boolean,
    val configJson: String,
)
