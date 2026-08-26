package io.abbafather.data.local.seed

import androidx.test.core.app.ApplicationProvider
import io.abbafather.data.local.AbbaDatabaseTestRule
import io.abbafather.data.repository.OfflinePrayerRepository
import io.abbafather.domain.model.Prayer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

        assertEquals("the corpus ships every prayer not held back for revision", 186, prayers.size)
        prayers.forEach { prayer ->
            assertTrue("${prayer.id} has no movements", prayer.movements.isNotEmpty())
            assertTrue("${prayer.id} has no lines", prayer.lines.isNotEmpty())
            assertTrue("${prayer.id} has a blank line", prayer.lines.none(String::isBlank))
            assertPausesAreWithinPrayer(prayer)
        }
    }

    /**
     * Everything the Reader and the session read off a movement, on every prayer: a heading to name
     * it, a passage behind it, and a first line index that actually addresses this prayer's lines.
     */
    @Test
    fun `every movement is whole`() = runTest(testDispatcher) {
        seeder.seedCatalogueIfEmpty()

        prayerRepository.observePrayers().first().forEach { prayer ->
            prayer.movements.forEach { movement ->
                val where = "${prayer.id} movement ${movement.index}"
                assertTrue("$where has no heading", movement.heading.isNotBlank())
                assertTrue("$where has no lines", movement.lines.isNotEmpty())
                assertTrue("$where rests on no passage", movement.scriptures.isNotEmpty())
                assertTrue(
                    "$where starts outside the prayer's lines",
                    movement.firstLineIndex in prayer.lines.indices,
                )
                assertEquals(
                    "$where does not line up with the flat lines",
                    movement.lines,
                    prayer.lines.slice(movement.lineIndices),
                )
            }
        }
    }

    /** Scripture is carried as a reference, never as the verse text. */
    @Test
    fun `every passage is a reference in a named translation`() = runTest(testDispatcher) {
        seeder.seedCatalogueIfEmpty()

        val scriptures = prayerRepository.observePrayers().first()
            .flatMap { prayer -> prayer.movements.flatMap { it.scriptures } }

        assertTrue("expected the corpus's passages, got ${scriptures.size}", scriptures.size > 2_000)
        scriptures.forEach { scripture ->
            assertTrue("a passage has no reference", scripture.reference.isNotBlank())
            assertTrue("${scripture.reference} has no translation", scripture.translation.isNotBlank())
        }
    }

    @Test
    fun `every prayer says where it came from`() = runTest(testDispatcher) {
        seeder.seedCatalogueIfEmpty()

        prayerRepository.observePrayers().first().forEach { prayer ->
            val provenance = prayer.provenance
            assertTrue("${prayer.id} has no original title", provenance.originalTitle.isNotBlank())
            assertTrue("${prayer.id} has no source", provenance.originalSource.isNotBlank())
            assertTrue("${prayer.id} has no copyright line", provenance.copyrightStatus.isNotBlank())
            assertTrue("${prayer.id} does not say it is adapted", provenance.adaptationNote.isNotBlank())
            assertEquals("The Valley of Vision, adapted", prayer.attribution)
        }
    }

    /** The reviewer's verdict is a publishing decision, and it is the asset that carries it out. */
    @Test
    fun `no prayer held back for revision is in the catalogue`() = runTest(testDispatcher) {
        seeder.seedCatalogueIfEmpty()

        val ids = prayerRepository.observePrayers().first().map { it.id }.toSet()

        listOf(
            "vov-001-the-valley-of-vision",
            "vov-002-the-trinity",
            "vov-009-divine-mercies",
        ).forEach { heldBack ->
            assertTrue("$heldBack was held back but is in the catalogue", heldBack !in ids)
        }
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

    private fun assertPausesAreWithinPrayer(prayer: Prayer) {
        assertEquals(
            "${prayer.id} should rest once at the end of every movement but the last",
            prayer.movements.size - 1,
            prayer.breathingPauseLineIndices.size,
        )
        prayer.breathingPauseLineIndices.forEach { pause ->
            assertTrue(
                "${prayer.id} rests after line $pause but has only ${prayer.lines.size} lines",
                pause in prayer.lines.indices,
            )
        }
    }
}
