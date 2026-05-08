package com.aleyn.navigation.annotations

/**
 *  @author : Aleyn
 *  @date  2025/11/18 10:50
 *
 * Marks a composable function as a navigable screen.
 *
 * [route] is the optional fixed key for the screen.
 *
 * When [route] is not blank, it should be globally unique so any module can navigate to the
 * destination through the same route contract.
 *
 * When [route] is blank, the screen still participates in generated destinations and local
 * navigation, but it is not registered into the global route resolver.
 *
 * The library does not constrain the route protocol or naming convention. Callers may choose keys
 * such as `https://www.app.cn/user/detail`, `app://user/detail`, or `user/detail`.
 *
 * Dynamic values are passed in the runtime URL query string, for example navigating to
 * `app://user/detail?id=1&tab=post`, while the annotation route stays as `app://user/detail`.
 *
 * Query restoration is intended for lightweight route data such as String, primitive values,
 * and `@Serializable` objects encoded as JSON query values. Complex or large objects should still
 * stay out of the route, and modules that declare `@Serializable` screen parameters should apply
 * the Kotlin serialization plugin.
 *
 * When [start] is true, this screen becomes the default start destination for the module registry
 * generated from the current Kotlin module.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class Screen(
    val route: String = "",
    val start: Boolean = false
)
