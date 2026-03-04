package com.calorietracker.ui.screen.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorietracker.data.preferences.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val hasOnboarded = settingsDataStore.settings
        .map { it.hasOnboarded }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun completeOnboarding(dailyGoal: Int, apiKey: String, model: String) {
        viewModelScope.launch {
            settingsDataStore.updateSettings {
                copy(
                    dailyGoal = dailyGoal,
                    apiKey = apiKey,
                    selectedModel = model,
                    hasOnboarded = true
                )
            }
        }
    }
}
