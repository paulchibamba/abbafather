package io.abbafather.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import io.abbafather.data.local.entity.PrayerEntity
import io.abbafather.data.local.entity.PrayerLineEntity
import io.abbafather.data.local.entity.PrayerMovementEntity
import io.abbafather.data.local.entity.PrayerMovementThemeEntity
import io.abbafather.data.local.entity.PrayerScriptureEntity
import io.abbafather.data.local.entity.PrayerTagEntity
import io.abbafather.data.local.entity.PrayerWithDetail
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerDao {

    @Transaction
    @Query("SELECT * FROM prayers ORDER BY catalogue_position")
    fun observePrayers(): Flow<List<PrayerWithDetail>>

    @Transaction
    @Query("SELECT * FROM prayers WHERE id = :prayerId")
    fun observePrayer(prayerId: String): Flow<PrayerWithDetail?>

    @Transaction
    @Query(
        "SELECT * FROM prayers WHERE last_opened_at IS NOT NULL " +
            "ORDER BY last_opened_at DESC LIMIT :limit",
    )
    fun observeRecentlyOpenedPrayers(limit: Int): Flow<List<PrayerWithDetail>>

    @Transaction
    @Query("SELECT * FROM prayers WHERE id = :prayerId")
    suspend fun getPrayer(prayerId: String): PrayerWithDetail?

    @Query("UPDATE prayers SET last_opened_at = :openedAt WHERE id = :prayerId")
    suspend fun updateLastOpenedAt(prayerId: String, openedAt: Long)

    @Query("SELECT COUNT(*) FROM prayers")
    suspend fun countPrayers(): Int

    /**
     * Seeding, as one unit: a half-inserted catalogue would look seeded to [countPrayers] and could
     * never repair itself. `abort` on conflict rather than `replace` so a seed that would tread on
     * existing rows fails loudly instead of overwriting them.
     */
    @Transaction
    suspend fun insertCatalogue(catalogue: CatalogueRows) {
        insertPrayers(catalogue.prayers)
        insertPrayerMovements(catalogue.movements)
        insertPrayerLines(catalogue.lines)
        insertPrayerMovementThemes(catalogue.movementThemes)
        insertPrayerScriptures(catalogue.scriptures)
        insertPrayerTags(catalogue.tags)
    }

    @Insert
    suspend fun insertPrayers(prayers: List<PrayerEntity>)

    @Insert
    suspend fun insertPrayerMovements(movements: List<PrayerMovementEntity>)

    @Insert
    suspend fun insertPrayerLines(lines: List<PrayerLineEntity>)

    @Insert
    suspend fun insertPrayerMovementThemes(themes: List<PrayerMovementThemeEntity>)

    @Insert
    suspend fun insertPrayerScriptures(scriptures: List<PrayerScriptureEntity>)

    @Insert
    suspend fun insertPrayerTags(tags: List<PrayerTagEntity>)
}

/**
 * A whole catalogue, flattened into the rows six tables need. One value rather than six parameters,
 * so a caller cannot pass the lines of one catalogue with the movements of another.
 */
data class CatalogueRows(
    val prayers: List<PrayerEntity>,
    val movements: List<PrayerMovementEntity>,
    val lines: List<PrayerLineEntity>,
    val movementThemes: List<PrayerMovementThemeEntity>,
    val scriptures: List<PrayerScriptureEntity>,
    val tags: List<PrayerTagEntity>,
)
