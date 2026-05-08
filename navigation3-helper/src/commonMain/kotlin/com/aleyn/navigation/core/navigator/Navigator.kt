package com.aleyn.navigation.core.navigator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import com.aleyn.navigation.core.route.NavRegistry
import com.aleyn.navigation.core.route.NavScreen
import com.aleyn.navigation.core.util.navSerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * @author : Aleyn
 * @date : 2025/12/2 20:17
 */

@Stable
class NavBackStackState internal constructor(
    override val startRoute: NavScreen,
    internal val entries: NavBackStack<NavScreen>
) : NavBackStackController {
    private val resultStore = mutableStateMapOf<String, MutableState<Any?>>()

    override val navBackStack: NavBackStack<NavScreen> get() = entries

    override val screens: List<NavScreen> get() = entries

    override val current: NavScreen? get() = entries.lastOrNull()

    override val previous: NavScreen? get() = entries.getOrNull(entries.lastIndex - 1)

    override fun navigate(direction: NavScreen) {
        entries.add(direction)
    }

    override fun goBack() {
        entries.removeLastOrNull()
    }

    override fun goBack(direction: NavScreen, inclusive: Boolean) {
        val index = entries.indexOf(direction)
        if (index == -1) return

        val fromIndex = if (inclusive) index else index + 1
        if (fromIndex >= entries.size) return
        if (fromIndex == 0) {
            entries.removeAll(entries.subList(1, entries.size))
            return
        }

        entries.removeAll(entries.subList(fromIndex, entries.size))
    }

    override fun replace(direction: NavScreen) {
        entries.lastOrNull() ?: return
        entries[entries.lastIndex] = direction
    }

    override fun remove(direction: NavScreen) {
        if (entries.size == 1 && entries.firstOrNull() == direction) return
        entries.remove(direction)
    }

    override fun resetToStart() {
        entries.clear()
        entries.add(startRoute)
    }

    /**
     * 设置返回值
     */
    override fun setResult(key: String, value: Any?) {
        resultStore[key] = mutableStateOf(value)
    }

    override fun peekResult(key: String): Any? {
        return resultStore[key]?.value
    }

    override fun consumeResult(key: String): Any? {
        val result = resultStore[key] ?: return null
        val value = result.value
        resultStore.remove(key)
        return value
    }

    override fun hasResult(key: String): Boolean = key in resultStore

    override fun clearResult(key: String) {
        resultStore.remove(key)
    }
}

val LocalNavBackStackState = compositionLocalOf<NavBackStackController> {
    error("NavBackStackController not found. Provide LocalNavBackStackState manually or use NavDisplayHelper.")
}

/**
 * Creates a restorable back stack from the shared host state.
 *
 * This is the recommended entry point because it reuses the same serializer configuration that the
 * host uses for destination resolution.
 */
@Composable
fun rememberHelperBackStack(
    startRoute: NavScreen,
    navRegistrySet: Set<NavRegistry>
): NavBackStackState {

    val serializersModule = remember(navRegistrySet) {
        navSerializersModule(navRegistrySet)
    }
    return rememberHelperBackStack(startRoute, serializersModule)
}

@Composable
fun rememberHelperBackStack(
    startRoute: NavScreen,
    serializersModule: SerializersModule
): NavBackStackState {
    val configuration = remember(serializersModule) {
        SavedStateConfiguration {
            this.serializersModule = serializersModule
        }
    }
    return rememberHelperBackStack(startRoute, configuration)
}

/**
 * Creates a restorable back stack with a caller-provided [SavedStateConfiguration].
 *
 * Prefer a serializers-module aware overload unless you need advanced serialization customization.
 */
@Suppress("UNCHECKED_CAST")
@Composable
fun rememberHelperBackStack(
    startRoute: NavScreen,
    configuration: SavedStateConfiguration
): NavBackStackState {
    val entries = rememberNavBackStack(configuration, startRoute)

    return remember(startRoute, entries) {
        NavBackStackState(
            startRoute,
            entries as NavBackStack<NavScreen>
        )
    }
}
