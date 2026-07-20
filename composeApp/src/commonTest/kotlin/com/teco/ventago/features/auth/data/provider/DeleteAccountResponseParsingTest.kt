package com.teco.ventago.features.auth.data.provider

import com.teco.ventago.utils.ApiError
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class DeleteAccountResponseParsingTest {
    @Test
    fun successResponseMapsToTrue() {
        val response = parseDeleteAccountResponse(
            statusCode = HttpStatusCode.OK,
            body = jsonObject(
                """
                {
                  "status": true
                }
                """.trimIndent()
            )
        )

        assertTrue(response.successful)
        assertEquals(ApiError.NO_ERROR, response.error)
        assertTrue(response.data!!.jsonPrimitive.boolean)
    }

    @Test
    fun statusFalseResponseMapsToFalseWithoutError() {
        val response = parseDeleteAccountResponse(
            statusCode = HttpStatusCode.OK,
            body = jsonObject(
                """
                {
                  "status": false
                }
                """.trimIndent()
            )
        )

        assertFalse(response.successful)
        assertEquals(ApiError.NO_ERROR, response.error)
        assertFalse(response.data!!.jsonPrimitive.boolean)
    }

    @Test
    fun yiiNotFoundResponseMapsToErrorWithBody() {
        val response = parseDeleteAccountResponse(
            statusCode = HttpStatusCode.NotFound,
            body = jsonObject(
                """
                {
                  "name": "Not Found",
                  "message": "The requested page does not exist.",
                  "code": 0,
                  "status": 404
                }
                """.trimIndent()
            )
        )

        assertFalse(response.successful)
        assertEquals(ApiError.UNDEFINED, response.error)
        assertEquals("404", response.errorCode)
        assertEquals("The requested page does not exist.", response.errorMessage)
        assertEquals(404, response.data!!.jsonObject["status"]!!.jsonPrimitive.content.toInt())
    }

    private fun jsonObject(value: String): JsonObject {
        return Json.parseToJsonElement(value).jsonObject
    }
}
