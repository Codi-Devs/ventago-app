package com.teco.ventago.features.invoicing.domain.models

enum class FeCustomerType(val code: String, val description: String) {
    CONTRIBUTING("01", "Contribuyente"),
    FINAL_CONSUMER("02", "Consumidor Final"),
    GOVERNMENT("03", "Gobierno"),
    FOREIGNER("04", "Extranjero"),
}

fun FeCustomerType.rucNeeded(): Boolean {
    return this != FeCustomerType.FINAL_CONSUMER && this != FeCustomerType.FOREIGNER
}