package com.ljyh.mei.ui.screen.podcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.mei.data.model.melox.Podcast
import com.ljyh.mei.data.model.melox.PodcastDetail
import com.ljyh.mei.data.model.melox.PodcastHome
import com.ljyh.mei.data.repository.MeloXRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PodcastUiState(
    val isLoading: Boolean = true,
    val home: PodcastHome? = null,
    val selectedCategoryId: Long? = null,
    val categoryPodcasts: List<Podcast> = emptyList(),
    val error: String? = null,
)

data class PodcastDetailUiState(
    val isLoading: Boolean = true,
    val detail: PodcastDetail? = null,
    val error: String? = null,
)

@HiltViewModel
class PodcastViewModel @Inject constructor(
    private val repository: MeloXRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(PodcastUiState())
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            runCatching { repository.podcastHome() }
                .onSuccess { _state.value = PodcastUiState(isLoading = false, home = it) }
                .onFailure { _state.value = _state.value.copy(isLoading = false, error = it.message) }
        }
    }

    fun selectCategory(id: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, selectedCategoryId = id, error = null)
            runCatching { repository.podcasts(id) }
                .onSuccess { podcasts ->
                    _state.value = _state.value.copy(isLoading = false, categoryPodcasts = podcasts)
                }
                .onFailure { _state.value = _state.value.copy(isLoading = false, error = it.message) }
        }
    }
}

@HiltViewModel
class PodcastDetailViewModel @Inject constructor(
    private val repository: MeloXRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(PodcastDetailUiState())
    val state = _state.asStateFlow()
    private var loadedId: Long? = null

    fun load(id: Long, force: Boolean = false) {
        if (!force && loadedId == id && _state.value.detail != null) return
        loadedId = id
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            runCatching { repository.podcastDetail(id) }
                .onSuccess { _state.value = PodcastDetailUiState(isLoading = false, detail = it) }
                .onFailure { _state.value = _state.value.copy(isLoading = false, error = it.message) }
        }
    }

    fun toggleSubscription() {
        val detail = _state.value.detail ?: return
        viewModelScope.launch {
            val target = !detail.podcast.isSubscribed
            runCatching { repository.setPodcastSubscribed(detail.podcast.id, target) }
                .onSuccess { load(detail.podcast.id, force = true) }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }
}
