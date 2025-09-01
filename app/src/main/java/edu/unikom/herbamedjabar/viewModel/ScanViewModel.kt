package edu.unikom.herbamedjabar.viewModel

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.unikom.herbamedjabar.repository.AnalysisResult
import edu.unikom.herbamedjabar.useCase.AnalyzePlantUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    object Success : UiState()
    data class Error(val message: String) : UiState()
}

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val analyzePlantUseCase: AnalyzePlantUseCase
) : ViewModel() {

    private val _uiState = MutableLiveData<UiState>(UiState.Idle)
    val uiState: LiveData<UiState> = _uiState

    private val _navigateToResult = MutableLiveData<AnalysisResult?>()
    val navigateToResult: LiveData<AnalysisResult?> = _navigateToResult

    data class ScanStats(val total: Int = 0, val herbal: Int = 0, val nonHerbal: Int = 0)

    private val _scanStats = MutableLiveData(ScanStats())
    val scanStats: LiveData<ScanStats> = _scanStats

    fun analyzeImage(bitmap: Bitmap) {
        if (_uiState.value is UiState.Loading) return
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val result = analyzePlantUseCase(bitmap)
            result.onSuccess { analysisResult ->
                _uiState.value = UiState.Success
                _navigateToResult.value = analysisResult
                val stats = _scanStats.value ?: ScanStats()
                val newStats = if (analysisResult.isHerbal) {
                    stats.copy(total = stats.total + 1, herbal = stats.herbal + 1)
                } else {
                    stats.copy(total = stats.total + 1, nonHerbal = stats.nonHerbal + 1)
                }
                _scanStats.value = newStats
            }.onFailure { error ->
                _uiState.value = UiState.Error(error.message ?: "Terjadi kesalahan tidak diketahui")
            }
        }
    }

    fun onNavigationComplete() {
        _navigateToResult.value = null
        if (_uiState.value !is UiState.Loading) {
            _uiState.value = UiState.Idle
        }
    }
}
