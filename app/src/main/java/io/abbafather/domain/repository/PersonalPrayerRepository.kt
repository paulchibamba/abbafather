package io.abbafather.domain.repository

import io.abbafather.domain.model.PersonalPrayer
import kotlinx.coroutines.flow.Flow

/** Prayers the reader wrote themselves. */
interface PersonalPrayerRepository {

    fun observePersonalPrayers(): Flow<List<PersonalPrayer>>

    fun observePersonalPrayer(personalPrayerId: String): Flow<PersonalPrayer?>

    suspend fun getPersonalPrayer(personalPrayerId: String): PersonalPrayer?

    suspend fun upsertPersonalPrayer(personalPrayer: PersonalPrayer)

    suspend fun deletePersonalPrayer(personalPrayerId: String, deletedAt: Long)
}
