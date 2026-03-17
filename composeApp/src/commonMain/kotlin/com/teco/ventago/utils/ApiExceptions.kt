package com.teco.ventago.utils

class AuthException(val error: ApiError) : Exception()
class MustChangePasswordException : Exception()
class NoInternetException : Exception()
class BadRequestException(override val message: String) : Exception()
class InvalidRucException : Exception()

class DomainInUseException(override val message: String) : Exception()
