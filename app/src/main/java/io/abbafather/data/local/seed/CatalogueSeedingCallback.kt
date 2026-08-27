package io.abbafather.data.local.seed

import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * What makes a cold install come up with prayers in it.
 *
 * The catalogue fills itself whenever it is empty, which is true on a fresh install and again after
 * a migration that replaced it — so the seed is offered from `onOpen` as well as `onCreate`, and
 * [CatalogueSeeder] decides whether there is anything to do. It is launched rather than awaited:
 * both callbacks run on the thread that is opening the database, and the seed's own transaction
 * cannot be taken out from inside them.
 */
class CatalogueSeedingCallback(
    private val applicationScope: CoroutineScope,
    private val seedCatalogueIfEmpty: suspend () -> Unit,
) : RoomDatabase.Callback() {

    override fun onCreate(connection: SQLiteConnection) {
        super.onCreate(connection)
        seed()
    }

    override fun onOpen(connection: SQLiteConnection) {
        super.onOpen(connection)
        seed()
    }

    private fun seed() {
        applicationScope.launch { seedCatalogueIfEmpty() }
    }
}
