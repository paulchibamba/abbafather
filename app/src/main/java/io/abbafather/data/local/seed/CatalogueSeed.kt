package io.abbafather.data.local.seed

import io.abbafather.domain.model.PrayerPart
import io.abbafather.domain.model.PrayerTag
import io.abbafather.domain.model.PrayerVoice
import kotlinx.serialization.Serializable

/**
 * The shape of `assets/prayer_catalogue.json`, which `tools/build_catalogue.py` writes from
 * `docs/prayers/`. Enum members are matched by name and unknown keys are refused, so an asset the
 * app does not fully understand fails the parse rather than seeding a prayer with a piece missing.
 */
@Serializable
data class CatalogueSeed(
    val version: Int,
    val corpusHash: String,
    val prayers: List<SeedPrayer>,
)

@Serializable
data class SeedPrayer(
    val id: String,
    val title: String,
    val part: PrayerPart,
    val voice: PrayerVoice,
    val tags: List<PrayerTag> = emptyList(),
    val movements: List<SeedMovement>,
    val provenance: SeedProvenance,
)

@Serializable
data class SeedMovement(
    val heading: String,
    val lines: List<String>,
    val themes: List<String> = emptyList(),
    val scriptures: List<SeedScripture> = emptyList(),
)

@Serializable
data class SeedScripture(
    val reference: String,
    val translation: String,
    val connection: String,
)

@Serializable
data class SeedProvenance(
    val originalTitle: String,
    val originalAuthor: String,
    val originalSource: String,
    val originalPublicationDate: String,
    val copyrightStatus: String,
    val adaptationType: String,
    val adaptationNote: String,
)
