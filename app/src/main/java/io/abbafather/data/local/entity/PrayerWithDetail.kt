package io.abbafather.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/** A catalogue prayer read whole: its lines in order and the themes attached to it. */
data class PrayerWithDetail(
    @Embedded val prayer: PrayerEntity,
    @Relation(parentColumn = "id", entityColumn = "prayer_id")
    val lines: List<PrayerLineEntity>,
    @Relation(parentColumn = "id", entityColumn = "prayer_id")
    val themes: List<PrayerThemeEntity>,
)

/** A collection read whole, with its membership in reader order. */
data class PrayerCollectionWithMembers(
    @Embedded val collection: PrayerCollectionEntity,
    @Relation(parentColumn = "id", entityColumn = "collection_id")
    val members: List<CollectionMemberEntity>,
)
