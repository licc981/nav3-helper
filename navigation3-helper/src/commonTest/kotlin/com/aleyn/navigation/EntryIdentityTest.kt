package com.aleyn.navigation

import com.aleyn.navigation.core.route.NavScreen
import com.aleyn.navigation.core.route.newScreenEntryId
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * @author : Aleyn
 * @date : 2026/8/13 20:10
 *
 * Runtime contract behind `@Screen(multiInstance = true)`:
 *
 * 1. every navigation instance gets a unique `entryId`,
 * 2. `toString()` includes `entryId`, so navigation3 `NavEntry.contentKey` (defaults to
 *    `key.toString()`) is unique per push and the same route can be pushed twice,
 * 3. `equals`/`hashCode` ignore `entryId`, so URL-based operations like `NavCenter.goBack(url)`
 *    still match structurally.
 */
class EntryIdentityTest {

    @Test
    fun new_screen_entry_id_is_not_blank() {
        assertTrue(newScreenEntryId().isNotBlank())
    }

    @Test
    fun new_screen_entry_id_generates_unique_values() {
        val ids = (1..1000).map { newScreenEntryId() }
        assertEquals(1000, ids.distinct().size)
    }

    @Test
    fun multi_instance_destinations_with_same_params_have_distinct_content_keys() {
        val first = MultiInstanceDestination(id = 110)
        val second = MultiInstanceDestination(id = 110)

        // contentKey 由 toString() 推导，两个实例必须不同，否则会复用同一个页面槽位
        assertNotEquals(first.toString(), second.toString())
        assertNotEquals(first.entryId, second.entryId)
    }

    @Test
    fun multi_instance_structural_equality_ignores_entry_id() {
        val first = MultiInstanceDestination(id = 110)
        val second = MultiInstanceDestination(id = 110)

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())

        // 真正的业务参数不同仍然不相等
        assertNotEquals(first, MultiInstanceDestination(id = 111))
    }

    @Serializable
    private data class MultiInstanceDestination(
        val id: Int,
        val entryId: String = newScreenEntryId()
    ) : NavScreen {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || other !is MultiInstanceDestination) return false
            return id == other.id
        }

        override fun hashCode(): Int = id.hashCode()
    }
}
