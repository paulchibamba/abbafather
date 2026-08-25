package io.abbafather.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * v1 → v2: the catalogue becomes the Valley of Vision adaptations.
 *
 * A catalogue prayer is derived content — it is rebuilt from `docs/prayers/` on every release and
 * every id in it changed here — so its tables are dropped and recreated rather than migrated row by
 * row, and `CatalogueSeeder` fills them again on the next open. Nothing the reader owns is touched:
 * `saved_lines`, `personal_prayers` and `prayer_collections` keep every row, which is exactly why a
 * kept line stores its own copy of the text and holds no foreign key back to the prayer.
 *
 * What the reader does lose is collection *membership*, since `collection_members` has a cascading
 * foreign key on a prayer id that no longer exists. Nothing in the app creates a collection yet, so
 * there is nothing there to lose today; when collections become real, this is the case to think
 * about again.
 *
 * The tag columns on `saved_lines` and `personal_prayers` are renamed, not rewritten. Names that
 * were valid under the old vocabulary and are not under the new one stay in the column and are
 * dropped on read by the lenient converter — a kept line keeps its text, its source and its note
 * whatever happens to its tags.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {

    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS prayer_themes")
        connection.execSQL("DROP TABLE IF EXISTS prayer_lines")
        connection.execSQL("DROP TABLE IF EXISTS prayers")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `prayers` (
                `id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `part` TEXT NOT NULL,
                `voice` TEXT NOT NULL,
                `catalogue_position` INTEGER NOT NULL,
                `original_title` TEXT NOT NULL,
                `original_author` TEXT NOT NULL,
                `original_source` TEXT NOT NULL,
                `original_publication_date` TEXT NOT NULL,
                `copyright_status` TEXT NOT NULL,
                `adaptation_type` TEXT NOT NULL,
                `adaptation_note` TEXT NOT NULL,
                `last_opened_at` INTEGER,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `prayer_movements` (
                `prayer_id` TEXT NOT NULL,
                `movement_index` INTEGER NOT NULL,
                `heading` TEXT NOT NULL,
                PRIMARY KEY(`prayer_id`, `movement_index`),
                FOREIGN KEY(`prayer_id`) REFERENCES `prayers`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `prayer_lines` (
                `prayer_id` TEXT NOT NULL,
                `line_index` INTEGER NOT NULL,
                `movement_index` INTEGER NOT NULL,
                `text` TEXT NOT NULL,
                PRIMARY KEY(`prayer_id`, `line_index`),
                FOREIGN KEY(`prayer_id`) REFERENCES `prayers`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `prayer_movement_themes` (
                `prayer_id` TEXT NOT NULL,
                `movement_index` INTEGER NOT NULL,
                `position` INTEGER NOT NULL,
                `text` TEXT NOT NULL,
                PRIMARY KEY(`prayer_id`, `movement_index`, `position`),
                FOREIGN KEY(`prayer_id`) REFERENCES `prayers`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `prayer_scriptures` (
                `prayer_id` TEXT NOT NULL,
                `movement_index` INTEGER NOT NULL,
                `position` INTEGER NOT NULL,
                `reference` TEXT NOT NULL,
                `translation` TEXT NOT NULL,
                `connection` TEXT NOT NULL,
                PRIMARY KEY(`prayer_id`, `movement_index`, `position`),
                FOREIGN KEY(`prayer_id`) REFERENCES `prayers`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `prayer_tags` (
                `prayer_id` TEXT NOT NULL,
                `tag` TEXT NOT NULL,
                PRIMARY KEY(`prayer_id`, `tag`),
                FOREIGN KEY(`prayer_id`) REFERENCES `prayers`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_prayer_tags_tag` ON `prayer_tags` (`tag`)",
        )

        connection.execSQL("ALTER TABLE `saved_lines` RENAME COLUMN `themes` TO `tags`")
        connection.execSQL("ALTER TABLE `personal_prayers` RENAME COLUMN `themes` TO `tags`")
    }
}

/** Every migration this database knows, in the order Room should consider them. */
val AbbaMigrations = arrayOf(MIGRATION_1_2)
