package io.abbafather.domain.repository

import io.abbafather.domain.model.FontLicence

/** The licences the app is obliged to carry. Read once and never written. */
interface LicenceRepository {

    suspend fun getFontLicences(): List<FontLicence>
}
