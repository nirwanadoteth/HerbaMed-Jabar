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

    // LiveData for scan counts
    private val _totalScan = MutableLiveData(0)
    val totalScan: LiveData<Int> = _totalScan

    private val _herbalScan = MutableLiveData(0)
    val herbalScan: LiveData<Int> = _herbalScan

    private val _nonHerbalScan = MutableLiveData(0)
    val nonHerbalScan: LiveData<Int> = _nonHerbalScan

    fun analyzeImage(bitmap: Bitmap) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val result = analyzePlantUseCase(bitmap)
            result.onSuccess { analysisResult ->
                _uiState.postValue(UiState.Success)
                _navigateToResult.postValue(analysisResult)
                // Increment scan counts
                _totalScan.postValue((_totalScan.value ?: 0) + 1)
                if (analysisResult.isHerbal) {
                    _herbalScan.postValue((_herbalScan.value ?: 0) + 1)
                } else {
                    _nonHerbalScan.postValue((_nonHerbalScan.value ?: 0) + 1)
                }
            }.onFailure { error ->
                _uiState.postValue(
                    UiState.Error(
                        error.message ?: "Terjadi kesalahan tidak diketahui"
                    )
                )
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
