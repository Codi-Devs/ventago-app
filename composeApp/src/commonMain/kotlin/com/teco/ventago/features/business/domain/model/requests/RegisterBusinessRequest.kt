package com.teco.ventago.features.business.domain.model.requests

data class RegisterBusinessRequest(
    val name: String,
    val desc: String,
    val img: String,
    val ruc: String,
    val web: String,
    val businessEmail: String,
    val businessPhone: String)