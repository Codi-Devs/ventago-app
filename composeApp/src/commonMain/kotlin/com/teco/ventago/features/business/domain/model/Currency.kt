package com.teco.ventago.features.business.domain.model

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

data class Currency(
    var currencyId: Int,
    val currency: String,
    val currencyCode:String,
    val symbol: String
) {

    fun getLabel(): String {
        return if (symbol.equals("null", true)) {
            "$currency - $currencyCode"
        } else {
            "$currency - $symbol"
        }
    }

    fun toJSON(): JsonObject {
        return JsonObject(mapOf(
            "id_currency" to JsonPrimitive(currencyId),
            "currency" to JsonPrimitive(currency),
            "code" to JsonPrimitive(currencyCode),
            "symbol" to JsonPrimitive(symbol)
        ))
    }
    companion object {
        fun fromJSON(response: JsonObject): Currency {
            return try {
                Currency(response["id_currency"]!!.jsonPrimitive.int,
                    response["currency"]!!.jsonPrimitive.content,
                    response["code"]!!.jsonPrimitive.content,
                    response["symbol"]!!.jsonPrimitive.content)
            } catch (_: Exception) {
                Currency(-1, "", "", "")
            }
        }

        fun fromId(id: Int): Currency {
            return fromMemory().find { it.currencyId == id } ?: Currency(-1, "", "", "")
        }



        fun fromMemory(): ArrayList<Currency> {
            val currencies = ArrayList<Currency>()
            currencies.add(Currency(140,"US Dollar","USD","$"))
            currencies.add(Currency(141,"Euro","EUR","€"))
            currencies.add(Currency(142,"Mexican Peso","MXN","$"))
            currencies.add(Currency(156,"Afghani","AFN","؋"))
            currencies.add(Currency(158,"Lek","ALL","Lek"))
            currencies.add(Currency(159,"Algerian Dinar","DZD","null"))
            currencies.add(Currency(162,"Kwanza","AOA","null"))
            currencies.add(Currency(164,"East Caribbean Dollar","XCD","null"))
            currencies.add(Currency(165,"Argentine Peso","ARS","$"))
            currencies.add(Currency(166,"Armenian Dram","AMD","null"))
            currencies.add(Currency(168,"Australian Dollar","AUD","$"))
            currencies.add(Currency(170,"Azerbaijan Manat","AZN","null"))
            currencies.add(Currency(171,"Bahamian Dollar","BSD","$"))
            currencies.add(Currency(172,"Bahraini Dinar","BHD","null"))
            currencies.add(Currency(173,"Taka","BDT","৳"))
            currencies.add(Currency(174,"Barbados Dollar","BBD","$"))
            currencies.add(Currency(175,"Belarusian Ruble","BYN","null"))
            currencies.add(Currency(177,"Belize Dollar","BZD","BZ$"))
            currencies.add(Currency(178,"CFA Franc BCEAO","XOF","null"))
            currencies.add(Currency(181,"Ngultrum","BTN","null"))
            currencies.add(Currency(182,"Boliviano","BOB","null"))
            currencies.add(Currency(183,"Mvdol","BOV","null"))
            currencies.add(Currency(185,"Convertible Mark","BAM","null"))
            currencies.add(Currency(186,"Pula","BWP","null"))
            currencies.add(Currency(188,"Brazilian Real","BRL","R$"))
            currencies.add(Currency(190,"Brunei Dollar","BND","null"))
            currencies.add(Currency(191,"Bulgarian Lev","BGN","лв"))
            currencies.add(Currency(193,"Burundi Franc","BIF","null"))
            currencies.add(Currency(195,"Riel","KHR","៛"))
            currencies.add(Currency(196,"CFA Franc BEAC","XAF","null"))
            currencies.add(Currency(197,"Canadian Dollar","CAD","$"))
            currencies.add(Currency(201,"Chilean Peso","CLP","$"))
            currencies.add(Currency(202,"Unidad de Fomento","CLF","null"))
            currencies.add(Currency(203,"Yuan Renminbi","CNY","¥"))
            currencies.add(Currency(206,"Colombian Peso","COP","$"))
            currencies.add(Currency(207,"Unidad de Valor Real","COU","null"))
            currencies.add(Currency(208,"Comorian Franc ","KMF","null"))
            currencies.add(Currency(209,"Congolese Franc","CDF","null"))
            currencies.add(Currency(212,"Costa Rican Colon","CRC","null"))
            currencies.add(Currency(214,"Kuna","HRK","kn"))
            currencies.add(Currency(215,"Cuban Peso","CUP","MN"))
            currencies.add(Currency(219,"Czech Koruna","CZK","Kč"))
            currencies.add(Currency(221,"Djibouti Franc","DJF","null"))
            currencies.add(Currency(223,"Dominican Peso","DOP","null"))
            currencies.add(Currency(225,"Egyptian Pound","EGP","null"))
            currencies.add(Currency(226,"El Salvador Colon","SVC","null"))
            currencies.add(Currency(229,"Nakfa","ERN","null"))
            currencies.add(Currency(232,"Ethiopian Birr","ETB","null"))
            currencies.add(Currency(236,"Fiji Dollar","FJD","null"))
            currencies.add(Currency(240,"CFP Franc","XPF","null"))
            currencies.add(Currency(243,"Dalasi","GMD","null"))
            currencies.add(Currency(244,"Lari","GEL","₾"))
            currencies.add(Currency(246,"Ghana Cedi","GHS","null"))
            currencies.add(Currency(247,"Gibraltar Pound","GIP","null"))
            currencies.add(Currency(249,"Danish Krone","DKK","null"))
            currencies.add(Currency(253,"Quetzal","GTQ","null"))
            currencies.add(Currency(255,"Guinean Franc","GNF","null"))
            currencies.add(Currency(257,"Guyana Dollar","GYD","null"))
            currencies.add(Currency(258,"Gourde","HTG","null"))
            currencies.add(Currency(262,"Lempira","HNL","null"))
            currencies.add(Currency(263,"Hong Kong Dollar","HKD","$"))
            currencies.add(Currency(264,"Forint","HUF","ft"))
            currencies.add(Currency(265,"Iceland Krona","ISK","null"))
            currencies.add(Currency(266,"Indian Rupee","INR","₹"))
            currencies.add(Currency(267,"Rupiah","IDR","Rp"))
            currencies.add(Currency(269,"Iranian Rial","IRR","null"))
            currencies.add(Currency(270,"Iraqi Dinar","IQD","null"))
            currencies.add(Currency(273,"Israeli Sheqel","ILS","₪"))
            currencies.add(Currency(275,"Jamaican Dollar","JMD","null"))
            currencies.add(Currency(276,"Yen","JPY","¥"))
            currencies.add(Currency(278,"Jordanian Dinar","JOD","null"))
            currencies.add(Currency(279,"Tenge","KZT","null"))
            currencies.add(Currency(280,"Kenyan Shilling","KES","Ksh"))
            currencies.add(Currency(282,"North Korean Won","KPW","null"))
            currencies.add(Currency(283,"Won","KRW","₩"))
            currencies.add(Currency(284,"Kuwaiti Dinar","KWD","null"))
            currencies.add(Currency(285,"Som","KGS","null"))
            currencies.add(Currency(286,"Lao Kip","LAK","null"))
            currencies.add(Currency(288,"Lebanese Pound","LBP","null"))
            currencies.add(Currency(289,"Loti","LSL","null"))
            currencies.add(Currency(290,"Rand","ZAR","null"))
            currencies.add(Currency(291,"Liberian Dollar","LRD","null"))
            currencies.add(Currency(292,"Libyan Dinar","LYD","null"))
            currencies.add(Currency(293,"Swiss Franc","CHF","null"))
            currencies.add(Currency(296,"Pataca","MOP","null"))
            currencies.add(Currency(297,"Denar","MKD","null"))
            currencies.add(Currency(298,"Malagasy Ariary","MGA","null"))
            currencies.add(Currency(299,"Malawi Kwacha","MWK","null"))
            currencies.add(Currency(300,"Malaysian Ringgit","MYR","RM"))
            currencies.add(Currency(301,"Rufiyaa","MVR","null"))
            currencies.add(Currency(306,"Ouguiya","MRU","null"))
            currencies.add(Currency(307,"Mauritius Rupee","MUR","null"))
            currencies.add(Currency(313,"Moldovan Leu","MDL","null"))
            currencies.add(Currency(315,"Tugrik","MNT","null"))
            currencies.add(Currency(319,"Mozambique Metical","MZN","null"))
            currencies.add(Currency(320,"Kyat","MMK","null"))
            currencies.add(Currency(321,"Namibia Dollar","NAD","null"))
            currencies.add(Currency(324,"Nepalese Rupee","NPR","null"))
            currencies.add(Currency(327,"Zealand Dollar","NZD","$"))
            currencies.add(Currency(328,"Cordoba Oro","NIO","null"))
            currencies.add(Currency(330,"Naira","NGN","₦"))
            currencies.add(Currency(334,"Norwegian Krone","NOK","kr"))
            currencies.add(Currency(335,"Rial Omani","OMR","null"))
            currencies.add(Currency(336,"Pakistan Rupee","PKR","Rs"))
            currencies.add(Currency(338,"Balboa","PAB","B./"))
            currencies.add(Currency(340,"Kina","PGK","null"))
            currencies.add(Currency(341,"Guarani","PYG","null"))
            currencies.add(Currency(342,"Sol","PEN","S"))
            currencies.add(Currency(343,"Philippine Peso","PHP","₱"))
            currencies.add(Currency(345,"Zloty","PLN","zł"))
            currencies.add(Currency(348,"Qatari Rial","QAR","null"))
            currencies.add(Currency(350,"Romanian Leu","RON","lei"))
            currencies.add(Currency(351,"Russian Ruble","RUB","₽"))
            currencies.add(Currency(352,"Rwanda Franc","RWF","null"))
            currencies.add(Currency(360,"Tala","WST","null"))
            currencies.add(Currency(362,"Dobra","STN","null"))
            currencies.add(Currency(363,"Saudi Riyal","SAR","null"))
            currencies.add(Currency(365,"Serbian Dinar","RSD","null"))
            currencies.add(Currency(366,"Seychelles Rupee","SCR","null"))
            currencies.add(Currency(367,"Leone","SLL","null"))
            currencies.add(Currency(368,"Singapore Dollar","SGD","$"))
            currencies.add(Currency(373,"Solomon Islands Dollar","SBD","null"))
            currencies.add(Currency(374,"Somali Shilling","SOS","null"))
            currencies.add(Currency(378,"Sri Lanka Rupee","LKR","Rs"))
            currencies.add(Currency(379,"Sudanese Pound","SDG","null"))
            currencies.add(Currency(380,"Surinam Dollar","SRD","null"))
            currencies.add(Currency(382,"Swedish Krona","SEK","kr"))
            currencies.add(Currency(384,"WIR Euro","CHE","null"))
            currencies.add(Currency(385,"WIR Franc","CHW","null"))
            currencies.add(Currency(386,"Syrian Pound","SYP","null"))
            currencies.add(Currency(387,"Taiwan Dollar","TWD","null"))
            currencies.add(Currency(388,"Somoni","TJS","null"))
            currencies.add(Currency(389,"Tanzanian Shilling","TZS","null"))
            currencies.add(Currency(390,"Baht","THB","฿"))
            currencies.add(Currency(394,"Pa’anga","TOP","null"))
            currencies.add(Currency(395,"Trinidad and Tobago Dollar","TTD","null"))
            currencies.add(Currency(396,"Tunisian Dinar","TND","null"))
            currencies.add(Currency(397,"Turkish Lira","TRY","₺"))
            currencies.add(Currency(398,"Turkmenistan Manat","TMT","null"))
            currencies.add(Currency(401,"Uganda Shilling","UGX","null"))
            currencies.add(Currency(402,"Hryvnia","UAH","₴"))
            currencies.add(Currency(403,"UAE Dirham","AED","د.إ"))
            currencies.add(Currency(404,"Pound Sterling","GBP","£"))
            currencies.add(Currency(408,"Peso Uruguayo","UYU","null"))
            currencies.add(Currency(409,"Uruguay Peso en Unidades Indexadas (UI)","UYI","null"))
            currencies.add(Currency(410,"Unidad Previsional","UYW","null"))
            currencies.add(Currency(411,"Uzbekistan Sum","UZS","null"))
            currencies.add(Currency(412,"Vatu","VUV","null"))
            currencies.add(Currency(413,"Bolívar Soberano","VES","null"))
            currencies.add(Currency(414,"Dong","VND","₫"))
            currencies.add(Currency(418,"Moroccan Dirham","MAD","null"))
            currencies.add(Currency(419,"Yemeni Rial","YER","null"))
            currencies.add(Currency(420,"Zambian Kwacha","ZMW","null"))
            currencies.add(Currency(421,"Zimbabwe Dollar","ZWL","null"))
            return currencies
        }
    }
    
}