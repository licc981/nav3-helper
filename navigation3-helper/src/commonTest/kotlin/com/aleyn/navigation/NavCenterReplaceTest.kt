package com.aleyn.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import com.aleyn.navigation.core.navigator.NavBackStackState
import com.aleyn.navigation.core.route.NavCenter
import com.aleyn.navigation.core.route.NavRegistry
import com.aleyn.navigation.core.route.NavScreen
import com.aleyn.navigation.core.route.ParsedRouteUrl
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 复现报告：NavCenter.replace 中，如果路由 path 包含多个 path segment（含 `/` 或占位符），
 * 则找不到 NavScreen。
 */
class NavCenterReplaceTest {

    @AfterTest
    fun cleanUp() {
        NavCenter.clearRegistries()
        NavCenter.clearInterceptors()
        NavCenter.clearRouteNotFoundHandler()
        attachedHost?.let(NavCenter::detachHost)
        attachedHost = null
    }

    private fun attachHost(): NavBackStackState {
        return NavBackStackState(PublicScreen, NavBackStack<NavScreen>(PublicScreen)).also {
            attachedHost = it
            NavCenter.attachHost(it)
        }
    }

    @Test
    fun replace_single_segment_static_route_resolves() {
        NavCenter.setRegistries(setOf(ReplaceTestRegistry))
        val host = attachHost()
        host.navigate(PublicScreen)

        assertTrue(NavCenter.replace("https://www.myapp.com/public"))
        assertEquals(PublicScreen, host.current)
    }

    @Test
    fun replace_multi_segment_static_route_resolves() {
        NavCenter.setRegistries(setOf(ReplaceTestRegistry))
        val host = attachHost()
        host.navigate(PublicScreen)

        assertTrue(NavCenter.replace("https://www.myapp.com/settings/profile"))
        assertEquals(ProfileScreen, host.current)
    }

    @Test
    fun replace_multi_segment_placeholder_route_resolves() {
        NavCenter.setRegistries(setOf(ReplaceTestRegistry))
        val host = attachHost()
        host.navigate(PublicScreen)

        assertTrue(NavCenter.replace("https://www.myapp.com/users/active/42"))
        assertEquals(DetailScreen("active", "42"), host.current)
    }

    @Test
    fun navigate_and_replace_agree_on_multi_segment_routes() {
        NavCenter.setRegistries(setOf(ReplaceTestRegistry))
        val host = attachHost()

        // navigate 应当能解析
        assertTrue(NavCenter.navigate("https://www.myapp.com/users/active/42"))
        assertEquals(DetailScreen("active", "42"), host.current)

        // replace 应当同样解析
        assertTrue(NavCenter.replace("https://www.myapp.com/settings/profile"))
        assertEquals(ProfileScreen, host.current)
    }

    private companion object {
        var attachedHost: NavBackStackState? = null
    }
}

private object ReplaceTestRegistry : NavRegistry {
    override val routes = setOf(
        "https://www.myapp.com/public",
        "https://www.myapp.com/settings/profile",
        "https://www.myapp.com/users/{filter}/{id}"
    )
    override val loginRoutes = setOf("https://www.myapp.com/users/{filter}/{id}")

    override fun entryProvider(scope: EntryProviderScope<NavScreen>) = Unit

    override fun resolve(parsedRouteUrl: ParsedRouteUrl): NavScreen? {
        return when (parsedRouteUrl.routeKey) {
            "https://www.myapp.com/public" -> PublicScreen
            "https://www.myapp.com/settings/profile" -> ProfileScreen
            "https://www.myapp.com/users/{filter}/{id}" -> DetailScreen(
                filter = parsedRouteUrl.queryParameters["filter"] ?: return null,
                id = parsedRouteUrl.queryParameters["id"] ?: return null
            )
            else -> null
        }
    }
}

private data object PublicScreen : NavScreen
private data object ProfileScreen : NavScreen
private data class DetailScreen(val filter: String, val id: String) : NavScreen
