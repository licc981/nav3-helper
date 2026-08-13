package com.aleyn.navigation.core.route

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

/**
 *  @author : Aleyn
 *  @date : 2026/3/2 15:48
 */

/**
 * Parsed route data reused across the resolve pipeline so the same route string does not need to
 * be normalized and split multiple times during a single navigation.
 */
data class ParsedRouteUrl(
    val rawUrl: String,
    val routeKey: String,
    val queryParameters: Map<String, String?>
)

/**
 * Matches [routePattern] against [parsedRouteUrl] and merges path placeholders into the route
 * parameters. A placeholder must occupy a complete path segment, such as `{id}`.
 *
 * Path parameters take precedence over query parameters with the same name.
 */
fun matchRoutePattern(
    routePattern: String,
    parsedRouteUrl: ParsedRouteUrl
): ParsedRouteUrl? {
    val pattern = parseRoute(routePattern)
    val route = parseRoute(parsedRouteUrl.rawUrl)

    if (!pattern.scheme.equals(route.scheme, ignoreCase = true)) return null
    if (!pattern.authority.equals(route.authority, ignoreCase = true)) return null
    if (pattern.pathSegments.size != route.pathSegments.size) return null

    val pathParameters = buildMap {
        pattern.pathSegments.zip(route.pathSegments).forEach { (expected, actual) ->
            val placeholder = expected.placeholderName()
            when {
                placeholder != null -> put(placeholder, actual)
                expected != actual -> return null
            }
        }
    }

    return parsedRouteUrl.copy(
        routeKey = buildRouteKey(pattern),
        queryParameters = parsedRouteUrl.queryParameters + pathParameters
    )
}

fun routePatternMatches(routePattern: String, parsedRouteUrl: ParsedRouteUrl): Boolean {
    return matchRoutePattern(routePattern, parsedRouteUrl) != null
}

fun routePatternIdentity(route: String): String {
    val parsedRoute = parseRoute(route)
    return buildRouteKey(
        parsedRoute.copy(
            pathSegments = parsedRoute.pathSegments.map { segment ->
                if (segment.placeholderName() != null) "{}" else segment
            }
        )
    )
}

internal fun isRoutePattern(route: String): Boolean {
    return parseRoute(route).pathSegments.any { it.placeholderName() != null }
}

val NavRouteJson: Json = Json {
    ignoreUnknownKeys = true
}

private data class ParsedRoute(
    val scheme: String?,
    val authority: String?,
    val pathSegments: List<String>,
    val queryParameters: Map<String, String?>
)

/**
 * Parses a runtime route string into a normalized key plus decoded query parameters.
 *
 * The query string is not part of [ParsedRouteUrl.routeKey], so `user/detail?id=1` and
 * `user/detail?id=2` resolve to the same page identity.
 *
 * Normalization rules:
 *
 * - query parameters and fragments do not participate in the route key
 * - empty path segments are ignored, so trailing slashes do not change the key
 * - scheme and authority are normalized to lowercase
 * - when the same query key appears multiple times, the last value wins
 */
fun parseRouteUrl(route: String): ParsedRouteUrl {
    val parsedRoute = parseRoute(route)
    return ParsedRouteUrl(
        rawUrl = route,
        routeKey = buildRouteKey(parsedRoute),
        queryParameters = parsedRoute.queryParameters
    )
}

fun routeKey(route: String): String {
    return parseRouteUrl(route).routeKey
}

fun encodeRouteQueryValue(value: String): String {
    if (value.isEmpty()) return value

    val out = StringBuilder(value.length * 2)
    value.encodeToByteArray().forEach { byte ->
        val code = byte.toInt() and 0xFF
        val char = code.toChar()
        if (char.isRouteQueryUnreserved()) {
            out.append(char)
        } else {
            out.append('%')
            out.append(HEX_DIGITS[code ushr 4])
            out.append(HEX_DIGITS[code and 0x0F])
        }
    }
    return out.toString()
}

inline fun <reified T> serializeRouteQueryValue(
    value: T,
    json: Json = NavRouteJson
): String {
    return encodeRouteQueryValue(json.encodeToString(value))
}

inline fun <reified T> deserializeRouteQueryValue(
    value: String?,
    json: Json = NavRouteJson
): T? {
    val source = value ?: return null
    return runCatching {
        json.decodeFromString<T>(source)
    }.getOrNull()
}

private fun parseRoute(route: String): ParsedRoute {
    val normalized = route.substringBefore('#')
    val queryIndex = normalized.indexOf('?')
    val basePart = if (queryIndex >= 0) normalized.substring(0, queryIndex) else normalized
    val queryPart = if (queryIndex >= 0) normalized.substring(queryIndex + 1) else ""

    val schemeIndex = basePart.indexOf("://")
    val scheme = if (schemeIndex >= 0) basePart.substring(0, schemeIndex) else null
    val remainder = if (schemeIndex >= 0) basePart.substring(schemeIndex + 3) else basePart
    val authority = if (schemeIndex >= 0) remainder.substringBefore('/') else null
    val rawPath = if (schemeIndex >= 0) remainder.substringAfter('/', "") else remainder
    val pathSegments = rawPath
        .split('/')
        .filter { it.isNotEmpty() }
        .map(::decodeComponent)

    return ParsedRoute(
        scheme = scheme,
        authority = authority,
        pathSegments = pathSegments,
        queryParameters = parseQueryParameters(queryPart)
    )
}

private fun buildRouteKey(parsedRoute: ParsedRoute): String {
    return buildString {
        if (parsedRoute.scheme != null) {
            append(parsedRoute.scheme.lowercase())
            append("://")
            parsedRoute.authority?.lowercase()?.let(::append)
            if (parsedRoute.pathSegments.isNotEmpty()) {
                append('/')
            }
        }
        append(parsedRoute.pathSegments.joinToString("/"))
    }
}

private fun String.placeholderName(): String? {
    return takeIf { length > 2 && first() == '{' && last() == '}' }
        ?.substring(1, lastIndex)
}

private fun parseQueryParameters(query: String): Map<String, String?> {
    if (query.isBlank()) return emptyMap()

    return buildMap {
        query.split('&')
            .filter { it.isNotBlank() }
            .forEach { entry ->
                val equalsIndex = entry.indexOf('=')
                val rawKey = if (equalsIndex >= 0) entry.substring(0, equalsIndex) else entry
                val rawValue = if (equalsIndex >= 0) entry.substring(equalsIndex + 1) else null
                put(
                    decodeComponent(rawKey),
                    rawValue?.let(::decodeComponent)
                )
            }
    }
}

private fun decodeComponent(value: String): String {
    if ('%' !in value && '+' !in value) return value

    val out = StringBuilder(value.length)
    val bytes = mutableListOf<Byte>()

    fun flushBytes() {
        if (bytes.isEmpty()) return
        out.append(bytes.toByteArray().decodeToString())
        bytes.clear()
    }

    var index = 0
    while (index < value.length) {
        when (val ch = value[index]) {
            '%' -> {
                if (index + 2 >= value.length) return value
                val code = value.substring(index + 1, index + 3).toIntOrNull(16) ?: return value
                bytes += code.toByte()
                index += 3
            }

            '+' -> {
                flushBytes()
                out.append(' ')
                index++
            }

            else -> {
                flushBytes()
                out.append(ch)
                index++
            }
        }
    }
    flushBytes()
    return out.toString()
}

private fun Char.isRouteQueryUnreserved(): Boolean {
    return this in 'a'..'z'
            || this in 'A'..'Z'
            || this in '0'..'9'
            || this == '-'
            || this == '.'
            || this == '_'
            || this == '~'
}

private const val HEX_DIGITS = "0123456789ABCDEF"
