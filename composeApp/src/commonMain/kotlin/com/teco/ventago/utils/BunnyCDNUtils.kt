package com.teco.ventago.utils

import com.teco.ventago.client
import io.ktor.client.request.headers
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

suspend fun uploadImageToBunnyCdn(
    imageData: ByteArray,
    fileName: String,
    businessId: String
): String? {
    val storageZoneName = "ventago"
    val apiKey = "cbcbc014-a34c-458a-aab5209ba9c2-c955-401b"
    val path = "products/$businessId/"
    val url = "https://la.storage.bunnycdn.com/$storageZoneName/$path$fileName"
    val publicUrl = "https://ventago.b-cdn.net/$path$fileName"

    return try {
        val response = client.put(url){
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.ContentType, "application/json; charset=UTF-8")
                append("AccessKey", apiKey)
            }
            contentType(ContentType.Application.Json)
            setBody(imageData)
        }

        if (response.status.value == 200 || response.status.value == 201) {
            println("Upload Success: HTTP ${response.status.value}")
            println("Upload Success: Headers ${response.request.headers.toString()}")
            publicUrl
        } else {
            println("Upload failed: HTTP ${response.status.value}")
            println("Upload failed: Headers ${response.request.headers.toString()}")
            null
        }
    } catch (e: Exception) {
        println("Upload failed: ${e.message}")
        null
    }
}


//@OptIn(InternalAPI::class)
//suspend fun uploadImageToBunnyCdn(
//    imageData: ByteArray,
//    fileName: String,
//    businessId: String
//): String? {
//    val storageZoneName = "ventago"
//    val apiKey = "cbcbc014-a34c-458a-aab5209ba9c2-c955-401b"
//    val path = "products/"
//    val url = "https://la.storage.bunnycdn.com/$storageZoneName/$path$fileName"
//    val publicUrl = "https://ventago.b-cdn.net/$path$fileName"
//
//    return try {
//        val response = client.put {
//            url {
//                takeFrom(url)
//            }
//            body = MultiPartFormDataContent(
//                formData{
//                    append(FormPart("image", "item_asd.png"))
//                    appendInput(
//                        key = "image",
//                        headers = Headers.build {
//                            append(HttpHeaders.ContentDisposition, "filename=item_asd.png")
//                            append("AccessKey", apiKey)
//                            append("Content-Type", "application/octet-stream")
//                        }
//                    ) {
//                        buildPacket { writeFully(imageData) }
//                    }
//                }
//            )
//        }
//
//        if (response.status.value == 200 || response.status.value == 201) {
//            publicUrl
//        } else {
//            println("Upload failed: HTTP ${response.status.value}")
//            null
//        }
//    } catch (e: Exception) {
//        println("Upload failed: ${e.message}")
//        null
//    }
//}