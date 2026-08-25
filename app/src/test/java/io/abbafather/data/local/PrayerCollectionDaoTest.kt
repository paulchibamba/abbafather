package io.abbafather.data.local

import io.abbafather.data.local.entity.PrayerCollectionEntity
import io.abbafather.data.local.entity.PrayerEntity
import io.abbafather.data.mapper.toDomain
import io.abbafather.domain.model.PrayerGroup
import io.abbafather.domain.model.PrayerKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PrayerCollectionDaoTest {

    @get:Rule val databaseRule = AbbaDatabaseTestRule()

    private val collectionDao get() = databaseRule.database.prayerCollectionDao()

    @Before
    fun insertCataloguePrayers() = runTest {
        databaseRule.database.prayerDao().insertPrayers(
            listOf(prayerEntity("psalm-063", 0), prayerEntity("psalm-121", 1)),
        )
    }

    @Test
    fun `membership reads back in the order the reader put it in`() = runTest {
        collectionDao.upsertCollection(collectionEntity())

        collectionDao.addMember("morning", "psalm-121", updatedAt = 2_000L)
        collectionDao.addMember("morning", "psalm-063", updatedAt = 3_000L)

        val collection = collectionDao.observeCollection("morning").first()!!.toDomain()

        assertEquals(listOf("psalm-121", "psalm-063"), collection.memberPrayerIds)
        assertEquals(2, collection.prayerCount)
        assertEquals(3_000L, collection.updatedAt)
    }

    @Test
    fun `removing a prayer leaves the rest of the collection intact`() = runTest {
        collectionDao.upsertCollection(collectionEntity())
        collectionDao.addMember("morning", "psalm-121", updatedAt = 2_000L)
        collectionDao.addMember("morning", "psalm-063", updatedAt = 3_000L)

        collectionDao.removeMember("morning", "psalm-121", updatedAt = 4_000L)

        val collection = collectionDao.observeCollection("morning").first()!!.toDomain()

        assertEquals(listOf("psalm-063"), collection.memberPrayerIds)
    }

    @Test
    fun `a deleted collection stops being read`() = runTest {
        collectionDao.upsertCollection(collectionEntity())

        collectionDao.markCollectionDeleted("morning", deletedAt = 9_000L)

        assertEquals(emptyList<Any>(), collectionDao.observeCollections().first())
    }

    private fun collectionEntity() = PrayerCollectionEntity(
        id = "morning",
        name = "Morning prayers",
        createdAt = 1_000L,
        updatedAt = 1_000L,
    )

    private fun prayerEntity(id: String, position: Int) = PrayerEntity(
        id = id,
        title = id,
        author = null,
        kind = PrayerKind.Psalm,
        group = PrayerGroup.Psalter,
        breathingPauseAfterLine = null,
        cataloguePosition = position,
    )
}
