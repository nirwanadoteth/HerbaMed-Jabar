package edu.unikom.herbamedjabar.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.unikom.herbamedjabar.data.ScanHistory
import edu.unikom.herbamedjabar.repository.PlantRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryDetailViewModel @Inject constructor(private val plantRepository: PlantRepository) :
    ViewModel() {

    fun deleteHistory(history: ScanHistory) {
        viewModelScope.launch { plantRepository.deleteHistory(history) }
    }
}
