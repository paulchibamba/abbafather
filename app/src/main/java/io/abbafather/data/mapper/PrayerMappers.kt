package io.abbafather.data.mapper

import io.abbafather.data.local.entity.CollectionMemberEntity
import io.abbafather.data.local.entity.PersonalPrayerEntity
import io.abbafather.data.local.entity.PrayerCollectionEntity
import io.abbafather.data.local.entity.PrayerCollectionWithMembers
import io.abbafather.data.local.entity.PrayerWithDetail
import io.abbafather.data.local.entity.SavedLineEntity
import io.abbafather.domain.model.PersonalPrayer
import io.abbafather.domain.model.Prayer
import io.abbafather.domain.model.PrayerCollection
import io.abbafather.domain.model.SavedLine

/**
 * The boundary Room stops at. Entities are the storage shape — composite keys, join rows, ordering
 * columns — and nothing above `data/` is asked to know about any of it.
 */
fun PrayerWithDetail.toDomain(): Prayer = Prayer(
    id = prayer.id,
    title = prayer.title,
    author = prayer.author,
    kind = prayer.kind,
    group = prayer.group,
    themes = themes.mapTo(mutableSetOf()) { it.theme },
    lines = lines.sortedBy { it.lineIndex }.map { it.text },
    breathingPauseAfterLine = prayer.breathingPauseAfterLine,
    lastOpenedAt = prayer.lastOpenedAt,
)

fun SavedLineEntity.toDomain(): SavedLine = SavedLine(
    id = id,
    text = text,
    sourcePrayerId = sourcePrayerId,
    sourcePrayerTitle = sourcePrayerTitle,
    sourceAttribution = sourceAttribution,
    sourceLineIndex = sourceLineIndex,
    themes = themes,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
)

fun SavedLine.toEntity(): SavedLineEntity = SavedLineEntity(
    id = id,
    text = text,
    sourcePrayerId = sourcePrayerId,
    sourcePrayerTitle = sourcePrayerTitle,
    sourceAttribution = sourceAttribution,
    sourceLineIndex = sourceLineIndex,
    themes = themes,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
)

fun PersonalPrayerEntity.toDomain(): PersonalPrayer = PersonalPrayer(
    id = id,
    title = title,
    body = body,
    themes = themes,
    seededFromSavedLineId = seededFromSavedLineId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
)

fun PersonalPrayer.toEntity(): PersonalPrayerEntity = PersonalPrayerEntity(
    id = id,
    title = title,
    body = body,
    themes = themes,
    seededFromSavedLineId = seededFromSavedLineId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
)

fun PrayerCollectionWithMembers.toDomain(): PrayerCollection = PrayerCollection(
    id = collection.id,
    name = collection.name,
    memberPrayerIds = members.sortedBy { it.position }.map { it.prayerId },
    createdAt = collection.createdAt,
    updatedAt = collection.updatedAt,
    isDeleted = collection.isDeleted,
)

fun PrayerCollection.toEntity(): PrayerCollectionEntity = PrayerCollectionEntity(
    id = id,
    name = name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
)

fun PrayerCollection.toMemberEntities(): List<CollectionMemberEntity> =
    memberPrayerIds.mapIndexed { position, prayerId ->
        CollectionMemberEntity(collectionId = id, prayerId = prayerId, position = position)
    }
