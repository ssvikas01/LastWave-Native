package com.lastwave.app.ui.search

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastwave.app.data.generate.LocalAudioFormats
import com.lastwave.app.data.generate.isLocalAudioPath
import com.lastwave.app.data.search.SearchHistoryRepository
import com.lastwave.app.data.search.SearchRepository
import com.lastwave.app.data.search.SearchResultItem
import com.lastwave.app.data.search.SearchResultType
import com.lastwave.app.data.search.SearchTab
import com.lastwave.app.playback.MediaType
import com.lastwave.app.playback.MusicPlayer
import com.lastwave.app.playback.PlayableTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SearchStatus { IDLE, LOADING, EMPTY, RESULTS }

@Immutable
data class SearchUiState(
    val query: String = "",
    val tab: SearchTab = SearchTab.TRACKS,
    val status: SearchStatus = SearchStatus.IDLE,
    val results: List<SearchResultItem> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val songSuggestions: List<SearchResultItem> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val isShowingSuggestions: Boolean = false,
)

/**
 * YouTube Music & Last.fm search with live auto-complete suggestions,
 * persistent search history, debounced search, and multi-tab results.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SearchRepository,
    private val historyRepository: SearchHistoryRepository,
    private val musicPlayer: MusicPlayer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var debounceJob: Job? = null
    private var suggestionsJob: Job? = null
    private var searchJob: Job? = null
    private var searchQueueJob: Job? = null
    private var lastIssuedQuery: String = ""

    init {
        // Opened with a ready-made query (e.g. from music recognition).
        savedStateHandle.get<String>("q")?.takeIf { it.isNotBlank() }?.let(::executeSearch)
        viewModelScope.launch {
            historyRepository.history.collect { history ->
                _uiState.update { it.copy(recentSearches = history) }
            }
        }
    }

    fun setQuery(query: String) {
        val cached = if (query.isNotBlank()) repository.getCachedSmartSearch(query) else null
        _uiState.update {
            it.copy(
                query = query,
                songSuggestions = cached?.filter { r -> r.type == SearchResultType.TRACK }?.take(5).orEmpty(),
                results = cached ?: it.results,
                status = if (cached != null) SearchStatus.RESULTS else it.status,
                isShowingSuggestions = query.isNotBlank() && cached == null,
            )
        }
        debounceJob?.cancel()
        suggestionsJob?.cancel()

        if (query.isBlank()) {
            musicPlayer.cancelSearchResultWarm()
            searchJob?.cancel()
            _uiState.update {
                it.copy(
                    status = SearchStatus.IDLE,
                    results = emptyList(),
                    suggestions = emptyList(),
                    isShowingSuggestions = false,
                )
            }
            return
        }

        if (cached != null) {
            lastIssuedQuery = query.trim()
            return
        }

        // Fast suggestions debounce (120ms)
        suggestionsJob = viewModelScope.launch {
            delay(120)
            val suggestions = repository.getSuggestions(query)
            if (_uiState.value.query == query) {
                _uiState.update { it.copy(suggestions = suggestions) }
            }
        }

        // Fast smart search debounce (200ms)
        debounceJob = viewModelScope.launch {
            delay(200)
            triggerSearch(query, saveToHistory = false)
        }
    }

    fun setTab(tab: SearchTab) {
        if (_uiState.value.tab == tab) return
        _uiState.update {
            it.copy(
                tab = tab,
                songSuggestions = emptyList(),
                isShowingSuggestions = false,
                results = emptyList(),
                status = if (it.query.isBlank()) SearchStatus.IDLE else SearchStatus.LOADING,
            )
        }
        val q = _uiState.value.query
        if (q.isNotBlank()) {
            debounceJob?.cancel()
            suggestionsJob?.cancel()
            triggerSearch(q, saveToHistory = false)
        }
    }

    fun searchNow() {
        val q = _uiState.value.query
        if (q.isBlank()) return
        executeSearch(q, autoPlayTop = true)
    }

    fun executeSearch(query: String, autoPlayTop: Boolean = false) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        debounceJob?.cancel()
        suggestionsJob?.cancel()
        historyRepository.add(trimmed)

        val currentState = _uiState.value
        if (autoPlayTop &&
            currentState.query.trim().equals(trimmed, ignoreCase = true) &&
            currentState.status == SearchStatus.RESULTS &&
            currentState.results.isNotEmpty()
        ) {
            _uiState.update { it.copy(query = trimmed, isShowingSuggestions = false) }
            val top = currentState.results.first()
            if (top.type == SearchResultType.TRACK || top.type == SearchResultType.ALBUM) {
                playResult(top)
            }
            return
        }

        _uiState.update {
            it.copy(
                query = trimmed,
                isShowingSuggestions = false,
            )
        }
        triggerSearch(trimmed, saveToHistory = true, autoPlayTop = autoPlayTop)
    }

    private fun triggerSearch(query: String, saveToHistory: Boolean, autoPlayTop: Boolean = false) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            runSearch(query, saveToHistory, autoPlayTop)
        }
    }

    fun removeRecentSearch(query: String) {
        historyRepository.remove(query)
    }

    fun clearRecentSearches() {
        historyRepository.clear()
    }

    fun dismissSuggestions() {
        _uiState.update { it.copy(isShowingSuggestions = false) }
    }

    fun playAlbumPreviewTrack(album: SearchResultItem, trackIndex: Int) {
        val previews = album.previewTracks
        if (previews.isEmpty()) return
        musicPlayer.playQueue(
            tracks = previews.map { it.toPlayableTrack() },
            startIndex = trackIndex.coerceIn(0, previews.lastIndex),
            sourceLabel = album.name,
            enableRadioContinuation = true,
        )
    }

    fun playResult(item: SearchResultItem) {
        searchQueueJob?.cancel()
        when (item.type) {
            SearchResultType.TRACK -> {
                val selected = item.toPlayableTrack()
                // Start immediately. Similar song radio queue loads and extends infinitely.
                musicPlayer.play(selected, sourceLabel = "Search", startRadio = true)
            }
            SearchResultType.ARTIST, SearchResultType.ALBUM -> viewModelScope.launch {
                val tracks = runCatching { repository.songsFor(item) }.getOrDefault(emptyList())
                if (tracks.isNotEmpty()) {
                    musicPlayer.playQueue(tracks.map { track ->
                        PlayableTrack(
                            title = track.title,
                            artist = track.artist.takeUnless { it == "Unknown artist" } ?: item.artist ?: item.name,
                            album = track.album ?: if (item.type == SearchResultType.ALBUM) item.name else null,
                            artworkUrl = track.artworkUrl ?: item.artworkUrl,
                            videoId = track.videoId,
                            durationMs = track.durationSeconds?.takeIf { it > 0 }?.times(1_000L),
                        )
                    }, sourceLabel = item.name)
                }
            }
            SearchResultType.PLAYLIST -> Unit
        }
    }

    private suspend fun runSearch(query: String, saveToHistory: Boolean, autoPlayTop: Boolean = false) {
        lastIssuedQuery = query
        _uiState.update { it.copy(status = SearchStatus.LOADING) }
        if (saveToHistory) {
            historyRepository.add(query)
        }
        try {
            val results = repository.smartSearch(query)
            // Stale-response guard: discard if the user has typed something
            // new since this call was issued.
            if (lastIssuedQuery != query || _uiState.value.query.trim() != query.trim()) return
            _uiState.update {
                it.copy(
                    status = if (results.isEmpty()) SearchStatus.EMPTY else SearchStatus.RESULTS,
                    results = results,
                    songSuggestions = results.filter { r -> r.type == SearchResultType.TRACK }.take(5),
                )
            }
            val trackItems = results.filter { r -> r.type == SearchResultType.TRACK }
            if (trackItems.isNotEmpty()) {
                musicPlayer.warmLikelyPlays(trackItems.map { it.toPlayableTrack() })
            }
            if (autoPlayTop && results.isNotEmpty()) {
                val top = results.first()
                if (top.type == SearchResultType.TRACK || top.type == SearchResultType.ALBUM) {
                    playResult(top)
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            if (lastIssuedQuery != query || _uiState.value.query.trim() != query.trim()) return
            _uiState.update { it.copy(status = SearchStatus.EMPTY, results = emptyList()) }
        }
    }

    fun clearQuery() {
        musicPlayer.cancelSearchResultWarm()
        debounceJob?.cancel()
        suggestionsJob?.cancel()
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                query = "",
                status = SearchStatus.IDLE,
                results = emptyList(),
                suggestions = emptyList(),
                songSuggestions = emptyList(),
                isShowingSuggestions = false,
            )
        }
    }
}

/** Taps, menus and warm-ups keep the same recording rather than resolving its title again. */
internal fun SearchResultItem.toPlayableTrack(podcast: Boolean = false): PlayableTrack {
    val isLocal = isLocalAudioPath(url)
    return PlayableTrack(
        title = name,
        artist = artist.orEmpty().ifBlank { if (podcast) "Podcast" else "" },
        album = if (podcast) null else subtitle,
        artworkUrl = artworkUrl,
        videoId = videoId,
        playbackUrl = if (isLocal) url else null,
        playbackMimeType = if (isLocal) LocalAudioFormats.mimeFor(url) else null,
        durationMs = durationMs,
        trackNumber = trackNumber,
        mediaType = if (podcast) MediaType.PODCAST_EPISODE else MediaType.SONG,
        showId = if (podcast) showId else null,
    )
}
