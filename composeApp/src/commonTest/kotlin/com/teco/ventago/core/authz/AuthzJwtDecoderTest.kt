package com.teco.ventago.core.authz

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuthzJwtDecoderTest {

    @OptIn(ExperimentalEncodingApi::class)
    private fun jwtForPayload(payload: String): String {
        val encoded = Base64.UrlSafe.encode(payload.encodeToByteArray()).trimEnd('=')
        return "header.$encoded.signature"
    }

    @Test
    fun decodeExtractsScopesFromTopLevelAndNestedData() {
        val token = jwtForPayload(
            """
            {
              "scopes": ["invoice:view", "products:create"],
              "data": {
                "scopes": ["home:dashboard", "quotes:create"]
              },
              "is_sub_user": false,
              "target": "businessOwner"
            }
            """.trimIndent()
        )

        val claims = AuthzJwtDecoder.decode(token)

        assertNotNull(claims)
        assertEquals(
            setOf("invoice:view", "products:create", "home:dashboard", "quotes:create"),
            claims.scopes
        )
        assertFalse(claims.isSubUser)
        assertTrue(claims.isOwnerMain)
        assertFalse(claims.mustChangePassword)
    }

    @Test
    fun decodeMarksSubUserEvenWhenTargetMatchesBusinessOwner() {
        val token = jwtForPayload(
            """
            {
              "data": {
                "scopes": ["expenses:create"],
                "is_sub_user": true,
                "target": "businessOwner"
              }
            }
            """.trimIndent()
        )

        val claims = AuthzJwtDecoder.decode(token)

        assertNotNull(claims)
        assertEquals(setOf("expenses:create"), claims.scopes)
        assertTrue(claims.isSubUser)
        assertFalse(claims.isOwnerMain)
        assertFalse(claims.mustChangePassword)
    }

    @Test
    fun decodeReadsMustChangePasswordFromTopLevelOrNestedData() {
        val token = jwtForPayload(
            """
            {
              "must_change_password": "true",
              "data": {
                "must_change_password": false
              }
            }
            """.trimIndent()
        )

        val claims = AuthzJwtDecoder.decode(token)

        assertNotNull(claims)
        assertTrue(claims.mustChangePassword)
    }
}
