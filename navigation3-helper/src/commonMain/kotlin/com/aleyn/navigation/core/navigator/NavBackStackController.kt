package com.aleyn.navigation.core.navigator

import androidx.navigation3.runtime.NavBackStack
import com.aleyn.navigation.core.route.NavScreen

/**
 * @author: Aleyn
 * @date: 2026/04/29 16:09
 */
interface NavBackStackController {

    val startRoute: NavScreen

    val navBackStack: NavBackStack<NavScreen>

    val screens: List<NavScreen>

    val current: NavScreen?

    val previous: NavScreen?

    fun navigate(direction: NavScreen)

    fun goBack()

    fun goBack(direction: NavScreen, inclusive: Boolean = false)

    fun replace(direction: NavScreen)

    fun remove(direction: NavScreen)

    fun resetToStart()

    fun setResult(key: String, value: Any?)

    fun peekResult(key: String): Any?

    fun consumeResult(key: String): Any?

    fun hasResult(key: String): Boolean

    fun clearResult(key: String)
}
