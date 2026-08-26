package io.abbafather.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * A catalogue prayer read whole. `@Relation` makes no promise about order, so every one of these
 * lists is sorted by its index in the mapper rather than trusted as it arrives.
 */
data class PrayerWithDetail(
    @Embedded val prayer: PrayerEntity,
    @Relation(parentColumn = "id", entityColumn = "prayer_id")
    val movements: List<PrayerMovementEntity>,
    @Relation(parentColumn = "id", entityColumn = "prayer_id")
    val lines: List<PrayerLineEntity>,
    @Relation(parentColumn = "id", entityColumn = "prayer_id")
    val movementThemes: List<PrayerMovementThemeEntity>,
    @Relation(parentColumn = "id", entityColumn = "prayer_id")
    val scriptures: List<PrayerScriptureEntity>,
    @Relation(parentColumn = "id", entityColumn = "prayer_id")
    val tags: List<PrayerTagEntity>,
)

/** A collection read whole, with its membership in reader order. */
data class PrayerCollectionWithMembers(
    @Embedded val collection: PrayerCollectionEntity,
    @Relation(parentColumn = "id", entityColumn = "collection_id")
    val members: List<CollectionMemberEntity>,
)
