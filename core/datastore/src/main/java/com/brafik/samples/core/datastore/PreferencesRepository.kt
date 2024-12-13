package com.brafik.samples.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    val isOnboarded: Flow<Boolean> = dataStore.data
        .map { it[PreferencesKeys.ONBOARDED] ?: false }

    suspend fun setOnboarded(isOnboarded: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDED] = isOnboarded
        }
    }
}