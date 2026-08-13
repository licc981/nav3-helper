package com.aleyn.nav.compiler.generator

import com.squareup.kotlinpoet.ClassName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @Test
    fun entry_id_parameter_defaults_to_runtime_generator() {
        val (parameter, property) = buildEntryIdParameter()
        val parameterCode = parameter.toString()
        assertTrue("entryId" in parameterCode, "parameter should be named entryId: $parameterCode")
        assertTrue("newScreenEntryId()" in parameterCode, "default should call newScreenEntryId(): $parameterCode")
        assertTrue("kotlin.String" in parameterCode, "entryId should be String: $parameterCode")
        assertEquals("entryId", property.name)
        assertEquals("kotlin.String", property.type.toString())
    }

    @Test
    fun entry_id_ignoring_equals_compares_only_structural_fields() {
        val funSpec = buildEntryIdIgnoringEquals(
            ClassName("sample.app.ui.page", "DetailScreenDestination"),
            listOf("filter", "id")
        )
        val code = funSpec.toString()
        assertTrue("override fun equals(other: kotlin.Any?)" in code, code)
        assertTrue("other !is sample.app.ui.page.DetailScreenDestination" in code, code)
        assertTrue("return other.filter == this.filter && other.id == this.id" in code, code)
        assertTrue("entryId" !in code, "equals must ignore entryId: $code")
    }

    @Test
    fun entry_id_ignoring_equals_supports_parameterless_screen() {
        val funSpec = buildEntryIdIgnoringEquals(
            ClassName("sample.app.ui.page", "CreateNoteScreenDestination"),
            emptyList()
        )
        val code = funSpec.toString()
        assertTrue("return true" in code, code)
        assertTrue("entryId" !in code, "equals must ignore entryId: $code")
    }

    @Test
    fun entry_id_ignoring_hash_code_uses_only_structural_fields() {
        val funSpec = buildEntryIdIgnoringHashCode(listOf("filter", "id"))
        val code = funSpec.toString()
        assertTrue("var result = filter.hashCode()" in code, code)
        assertTrue("result = 31 * result + id.hashCode()" in code, code)
        assertTrue("return result" in code, code)
        assertTrue("entryId" !in code, "hashCode must ignore entryId: $code")
    }

    @Test
    fun entry_id_ignoring_hash_code_supports_parameterless_screen() {
        val funSpec = buildEntryIdIgnoringHashCode(emptyList())
        val code = funSpec.toString()
        assertTrue("= 0" in code, code)
    }
}