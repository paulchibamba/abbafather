package io.abbafather.domain.util

/**
 * Ids for reader-owned rows. Behind an interface so a test can hand out predictable ids and so the
 * domain layer stays free of a platform UUID implementation.
 */
fun interface IdGenerator {
    fun newId(): String
}
