package io.abbafather.data.local.seed

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.abbafather.core.common.IoDispatcher
import io.abbafather.data.local.dao.CatalogueRows
import io.abbafather.data.local.dao.PrayerDao
import io.abbafather.data.local.entity.PrayerEntity
import io.abbafather.data.local.entity.PrayerLineEntity
import io.abbafather.data.local.entity.PrayerMovementEntity
import io.abbafather.data.local.entity.PrayerMovementThemeEntity
import io.abbafather.data.local.entity.PrayerScriptureEntity
import io.abbafather.data.local.entity.PrayerTagEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fills the catalogue from the bundled asset whenever it is empty.
 *
 * "Whenever it is empty" rather than "on first creation": a migration that replaces the catalogue
 * leaves those tables empty on purpose, and this is what fills them again. Reseeding a catalogue
 * that already has prayers would duplicate every one of them, so [seedCatalogueIfEmpty] returns
 * early unless the table is genuinely empty, and the insert is one transaction so a half-written
 * catalogue can never look finished. It only ever writes catalogue tables, so it cannot touch
 * anything the reader owns.
 */
@Singleton
class CatalogueSeeder @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val prayerDao: PrayerDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun seedCatalogueIfEmpty() = withContext(ioDispatcher) {
        if (prayerDao.countPrayers() > 0) return@withContext
        prayerDao.insertCatalogue(readSeed().toRows())
    }

    private fun readSeed(): CatalogueSeed {
        val json = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        return SeedJson.decodeFromString<CatalogueSeed>(json)
    }

    /**
     * Flattens the asset into rows. Line indices are assigned across the whole prayer rather than
     * per movement, because a flat line index is the address the reader's kept lines hold.
     */
    private fun CatalogueSeed.toRows(): CatalogueRows {
        val prayerRows = mutableListOf<PrayerEntity>()
        val movementRows = mutableListOf<PrayerMovementEntity>()
        val lineRows = mutableListOf<PrayerLineEntity>()
        val themeRows = mutableListOf<PrayerMovementThemeEntity>()
        val scriptureRows = mutableListOf<PrayerScriptureEntity>()
        val tagRows = mutableListOf<PrayerTagEntity>()

        prayers.forEachIndexed { position, prayer ->
            prayerRows += prayer.toEntity(cataloguePosition = position)
            tagRows += prayer.tags.distinct().map { PrayerTagEntity(prayer.id, it) }

            var lineIndex = 0
            prayer.movements.forEachIndexed { movementIndex, movement ->
                movementRows += PrayerMovementEntity(prayer.id, movementIndex, movement.heading)
                movement.lines.forEach { text ->
                    lineRows += PrayerLineEntity(prayer.id, lineIndex++, movementIndex, text)
                }
                movement.themes.forEachIndexed { position, text ->
                    themeRows += PrayerMovementThemeEntity(prayer.id, movementIndex, position, text)
                }
                movement.scriptures.forEachIndexed { position, scripture ->
                    scriptureRows += PrayerScriptureEntity(
                        prayerId = prayer.id,
                        movementIndex = movementIndex,
                        position = position,
                        reference = scripture.reference,
                        translation = scripture.translation,
                        connection = scripture.connection,
                    )
                }
            }
        }

        return CatalogueRows(
            prayers = prayerRows,
            movements = movementRows,
            lines = lineRows,
            movementThemes = themeRows,
            scriptures = scriptureRows,
            tags = tagRows,
        )
    }

    private fun SeedPrayer.toEntity(cataloguePosition: Int) = PrayerEntity(
        id = id,
        title = title,
        part = part,
        voice = voice,
        cataloguePosition = cataloguePosition,
        originalTitle = provenance.originalTitle,
        originalAuthor = provenance.originalAuthor,
        originalSource = provenance.originalSource,
        originalPublicationDate = provenance.originalPublicationDate,
        copyrightStatus = provenance.copyrightStatus,
        adaptationType = provenance.adaptationType,
        adaptationNote = provenance.adaptationNote,
    )

    private companion object {
        const val ASSET_NAME = "prayer_catalogue.json"

        /**
         * Strict on purpose. The asset and this parser are built from the same corpus, so a key
         * this build does not recognise means they have drifted apart — which should stop the app
         * at the seam rather than seed a catalogue with something quietly missing.
         */
        val SeedJson = Json { ignoreUnknownKeys = false }
    }
}
