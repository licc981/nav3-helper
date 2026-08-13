package com.aleyn.navigation

import androidx.navigation3.runtime.NavBackStack
import com.aleyn.navigation.core.route.NavScreen
import com.aleyn.navigation.core.navigator.NavBackStackState
import kotlin.test.Test
import kotlin.test.assertEquals

class NavigatorStateTest {

    @Test
    fun navigator_instances_keep_separate_back_stacks() {
        val first = navigatorState()
        val second = navigatorState()

        first.navigate(TestDetail("first"))
        second.navigate(TestDetail("second"))

        assertEquals(listOf(TestRoot, TestDetail("first")), first.screens)
        assertEquals(listOf(TestRoot, TestDetail("second")), second.screens)
    }

    @Test
    fun go_back_to_target_removes_entries_above_target() {
        val navigator = navigatorState()
        val detail = TestDetail("detail")
        val result = TestDetail("result")

        navigator.navigate(detail)
        navigator.navigate(result)
        navigator.goBack(detail)

        assertEquals(listOf(TestRoot, detail), navigator.screens)
    }

    @Test
    fun inclusive_go_back_removes_target_too() {
        val navigator = navigatorState()
        val detail = TestDetail("detail")

        navigator.navigate(detail)
        navigator.goBack(detail, inclusive = true)

        assertEquals(listOf(TestRoot), navigator.screens)
    }

    @Test
    fun replace_updates_top_of_back_stack() {
        val navigator = navigatorState()

        navigator.navigate(TestDetail("detail"))
        navigator.replace(TestDetail("replacement"))

        assertEquals(listOf(TestRoot, TestDetail("replacement")), navigator.screens)
    }

    @Test
    fun remove_deletes_matching_entry_without_touching_others() {
        val navigator = navigatorState()
        val detail = TestDetail("detail")
        val another = TestDetail("another")

        navigator.navigate(detail)
        navigator.navigate(another)
        navigator.remove(detail)

        assertEquals(listOf(TestRoot, another), navigator.screens)
    }

    @Test
    fun go_back_does_not_remove_root_entry() {
        val navigator = navigatorState()

        navigator.goBack()

        assertEquals(listOf(TestRoot), navigator.screens)
    }

    @Test
    fun remove_does_not_clear_last_root_entry() {
        val navigator = navigatorState()

        navigator.remove(TestRoot)

        assertEquals(listOf(TestRoot), navigator.screens)
    }

    @Test
    fun reset_to_start_restores_root_only() {
        val navigator = navigatorState()

        navigator.navigate(TestDetail("detail"))
        navigator.navigate(TestDetail("another"))
        navigator.resetToStart()

        assertEquals(listOf(TestRoot), navigator.screens)
        assertEquals(TestRoot, navigator.current)
    }

    @Test
    fun query_properties_expose_root_previous_and_membership() {
        val navigator = navigatorState()
        val detail = TestDetail("detail")

        navigator.navigate(detail)

        assertEquals(detail, navigator.current)
        assertEquals(TestRoot, navigator.previous)
        assertEquals(true, detail in navigator.screens)
        assertEquals(1, navigator.screens.indexOf(detail))
    }
}

@kotlinx.serialization.Serializable
private data object TestRoot : NavScreen

@kotlinx.serialization.Serializable
private data class TestDetail(val value: String) : NavScreen

private fun navigatorState() = NavBackStackState(TestRoot, NavBackStack<NavScreen>(TestRoot))
