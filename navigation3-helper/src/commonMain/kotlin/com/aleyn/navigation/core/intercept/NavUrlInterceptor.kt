package com.aleyn.navigation.core.intercept


/**
 * @author: Aleyn
 * @date: 2026/04/22 10:44
 */
fun interface NavUrlInterceptor {
    fun intercept(url: String): String?
}