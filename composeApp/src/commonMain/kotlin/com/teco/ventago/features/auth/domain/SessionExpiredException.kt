package com.teco.ventago.features.auth.domain

import kotlinx.coroutines.CancellationException

/** Stops work belonging to an expired session without a fatal coroutine exception. */
class SessionExpiredException : CancellationException("Session expired")
