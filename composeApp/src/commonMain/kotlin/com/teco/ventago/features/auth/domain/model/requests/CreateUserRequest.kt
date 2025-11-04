package com.teco.ventago.features.auth.domain.model.requests

data class CreateUserRequest(
    val email: String,
    val password: String,
    val name: String)