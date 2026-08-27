package io.abbafather.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.abbafather.core.common.IoDispatcher
import io.abbafather.domain.model.FontLicence
import io.abbafather.domain.repository.LicenceRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the OFL notices out of the bundled assets. The files are the licences as they were shipped
 * with the fonts, so the copyright line is taken from the text itself rather than restated here —
 * nothing in this file can drift away from what the notice actually says.
 */
@Singleton
class AssetLicenceRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : LicenceRepository {

    override suspend fun getFontLicences(): List<FontLicence> = withContext(ioDispatcher) {
        BundledFonts.map { (fontName, assetPath) ->
            val text = context.assets.open(assetPath).bufferedReader().use { it.readText() }.trim()
            FontLicence(
                fontName = fontName,
                copyrightLine = text.lineSequence().first { it.isNotBlank() }.trim(),
                text = text,
            )
        }
    }

    private companion object {
        /** In the order they are read on screen: the prayer face first, then the functional one. */
        val BundledFonts = listOf(
            "Cormorant Garamond" to "licenses/cormorant_garamond_ofl.txt",
            "Work Sans" to "licenses/work_sans_ofl.txt",
        )
    }
}
