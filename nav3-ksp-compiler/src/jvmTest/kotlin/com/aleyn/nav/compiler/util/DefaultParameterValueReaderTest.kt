package com.aleyn.nav.compiler.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DefaultParameterValueReaderTest {

    @Test
    fun imported_default_value_keeps_explicit_import() {
        val result = DefaultParameterValueReader.readDefaultValue(
            resolver = { _, _ -> null },
            srcCodeLines = listOf("fun Demo(count: Int = DEFAULT_COUNT)"),
            packageName = "sample.app",
            imports = listOf("sample.constants.DEFAULT_COUNT"),
            argName = "count",
            argType = "Int"
        )

        val available = assertIs<DefaultValue.Available>(result)
        assertEquals("DEFAULT_COUNT", available.code)
        assertEquals(listOf("sample.constants.DEFAULT_COUNT"), available.imports)
    }

    @Test
    fun imported_default_value_keeps_wildcard_import_when_symbol_resolves() {
        val result = DefaultParameterValueReader.readDefaultValue(
            resolver = { pckg, name ->
                if (pckg == "sample.constants" && name == "DEFAULT_COUNT") {
                    ResolvedSymbol(isAccessible = true)
                } else {
                    null
                }
            },
            srcCodeLines = listOf("fun Demo(count: Int = DEFAULT_COUNT)"),
            packageName = "sample.app",
            imports = listOf("sample.constants.*"),
            argName = "count",
            argType = "Int"
        )

        val available = assertIs<DefaultValue.Available>(result)
        assertEquals("DEFAULT_COUNT", available.code)
        assertEquals(listOf("sample.constants.*"), available.imports)
    }

    @Test
    fun function_call_with_arguments_returns_error() {
        val result = DefaultParameterValueReader.readDefaultValue(
            resolver = { _, _ -> null },
            srcCodeLines = listOf("fun Demo(count: Int = provideCount(1))"),
            packageName = "sample.app",
            imports = emptyList(),
            argName = "count",
            argType = "Int"
        )

        assertIs<DefaultValue.Error>(result)
    }
}
