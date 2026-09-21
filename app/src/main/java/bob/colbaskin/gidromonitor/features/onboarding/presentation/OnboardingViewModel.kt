package bob.colbaskin.gidromonitor.features.onboarding.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bob.colbaskin.gidromonitor.features.onboarding.domain.OnboardingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingState(
    val isLoading: Boolean = true,
    val isCompleted: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: OnboardingRepository
) : ViewModel() {
    var state by mutableStateOf(OnboardingState())
        private set

    init {
        viewModelScope.launch {
            repository.isCompleted.collectLatest { isCompleted ->
                state = OnboardingState(isLoading = false, isCompleted = isCompleted)
            }
        }
    }

    fun startAnalysis() {
        viewModelScope.launch { repository.complete() }
    }
}
