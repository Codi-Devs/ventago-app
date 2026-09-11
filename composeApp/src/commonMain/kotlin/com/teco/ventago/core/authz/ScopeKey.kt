package com.teco.ventago.core.authz

object ScopeKey {
    const val HOME_DASHBOARD = "home:dashboard"

    const val INVOICE_VIEW = "invoice:view"
    const val INVOICE_CREATE = "invoice:create_invoice"
    const val INVOICE_CREATE_DRAFT = "invoice:create_draft"
    const val INVOICE_CREATE_NON_FISCAL = "invoice:create_non_fiscal"
    const val INVOICE_CREATE_PAYMENT_LINK = "invoice:create_payment_link"
    const val INVOICE_CANCEL = "invoice:cancel"
    const val INVOICE_CREDIT_NOTES = "invoice:credit_notes"
    const val INVOICE_CUSTOM_PRODUCT = "invoice:custom_product"
    const val INVOICE_EDIT_PRODUCT = "invoice:edit_product"
    const val INVOICE_YAPPY_ONSITE = "invoice:yappy_onsite"
    const val ACH_PAYMENT_VIEW = "ach_payment:view"
    const val ACH_PAYMENT_APPROVE = "ach_payment:approve"
    const val ACH_PAYMENT_REJECT = "ach_payment:reject"

    const val PAYMENTS_CONFIGURE = "payments:configure"
    const val PAYMENTS_VIEW = "payments:view"
    const val PAYMENTS_PAY = "payments:pay"

    const val CUSTOMER_VIEW = "customer:view"
    const val CUSTOMER_CREATE = "customer:create"
    const val CUSTOMER_DELETE = "customer:delete"

    const val PRODUCTS_VIEW = "products:view"
    const val PRODUCTS_CREATE = "products:create"
    const val PRODUCTS_DELETE = "products:delete"

    const val QUOTES_VIEW = "quotes:view"
    const val QUOTES_CREATE = "quotes:create"
    const val QUOTES_ACCEPT = "quotes:accept"

    const val EXPENSES_VIEW = "expenses:view"
    const val EXPENSES_CREATE = "expenses:create"
    const val EXPENSES_DELETE = "expenses:delete"

    const val REPORTS_VIEW = "reports:view"
    const val REPORTS_EXECUTE = "reports:execute"

    const val SETTINGS_VIEW = "settings:view"
    const val SETTINGS_MODIFY_POS_DEVICES = "settings:modify_pos_devices"

    const val INVENTORY_VIEW = "inventory:view"
    const val INVENTORY_RECEIVE = "inventory:receive"
    const val INVENTORY_TRANSFER = "inventory:transfer"
    const val INVENTORY_COUNT = "inventory:count"
    const val INVENTORY_ADJUST = "inventory:adjust"
    const val INVENTORY_CONFIGURE = "inventory:configure"

    const val RECURRING_VIEW = "recurring_invoice:view"
    const val RECURRING_CREATE = "recurring_invoice:create_invoice"
    const val RECURRING_CANCEL = "recurring_invoice:cancel"
    const val RECURRING_STOP = "recurring_invoice:stop"
    const val RECURRING_EXECUTE_NOW = "recurring_invoice:execute_now"
    const val RECURRING_SKIP_NEXT = "recurring_invoice:skip_next"
}
