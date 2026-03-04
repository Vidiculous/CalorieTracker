package com.calorietracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val PROTEIN_GOAL = intPreferencesKey("protein_goal")
        val CARBS_GOAL = intPreferencesKey("carbs_goal")
        val FAT_GOAL = intPreferencesKey("fat_goal")
        val CURRENT_WEIGHT = floatPreferencesKey("current_weight")
        val GOAL_WEIGHT = floatPreferencesKey("goal_weight")
        val HAS_ONBOARDED = booleanPreferencesKey("has_onboarded")
        val SELECTED_MODEL = stringPreferencesKey("selected_model")
        val API_KEY = stringPreferencesKey("api_key")
        val AUTO_SUBMIT = booleanPreferencesKey("auto_submit")
    }

    val settings: Flow<AppSettings> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            AppSettings(
                dailyGoal = prefs[Keys.DAILY_GOAL] ?: 2500,
                proteinGoal = prefs[Keys.PROTEIN_GOAL] ?: 150,
                carbsGoal = prefs[Keys.CARBS_GOAL] ?: 250,
                fatGoal = prefs[Keys.FAT_GOAL] ?: 70,
                currentWeight = prefs[Keys.CURRENT_WEIGHT] ?: 0f,
                goalWeight = prefs[Keys.GOAL_WEIGHT] ?: 0f,
                hasOnboarded = prefs[Keys.HAS_ONBOARDED] ?: false,
                selectedModel = prefs[Keys.SELECTED_MODEL] ?: "gemini-2.0-flash-exp",
                apiKey = prefs[Keys.API_KEY] ?: "",
                autoSubmit = prefs[Keys.AUTO_SUBMIT] ?: true
            )
        }

    suspend fun updateSettings(update: AppSettings.() -> AppSettings) {
        context.dataStore.edit { prefs ->
            val current = AppSettings(
                dailyGoal = prefs[Keys.DAILY_GOAL] ?: 2500,
                proteinGoal = prefs[Keys.PROTEIN_GOAL] ?: 150,
                carbsGoal = prefs[Keys.CARBS_GOAL] ?: 250,
                fatGoal = prefs[Keys.FAT_GOAL] ?: 70,
                currentWeight = prefs[Keys.CURRENT_WEIGHT] ?: 0f,
                goalWeight = prefs[Keys.GOAL_WEIGHT] ?: 0f,
                hasOnboarded = prefs[Keys.HAS_ONBOARDED] ?: false,
                selectedModel = prefs[Keys.SELECTED_MODEL] ?: "gemini-2.0-flash-exp",
                apiKey = prefs[Keys.API_KEY] ?: "",
                autoSubmit = prefs[Keys.AUTO_SUBMIT] ?: true
            )
            val updated = current.update()
            prefs[Keys.DAILY_GOAL] = updated.dailyGoal
            prefs[Keys.PROTEIN_GOAL] = updated.proteinGoal
            prefs[Keys.CARBS_GOAL] = updated.carbsGoal
            prefs[Keys.FAT_GOAL] = updated.fatGoal
            prefs[Keys.CURRENT_WEIGHT] = updated.currentWeight
            prefs[Keys.GOAL_WEIGHT] = updated.goalWeight
            prefs[Keys.HAS_ONBOARDED] = updated.hasOnboarded
            prefs[Keys.SELECTED_MODEL] = updated.selectedModel
            prefs[Keys.API_KEY] = updated.apiKey
            prefs[Keys.AUTO_SUBMIT] = updated.autoSubmit
        }
    }
}
