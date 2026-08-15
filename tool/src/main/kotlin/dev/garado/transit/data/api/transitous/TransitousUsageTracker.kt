package dev.garado.transit.data.api.transitous

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Tracks how many real Transitous API calls have been made in the current calendar month.
 */
class TransitousUsageTracker(private val dataStore: DataStore<Preferences>) {

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
        const val MONTHLY_CALL_LIMIT = 10_000

        private val COUNT_KEY = intPreferencesKey("transitous_api_call_count")
        private val MONTH_KEY = stringPreferencesKey("transitous_api_call_count_month")
    }
}
