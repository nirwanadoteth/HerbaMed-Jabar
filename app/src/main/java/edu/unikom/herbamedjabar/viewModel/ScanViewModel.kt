package edu.unikom.herbamedjabar.viewModel

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.unikom.herbamedjabar.repository.AnalysisResult
import edu.unikom.herbamedjabar.useCase.AnalyzePlantUseCase
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed class UiState {
    object Idle : UiState()

    object Loading : UiState()

    object Success : UiState()

    data class Error(val message: String) : UiState()
}

@HiltViewModel
class ScanViewModel @Inject constructor(private val analyzePlantUseCase: AnalyzePlantUseCase) :
    ViewModel() {

    // Cancellable job for in-flight analyzeImage work. This lets callers cancel
    // existing analysis without coupling concurrency to UI state.
    private var analyzeJob: Job? = null

    private val _uiState = MutableLiveData<UiState>(UiState.Idle)
    val uiState: LiveData<UiState> = _uiState

    private val _navigateToResult = MutableLiveData<AnalysisResult?>()
    val navigateToResult: LiveData<AnalysisResult?> = _navigateToResult

    data class ScanStats(val total: Int = 0, val herbal: Int = 0, val nonHerbal: Int = 0)

    private val _scanStats = MutableLiveData(ScanStats())
    val scanStats: LiveData<ScanStats> = _scanStats

    fun analyzeImage(bitmap: Bitmap) {
        // Cancel any in-flight analysis before starting a new one.
        analyzeJob?.cancel()
        _uiState.value = UiState.Loading

        analyzeJob = viewModelScope.launch {
            try {
                val result = analyzePlantUseCase(bitmap)
                result.onSuccess { analysisResult ->
                    _uiState.value = UiState.Success
                    _navigateToResult.value = analysisResult
                    val stats = _scanStats.value ?: ScanStats()
                    val newStats =
                        if (analysisResult.isHerbal) {
                            stats.copy(total = stats.total + 1, herbal = stats.herbal + 1)
                        } else {
                            stats.copy(total = stats.total + 1, nonHerbal = stats.nonHerbal + 1)
                        }
                    _scanStats.value = newStats
                }.onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Terjadi kesalahan tidak diketahui")
                }
            } catch (_: CancellationException) {
                // Job was cancelled — don't update UI state to error.
                return@launch
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Terjadi kesalahan tidak diketahui")
            } finally {
                // Clear reference to allow GC and signal there's no active job.
                analyzeJob = null
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
