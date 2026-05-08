package com.aleyn.navigation.core.util

import com.aleyn.navigation.core.route.NavRegistry
import com.aleyn.navigation.core.route.routeKey
import kotlinx.serialization.modules.SerializersModule

/**
 * @author: Aleyn
 * @date: 2026/04/22 15:37
 */

fun navSerializersModule(navRegistrySet: Set<NavRegistry>): SerializersModule =
    SerializersModule {
        navRegistrySet.forEach { include(it.serializersModule) }
    }

internal fun duplicateRouteDetails(navRegistrySet: Set<NavRegistry>): Map<String, List<String>> =
    navRegistrySet
        .flatMap { registry ->
            registry.routes.map { routeKey(it) to registry.registryName() }
        }
        .groupBy(
            keySelector = { it.first },
            valueTransform = { it.second }
        )
        .mapValues { (_, registryNames) -> registryNames.distinct() }
        .filterValues { it.size > 1 }

private fun NavRegistry.registryName(): String {
    return this::class.simpleName ?: "UnknownRegistry"
}