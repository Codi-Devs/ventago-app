package com.teco.ventago.features.auth.domain

import kotlinx.coroutines.CancellationException

/** Stops work belonging to an expired session without a fatal coroutine exception. */
class SessionExpiredException : CancellationException("Session expired")

/** The access token is still valid. The call belongs to another business and must not renew the session. */
class StaleTenantRequestException : Exception("Access token still belongs to the current business")
