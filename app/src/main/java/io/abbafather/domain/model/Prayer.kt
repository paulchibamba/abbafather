package io.abbafather.domain.model

/**
 * A prayer from the catalogue. Read-only to the reader: prayers they write themselves are
 * [PersonalPrayer]s.
 *
 * A prayer is a sequence of [movements] — the turns the praying takes, each with its own heading,
 * the scripture it leans on and the lines it is prayed in. [lines] flattens those into the order
 * they are prayed, and a line is addressed by its index in that flat list everywhere in the app: it
 * is what a session advances through and what a kept line points back to.
 */
data class Prayer(
    val id: String,
    val title: String,
    val part: PrayerPart,
    val voice: PrayerVoice,
    val tags: Set<PrayerTag>,
    val movements: List<PrayerMovement>,
    val provenance: PrayerProvenance,
    val lastOpenedAt: Long? = null,
) {
    val lines: List<String> = movements.flatMap(PrayerMovement::lines)

    val openingLine: String get() = lines.first()

    val attribution: String get() = provenance.shortAttribution

    /**
     * Where the prayer rests. Every movement but the last ends in a pause, because a movement is a
     * complete turn of the praying and the next one begins something new.
     */
    val breathingPauseLineIndices: Set<Int> =
        movements.dropLast(1).mapTo(mutableSetOf()) { it.firstLineIndex + it.lines.lastIndex }

    fun hasBreathingPauseAfter(lineIndex: Int): Boolean = lineIndex in breathingPauseLineIndices

    /**
     * Every line of the prayer with a rest between each movement and the next — the whole of what a
     * session moves through, in the order it is prayed. Held rather than derived on demand because
     * the session screen draws all of it at once.
     */
    val sessionSteps: List<SessionStep> = buildList {
        movements.forEach { movement ->
            movement.lines.indices.forEach { position ->
                add(SessionStep.Line(movement.firstLineIndex + position))
            }
            val nextMovement = movements.getOrNull(movement.index + 1)
            if (nextMovement != null) add(SessionStep.Pause(nextMovement.index))
        }
    }

    /** The movement a line belongs to — what the session names at a pause and the reader can read about. */
    fun movementOfLine(lineIndex: Int): PrayerMovement =
        movements.last { lineIndex >= it.firstLineIndex }
}

/**
 * One turn of a prayer: a heading naming what is being prayed, the lines it is prayed in, what it
 * holds theologically, and the passages it rests on.
 *
 * [firstLineIndex] is this movement's place in the prayer's flat line list, so a line index can be
 * traced back to the movement it came from without searching.
 */
data class PrayerMovement(
    val index: Int,
    val heading: String,
    val lines: List<String>,
    val firstLineIndex: Int,
    val themes: List<String> = emptyList(),
    val scriptures: List<ScriptureReference> = emptyList(),
) {
    val lineIndices: IntRange get() = firstLineIndex until firstLineIndex + lines.size
}

/**
 * A passage a movement leans on. Only the reference and the translation it was read in are carried —
 * never the verse text itself, which stays in the reader's own Bible. [connection] is ours: why this
 * passage stands under this part of the prayer.
 */
data class ScriptureReference(
    val reference: String,
    val translation: String,
    val connection: String,
)

/**
 * Where a prayer came from before it was ours. These prayers are modern adaptations, not the
 * historical wording, and the app says so wherever a reader asks.
 */
data class PrayerProvenance(
    val originalTitle: String,
    val originalAuthor: String,
    val originalSource: String,
    val originalPublicationDate: String,
    val copyrightStatus: String,
    val adaptationType: String,
    val adaptationNote: String,
) {
    /**
     * The byline under a title: short enough for a card, honest about being an adaptation. The
     * collection's name is the part of [originalSource] before its subtitle, so a second source
     * added later reads correctly without a change here.
     */
    val shortAttribution: String
        get() = "${originalSource.substringBefore(':').substringBefore(',').trim()}, adapted"
}
