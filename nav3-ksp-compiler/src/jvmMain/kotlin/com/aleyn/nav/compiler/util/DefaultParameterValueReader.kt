package com.aleyn.nav.compiler.util

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.isInternal
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.NonExistLocation
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader


/**
 * @author : Aleyn
 * @date : 2025/12/2 09:42
 * @desc : 默认参数读取
 *
 * 由于 KSP 一直没有提供获取默认值的方法,官方给出的原因是获取默认值性能开销太大了。参考了 compose-destinations 库的获取方法，从源文件中读取。
 * 虽然这种方式也不太好，但是是目前能找到的相对可靠的方式
 *
 * Reference : https://github.com/raamcosta/compose-destinations
 * File : https://github.com/raamcosta/compose-destinations/blob/main/compose-destinations-ksp/src/main/kotlin/com/ramcosta/composedestinations/ksp/commons/DefaultParameterValueReader.kt
 */

object DefaultParameterValueReader {

    fun readDefaultValue(
        resolver: (pckg: String, name: String) -> ResolvedSymbol?,
        srcCodeLines: List<String>,
        packageName: String,
        imports: List<String>,
        argName: String,
        argType: String,
    ): DefaultValue {
        var auxText = srcCodeLines
            .map { it.removeLineComments() }
            .joinToString("") { it.trim() }
            .removeMultilineComments()

        val anchors = arrayOf(argName, ":", argType, "=")

        var index: Int

        anchors.forEach {
            index = auxText.indexOf(it)
            auxText = auxText.removeRange(0, index)
        }
        auxText = auxText.removeRange(0, 1)

        index = auxText.indexOfFirst { it != ' ' }
        auxText = auxText.removeRange(0, index)

        return if (auxText.startsWith("\"")) {
            if (auxText.contains("\"\"\"")) {
                DefaultValue.Error(
                    IllegalDestinationsSetup("Multiline string literals are not supported as navigation argument defaults (near: '$auxText'")
                )
            } else {
                DefaultValue.Available(stringLiteralValue(auxText))
            }
        } else {
            importedDefaultValue(resolver, auxText, packageName, imports)
        }
    }

    private fun String.removeMultilineComments(): String {
        val idxOfMultiLineComment = this.indexOf("/*")
        return if (idxOfMultiLineComment != -1 && !this.isInsideString(idxOfMultiLineComment)) {
            this.removeFromTo("/*", "*/")
        } else {
            this
        }
    }

    private fun String.removeLineComments(): String {
        val idxOfLineComment = this.indexOf("//")
        return if (idxOfLineComment != -1 && !this.isInsideString(idxOfLineComment)) {
            this.replaceAfter("//", "")
                .removeSuffix("//")
        } else {
            this
        }
    }

    private fun stringLiteralValue(auxText: String): String {
        var finalText = auxText
        val splits = finalText.split("\"")
        finalText = splits[1]

        var i = 2
        while (finalText.endsWith('\\')) {
            finalText += "\"${splits[i]}"
            i++
        }

        return "\"$finalText\""
    }

    private fun importedDefaultValue(
        resolver: (pckg: String, name: String) -> ResolvedSymbol?,
        auxText: String,
        packageName: String,
        imports: List<String>
    ): DefaultValue {

        var result = auxText
        val indexOfFinalClosingParenthesis = result.indexOfFinalClosingParenthesis()
        if (indexOfFinalClosingParenthesis != null) {
            result = result.removeRange(indexOfFinalClosingParenthesis, result.length)
        }

        // ':' means its another parameter (I think.. I don't know what other meaning a ':' would have here..)
        val indexOfNextParam = result.indexOfFirst { it == ':' }.takeIf { it != -1 }

        if (result.firstParenthesisIsOpening() && // if first parenthesis is "(" then it is not closing list of function params
            result.contains("(") && // we have a "(" and it's before a ")"
            result.indexOf('(') < (indexOfNextParam
                ?: result.lastIndex) // "(" is before next param if it exists
        ) {
            if (indexOfNextParam != null) {
                result = result.removeRange(indexOfNextParam, result.length)
            }

            val commaIndex = result.indexOfLast { it == ',' }
            if (commaIndex != -1) {
                result = result.removeRange(commaIndex, result.length)
            }
        } else {
            val index = result.indexOfFirst { it == ' ' || it == ',' || it == ')' }
            if (index != -1)
                result = result.removeRange(index, result.length)
        }

        if (result == "true"
            || result == "false"
            || result == "null"
            || result.first().isDigit()
        ) {
            return DefaultValue.Available(result)
        }

        val importableAux = result.removeFromTo("(", ")")

        if (result.length - importableAux.length > 2) {
            //we detected a function call with args, we can't resolve this
            return DefaultValue.Error(
                IllegalDestinationsSetup(
                    "Navigation arguments using function calls with parameters as their default value " +
                            "are not currently supported (near: '$auxText')"
                )
            )
        }

        val importable = importableAux.split(".")[0]
        val defValueImports = imports.filter { it.endsWith(".$importable") }

        if (defValueImports.isNotEmpty()) {
            return DefaultValue.Available(result, defValueImports)
        }

        if (resolver.invoke(packageName, importable).existsAndIsAccessible()) {
            return DefaultValue.Available(result, listOf("${packageName}.$importable"))
        }

        val wholePackageImports = imports
            .filter { it.endsWith(".*") }

        val validImports = wholePackageImports
            .filter { resolver.invoke(it.removeSuffix(".*"), importable).existsAndIsAccessible() }

        if (validImports.size == 1) {
            return DefaultValue.Available(result, listOf(validImports[0]))
        }

        if (result.startsWith("arrayListOf(") //std kotlin lib
            || result.startsWith("arrayOf(") //std kotlin lib
        ) {
            return DefaultValue.Available(result)
        }

        if (resolver.invoke(packageName, importable).existsAndIsPrivate()) {
            return DefaultValue.Error(IllegalDestinationsSetup("Navigation arguments with default values which uses a private declaration are not currently supported (near: '$auxText')"))
        }

        return DefaultValue.Available(result, wholePackageImports)
    }
}

