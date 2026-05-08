package com.aleyn.navigation.core.ext

import com.aleyn.navigation.annotations.NavDslMarker
import com.aleyn.navigation.core.route.NavCenter
import com.aleyn.navigation.core.route.NavRegistry

/**
 * @author: Aleyn
 * @date: 2026/04/28 11:43
 * @desc: 入口扩展
 */


/**
 * 入口
 */
@NavDslMarker
fun loadNavRegistry(vararg registry: NavRegistry) {
    NavCenter.setRegistries(registry.toSet())
}