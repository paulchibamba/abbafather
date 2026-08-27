package io.abbafather.data.local.seed

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.abbafather.data.local.AbbaDatabase
import io.abbafather.data.repository.OfflinePrayerRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The first run, assembled the way the app assembles it: a database with nothing in it, opened with
 * the real [CatalogueSeedingCallback] attached. Every other seeding test calls the seeder directly;
 * this one is here because a cold install is the one path where nobody is left to fix it by hand.
 */
@RunWith(RobolectricTestRunner::class)
class ColdInstallSeedingTest {

    private val testDispatcher = StandardTestDispatcher()
    private val applicationScope = TestScope(testDispatcher)

    private val database: AbbaDatabase by lazy {
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AbbaDatabase::class.java,
        )
            .addCallback(
                CatalogueSeedingCallback(
                    applicationScope = applicationScope,
                    seedCatalogueIfEmpty = { seeder.seedCatalogueIfEmpty() },
                ),
            )
            .build()
    }

    private val seeder by lazy {
        CatalogueSeeder(
            context = ApplicationProvider.getApplicationContext(),
            prayerDao = database.prayerDao(),
            ioDispatcher = testDispatcher,
        )
    }

    private val prayerRepository by lazy {
        OfflinePrayerRepository(
            prayerDao = database.prayerDao(),
            collectionDao = database.prayerCollectionDao(),
            ioDispatcher = testDispatcher,
        )
    }

    @After
    fun closeDatabase() {
        if (database.isOpen) database.close()
    }

    @Test
    fun `a database with nothing in it comes up seeded`() = runTest(testDispatcher) {
        // Asking for prayers is what the first screen does, and what opens the database: nothing is
        // there yet, and the callback the app installs is what fills it.
        assertEquals(0, database.prayerDao().countPrayers())
        testDispatcher.scheduler.advanceUntilIdle()

        // The seed lands as a change the catalogue's own flow reports, which is how the Library and
        // the Home screen come to have prayers on them without being told to look again.
        val prayers = prayerRepository.observePrayers().first { it.isNotEmpty() }

        assertEquals(186, prayers.size)
        assertTrue(prayers.all { it.lines.isNotEmpty() })
    }
}
