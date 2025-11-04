package com.teco.ventago.features.auth.data.provider

import com.teco.ventago.features.auth.domain.model.requests.CreateUserRequest
import com.teco.ventago.features.auth.domain.model.requests.EmailLoginRequest


object AuthRequests {

    /**
     * endpoint: user-exists
     */
    fun userExistsRequest(email: String) : String =
        """
            {
                "email": "$email"
            }
        """.trimIndent()


    /**
     * endpoint: email-login
     */
    fun emailLoginRequest(request: EmailLoginRequest) : String =
        """
            {
                "email": "${request.email}",
                "password": "${request.password}"
            }
        """.trimIndent()


    /**
     * endpoint: email-login
     */
    fun emailRegisterRequest(request: CreateUserRequest) : String =
        """
            {
                "name": "${request.name}",
                "email": "${request.email}",
                "password": "${request.password}"
            }
        """.trimIndent()


    /**
     * endpoint: email-login
     */
    fun googleLoginRequest(token: String) : String =
        """
            {
                "id_token": "$token"               
            }
        """.trimIndent()
}