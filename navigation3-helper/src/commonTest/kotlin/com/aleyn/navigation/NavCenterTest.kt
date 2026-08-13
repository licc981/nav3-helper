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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NavCenterTest {

    @AfterTest
    fun cleanUp() {
        NavCenter.clearInterceptors()
        NavCenter.clearRegistries()
        attachedHost?.let(NavCenter::detachHost)
        attachedHost = null
    }

    @Test
    fun template_route_resolves_path_parameters_and_login_metadata() {
        NavCenter.setRegistries(setOf(TestRegistry))

        assertTrue(NavCenter.needLogin("https://www.myapp.com/users/active/42"))
        assertFalse(NavCenter.needLogin("https://www.myapp.com/public"))
        assertEquals(
            TestUser("active", "42"),
            NavCenter.resolve("https://www.myapp.com/users/active/42")
        )
    }

    @Test
    fun block_stops_resolution_and_subsequent_interceptors() {
        NavCenter.setRegistries(setOf(TestRegistry))
        var subsequentCalled = false
        NavCenter.setInterceptors(
            listOf(
                NavUrlInterceptor { InterceptResult.Block("not logged in") },
                NavUrlInterceptor {
                    subsequentCalled = true
                    InterceptResult.Proceed
                }
            )
        )

        assertNull(NavCenter.resolve("https://www.myapp.com/public"))
        assertFalse(subsequentCalled)
    }

    @Test
    fun redirect_restarts_interceptor_chain_for_new_route() {
        NavCenter.setRegistries(setOf(TestRegistry))
        val interceptedUrls = mutableListOf<String>()
        NavCenter.addInterceptor { url ->
            interceptedUrls += url
            if (url.endsWith("/old")) {
                InterceptResult.Redirect("https://www.myapp.com/public")
            } else {
                InterceptResult.Proceed
            }
        }

        assertEquals(TestPublic, NavCenter.resolve("https://www.myapp.com/old"))
        assertEquals(
            listOf("https://www.myapp.com/old", "https://www.myapp.com/public"),
            interceptedUrls
        )
    }

    @Test
    fun redirect_loop_is_blocked() {
        NavCenter.setRegistries(setOf(TestRegistry))
        NavCenter.addInterceptor { url -> InterceptResult.Redirect(url) }

        assertNull(NavCenter.resolve("https://www.myapp.com/public"))
    }

    @Test
    fun stack_operations_return_false_without_an_attached_host() {
        assertFalse(NavCenter.goBack(TestPublic))
        assertFalse(NavCenter.replace(TestPublic))
        assertFalse(NavCenter.remove(TestPublic))
        assertFalse(NavCenter.resetToStart())
    }

    @Test
    fun stack_operations_are_forwarded_to_the_attached_host() {
        val host = attachHost()
        val first = TestUser("active", "1")
        val second = TestUser("active", "2")

        host.navigate(first)
        host.navigate(second)
        assertTrue(NavCenter.goBack(first))
        assertEquals(listOf(TestPublic, first), host.screens)

        assertTrue(NavCenter.replace(second))
        assertEquals(listOf(TestPublic, second), host.screens)

        assertTrue(NavCenter.remove(second))
        assertEquals(listOf(TestPublic), host.screens)

        host.navigate(first)
        assertTrue(NavCenter.resetToStart())
        assertEquals(listOf(TestPublic), host.screens)
    }

    private fun attachHost(): NavBackStackState {
        return NavBackStackState(TestPublic, NavBackStack<NavScreen>(TestPublic)).also {
            attachedHost = it
            NavCenter.attachHost(it)
        }
    }

    private companion object {
        var attachedHost: NavBackStackState? = null
    }
}

private object TestRegistry : NavRegistry {
    override val routes = setOf(
        "https://www.myapp.com/users/{filter}/{id}",
        "https://www.myapp.com/public"
    )
    override val loginRoutes = setOf("https://www.myapp.com/users/{filter}/{id}")

    override fun entryProvider(scope: EntryProviderScope<NavScreen>) = Unit

    override fun resolve(parsedRouteUrl: ParsedRouteUrl): NavScreen? {
        return when (parsedRouteUrl.routeKey) {
            "https://www.myapp.com/users/{filter}/{id}" -> TestUser(
                filter = parsedRouteUrl.queryParameters["filter"] ?: return null,
                id = parsedRouteUrl.queryParameters["id"] ?: return null
            )
            "https://www.myapp.com/public" -> TestPublic
            else -> null
        }
    }
}

private data class TestUser(val filter: String, val id: String) : NavScreen
private data object TestPublic : NavScreen
