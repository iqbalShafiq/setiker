package data.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SyncCursorStore(
    private val dataStore: DataStore<Preferences>,
) {
    suspend fun getLastPullSyncAt(): Long? = dataStore.data
        .map { preferences -> preferences[KEY_LAST_PULL_SYNC_AT] }
        .first()

    suspend fun setLastPullSyncAt(timestampMillis: Long) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_PULL_SYNC_AT] = timestampMillis
        }
    }

    private companion object {
        val KEY_LAST_PULL_SYNC_AT = longPreferencesKey("last_pull_sync_at")
    }
}
