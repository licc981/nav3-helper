package com.aleyn.navigation.core.intercept


/**
 * @author: Aleyn
 * @date: 2026/04/22 10:44
 */
fun interface NavUrlInterceptor {
    suspend fun intercept(url: String): InterceptResult
}

sealed class InterceptResult {
    /** Continue executing subsequent interceptors and navigate to the current route. */
    data object Proceed : InterceptResult()

    /** Stop navigation without executing subsequent interceptors. */
    data class Block(val reason: String = "") : InterceptResult()

    /** Restart interception with another route, for example a login screen. */
    data class Redirect(val newRoute: String) : InterceptResult()
}
