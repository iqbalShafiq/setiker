package data.util

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

fun createIOSDataStore(): DataStore<Preferences> {
    val documentDirectory = NSFileManager.defaultManager.URLsForDirectory(
        NSDocumentDirectory,
        NSUserDomainMask
    ).firstOrNull()?.path ?: ""
    
    return PreferenceDataStoreFactory.createWithPath {
        "$documentDirectory/setiker_preferences.preferences_pb".toPath()
    }
}