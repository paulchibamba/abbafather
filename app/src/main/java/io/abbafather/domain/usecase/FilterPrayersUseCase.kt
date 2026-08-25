package io.abbafather.domain.usecase

import io.abbafather.domain.model.Prayer
import io.abbafather.domain.model.PrayerFilter
import io.abbafather.domain.repository.PrayerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Narrows the catalogue by the Library's chips. Groups, kinds and themes narrow together. */
class FilterPrayersUseCase @Inject constructor(
    private val prayerRepository: PrayerRepository,
) {
    operator fun invoke(filter: PrayerFilter): Flow<List<Prayer>> =
        prayerRepository.observePrayers().map { prayers ->
            if (filter.isEmpty) prayers else prayers.filter(filter::matches)
        }
}
