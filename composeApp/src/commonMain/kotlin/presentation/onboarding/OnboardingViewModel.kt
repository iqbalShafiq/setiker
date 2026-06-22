package presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.preferences.UserPreferencesRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    private val _effect = Channel<OnboardingEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: OnboardingIntent) {
        when (intent) {
            OnboardingIntent.Next -> {
                val current = _state.value
                if (current.pageIndex >= current.pageCount - 1) {
                    completeOnboarding()
                } else {
                    _state.update { it.copy(pageIndex = it.pageIndex + 1) }
                }
            }
            is OnboardingIntent.SelectPage -> {
                _state.update { current ->
                    current.copy(
                        pageIndex = intent.pageIndex.coerceIn(
                            minimumValue = 0,
                            maximumValue = current.pageCount - 1
                        )
                    )
                }
            }
            OnboardingIntent.Skip, OnboardingIntent.Finish -> completeOnboarding()
        }
    }

    private fun completeOnboarding() {
        viewModelScope.launch {
            userPreferencesRepository.setOnboardingCompleted(true)
            _effect.send(OnboardingEffect.NavigateToHome)
        }
    }
}
