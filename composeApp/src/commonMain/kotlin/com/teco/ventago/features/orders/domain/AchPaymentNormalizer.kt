package com.teco.ventago.features.orders.domain

import com.teco.ventago.features.orders.domain.models.AchPaymentDetail
import com.teco.ventago.features.orders.domain.models.AchTimelineItem
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

object AchPaymentNormalizer {

    fun fromApiPayload(data: JsonObject?): AchPaymentDetail {
        if (data == null) return AchPaymentDetail()

        val payment = obj(data, "payment")
        val account = obj(data, "account")
        val proof = obj(data, "proof")
        val primaryProof = primaryProof(data)
        val latestFraud = obj(data, "latest_fraud")
        val latestOcr = obj(data, "latest_ocr")

        val paymentId = firstNonBlank(
            strOrNull(payment, "id"),
            strOrNull(data, "payment_id", "id"),
            strOrNull(payment, "payment_intent_id", "uid"),
            strOrNull(data, "payment_uid", "payment_intent_id", "uid")
        )
        val paymentUid = firstNonBlank(
            strOrNull(data, "payment_uid", "payment_intent_id", "uid", "id"),
            strOrNull(payment, "id", "payment_intent_id", "uid"),
            paymentId
        )

        val proofId = firstNonBlank(
            strOrNull(primaryProof, "id", "proof_id", "uid"),
            strOrNull(proof, "id", "proof_id", "uid"),
            strOrNull(data, "proof_id")
        )
        val proofContentType = firstNonBlank(
            strOrNull(primaryProof, "mime_type", "content_type", "proof_content_type"),
            strOrNull(proof, "mime_type", "content_type", "proof_content_type"),
            strOrNull(data, "proof_content_type")
        )
        val proofFileName = firstNonBlank(
            strOrNull(primaryProof, "file_name", "filename", "name", "proof_file_name"),
            strOrNull(proof, "file_name", "filename", "name", "proof_file_name"),
            strOrNull(data, "proof_file_name"),
            buildProofFallbackFileName(proofId = proofId, contentType = proofContentType)
        )

        return AchPaymentDetail(
            paymentId = paymentId,
            paymentUid = paymentUid.orEmpty(),
            paymentStatus = str(payment, "status", "payment_status_str", "payment_status", "status")
                .ifBlank { str(data, "payment_status_str", "payment_status", "status") },
            amount = firstNonNull(
                dblOrNull(data, "order_amount", "amount", "total_amount"),
                dblOrNull(payment, "amount", "order_amount"),
                dblOrNull(latestOcr, "detected_amount")
            ) ?: 0.0,
            currencyCode = firstNonBlank(
                strOrNull(data, "currency_code", "currency"),
                strOrNull(payment, "currency_code", "currency"),
                strOrNull(account, "currency_code"),
                "USD"
            ).orEmpty(),
            reference = firstNonBlank(
                strOrNull(payment, "reference_code", "reference", "payment_reference"),
                strOrNull(data, "reference", "payment_reference"),
                eventReference(data)
            ).orEmpty(),
            paymentDate = firstNonBlank(
                strOrNull(payment, "created_at", "payment_date"),
                strOrNull(data, "payment_date", "created_at")
            ),
            customerName = firstNonBlank(
                strOrNull(payment, "payer_name", "customer_name", "customer"),
                strOrNull(data, "customer_name", "customer")
            ).orEmpty(),
            customerEmail = firstNonBlank(
                strOrNull(payment, "payer_email", "customer_email"),
                strOrNull(data, "customer_email")
            ).orEmpty(),
            orderNumber = firstNonBlank(
                strOrNull(data, "order_number", "internal_number"),
                strOrNull(payment, "order_internal_id", "order_number"),
                strOrNull(data, "order_internal_id", "order_external_uuid")
            ).orEmpty(),
            bankName = firstNonBlank(
                strOrNull(account, "bank_name"),
                strOrNull(data, "bank_name"),
                strOrNull(latestOcr, "detected_bank_name")
            ).orEmpty(),
            destinationAccount = firstNonBlank(
                strOrNull(account, "account_number_masked", "destination_account"),
                strOrNull(data, "destination_account", "account_number_masked")
            ).orEmpty(),
            proofId = proofId,
            proofFileUrl = firstNonBlank(
                strOrNull(primaryProof, "proof_file_url", "proof_url", "file_url", "url"),
                strOrNull(proof, "proof_file_url", "proof_url", "file_url", "url"),
                strOrNull(data, "proof_file_url", "proof_url")
            ),
            proofFileName = proofFileName,
            proofContentType = proofContentType,
            riskScore = firstNonNull(
                int(payment, "risk_score"),
                int(data, "risk_score"),
                int(latestFraud, "final_score")
            ),
            riskLevel = firstNonBlank(
                strOrNull(payment, "risk_status", "risk_level"),
                strOrNull(data, "risk_level"),
                strOrNull(latestFraud, "risk_band")
            ),
            decisionSuggested = firstNonBlank(
                strOrNull(payment, "decision_suggested"),
                strOrNull(data, "decision_suggested"),
                strOrNull(latestFraud, "decision")
            ),
            latestFraud = latestFraud?.toString() ?: data["latest_fraud"]?.toString(),
            latestOcr = latestOcr?.toString() ?: data["latest_ocr"]?.toString(),
            timeline = timeline(data),
        )
    }

