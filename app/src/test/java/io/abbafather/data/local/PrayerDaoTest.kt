package io.abbafather.data.local

import io.abbafather.data.local.dao.CatalogueRows
import io.abbafather.data.local.entity.PrayerEntity
import io.abbafather.data.local.entity.PrayerLineEntity
import io.abbafather.data.local.entity.PrayerMovementEntity
import io.abbafather.data.local.entity.PrayerMovementThemeEntity
import io.abbafather.data.local.entity.PrayerScriptureEntity
import io.abbafather.data.local.entity.PrayerTagEntity
import io.abbafather.data.mapper.toDomain
import io.abbafather.domain.model.PrayerPart
import io.abbafather.domain.model.PrayerTag
import io.abbafather.domain.model.PrayerVoice
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PrayerDaoTest {

    @get:Rule val databaseRule = AbbaDatabaseTestRule()

    private val prayerDao get() = databaseRule.database.prayerDao()

    /**
     * Every row of this prayer is inserted out of order on purpose: `@Relation` promises nothing
     * about the order it hands lists back, so the ordering has to be the mapper's doing.
     */
    @Test
    fun `a prayer reads back whole, in the order it is prayed`() = runTest {
        insertMorning()

        val prayer = prayerDao.observePrayers().first().single().toDomain()

        assertEquals("Morning", prayer.title)
        assertEquals(PrayerPart.NeedsAndDevotions, prayer.part)
        assertEquals(PrayerVoice.Personal, prayer.voice)
        assertEquals(setOf(PrayerTag.Grace, PrayerTag.MorningAndEvening), prayer.tags)
        assertEquals(
            listOf("Receiving this new day as mercy", "Asking for the day to matter"),
            prayer.movements.map { it.heading },
        )
        assertEquals(
            listOf(
                "Compassionate Lord, I woke up today because your mercy carried me here.",
                "Thank you for the gift of another morning.",
                "Let it matter for my soul.",
            ),
            prayer.lines,
        )
        assertEquals(listOf(0, 2), prayer.movements.map { it.firstLineIndex })
        assertEquals(
            listOf("Psalm 90:14", "Ephesians 5:16"),
            prayer.movements.flatMap { movement -> movement.scriptures.map { it.reference } },
        )
        assertEquals(listOf("Mercy is new each morning."), prayer.movements.first().themes)
    }

    /** A pause belongs at the end of every movement but the last, wherever those lines fall. */
    @Test
    fun `the pauses land where the movements end`() = runTest {
        insertMorning()

        val prayer = prayerDao.observePrayers().first().single().toDomain()

        assertEquals(setOf(1), prayer.breathingPauseLineIndices)
        assertTrue(prayer.hasBreathingPauseAfter(1))
        assertEquals(0, prayer.movementOfLine(0).index)
        assertEquals(1, prayer.movementOfLine(2).index)
    }

    @Test
    fun `the catalogue is ordered as the seed file wrote it, not alphabetically`() = runTest {
        prayerDao.insertPrayers(
            listOf(
                prayerEntity(id = "second", title = "Contentment", position = 1),
                prayerEntity(id = "first", title = "A Ministers Prayer", position = 0),
            ),
        )

        val titles = prayerDao.observePrayers().first().map { it.prayer.title }

        assertEquals(listOf("A Ministers Prayer", "Contentment"), titles)
    }

    @Test
    fun `recently opened is most recent first and never includes an unopened prayer`() = runTest {
        prayerDao.insertPrayers(
            listOf(
                prayerEntity(id = "opened-early", position = 0),
                prayerEntity(id = "opened-late", position = 1),
                prayerEntity(id = "never-opened", position = 2),
            ),
        )

        prayerDao.updateLastOpenedAt("opened-early", openedAt = 1_000L)
        prayerDao.updateLastOpenedAt("opened-late", openedAt = 2_000L)

        val recent = prayerDao.observeRecentlyOpenedPrayers(limit = 5).first()

        assertEquals(listOf("opened-late", "opened-early"), recent.map { it.prayer.id })
    }

    @Test
    fun `a prayer that is not there reads as nothing rather than failing`() = runTest {
        assertNull(prayerDao.getPrayer("no-such-prayer"))
        assertEquals(0, prayerDao.countPrayers())
    }

    @Test
    fun `deleting a prayer takes everything of its own with it`() = runTest {
        insertMorning()

        databaseRule.database.openHelper.writableDatabase
            .execSQL("DELETE FROM prayers WHERE id = '$MorningId'")

        assertEquals(0, prayerDao.countPrayers())
        assertNull(prayerDao.getPrayer(MorningId))
        listOf(
            "prayer_movements",
            "prayer_lines",
            "prayer_movement_themes",
            "prayer_scriptures",
            "prayer_tags",
        ).forEach { table ->
            assertEquals("$table still has rows", 0, countRowsIn(table))
        }
    }

    private fun countRowsIn(table: String): Int =
        databaseRule.database.openHelper.writableDatabase
            .query("SELECT COUNT(*) FROM $table")
            .use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }

    private suspend fun insertMorning() {
        prayerDao.insertCatalogue(
            CatalogueRows(
                prayers = listOf(prayerEntity(id = MorningId, title = "Morning")),
                movements = listOf(
                    PrayerMovementEntity(MorningId, 1, "Asking for the day to matter"),
                    PrayerMovementEntity(MorningId, 0, "Receiving this new day as mercy"),
                ),
                lines = listOf(
                    PrayerLineEntity(MorningId, 2, 1, "Let it matter for my soul."),
                    PrayerLineEntity(
                        MorningId,
                        0,
                        0,
                        "Compassionate Lord, I woke up today because your mercy carried me here.",
                    ),
                    PrayerLineEntity(MorningId, 1, 0, "Thank you for the gift of another morning."),
                ),
                movementThemes = listOf(
                    PrayerMovementThemeEntity(MorningId, 0, 0, "Mercy is new each morning."),
                ),
                scriptures = listOf(
                    PrayerScriptureEntity(MorningId, 1, 0, "Ephesians 5:16", "ESV", "On the days we are given."),
                    PrayerScriptureEntity(MorningId, 0, 0, "Psalm 90:14", "ESV", "On morning mercy."),
                ),
                tags = listOf(
                    PrayerTagEntity(MorningId, PrayerTag.Grace),
                    PrayerTagEntity(MorningId, PrayerTag.MorningAndEvening),
                ),
            ),
        )
    }

    private fun prayerEntity(
        id: String,
        title: String = "Morning",
        position: Int = 0,
    ) = PrayerEntity(
        id = id,
        title = title,
        part = PrayerPart.NeedsAndDevotions,
        voice = PrayerVoice.Personal,
        cataloguePosition = position,
        originalTitle = title,
        originalAuthor = "Unattributed Puritan source (compiled and edited by Arthur Bennett)",
        originalSource = "The Valley of Vision: A Collection of Puritan Prayers and Devotions",
        originalPublicationDate = "1975",
        copyrightStatus = "Compilation in copyright; underlying Puritan sources are public domain.",
        adaptationType = "thematic modern adaptation",
        adaptationNote = "Contemporary prayer based on the themes of the historical source.",
    )

    private companion object {
        const val MorningId = "vov-106-morning"
    }
}
