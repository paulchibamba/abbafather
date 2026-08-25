package io.abbafather.data.local.seed

import androidx.test.core.app.ApplicationProvider
import io.abbafather.data.local.AbbaDatabaseTestRule
import io.abbafather.data.repository.OfflinePrayerRepository
import io.abbafather.domain.model.Prayer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The seed is the app's whole content on first run, so these tests cover both halves of the promise:
 * the bundled asset parses into the schema, and seeding a database that already holds prayers is a
 * no-op rather than a second copy of the catalogue.
 */
@RunWith(RobolectricTestRunner::class)
class CatalogueSeederTest {

    @get:Rule val databaseRule = AbbaDatabaseTestRule()

    private val testDispatcher = StandardTestDispatcher()

    private val seeder by lazy {
        CatalogueSeeder(
            context = ApplicationProvider.getApplicationContext(),
            prayerDao = databaseRule.database.prayerDao(),
            ioDispatcher = testDispatcher,
        )
    }

    private val prayerRepository by lazy {
        OfflinePrayerRepository(
            prayerDao = databaseRule.database.prayerDao(),
            collectionDao = databaseRule.database.prayerCollectionDao(),
            ioDispatcher = testDispatcher,
        )
    }

    @Test
    fun `the bundled catalogue seeds and reads back through the repository`() = runTest(testDispatcher) {
        seeder.seedCatalogueIfEmpty()

        val prayers = prayerRepository.observePrayers().first()

        assertTrue("expected a substantial catalogue, got ${prayers.size}", prayers.size >= 25)
        prayers.forEach { prayer ->
            assertTrue("${prayer.id} has no lines", prayer.lines.isNotEmpty())
            assertTrue("${prayer.id} has a blank line", prayer.lines.none(String::isBlank))
            assertPauseIsWithinPrayer(prayer)
        }
    }

    @Test
    fun `the seven prayers the design names are all there, whole`() = runTest(testDispatcher) {
        seeder.seedCatalogueIfEmpty()

        val byTitle = prayerRepository.observePrayers().first().associateBy { it.title }

        listOf(
            "A Collect for Peace",
            "Aid Against All Perils",
            "A General Thanksgiving",
            "Psalm 63",
            "Late Have I Loved You",
            "A Prayer in Distress",
            "Prayer of Saint Chrysostom",
        ).forEach { title ->
            val prayer = byTitle[title]
            assertNotNull("the catalogue is missing $title", prayer)
            assertTrue("$title has too few lines to pray", prayer!!.lines.size >= 3)
        }

        assertEquals(
            "Lighten our darkness, we beseech thee, O Lord;",
            byTitle.getValue("Aid Against All Perils").openingLine,
        )
    }

    @Test
    fun `seeding twice leaves one catalogue, not two`() = runTest(testDispatcher) {
        seeder.seedCatalogueIfEmpty()
        val afterFirstSeed = prayerRepository.observePrayers().first()

        seeder.seedCatalogueIfEmpty()

        assertEquals(afterFirstSeed, prayerRepository.observePrayers().first())
    }

    @Test
    fun `every prayer keeps its own identity`() = runTest(testDispatcher) {
        seeder.seedCatalogueIfEmpty()

        val prayers = prayerRepository.observePrayers().first()

        assertEquals(prayers.size, prayers.map { it.id }.toSet().size)
        assertEquals(prayers.size, prayers.map { it.title }.toSet().size)
    }

    private fun assertPauseIsWithinPrayer(prayer: Prayer) {
        val pause = prayer.breathingPauseAfterLine ?: return
        assertTrue(
            "${prayer.id} rests after line $pause but has only ${prayer.lines.size} lines",
            pause in prayer.lines.indices,
        )
    }
}
