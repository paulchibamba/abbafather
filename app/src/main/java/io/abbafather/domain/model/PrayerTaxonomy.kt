package io.abbafather.domain.model

/**
 * Where a prayer sits in the collection it came from (see [PrayerProvenance]). The Library browses by these, and a part is
 * the closest thing the catalogue has to a shelf.
 */
enum class PrayerPart(val displayName: String) {
    Introductory("Introductory"),
    FatherSonAndHolySpirit("Father, Son, and Holy Spirit"),
    RedemptionAndReconciliation("Redemption and Reconciliation"),
    PenitenceAndDeprecation("Penitence and Deprecation"),
    NeedsAndDevotions("Needs and Devotions"),
    HolyAspirations("Holy Aspirations"),
    ApproachToGod("Approach to God"),
    GiftsOfGrace("Gifts of Grace"),
    ServiceAndMinistry("Service and Ministry"),
    Valediction("Valediction"),
    AWeeksSharedPrayers("A Week's Shared Prayers"),
}

/**
 * Who the prayer says "I". Most are prayed alone; the week's shared prayers say "we" and are meant
 * for a household or a gathering praying together.
 */
enum class PrayerVoice(val displayName: String) {
    Personal("Prayed alone"),
    Corporate("Prayed together"),
}

/**
 * What a prayer is about. This is the catalogue's own vocabulary rather than one invented for the
 * app, and it is closed on purpose: a tag that is not here fails the catalogue build rather than
 * arriving quietly on someone's phone. Kept lines and written prayers carry the same tags.
 */
enum class PrayerTag(val displayName: String) {
    Adoption("Adoption"),
    Assurance("Assurance"),
    Atonement("Atonement"),
    ChristSufficiency("Christ's sufficiency"),
    ChurchAndCommunity("Church and community"),
    Contentment("Contentment"),
    DeathAndEternity("Death and eternity"),
    Evangelism("Evangelism"),
    Faith("Faith"),
    Family("Family"),
    FatherHeartOfGod("The Father's heart"),
    FearAndAnxiety("Fear and anxiety"),
    Forgiveness("Forgiveness"),
    Grace("Grace"),
    Guidance("Guidance"),
    HolinessOfGod("God's holiness"),
    HolySpirit("The Holy Spirit"),
    Humility("Humility"),
    Incarnation("Incarnation"),
    Joy("Joy"),
    Justification("Justification"),
    LordsDay("The Lord's Day"),
    LoveForOthers("Love for others"),
    LoveOfGod("God's love"),
    MinistryAndService("Ministry and service"),
    MorningAndEvening("Morning and evening"),
    Prayer("Prayer"),
    Providence("Providence"),
    Reconciliation("Reconciliation"),
    Redemption("Redemption"),
    Repentance("Repentance"),
    Resurrection("Resurrection"),
    Sanctification("Sanctification"),
    Scripture("Scripture"),
    SecondComing("The second coming"),
    SelfExamination("Self-examination"),
    SinAndConviction("Sin and conviction"),
    Sovereignty("Sovereignty"),
    SpiritualDryness("Spiritual dryness"),
    SpiritualWarfare("Spiritual warfare"),
    Suffering("Suffering"),
    Surrender("Surrender"),
    Temptation("Temptation"),
    Thanksgiving("Thanksgiving"),
    Trinity("The Trinity"),
    UnionWithChrist("Union with Christ"),
    WorkAndVocation("Work and vocation"),
    Worship("Worship"),
}
