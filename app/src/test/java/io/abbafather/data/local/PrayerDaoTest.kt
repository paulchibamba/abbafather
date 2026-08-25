package io.abbafather.data.local

import io.abbafather.data.local.entity.PrayerEntity
import io.abbafather.data.local.entity.PrayerLineEntity
import io.abbafather.data.local.entity.PrayerThemeEntity
import io.abbafather.data.mapper.toDomain
import io.abbafather.domain.model.PrayerGroup
import io.abbafather.domain.model.PrayerKind
import io.abbafather.domain.model.PrayerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PrayerDaoTest {

    @get:Rule val databaseRule = AbbaDatabaseTestRule()

    private val prayerDao get() = databaseRule.database.prayerDao()

    @Test
    fun `a prayer reads back whole, with its lines in the order they are prayed`() = runTest {
        insertCollectForPeace()

        val prayer = prayerDao.observePrayers().first().single().toDomain()

        assertEquals("A Collect for Peace", prayer.title)
        assertEquals(PrayerKind.Evening, prayer.kind)
        assertEquals(PrayerGroup.BookOfCommonPrayer, prayer.group)
        assertEquals(setOf(PrayerTheme.Peace, PrayerTheme.Protection), prayer.themes)
        assertEquals(
            listOf("O God, from whom all holy desires,", "all good counsels do proceed;", "Amen."),
            prayer.lines,
        )
        assertEquals(1, prayer.breathingPauseAfterLine)
    }

    @Test
    fun `the catalogue is ordered as the seed file wrote it, not alphabetically`() = runTest {
        prayerDao.insertPrayers(
            listOf(
                prayerEntity(id = "second", title = "A Collect for Grace", position = 1),
                prayerEntity(id = "first", title = "Psalm 63", position = 0),
            ),
        )

        val titles = prayerDao.observePrayers().first().map { it.prayer.title }

        assertEquals(listOf("Psalm 63", "A Collect for Grace"), titles)
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
    fun `deleting a prayer takes its lines and themes with it`() = runTest {
        insertCollectForPeace()

        databaseRule.database.openHelper.writableDatabase
            .execSQL("DELETE FROM prayers WHERE id = 'bcp-collect-for-peace'")

        assertEquals(0, prayerDao.countPrayers())
        assertNull(prayerDao.getPrayer("bcp-collect-for-peace"))
    }

    private suspend fun insertCollectForPeace() {
        prayerDao.insertCatalogue(
            prayers = listOf(prayerEntity(id = "bcp-collect-for-peace", breathingPauseAfterLine = 1)),
            lines = listOf(
                PrayerLineEntity("bcp-collect-for-peace", 2, "Amen."),
                PrayerLineEntity("bcp-collect-for-peace", 0, "O God, from whom all holy desires,"),
                PrayerLineEntity("bcp-collect-for-peace", 1, "all good counsels do proceed;"),
            ),
            themes = listOf(
                PrayerThemeEntity("bcp-collect-for-peace", PrayerTheme.Peace),
                PrayerThemeEntity("bcp-collect-for-peace", PrayerTheme.Protection),
            ),
        )
    }

    private fun prayerEntity(
        id: String,
        title: String = "A Collect for Peace",
        position: Int = 0,
        breathingPauseAfterLine: Int? = null,
    ) = PrayerEntity(
        id = id,
        title = title,
        author = "Book of Common Prayer, 1662",
        kind = PrayerKind.Evening,
        group = PrayerGroup.BookOfCommonPrayer,
        breathingPauseAfterLine = breathingPauseAfterLine,
        cataloguePosition = position,
    )
}
