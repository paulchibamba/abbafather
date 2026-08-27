package io.abbafather.data.repository

import androidx.test.core.app.ApplicationProvider
import io.abbafather.domain.model.FontLicence
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The licences are an obligation rather than a feature, so this reads the real bundled assets: a
 * font added without its notice, or a notice that stopped being the OFL, fails here.
 */
@RunWith(RobolectricTestRunner::class)
class AssetLicenceRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    private val repository = AssetLicenceRepository(
        context = ApplicationProvider.getApplicationContext(),
        ioDispatcher = testDispatcher,
    )

    @Test
    fun `both bundled faces carry their notice whole`() = runTest(testDispatcher) {
        val licences = repository.getFontLicences()

        assertEquals(
            listOf("Cormorant Garamond", "Work Sans"),
            licences.map(FontLicence::fontName),
        )
        licences.forEach { licence ->
            assertTrue(
                "${licence.fontName} does not carry the OFL",
                licence.text.contains("SIL OPEN FONT LICENSE Version 1.1"),
            )
            assertTrue(
                "${licence.fontName} does not carry the licence conditions",
                licence.text.contains("PERMISSION & CONDITIONS"),
            )
        }
    }

    @Test
    fun `the copyright line is taken from the notice itself`() = runTest(testDispatcher) {
        val licences = repository.getFontLicences().associateBy(FontLicence::fontName)

        assertTrue(
            licences.getValue("Cormorant Garamond").copyrightLine.startsWith("Copyright 2015"),
        )
        assertTrue(
            licences.getValue("Work Sans").copyrightLine.startsWith("Copyright 2019"),
        )
        licences.values.forEach { licence ->
            assertTrue(licence.text.startsWith(licence.copyrightLine))
        }
    }
}
