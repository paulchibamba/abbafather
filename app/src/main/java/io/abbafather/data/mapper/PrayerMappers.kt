package io.abbafather.data.mapper

import io.abbafather.data.local.entity.CollectionMemberEntity
import io.abbafather.data.local.entity.PersonalPrayerEntity
import io.abbafather.data.local.entity.PrayerCollectionEntity
import io.abbafather.data.local.entity.PrayerCollectionWithMembers
import io.abbafather.data.local.entity.PrayerEntity
import io.abbafather.data.local.entity.PrayerScriptureEntity
import io.abbafather.data.local.entity.PrayerWithDetail
import io.abbafather.data.local.entity.SavedLineEntity
import io.abbafather.domain.model.PersonalPrayer
import io.abbafather.domain.model.Prayer
import io.abbafather.domain.model.PrayerCollection
import io.abbafather.domain.model.PrayerMovement
import io.abbafather.domain.model.PrayerProvenance
import io.abbafather.domain.model.SavedLine
import io.abbafather.domain.model.ScriptureReference

/**
 * The boundary Room stops at. Entities are the storage shape — composite keys, join rows, ordering
 * columns — and nothing above `data/` is asked to know about any of it.
 */
fun PrayerWithDetail.toDomain(): Prayer {
    val linesByMovement = lines.sortedBy { it.lineIndex }.groupBy { it.movementIndex }
    val themesByMovement = movementThemes.groupBy { it.movementIndex }
    val scripturesByMovement = scriptures.groupBy { it.movementIndex }

    return Prayer(
        id = prayer.id,
        title = prayer.title,
        part = prayer.part,
        voice = prayer.voice,
        tags = tags.mapTo(mutableSetOf()) { it.tag },
        movements = movements.sortedBy { it.movementIndex }.map { movement ->
            val movementLines = linesByMovement[movement.movementIndex].orEmpty()
            PrayerMovement(
                index = movement.movementIndex,
                heading = movement.heading,
                lines = movementLines.map { it.text },
                // The flat line index is the address a kept line and a session both hold, so it is
                // read from the row rather than recomputed by counting movements.
                firstLineIndex = movementLines.firstOrNull()?.lineIndex ?: 0,
                themes = themesByMovement[movement.movementIndex]
                    .orEmpty()
                    .sortedBy { it.position }
                    .map { it.text },
                scriptures = scripturesByMovement[movement.movementIndex]
                    .orEmpty()
                    .sortedBy { it.position }
                    .map(PrayerScriptureEntity::toDomain),
            )
        },
        provenance = prayer.toProvenance(),
        lastOpenedAt = prayer.lastOpenedAt,
    )
}

private fun PrayerScriptureEntity.toDomain() = ScriptureReference(
    reference = reference,
    translation = translation,
    connection = connection,
)

private fun PrayerEntity.toProvenance() = PrayerProvenance(
    originalTitle = originalTitle,
    originalAuthor = originalAuthor,
    originalSource = originalSource,
    originalPublicationDate = originalPublicationDate,
    copyrightStatus = copyrightStatus,
    adaptationType = adaptationType,
    adaptationNote = adaptationNote,
)

fun SavedLineEntity.toDomain(): SavedLine = SavedLine(
    id = id,
    text = text,
    sourcePrayerId = sourcePrayerId,
    sourcePrayerTitle = sourcePrayerTitle,
    sourceAttribution = sourceAttribution,
    sourceLineIndex = sourceLineIndex,
    tags = tags,
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
    tags = tags,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
)

fun PersonalPrayerEntity.toDomain(): PersonalPrayer = PersonalPrayer(
    id = id,
    title = title,
    body = body,
    tags = tags,
    seededFromSavedLineId = seededFromSavedLineId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
)

fun PersonalPrayer.toEntity(): PersonalPrayerEntity = PersonalPrayerEntity(
    id = id,
    title = title,
    body = body,
    tags = tags,
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
