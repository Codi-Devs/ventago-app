package com.teco.ventago

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform