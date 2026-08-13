package com.aleyn.navigation.core.route

import kotlin.random.Random
import kotlin.time.TimeSource

/**
 * @author : Aleyn
 * @date : 2026/8/13 20:00
 *
 * Runtime identity support for screens annotated with `@Screen(multiInstance = true)`.
 *
 * navigation3 derives `NavEntry.contentKey` from `NavKey.toString()` by default. Two destinations
 * that produce the same string are treated as the same content, so pushing the same route with the
 * same parameters twice reuses the same content slot and saved state instead of creating a new
 * page instance. The generated destination class for multi-instance screens appends a unique
 * [newScreenEntryId] to its primary constructor, which makes every navigation instance produce a
 * distinct `toString()` (and therefore a distinct `contentKey`).
 */

private val entryIdTimeSource = TimeSource.Monotonic

private var entryIdSequence = 0L

/**
 * Generates a best-effort unique id for a navigation instance.
 *
 * The id combines a monotonic clock reading, a process-local sequence number and a random value,
 * so it stays unique within a process without depending on any experimental API. It is intended to
 * be used as the default value of the generated destination's `entryId` parameter, and callers may
 * also pass their own id (for example a business id) when constructing the destination directly.
 */
fun newScreenEntryId(): String {
    val now = entryIdTimeSource.markNow().elapsedNow().inWholeNanoseconds
    val sequence = entryIdSequence++
    return "$now-$sequence-${Random.nextInt(0x1000000).toString(16)}"
}
