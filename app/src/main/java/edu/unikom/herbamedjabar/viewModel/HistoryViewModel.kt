package edu.unikom.herbamedjabar.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.unikom.herbamedjabar.repository.PlantRepository
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    plantRepository: PlantRepository
) : ViewModel() {

    val allHistory = plantRepository.getAllHistory().asLiveData()
}
