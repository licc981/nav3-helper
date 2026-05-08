package com.aleyn.navigation.core.route

import com.aleyn.navigation.core.intercept.NavUrlInterceptor
import com.aleyn.navigation.core.navigator.NavBackStackController
import com.aleyn.navigation.core.util.duplicateRouteDetails

/**
 *  @author : Aleyn
 *  @date : 2026/2/25 16:36
 *
 * Global URL navigation entry.
 *
 * This object does not own navigation state. It holds global registries and interceptors, then
 * forwards resolved destinations to the currently attached host.
 */
object NavCenter {

    private var backStack: NavBackStackController? = null

    private val _registries = linkedSetOf<NavRegistry>()
    private val routeRegistryIndex = linkedMapOf<String, NavRegistry>()

    val registries: Set<NavRegistry> get() = _registries

    private val interceptors = mutableListOf<NavUrlInterceptor>()

    internal fun attachHost(host: NavBackStackController) {
        backStack = host
    }

    internal fun detachHost(host: NavBackStackController) {
        if (backStack === host) {
            backStack = null
        }
    }

    fun setRegistries(registries: Set<NavRegistry>) {
        val duplicatedRoutes = duplicateRouteDetails(registries)
        require(duplicatedRoutes.isEmpty()) {
            buildString {
                append("Duplicate navigation routes found:")
                duplicatedRoutes.forEach { (route, registryNames) ->
                    append("\n- ")
                    append(route)
                    append(" in ")
                    append(registryNames.joinToString())
                }
            }
        }
        this._registries.clear()
        this._registries.addAll(registries)
        routeRegistryIndex.clear()
        registries.forEach { registry ->
            registry.routes.forEach { route ->
                routeRegistryIndex[routeKey(route)] = registry
            }
        }
    }

    fun clearRegistries() {
        _registries.clear()
        routeRegistryIndex.clear()
    }

    fun setInterceptors(interceptors: List<NavUrlInterceptor>) {
        this.interceptors.clear()
        this.interceptors.addAll(interceptors)
    }

    fun addInterceptor(interceptor: NavUrlInterceptor) {
        interceptors += interceptor
    }

    fun clearInterceptors() {
        interceptors.clear()
    }

    fun resolve(url: String): NavScreen? {
        val finalUrl = interceptedUrl(url) ?: return null
        val parsedRouteUrl = parseRouteUrl(finalUrl)
        val registry = routeRegistryIndex[parsedRouteUrl.routeKey] ?: return null
        return registry.resolve(parsedRouteUrl)
    }

    fun navigate(url: String): Boolean {
        val screen = resolve(url) ?: return false
        backStack?.navigate(screen) ?: return false
        return true
    }

    fun navigate(direction: NavScreen): Boolean {
        backStack ?: return false
        backStack?.navigate(direction)
        return true
    }

    fun goBack(): Boolean {
        backStack ?: return false
        backStack?.goBack()
        return true
    }

    private fun interceptedUrl(url: String): String? {
        var currentUrl: String? = url
        interceptors.forEach { interceptor ->
            currentUrl = currentUrl?.let(interceptor::intercept) ?: return null
        }
        return currentUrl
    }
}
