package com.lazyshopper.app.feature.customer.home

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.customerLocationDataStore by preferencesDataStore(name = "ls_customer_location")

data class LocationSelection(val state: String = "", val district: String = "", val area: String = "") {
    val isSet: Boolean get() = state.isNotBlank()
    val label: String get() = listOf(area, district, state).filter { it.isNotBlank() }.joinToString(", ")
}

/**
 * Client-side cache of the customer's chosen delivery location (state/district/area), used to
 * filter /home, /shops, /products/search results. Mirrors (and pushes to) the server-side
 * `location_pref` on the user profile via `PUT /api/me/location`, but is kept locally too so the
 * storefront doesn't need a network round-trip just to know which location is selected.
 */
@Singleton
class LocationPrefStore @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val STATE = stringPreferencesKey("state")
        val DISTRICT = stringPreferencesKey("district")
        val AREA = stringPreferencesKey("area")
    }

    val flow: Flow<LocationSelection> = context.customerLocationDataStore.data.map { prefs ->
        LocationSelection(
            state = prefs[Keys.STATE].orEmpty(),
            district = prefs[Keys.DISTRICT].orEmpty(),
            area = prefs[Keys.AREA].orEmpty(),
        )
    }

    suspend fun save(state: String, district: String, area: String) {
        context.customerLocationDataStore.edit { prefs ->
            prefs[Keys.STATE] = state
            prefs[Keys.DISTRICT] = district
            prefs[Keys.AREA] = area
        }
    }
}
