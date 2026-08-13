package com.aleyn.navigation

import com.aleyn.navigation.core.route.matchRoutePattern
import com.aleyn.navigation.core.route.parseRouteUrl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RouteUrlTest {

    @Test
    fun path_placeholders_are_extracted_and_merged_with_query_parameters() {
        val parsed = parseRouteUrl(
            "https://www.myapp.com/users/active/42?tab=posts&id=query-value"
        )

        val matched = matchRoutePattern(
            "https://www.myapp.com/users/{filter}/{id}",
            parsed
        )

        assertEquals("active", matched?.queryParameters?.get("filter"))
        assertEquals("42", matched?.queryParameters?.get("id"))
        assertEquals("posts", matched?.queryParameters?.get("tab"))
    }

    @Test
    fun path_placeholder_values_are_percent_decoded() {
        val matched = matchRoutePattern(
            "app://users/{name}",
            parseRouteUrl("app://users/Aleyn%20Lin")
        )

        assertEquals("Aleyn Lin", matched?.queryParameters?.get("name"))
    }

    @Test
    fun template_rejects_different_static_segments() {
        assertNull(
            matchRoutePattern(
                "https://www.myapp.com/users/{id}",
                parseRouteUrl("https://www.myapp.com/admin/42")
            )
        )
    }
}
