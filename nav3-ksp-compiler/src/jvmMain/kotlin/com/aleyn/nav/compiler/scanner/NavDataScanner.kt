package com.aleyn.nav.compiler.scanner

import com.aleyn.navigation.annotations.Screen
import com.aleyn.nav.compiler.model.MetaData
import com.aleyn.navigation.core.route.routeKey
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.validate

/**
 * @author : Aleyn
 * @date : 2025/11/19 14:53
 */

class NavDataScanner(private val logger: KSPLogger) {

    fun findInvalidSymbols(resolver: Resolver): List<KSAnnotated> {
        val invalidSymbols = resolver.getInvalidSymbols<Screen>()

        if (invalidSymbols.isNotEmpty()) {
            logger.logging("Invalid definition symbols found.")
            return invalidSymbols
        }
        return emptyList()
    }

    fun scannerScreenFun(
        moduleName: String,
        resolver: Resolver
    ): List<MetaData.RegistryModel> {
        val screenModels = resolver.getValidSymbols<Screen>()
            .filterIsInstance<KSFunctionDeclaration>()
            .mapNotNull { functionDeclaration ->
                if (!functionDeclaration.annotations.any { it.shortName.asString() == "Composable" }) {
                    logger.error("@Screen can only be used on methods with @Composable", functionDeclaration)
                    return@mapNotNull null
                }

                val simpleName = functionDeclaration.simpleName.asString()
                val screenAnnotation = functionDeclaration.annotations.firstOrNull {
                    it.shortName.asString() == Screen::class.simpleName
                }
                val route = screenAnnotation?.arguments?.firstOrNull {
                    it.name?.asString() == Screen::route.name
                }?.value?.toString().orEmpty()
                val start = (screenAnnotation?.arguments?.firstOrNull {
                    it.name?.asString() == Screen::start.name
                }?.value).toString().toBooleanStrictOrNull() ?: false

                if (route.isBlank()) {
                    return@mapNotNull MetaData.ScreenModel(
                        qualifiedName = functionDeclaration.qualifiedName?.asString().orEmpty(),
                        simpleName = simpleName,
                        funParams = functionDeclaration.parameters,
                        route = "",
                        start = start,
                        containingFile = functionDeclaration.containingFile!!
                    )
                }

                if ('{' in route || '}' in route) {
                    logger.error(
                        "@Screen route must be a fixed route key. Put dynamic values in the runtime query string instead of using placeholders.",
                        functionDeclaration
                    )
                    return@mapNotNull null
                }

                if ('?' in route || '#' in route) {
                    logger.error(
                        "@Screen route must not declare query parameters or fragments. Put dynamic values in the runtime query string instead.",
                        functionDeclaration
                    )
                    return@mapNotNull null
                }

                validateRouteContract(route)?.let { message ->
                    logger.error(message, functionDeclaration)
                    return@mapNotNull null
                }

                MetaData.ScreenModel(
                    qualifiedName = functionDeclaration.qualifiedName?.asString().orEmpty(),
                    simpleName = simpleName,
                    funParams = functionDeclaration.parameters,
                    route = route,
                    start = start,
                    containingFile = functionDeclaration.containingFile!!
                )
            }

        if (screenModels.isEmpty()) return emptyList()

        return screenModels
            .groupBy { screenModel ->
                screenSourceSetName(screenModel.containingFile.filePath)
            }
            .map { (sourceSetName, models) ->
                MetaData.RegistryModel(
                    registryName = moduleRegistryName(
                        moduleName = moduleName,
                        filePath = models.first().containingFile.filePath,
                        sourceSetName = sourceSetName
                    ),
                    screenModels = models
                )
            }
    }
}

internal fun moduleRegistryName(
    moduleName: String,
    filePath: String? = null,
    sourceSetName: String? = null
): String {
    val pathModuleName = filePath
        ?.replace('\\', '/')
        ?.split('/')
        ?.let { segments ->
            val srcIndex = segments.indexOfLast { it == "src" }
            if (srcIndex > 0) segments[srcIndex - 1] else null
        }

    val sourceModuleName = pathModuleName?.takeIf { it.isNotBlank() } ?: moduleName
    val trimmedCommonMain = sourceModuleName
        .removeSuffix("-commonMain")
        .removeSuffix("_commonMain")
        .removeSuffix(".commonMain")
        .removeSuffix("commonMain")
        .trim('-', '_', '.')

    val source = trimmedCommonMain.ifBlank { sourceModuleName }
    val parts = source.split(Regex("[^A-Za-z0-9]+"))
        .filter { it.isNotBlank() }
        .filterNot {
            val token = it.lowercase()
            token == "common" || token == "main" || token == "metadata"
        }

    val normalized = buildString {
        parts.forEach { part ->
            append(part.replaceFirstChar { char -> char.uppercase() })
        }
    }.ifBlank { "Module" }

    val baseName = if (normalized.first().isDigit()) "Module$normalized" else normalized
    val sourceSetSuffix = sourceSetName
        ?.takeUnless { it.equals("commonMain", ignoreCase = true) || it.equals("main", ignoreCase = true) }
        ?.split(Regex("[^A-Za-z0-9]+"))
        ?.filter { it.isNotBlank() }
        ?.joinToString("") { part ->
            part.replaceFirstChar { char -> char.uppercase() }
        }
        .orEmpty()

    return baseName + sourceSetSuffix
}

internal fun screenSourceSetName(filePath: String?): String {
    val segments = filePath
        ?.replace('\\', '/')
        ?.split('/')
        ?: return "main"

    val srcIndex = segments.indexOfLast { it == "src" }
    if (srcIndex == -1 || srcIndex + 1 >= segments.size) return "main"
    return segments[srcIndex + 1]
}

private inline fun <reified T> Resolver.getValidSymbols(): List<KSAnnotated> {
    return this.getSymbolsWithAnnotation(T::class.qualifiedName!!)
        .filter { it.validate() }
        .toList()
}

private inline fun <reified T> Resolver.getInvalidSymbols(): List<KSAnnotated> {
    return this.getSymbolsWithAnnotation(T::class.qualifiedName!!)
        .filter { !it.validate() }
        .toList()
}

private fun validateRouteContract(route: String): String? {
    if (route != route.trim()) {
        return "@Screen route must not have leading or trailing whitespace."
    }

    if (route.any(Char::isWhitespace)) {
        return "@Screen route must not contain whitespace."
    }

    if (routeKey(route).isBlank()) {
        return "@Screen route must resolve to a non-blank key."
    }

    return null
}
