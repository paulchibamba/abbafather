package io.abbafather.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.rules.ExternalResource

/** An in-memory copy of the real schema, opened fresh for each test and closed after it. */
class AbbaDatabaseTestRule : ExternalResource() {

    lateinit var database: AbbaDatabase
        private set

    override fun before() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AbbaDatabase::class.java,
        ).build()
    }

    override fun after() {
        database.close()
    }
}
