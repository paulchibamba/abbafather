package io.abbafather.domain.model

/**
 * One bundled typeface and the licence it travels under. The [text] is the licence verbatim — an
 * OFL notice has to be reproduced, not summarised, so the app carries the file rather than a
 * paraphrase of it.
 */
data class FontLicence(
    val fontName: String,
    val copyrightLine: String,
    val text: String,
)
