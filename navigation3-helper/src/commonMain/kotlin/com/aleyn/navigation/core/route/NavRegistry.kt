package com.aleyn.navigation.core.route

import androidx.navigation3.runtime.EntryProviderScope
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * @author : Aleyn
 * @date : 2026/1/20 16:15
 */
interface NavRegistry {

    val serializersModule: SerializersModule
        get() = EmptySerializersModule()

    val routes: Set<String>
        get() = emptySet()

    val loginRoutes: Set<String>
        get() = emptySet()

    fun entryProvider(scope: EntryProviderScope<NavScreen>)

    fun resolve(parsedRouteUrl: ParsedRouteUrl): NavScreen? = null

}
