package bob.colbaskin.gidromonitor.features.onboarding.domain

import kotlinx.coroutines.flow.Flow

interface OnboardingRepository {
    val isCompleted: Flow<Boolean>

    suspend fun complete()
}
