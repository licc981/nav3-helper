package com.aleyn.navigation.core.navigator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * @author: Aleyn
 * @date: 2026/04/29 16:34
 * @desc: 
 */

inline fun <reified T> NavBackStackController.setResult(value: T?) {
    setResult(T::class.toString(), value)
}

inline fun <reified T> NavBackStackController.peekResult(key: String = T::class.toString()): T? {
    return readTypedResult(key = key, consume = false)
}

inline fun <reified T> NavBackStackController.consumeResult(key: String = T::class.toString()): T? {
    return readTypedResult(key = key, consume = true)
}

inline fun <reified T> NavBackStackController.clearResult(
    resultKey: String = T::class.toString()
) {
    this.clearResult(resultKey)
}

@Composable
inline fun <reified T> NavBackStackController.consumeResultEffect(
    key: String = T::class.toString(),
    crossinline onResult: (T?) -> Unit
) {
    val hasPendingResult = hasResult(key)
    val peekedResult = if (hasPendingResult) peekResult<T>(key) else null

    LaunchedEffect(key, hasPendingResult, peekedResult) {
        if (!hasPendingResult) return@LaunchedEffect
        onResult(consumeResult<T>(key))
    }
}



@PublishedApi
internal inline fun <reified T> NavBackStackController.readTypedResult(
    key: String,
    consume: Boolean
): T? {
    val value = if (consume) consumeResult(key) else peekResult(key)
    if (value != null && value !is T) {
        throw IllegalStateException(
            "Navigation result for key '$key' is ${value::class.simpleName}, " +
                    "but ${T::class.simpleName} was requested."
        )
    }
    @Suppress("UNCHECKED_CAST")
    return value as T?
}