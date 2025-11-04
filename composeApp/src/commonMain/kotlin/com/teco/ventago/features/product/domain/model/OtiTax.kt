package com.teco.ventago.features.product.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class OTITax(
    val id: String,
    val rate: Double
)


fun otiNameFromCode(code: String): String = when (code) {
    "01" -> "SUME911"
    "02" -> "Portabilidad Numérica"
    "03" -> "Seguro 5%"
    "04" -> "ATTT Seguro Autos 1%"
    "05" -> "Tasa Salida Aeropuerto FZ"
    "06" -> "Cargo Incentivo F3"
    "07" -> "Cargo Seguridad AH"
    "08" -> "Otros Cargos XT"
    "09" -> "Combustible YQ"
    "10" -> "FECI"
    "11" -> "Intereses"
    else -> code
}