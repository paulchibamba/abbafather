package io.abbafather.data.local.seed

import io.abbafather.domain.model.PrayerGroup
import io.abbafather.domain.model.PrayerKind
import io.abbafather.domain.model.PrayerTheme
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The shape of `assets/prayer_catalogue.json`. Enum members are matched by name, so a typo in the
 * asset fails the parse rather than seeding a prayer with a silently wrong group.
 */
@Serializable
data class CatalogueSeed(
    val version: Int,
    val prayers: List<SeedPrayer>,
)

@Serializable
data class SeedPrayer(
    val id: String,
    val title: String,
    val author: String? = null,
    val kind: PrayerKind,
    @SerialName("group") val group: PrayerGroup,
    val themes: List<PrayerTheme> = emptyList(),
    val breathingPauseAfterLine: Int? = null,
    val lines: List<String>,
)
