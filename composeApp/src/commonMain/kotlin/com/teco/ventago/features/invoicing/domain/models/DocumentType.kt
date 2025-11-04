package com.teco.ventago.features.invoicing.domain.models

enum class FEDocumentType(val code: String, val description: String) {
    INTERNAL_OPERATION_INVOICE("01", "Factura de Operación Interna"),
    IMPORT_INVOICE("02", "Factura de Importación"),
    EXPORT_INVOICE("03", "Factura de Exportación"),
    CREDIT_NOTE_REFERENCING_FE("04", "Nota de Crédito Referente a una FE"),
    DEBIT_NOTE_REFERENCING_FE("05", "Nota de Débito Referente a una o Varias FE"),
    GENERIC_CREDIT_NOTE("06", "Nota de Crédito Genérica"),
    GENERIC_DEBIT_NOTE("07", "Nota de Débito Genérica"),
    FREE_ZONE_INVOICE("08", "Factura de Zona Franca"),
    REIMBURSEMENT("09", "Reembolso"),
    FOREIGN_OPERATION_INVOICE("10", "Factura de Operación Extranjera");

    companion object {
        fun fromCode(code: String): FEDocumentType {
            return entries.find { it.code == code } ?: INTERNAL_OPERATION_INVOICE
        }
    }

}