private fun String.isInsideString(idxToCheck: Int): Boolean {
    var isInsideString = false

    for (i in indices) {
        when (this[i]) {
            '"' -> isInsideString = !isInsideString
            else -> {
                if (i == idxToCheck) {
                    return isInsideString
                }
            }
        }
    }

    return isInsideString
}

private fun String.firstParenthesisIsOpening(): Boolean {
    val indexOfFirstOpening = this.indexOfFirst { it == '(' }
    val indexOfFirstClosing = this.indexOfFirst { it == ')' }

    return indexOfFirstClosing >= indexOfFirstOpening
}

private fun String.indexOfFinalClosingParenthesis(): Int? {
    var closingsExpected = 0

    for (i in this.indices) {
        when (this[i]) {
            '(' -> closingsExpected++

            ')' -> if (closingsExpected > 0) {
                closingsExpected--
            } else {
                return i
            }
        }
    }

    return null
}

@OptIn(KspExperimental::class)
fun KSValueParameter.getDefaultValue(resolver: Resolver): DefaultValue {
    if (!hasDefault) return DefaultValue.NonExistent

    /*
        This is not ideal: having to read the first n lines of the file,
        and parse the default value manually from the source code
        I haven't found a better way yet, seems like there is no other
        way in KSP :/
    */

    if (location is NonExistLocation) {
        return DefaultValue.Error(
            IllegalDestinationsSetup(
                "Cannot detect default value for navigation" +
                        " argument '${name!!.asString()}' because we don't have access to source code. " +
                        "Nav argument classes from other modules with default values are not supported!"
            )
        )
    }

    val fileLocation = location as FileLocation
    val (lines, imports) = File(fileLocation.filePath)
        .readLinesAndImports(fileLocation.lineNumber, fileLocation.lineNumber + 10)

    return DefaultParameterValueReader.readDefaultValue(
        resolver = { pckg, name ->
            runCatching {
                resolver.getDeclarationsFromPackage(pckg)
                    .firstOrNull { it.simpleName.asString().contains(name) }
                    ?.let {
                        ResolvedSymbol(it.isPublic() || it.isInternal())
                    }
            }.getOrNull()
        },
        srcCodeLines = lines,
        packageName = this.containingFile!!.packageName.asString(),
        imports = imports,
        argName = name!!.asString(),
        argType = type.resolve().declaration.simpleName.asString()
    )
}

class ResolvedSymbol(val isAccessible: Boolean)

private fun ResolvedSymbol?.existsAndIsAccessible() = this != null && this.isAccessible
private fun ResolvedSymbol?.existsAndIsPrivate() = this != null && !this.isAccessible


fun String.removeFromTo(from: String, to: String): String {
    val startIndex = indexOf(from)
    val endIndex = indexOf(to) + to.length

    return kotlin.runCatching { removeRange(startIndex, endIndex) }.getOrNull() ?: this
}

fun File.readLinesAndImports(
    startLineNumber: Int,
    endLineNumber: Int
): Pair<List<String>, List<String>> {
    val bufferedReader = BufferedReader(InputStreamReader(FileInputStream(this), Charsets.UTF_8))
    return bufferedReader
        .useLines { lines: Sequence<String> ->
            val linesList = lines
                .take(endLineNumber)
                .toList()

            val linesRes = linesList.takeLast(linesList.size - (startLineNumber - 1))
            val imports = linesList.filter { it.startsWith("import") }
                .map { it.removePrefix("import ") }

            linesRes to imports
        }
}


sealed interface DefaultValue {

    data class Error(val throwable: Throwable) : DefaultValue

    data object NonExistent : DefaultValue

    data class Available(
        val code: String,
        val imports: List<String> = emptyList()
    ) : DefaultValue
}

class IllegalDestinationsSetup(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)