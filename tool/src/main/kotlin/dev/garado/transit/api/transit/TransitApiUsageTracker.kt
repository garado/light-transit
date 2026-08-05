package dev.garado.transit.api.transit

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth

/**
 * Tracks how many real Transit API calls have been made in the current calendar month
 * Developer free tier has 1500/month limit
 */
class TransitApiUsageTracker(private val dataStore: DataStore<Preferences>) {

    val callCountThisMonth: Flow<Int> = dataStore.data.map { prefs ->
        if (prefs[MONTH_KEY] == currentMonth()) prefs[COUNT_KEY] ?: 0 else 0
    }

    suspend fun recordApiCall() {
        val month = currentMonth()
        dataStore.edit { prefs ->
            val countForCurrentMonth = if (prefs[MONTH_KEY] == month) prefs[COUNT_KEY] ?: 0 else 0
            prefs[MONTH_KEY] = month
            prefs[COUNT_KEY] = countForCurrentMonth + 1
        }
    }

    private fun currentMonth(): String = YearMonth.now().toString()

    companion object {
        const val MONTHLY_CALL_LIMIT = 1500

        private val COUNT_KEY = intPreferencesKey("transit_api_call_count")
        private val MONTH_KEY = stringPreferencesKey("transit_api_call_count_month")
    }
}
