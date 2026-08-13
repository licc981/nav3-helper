package com.aleyn.navigation.core.route

import com.aleyn.navigation.core.intercept.NavUrlInterceptor
import com.aleyn.navigation.core.intercept.InterceptResult
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
    private val routePatternRegistrations = mutableListOf<RoutePatternRegistration>()

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
        routePatternRegistrations.clear()
        registries.forEach { registry ->
            registry.routes.forEach { route ->
                if (isRoutePattern(route)) {
                    routePatternRegistrations += RoutePatternRegistration(route, registry)
                } else {
                    routeRegistryIndex[routeKey(route)] = registry
                }
            }
        }
    }

    fun clearRegistries() {
        _registries.clear()
        routeRegistryIndex.clear()
        routePatternRegistrations.clear()
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
        routeRegistryIndex[parsedRouteUrl.routeKey]?.let { registry ->
            return registry.resolve(parsedRouteUrl)
        }

        routePatternRegistrations.forEach { registration ->
            val matchedRoute = matchRoutePattern(registration.route, parsedRouteUrl)
                ?: return@forEach
            return registration.registry.resolve(matchedRoute)
        }
        return null
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
        val host = backStack ?: return false
        host.goBack()
        return true
    }

    fun goBack(direction: NavScreen, inclusive: Boolean = false): Boolean {
        val host = backStack ?: return false
        host.goBack(direction, inclusive)
        return true
    }

    fun replace(direction: NavScreen): Boolean {
        val host = backStack ?: return false
        host.replace(direction)
        return true
    }

    fun remove(direction: NavScreen): Boolean {
        val host = backStack ?: return false
        host.remove(direction)
        return true
    }

    fun resetToStart(): Boolean {
        val host = backStack ?: return false
        host.resetToStart()
        return true
    }

    fun needLogin(url: String): Boolean {
        val parsedRouteUrl = parseRouteUrl(url)
        return _registries.any { registry ->
            registry.loginRoutes.any { route -> routePatternMatches(route, parsedRouteUrl) }
        }
    }

    private fun interceptedUrl(url: String): String? {
        var currentUrl = url
        val visitedRoutes = linkedSetOf<String>()
        var redirectCount = 0

        redirectLoop@ while (true) {
            if (!visitedRoutes.add(currentUrl)) return null

            for (interceptor in interceptors.toList()) {
                when (val result = interceptor.intercept(currentUrl)) {
                    InterceptResult.Proceed -> Unit
                    is InterceptResult.Block -> return null
                    is InterceptResult.Redirect -> {
                        if (++redirectCount > MAX_REDIRECTS) return null
                        currentUrl = result.newRoute
                        continue@redirectLoop
                    }
                }
            }
            return currentUrl
        }
    }

    private data class RoutePatternRegistration(
        val route: String,
        val registry: NavRegistry
    )

    private const val MAX_REDIRECTS = 16
}
