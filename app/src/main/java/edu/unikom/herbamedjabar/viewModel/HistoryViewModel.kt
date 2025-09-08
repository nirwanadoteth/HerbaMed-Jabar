package edu.unikom.herbamedjabar.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.unikom.herbamedjabar.data.ScanHistory
import edu.unikom.herbamedjabar.repository.PlantRepository
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel
@Inject
constructor(plantRepository: PlantRepository, private val savedStateHandle: SavedStateHandle) :
    ViewModel() {

    val allHistory: LiveData<List<ScanHistory>> = plantRepository.getAllHistory().asLiveData()

    companion object {
        private const val KEY_SCROLL_POSITION = "history_scroll_position"
    }

    fun saveScrollPosition(position: Int) {
        savedStateHandle[KEY_SCROLL_POSITION] = position
    }

    fun getScrollPosition(): Int = savedStateHandle[KEY_SCROLL_POSITION] ?: 0
}
