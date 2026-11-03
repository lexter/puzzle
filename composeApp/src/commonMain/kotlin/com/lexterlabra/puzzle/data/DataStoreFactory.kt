package com.lexterlabra.puzzle.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

const val DATA_STORE_FILE = "puzzle.preferences_pb"

/** Absolute path of [fileName] in the app's private storage. */
expect fun dataStorePath(fileName: String): String

fun createDataStore(path: String = dataStorePath(DATA_STORE_FILE)): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { path.toPath() })