    private fun timeline(data: JsonObject): List<AchTimelineItem> {
        val timelinePayload = timeline(data["timeline"])
        if (timelinePayload.isNotEmpty()) return timelinePayload
        return eventsTimeline(data["events"])
    }

    private fun timeline(node: JsonElement?): List<AchTimelineItem> {
        val arr = node as? JsonArray ?: return emptyList()
        return arr.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            AchTimelineItem(
                status = str(obj, "status", "state"),
                title = str(obj, "title", "event"),
                message = str(obj, "message", "description"),
                at = str(obj, "at", "created_at", "timestamp"),
            )
        }
    }

    private fun eventsTimeline(node: JsonElement?): List<AchTimelineItem> {
        val arr = node as? JsonArray ?: return emptyList()
        return arr.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val eventType = str(obj, "event_type", "event", "type").ifBlank { "evento" }
            val payload = obj["payload"] as? JsonObject
            AchTimelineItem(
                status = eventStatus(eventType, payload),
                title = eventTitle(eventType, obj),
                message = eventMessage(eventType, payload),
                at = str(obj, "created_at", "timestamp", "at"),
            )
        }
    }

    private fun eventTitle(eventType: String, source: JsonObject): String {
        return when (normalizeToken(eventType)) {
            "checkout_created" -> "Checkout ACH creado"
            "proof_uploaded" -> "Comprobante subido"
            "ocr_processed" -> "OCR procesado"
            "approved" -> "approved"
            else -> str(source, "title").ifBlank {
                eventType.replace('_', ' ').replaceFirstChar { it.uppercase() }
            }
        }
    }

    private fun eventStatus(eventType: String, payload: JsonObject?): String {
        return when (normalizeToken(eventType)) {
            "proof_uploaded" -> "pending_review"
            "ocr_processed" -> str(payload, "status").ifBlank { "pending_review" }
            else -> eventType
        }
    }

    private fun eventMessage(eventType: String, payload: JsonObject?): String {
        payload ?: return ""
        return when (normalizeToken(eventType)) {
            "checkout_created" -> {
                strOrNull(payload, "reference_id", "reference", "reference_code")
                    ?.let { "ID referencia: $it" }
                    .orEmpty()
            }
            "proof_uploaded" -> {
                strOrNull(payload, "proof_id")
                    ?.let { "Comprobante: $it" }
                    .orEmpty()
            }
            "ocr_processed" -> {
                val parts = listOfNotNull(
                    strOrNull(payload, "proof_id")?.let { "Comprobante: $it" },
                    strOrNull(payload, "risk_score")?.let { "Riesgo: $it" },
                    strOrNull(payload, "status")?.let { "Estado: ${formatOcrStatus(it)}" }
                )
                parts.joinToString(separator = " · ")
            }
            else -> defaultEventMessage(payload)
        }
    }

    private fun defaultEventMessage(payload: JsonObject?): String {
        payload ?: return ""
        return firstNonBlank(
            strOrNull(payload, "notes", "message", "description", "status", "reference_id"),
            strOrNull(payload, "proof_id")?.let { "Comprobante: $it" }
        ).orEmpty()
    }

    private fun eventReference(data: JsonObject): String? {
        val events = data["events"] as? JsonArray ?: return null
        val checkout = events
            .asSequence()
            .mapNotNull { it as? JsonObject }
            .firstOrNull { str(it, "event_type").equals("checkout_created", ignoreCase = true) }
            ?: return null
        val payload = checkout["payload"] as? JsonObject ?: return null
        return strOrNull(payload, "reference_id", "reference", "reference_code")
    }

    private fun primaryProof(data: JsonObject): JsonObject? {
        val proofs = data["proofs"] as? JsonArray ?: return null
        val mapped = proofs.mapNotNull { it as? JsonObject }
        return mapped.firstOrNull { bool(it, "is_primary") == true } ?: mapped.firstOrNull()
    }

    private fun obj(source: JsonObject?, key: String): JsonObject? {
        return source?.get(key) as? JsonObject
    }

    private fun bool(obj: JsonObject?, vararg keys: String): Boolean? {
        obj ?: return null
        return keys.firstNotNullOfOrNull { key ->
            obj[key]?.jsonPrimitive?.booleanOrNull
                ?: obj[key]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()
        }
    }

    private fun <T> firstNonNull(vararg values: T?): T? {
        return values.firstOrNull { it != null }
    }

    private fun firstNonBlank(vararg values: String?): String? {
        return values.firstOrNull { !it.isNullOrBlank() }?.trim()
    }

    private fun buildProofFallbackFileName(proofId: String?, contentType: String?): String? {
        val id = proofId?.ifBlank { null } ?: return null
        val ext = when {
            contentType.orEmpty().contains("pdf", ignoreCase = true) -> "pdf"
            contentType.orEmpty().contains("png", ignoreCase = true) -> "png"
            contentType.orEmpty().contains("jpeg", ignoreCase = true) ||
                contentType.orEmpty().contains("jpg", ignoreCase = true) -> "jpg"
            else -> "bin"
        }
        return "comprobante_ach_$id.$ext"
    }

    private fun formatOcrStatus(status: String): String {
        return when (normalizeToken(status)) {
            "pending_review" -> "Revisión pendiente"
            "completed" -> "Completado"
            else -> status.replace('_', ' ').replaceFirstChar { it.uppercase() }
        }
    }

    private fun normalizeToken(raw: String): String {
        return raw.trim().lowercase().replace('-', '_').replace(' ', '_')
    }

    private fun str(obj: JsonObject?, vararg keys: String): String {
        obj ?: return ""
        return keys.firstNotNullOfOrNull { key ->
            obj[key]?.let { el ->
                when (el) {
                    is JsonPrimitive -> el.content
                    is JsonObject -> el["value"]?.jsonPrimitive?.contentOrNull
                    else -> el.toString()
                }
            }?.takeIf { it.isNotBlank() }
        }.orEmpty()
    }

    private fun strOrNull(obj: JsonObject?, vararg keys: String): String? {
        val v = str(obj, *keys)
        return v.ifBlank { null }
    }

    private fun dblOrNull(obj: JsonObject?, vararg keys: String): Double? {
        obj ?: return null
        return keys.firstNotNullOfOrNull { key ->
            obj[key]?.jsonPrimitive?.doubleOrNull
                ?: obj[key]?.jsonPrimitive?.contentOrNull?.replace(',', '.')?.toDoubleOrNull()
        }
    }

    private fun int(obj: JsonObject?, vararg keys: String): Int? {
        obj ?: return null
        return keys.firstNotNullOfOrNull { key ->
            obj[key]?.jsonPrimitive?.intOrNull
                ?: obj[key]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
        }
    }
}
