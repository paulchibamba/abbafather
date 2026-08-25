package io.abbafather.data.local

import io.abbafather.data.local.entity.SavedLineEntity
import io.abbafather.domain.model.PrayerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SavedLineDaoTest {

    @get:Rule val databaseRule = AbbaDatabaseTestRule()

    private val savedLineDao get() = databaseRule.database.savedLineDao()

    @Test
    fun `kept lines read newest first`() = runTest {
        savedLineDao.upsertSavedLine(savedLine(id = "older", createdAt = 1_000L))
        savedLineDao.upsertSavedLine(savedLine(id = "newer", createdAt = 2_000L))

        assertEquals(
            listOf("newer", "older"),
            savedLineDao.observeSavedLines().first().map { it.id },
        )
    }

    @Test
    fun `themes survive the round trip`() = runTest {
        savedLineDao.upsertSavedLine(
            savedLine(id = "kept", themes = setOf(PrayerTheme.Grief, PrayerTheme.Presence)),
        )

        assertEquals(
            setOf(PrayerTheme.Grief, PrayerTheme.Presence),
            savedLineDao.getSavedLine("kept")?.themes,
        )
    }

    @Test
    fun `deleting is soft - the row stays but stops being read`() = runTest {
        savedLineDao.upsertSavedLine(savedLine(id = "kept"))

        savedLineDao.markSavedLineDeleted("kept", deletedAt = 5_000L)

        assertTrue(savedLineDao.observeSavedLines().first().isEmpty())
        assertNull(savedLineDao.getSavedLine("kept"))
        assertEquals(1, countRows("saved_lines"))
    }

    @Test
    fun `a line knows whether it has already been kept`() = runTest {
        savedLineDao.upsertSavedLine(savedLine(id = "kept", sourceLineIndex = 5))

        assertTrue(savedLineDao.observeIsLineSaved("augustine-late-have-i-loved-you", 5).first())
        assertFalse(savedLineDao.observeIsLineSaved("augustine-late-have-i-loved-you", 4).first())
    }

    @Test
    fun `a deleted line no longer counts as kept`() = runTest {
        savedLineDao.upsertSavedLine(savedLine(id = "kept", sourceLineIndex = 5))

        savedLineDao.markSavedLineDeleted("kept", deletedAt = 5_000L)

        assertFalse(savedLineDao.observeIsLineSaved("augustine-late-have-i-loved-you", 5).first())
    }

    private fun countRows(table: String): Int =
        databaseRule.database.openHelper.writableDatabase
            .query("SELECT COUNT(*) FROM $table")
            .use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }

    private fun savedLine(
        id: String,
        createdAt: Long = 1_000L,
        sourceLineIndex: Int? = 5,
        themes: Set<PrayerTheme> = emptySet(),
    ) = SavedLineEntity(
        id = id,
        text = "You were with me, and I was not with you.",
        sourcePrayerId = "augustine-late-have-i-loved-you",
        sourcePrayerTitle = "Late Have I Loved You",
        sourceAttribution = "Augustine of Hippo, Confessions X",
        sourceLineIndex = sourceLineIndex,
        themes = themes,
        note = null,
        createdAt = createdAt,
        updatedAt = createdAt,
    )
}
