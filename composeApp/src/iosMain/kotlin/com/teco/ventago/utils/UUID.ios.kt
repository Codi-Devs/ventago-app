package com.teco.ventago.utils

import platform.Foundation.NSUUID

actual fun randomUUID(): String = NSUUID().UUIDString()