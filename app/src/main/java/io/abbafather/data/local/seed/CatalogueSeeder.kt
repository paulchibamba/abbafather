package io.abbafather.data.local.seed

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.abbafather.core.common.IoDispatcher
import io.abbafather.data.local.dao.PrayerDao
import io.abbafather.data.local.entity.PrayerEntity
import io.abbafather.data.local.entity.PrayerLineEntity
import io.abbafather.data.local.entity.PrayerThemeEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Puts the bundled catalogue into the database the first time it is created.
 *
 * Two guards, because reseeding would duplicate every prayer: the callback that calls this only runs
 * on database creation, and [seedCatalogueIfEmpty] does nothing when any prayer is already present.
 * It only ever writes catalogue tables, so it cannot touch anything the reader owns.
 */
@Singleton
class CatalogueSeeder @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val prayerDao: PrayerDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun seedCatalogueIfEmpty() = withContext(ioDispatcher) {
        if (prayerDao.countPrayers() > 0) return@withContext
        insert(readSeed())
    }

    private fun readSeed(): CatalogueSeed {
        val json = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        return SeedJson.decodeFromString<CatalogueSeed>(json)
    }

    private suspend fun insert(seed: CatalogueSeed) {
        val prayers = seed.prayers.mapIndexed { position, prayer ->
            PrayerEntity(
                id = prayer.id,
                title = prayer.title,
                author = prayer.author,
                kind = prayer.kind,
                group = prayer.group,
                breathingPauseAfterLine = prayer.breathingPauseAfterLine,
                cataloguePosition = position,
            )
        }
        val lines = seed.prayers.flatMap { prayer ->
            prayer.lines.mapIndexed { lineIndex, text ->
                PrayerLineEntity(prayerId = prayer.id, lineIndex = lineIndex, text = text)
            }
        }
        val themes = seed.prayers.flatMap { prayer ->
            prayer.themes.distinct().map { PrayerThemeEntity(prayerId = prayer.id, theme = it) }
        }
        prayerDao.insertCatalogue(prayers, lines, themes)
    }

    private companion object {
        const val ASSET_NAME = "prayer_catalogue.json"
        val SeedJson = Json { ignoreUnknownKeys = true }
    }
}
