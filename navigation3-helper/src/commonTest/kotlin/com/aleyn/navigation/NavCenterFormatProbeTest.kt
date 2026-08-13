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
import kotlin.test.assertTrue

/**
 * 穷举 URL 格式组合，找出 replace 失败的具体场景。
 * 注册路由统一使用带 scheme 的标准格式。
 */
class NavCenterFormatProbeTest {

    @AfterTest
    fun cleanUp() {
        NavCenter.clearRegistries()
        NavCenter.clearInterceptors()
        NavCenter.clearRouteNotFoundHandler()
        attachedHost?.let(NavCenter::detachHost)
        attachedHost = null
    }

    private fun attachHost(): NavBackStackState {
        return NavBackStackState(ProbePublic, NavBackStack<NavScreen>(ProbePublic)).also {
            attachedHost = it
            NavCenter.attachHost(it)
        }
    }

    private fun probeReplace(url: String) {
        NavCenter.setRegistries(setOf(ProbeRegistry))
        attachHost()
        assertTrue(NavCenter.replace(url), "replace 失败: $url")
    }

    @Test
    fun static_single_segment_with_scheme() {
        probeReplace("https://www.app.cn/public")
    }

    @Test
    fun static_multi_segment_with_scheme() {
        probeReplace("https://www.app.cn/settings/profile")
    }

    @Test
    fun placeholder_multi_segment_with_scheme() {
        probeReplace("https://www.app.cn/users/active/42")
    }

    @Test
    fun static_multi_segment_uppercase_scheme_and_host() {
        probeReplace("HTTPS://WWW.APP.CN/settings/profile")
    }

    @Test
    fun static_multi_segment_trailing_slash() {
        probeReplace("https://www.app.cn/settings/profile/")
    }

    @Test
    fun static_multi_segment_with_query() {
        probeReplace("https://www.app.cn/settings/profile?tab=1")
    }

    @Test
    fun placeholder_multi_segment_with_query() {
        probeReplace("https://www.app.cn/users/active/42?tab=posts")
    }

    private companion object {
        var attachedHost: NavBackStackState? = null
    }
}

private object ProbeRegistry : NavRegistry {
    override val routes = setOf(
        "https://www.app.cn/public",
        "https://www.app.cn/settings/profile",
        "https://www.app.cn/users/{filter}/{id}"
    )
    override val loginRoutes = setOf("https://www.app.cn/users/{filter}/{id}")

    override fun entryProvider(scope: EntryProviderScope<NavScreen>) = Unit

    override fun resolve(parsedRouteUrl: ParsedRouteUrl): NavScreen? {
        return when (parsedRouteUrl.routeKey) {
            "https://www.app.cn/public" -> ProbePublic
            "https://www.app.cn/settings/profile" -> ProbeProfile
            "https://www.app.cn/users/{filter}/{id}" -> ProbeDetail(
                parsedRouteUrl.queryParameters["filter"] ?: return null,
                parsedRouteUrl.queryParameters["id"] ?: return null
            )
            else -> null
        }
    }
}

private data object ProbePublic : NavScreen
private data object ProbeProfile : NavScreen
private data class ProbeDetail(val filter: String, val id: String) : NavScreen
