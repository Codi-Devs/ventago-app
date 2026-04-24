package com.teco.ventago.features.orders.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class AchPaymentDetail(
    val paymentId: String? = null,
    val paymentUid: String = "",
    val paymentStatus: String = "",
    val amount: Double = 0.0,
    val currencyCode: String = "USD",
    val reference: String = "",
    val paymentDate: String? = null,
    val customerName: String = "",
    val customerEmail: String = "",
    val orderNumber: String = "",
    val bankName: String = "",
    val destinationAccount: String = "",
    val proofId: String? = null,
    val proofFileUrl: String? = null,
    val proofFileName: String? = null,
    val proofContentType: String? = null,
    val riskScore: Int? = null,
    val riskLevel: String? = null,
    val decisionSuggested: String? = null,
    val latestFraud: String? = null,
    val latestOcr: String? = null,
    val timeline: List<AchTimelineItem> = emptyList(),
)

@Serializable
data class AchTimelineItem(
    val status: String = "",
    val title: String = "",
    val message: String = "",
    val at: String = "",
)

data class AchProofFileDownload(
    val bytes: ByteArray,
    val contentType: String? = null,
    val fileName: String? = null,
)
