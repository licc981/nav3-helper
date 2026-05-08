package com.aleyn.navigation.core.route

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider

/**
 * @author : Aleyn
 * @date : 2025/11/28 15:48
 */

typealias EntryProvider = (NavScreen) -> NavEntry<NavScreen>


fun getEntryProvider(navRegistryList: Set<NavRegistry>) =
    getEntryProvider(*navRegistryList.toTypedArray())

fun getEntryProvider(vararg navRegistry: NavRegistry) = entryProvider {
    navRegistry.forEach { it.entryProvider(this) }
}