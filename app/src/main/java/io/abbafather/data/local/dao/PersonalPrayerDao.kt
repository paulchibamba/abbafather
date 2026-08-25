package io.abbafather.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.abbafather.data.local.entity.PersonalPrayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalPrayerDao {

    @Query("SELECT * FROM personal_prayers WHERE is_deleted = 0 ORDER BY updated_at DESC")
    fun observePersonalPrayers(): Flow<List<PersonalPrayerEntity>>

    @Query("SELECT * FROM personal_prayers WHERE id = :personalPrayerId AND is_deleted = 0")
    fun observePersonalPrayer(personalPrayerId: String): Flow<PersonalPrayerEntity?>

    @Query("SELECT * FROM personal_prayers WHERE id = :personalPrayerId AND is_deleted = 0")
    suspend fun getPersonalPrayer(personalPrayerId: String): PersonalPrayerEntity?

    @Upsert
    suspend fun upsertPersonalPrayer(personalPrayer: PersonalPrayerEntity)

    @Query(
        "UPDATE personal_prayers SET is_deleted = 1, updated_at = :deletedAt " +
            "WHERE id = :personalPrayerId",
    )
    suspend fun markPersonalPrayerDeleted(personalPrayerId: String, deletedAt: Long)
}
