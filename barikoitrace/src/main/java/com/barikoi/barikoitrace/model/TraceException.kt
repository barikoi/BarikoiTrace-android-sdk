package com.barikoi.barikoitrace.model

/**
 * The throwable wrapper around [TraceError].
 *
 * `suspend` entry points used to throw a bare `Exception(error.message)`, which
 * threw away the stable `code` — leaving callers to string-match on a message
 * that is meant to be human-readable and therefore free to change. The iOS SDK
 * throws a typed `TraceError` carrying that code, so a shared wrapper could not
 * branch the same way on both platforms.
 *
 * Extends [Exception], so anything already catching `Exception` keeps working.
 *
 * ```kotlin
 * try {
 *     BarikoiTrace.setOrCreateUser("Jane", null, phone)
 * } catch (e: TraceException) {
 *     when (e.error.code) { "NO_KEY" -> …; "NETWORK" -> … }
 * }
 * ```
 */
class TraceException(val error: TraceError) : Exception(error.message) {
    val code: String get() = error.code
}
