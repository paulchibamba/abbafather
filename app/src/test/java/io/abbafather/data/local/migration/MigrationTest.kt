package io.abbafather.data.local.migration

import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import io.abbafather.data.local.AbbaDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The promise this database makes: a version bump never costs the reader what they wrote.
 *
 * v1 → v2 replaced the whole catalogue, so it is the first migration that could have broken that
 * promise, and the first that proves it does not.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation(),
        databaseClass = AbbaDatabase::class.java,
    )

    @Test
    fun `migrating to 2 keeps the reader's kept line and empties the catalogue`() {
        helper.createDatabase(TestDatabaseName, version = 1).use { database ->
            database.execSQL(
                """
                INSERT INTO prayers
                    (id, title, author, kind, prayer_group, breathing_pause_after_line,
                     catalogue_position, last_opened_at)
                VALUES ('bcp-collect-for-peace', 'A Collect for Peace', 'BCP 1662', 'Evening',
                        'BookOfCommonPrayer', 1, 0, 1000)
                """.trimIndent(),
            )
            database.execSQL(
                "INSERT INTO prayer_lines (prayer_id, line_index, text) " +
                    "VALUES ('bcp-collect-for-peace', 0, 'O God, from whom all holy desires')",
            )
            database.execSQL(
                """
                INSERT INTO saved_lines
                    (id, text, source_prayer_id, source_prayer_title, source_attribution,
                     source_line_index, themes, note, created_at, updated_at, is_deleted)
                VALUES ('kept-1', 'O God, from whom all holy desires', 'bcp-collect-for-peace',
                        'A Collect for Peace', 'Book of Common Prayer, 1662', 0,
                        'Peace,Protection', 'the one I keep coming back to', 5000, 5000, 0)
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO personal_prayers
                    (id, title, body, themes, seeded_from_saved_line_id, created_at, updated_at,
                     is_deleted)
                VALUES ('mine-1', 'After A Collect for Peace', 'Lord, quiet me.', 'Peace', 'kept-1',
                        6000, 6000, 0)
                """.trimIndent(),
            )
        }

        val database = helper.runMigrationsAndValidate(TestDatabaseName, 2, true, MIGRATION_1_2)

        database.query("SELECT text, note, tags FROM saved_lines WHERE id = 'kept-1'").use { cursor ->
            assertTrue("the kept line did not survive the migration", cursor.moveToFirst())
            assertEquals("O God, from whom all holy desires", cursor.getString(0))
            assertEquals("the one I keep coming back to", cursor.getString(1))
            // The column is renamed, not rewritten: names outside the new vocabulary are dropped on
            // read rather than costing the reader the line.
            assertEquals("Peace,Protection", cursor.getString(2))
        }

        database.query("SELECT title, tags FROM personal_prayers WHERE id = 'mine-1'").use { cursor ->
            assertTrue("the written prayer did not survive the migration", cursor.moveToFirst())
            assertEquals("After A Collect for Peace", cursor.getString(0))
        }

        // The catalogue is rebuilt from the asset, so the migration leaves it empty on purpose and
        // CatalogueSeeder fills it on the next open.
        listOf("prayers", "prayer_lines", "prayer_movements", "prayer_scriptures", "prayer_tags")
            .forEach { table ->
                database.query("SELECT COUNT(*) FROM $table").use { cursor ->
                    cursor.moveToFirst()
                    assertEquals("$table should be empty after the migration", 0, cursor.getInt(0))
                }
            }
    }

    private companion object {
        const val TestDatabaseName = "migration-test.db"
    }
}
