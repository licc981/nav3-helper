package com.aleyn.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import com.aleyn.navigation.core.intercept.InterceptResult
import com.aleyn.navigation.core.intercept.NavUrlInterceptor
import com.aleyn.navigation.core.navigator.NavBackStackState
import com.aleyn.navigation.core.route.NavCenter
import com.aleyn.navigation.core.route.NavRegistry
import com.aleyn.navigation.core.route.NavScreen
import com.aleyn.navigation.core.route.ParsedRouteUrl
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 验证 replace(url) 与 navigate(url) 对同一 URL 的解析行为差异。
 * 复现报告：NavCenter.replace 中，如果路由 path 包含 path segment（多段路径）则找不到 NavScreen。
 */
class NavCenterReplaceConsistencyTest {

    @AfterTest
    fun cleanUp() {
        NavCenter.clearRegistries()
        NavCenter.clearInterceptors()
        NavCenter.clearRouteNotFoundHandler()
        attachedHost?.let(NavCenter::detachHost)
        attachedHost = null
    }

    private fun attachHost(): NavBackStackState {
        return NavBackStackState(ConsistencyPublic, NavBackStack<NavScreen>(ConsistencyPublic)).also {
            attachedHost = it
            NavCenter.attachHost(it)
        }
    }

    /** 拦截器把无 scheme 的相对路径补全为绝对 URL，模拟常见的"URL 规范化"拦截器。 */
    private fun installNormalizingInterceptor() {
        NavCenter.addInterceptor(
            NavUrlInterceptor { url ->
                if (!url.startsWith("http")) {
                    InterceptResult.Redirect("https://www.app.cn/$url")
                } else {
                    InterceptResult.Proceed
                }
            }
        )
    }

    @Test
    fun navigate_runs_interceptors_and_resolves_relative_multi_segment_url() {
        NavCenter.setRegistries(setOf(ConsistencyRegistry))
        val host = attachHost()
        installNormalizingInterceptor()

        assertTrue(NavCenter.navigate("users/active/42"))
        assertEquals(ConsistencyDetail("active", "42"), host.current)
    }

    @Test
    fun replace_runs_interceptors_and_resolves_relative_multi_segment_url() {
        NavCenter.setRegistries(setOf(ConsistencyRegistry))
        val host = attachHost()
        installNormalizingInterceptor()

        // 修复前：replace 直接走 findNavScreen，不经拦截器，相对路径无法解析
        // 修复后：replace 与 navigate 一致，先经过拦截器链，再解析
        assertTrue(NavCenter.replace("users/active/42"))
        assertEquals(ConsistencyDetail("active", "42"), host.current)
    }

    @Test
    fun replace_with_full_url_resolves_after_normalization() {
        NavCenter.setRegistries(setOf(ConsistencyRegistry))
        val host = attachHost()
        installNormalizingInterceptor()

        assertTrue(NavCenter.replace("https://www.app.cn/users/active/42"))
        assertEquals(ConsistencyDetail("active", "42"), host.current)
    }

    @Test
    fun replace_relative_url_fails_also_without_interceptor() {
        NavCenter.setRegistries(setOf(ConsistencyRegistry))
        attachHost()

        assertFalse(NavCenter.replace("users/active/42"))
    }

    private companion object {
        var attachedHost: NavBackStackState? = null
    }
}

private object ConsistencyRegistry : NavRegistry {
    override val routes = setOf(
        "https://www.app.cn/public",
        "https://www.app.cn/users/{filter}/{id}"
    )
    override val loginRoutes = setOf("https://www.app.cn/users/{filter}/{id}")

    override fun entryProvider(scope: EntryProviderScope<NavScreen>) = Unit

    override fun resolve(parsedRouteUrl: ParsedRouteUrl): NavScreen? {
        return when (parsedRouteUrl.routeKey) {
            "https://www.app.cn/public" -> ConsistencyPublic
            "https://www.app.cn/users/{filter}/{id}" -> ConsistencyDetail(
                filter = parsedRouteUrl.queryParameters["filter"] ?: return null,
                id = parsedRouteUrl.queryParameters["id"] ?: return null
            )
            else -> null
        }
    }
}

private data object ConsistencyPublic : NavScreen
private data class ConsistencyDetail(val filter: String, val id: String) : NavScreen
