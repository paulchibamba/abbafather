package io.abbafather.navigation

import kotlinx.serialization.Serializable

/**
 * Every destination in the app, as a type. Arguments travel as constructor parameters rather than as
 * strings pushed into a path, so a typo is a compile error rather than a crash at the tap.
 */
sealed interface AbbaRoute {

    @Serializable
    data object Home : AbbaRoute

    @Serializable
    data object Library : AbbaRoute

    @Serializable
    data object MyPrayers : AbbaRoute

    @Serializable
    data object Saved : AbbaRoute

    /** Reading a catalogue prayer whole, before or instead of praying it line by line. */
    @Serializable
    data class Reader(val prayerId: String) : AbbaRoute

    /** The full-bleed dark session. Owns the system bars and shows no bottom bar. */
    @Serializable
    data class Session(val prayerId: String) : AbbaRoute

    /**
     * Writing a prayer. Opened blank from My prayers, with [personalPrayerId] to edit an existing one,
     * or with [seedText] when a kept line is being grown into a prayer.
     */
    @Serializable
    data class ComposePrayer(
        val personalPrayerId: String? = null,
        val seedText: String? = null,
    ) : AbbaRoute
}
