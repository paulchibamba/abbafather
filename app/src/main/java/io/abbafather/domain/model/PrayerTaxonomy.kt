package io.abbafather.domain.model

/**
 * Where a catalogue prayer comes from. The Library browses by group, so the display name is the
 * wording that appears on a group tile and the attribution line under a prayer's title.
 */
enum class PrayerGroup(val displayName: String) {
    BookOfCommonPrayer("Book of Common Prayer"),
    Psalter("Psalms"),
    Scripture("Scripture"),
    Puritan("Puritan"),
    ChurchFathers("Fathers and saints"),
    Reformers("Reformers"),
    Celtic("Celtic"),
}

/**
 * The occasion a prayer is reached for. Distinct from [PrayerTheme], which is what the prayer is
 * about: a psalm of confession is `Psalm` by kind and `Confession` by theme.
 */
enum class PrayerKind(val displayName: String) {
    Morning("Morning"),
    Evening("Evening"),
    Thanksgiving("Thanksgiving"),
    Confession("Confession"),
    Intercession("Intercession"),
    Meditation("Meditation"),
    Psalm("Psalm"),
    Blessing("Blessing"),
}

/** What a prayer or a kept line is about. These are the chips the reader filters and tags with. */
enum class PrayerTheme(val displayName: String) {
    Praise("Praise"),
    Thanksgiving("Thanksgiving"),
    Confession("Confession"),
    Peace("Peace"),
    Guidance("Guidance"),
    Anxiety("Anxiety"),
    Healing("Healing"),
    Grief("Grief"),
    Protection("Protection"),
    Mercy("Mercy"),
    Presence("God's presence"),
    Family("Family"),
}
