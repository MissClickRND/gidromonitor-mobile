package bob.colbaskin.gidromonitor.features.onboarding.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import bob.colbaskin.gidromonitor.features.onboarding.domain.OnboardingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding")
private val onboardingCompletedKey = booleanPreferencesKey("completed")

class OnboardingRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : OnboardingRepository {
    override val isCompleted: Flow<Boolean> = context.onboardingDataStore.data.map { preferences ->
        preferences[onboardingCompletedKey] ?: false
    }

    override suspend fun complete() {
        context.onboardingDataStore.edit { preferences ->
            preferences[onboardingCompletedKey] = true
        }
    }
}
