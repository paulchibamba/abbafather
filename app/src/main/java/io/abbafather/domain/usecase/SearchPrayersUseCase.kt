package io.abbafather.domain.usecase

import io.abbafather.domain.model.Prayer
import io.abbafather.domain.repository.PrayerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Free-text search over the catalogue. A blank query is not an empty result — it is the whole
 * catalogue, which is what the Library shows before anyone types.
 */
class SearchPrayersUseCase @Inject constructor(
    private val prayerRepository: PrayerRepository,
) {
    operator fun invoke(query: String): Flow<List<Prayer>> =
        prayerRepository.observePrayers().map { prayers ->
            val terms = query.trim()
            if (terms.isEmpty()) prayers else prayers.filter { it.matches(terms) }
        }

    private fun Prayer.matches(query: String): Boolean =
        title.contains(query, ignoreCase = true) ||
            author?.contains(query, ignoreCase = true) == true ||
            group.displayName.contains(query, ignoreCase = true) ||
            themes.any { it.displayName.contains(query, ignoreCase = true) } ||
            lines.any { it.contains(query, ignoreCase = true) }
}
