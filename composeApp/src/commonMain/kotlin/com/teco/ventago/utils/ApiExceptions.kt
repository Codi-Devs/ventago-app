package com.teco.ventago.utils

class AuthException(val error: ApiError) : Exception()

data class BusinessChoice(val id: Int, val name: String, val role: String = "")

class BusinessSelectionRequiredException(val businesses: List<BusinessChoice>) : Exception()
class MustChangePasswordException : Exception()
class NoInternetException : Exception()
class BadRequestException(override val message: String) : Exception()
class InvalidRucException : Exception()
class DuplicateCustomerException(override val message: String) : Exception()

class DomainInUseException(override val message: String) : Exception()
