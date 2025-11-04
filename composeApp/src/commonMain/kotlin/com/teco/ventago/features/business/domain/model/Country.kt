package com.teco.ventago.features.business.domain.model

expect open class Country() {
    var country: String
    var currencyCode: String


    companion object {
        fun loadData(): ArrayList<Country>
    }
}