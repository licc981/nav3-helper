package com.aleyn.nav.compiler.generator

import kotlin.test.Test
import kotlin.test.assertEquals

class NavCodeGeneratorTest {

    @Test
    fun generated_package_uses_source_package_for_regular_screens() {
        assertEquals(
            "sample.app.ui.page",
            generatedPackageName("sample.app.ui.page.MainScreen")
        )
    }

    @Test
    fun generated_package_falls_back_for_main_annotation_package() {
        assertEquals(
            GENERATED_PACKAGE_PREFIX,
            generatedPackageName("com.aleyn.nav.annotations.Main")
        )
    }

    @Test
    fun generated_package_falls_back_for_top_level_symbols() {
        assertEquals(
            GENERATED_PACKAGE_PREFIX,
            generatedPackageName("MainScreen")
        )
    }

}