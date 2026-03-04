package com.calorietracker.ui.screen.barcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorietracker.data.model.FoodLogEntry
import com.calorietracker.data.repository.FoodLogRepository
import com.calorietracker.data.repository.FoodSearchRepository
import com.calorietracker.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class BarcodeScanState {
    object Scanning : BarcodeScanState()
    data class Loading(val barcode: String) : BarcodeScanState()
    data class Success(val entry: FoodLogEntry) : BarcodeScanState()
    data class Error(val message: String) : BarcodeScanState()
}

@HiltViewModel
class BarcodeScannerViewModel @Inject constructor(
    private val foodSearchRepository: FoodSearchRepository,
    private val foodLogRepository: FoodLogRepository
) : ViewModel() {

    private val _scanState = MutableStateFlow<BarcodeScanState>(BarcodeScanState.Scanning)
    val scanState = _scanState.asStateFlow()

    private val processedBarcodes = mutableSetOf<String>()

    fun onBarcodeDetected(barcode: String) {
        if (barcode in processedBarcodes || _scanState.value is BarcodeScanState.Loading) return
        processedBarcodes.add(barcode)
        _scanState.value = BarcodeScanState.Loading(barcode)

        viewModelScope.launch {
            val result = foodSearchRepository.getByBarcode(barcode)
            if (result != null) {
                val entry = FoodLogEntry(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    foodName = result.foodName,
                    calories = result.caloriesPer100g.toInt(),
                    protein = result.proteinPer100g,
                    carbs = result.carbsPer100g,
                    fat = result.fatPer100g,
                    quantity = "100g",
                    source = "barcode",
                    mealType = DateUtils.inferMealType(System.currentTimeMillis())
                )
                foodLogRepository.insert(entry)
                _scanState.value = BarcodeScanState.Success(entry)
                // Reset after 2 seconds
                kotlinx.coroutines.delay(2000)
                _scanState.value = BarcodeScanState.Scanning
                processedBarcodes.remove(barcode)
            } else {
                _scanState.value = BarcodeScanState.Error("Product not found in database")
                kotlinx.coroutines.delay(2000)
                _scanState.value = BarcodeScanState.Scanning
                processedBarcodes.remove(barcode)
            }
        }
    }

    fun resetScan() {
        _scanState.value = BarcodeScanState.Scanning
    }
}
