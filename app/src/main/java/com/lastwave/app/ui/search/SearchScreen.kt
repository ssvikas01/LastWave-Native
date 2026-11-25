package com.lastwave.app.ui.search

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.lastwave.app.ui.recognition.MusicRecognitionButton
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import com.lastwave.app.R
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.lastwave.app.data.search.SearchResultItem
import com.lastwave.app.data.search.SearchResultType
import com.lastwave.app.data.search.SearchTab
import com.lastwave.app.ui.common.ArtworkImage
import com.lastwave.app.ui.common.ArtworkViewModel
import com.lastwave.app.ui.common.ExpressiveMotion
import com.lastwave.app.ui.common.HeaderActionIcon
import com.lastwave.app.ui.common.TrackContextMenuSheet
import com.lastwave.app.ui.common.TrackMenuCapabilities
import com.lastwave.app.ui.common.TrackMenuTarget
import com.lastwave.app.ui.common.TrackMiniTrayData
import com.lastwave.app.ui.common.TrackMiniTraySheet
import com.lastwave.app.ui.common.safeDrawingBottomPadding
import com.lastwave.app.ui.common.safeHorizontalContentPadding
import com.lastwave.app.ui.common.adaptiveContentWidth
import com.lastwave.app.ui.player.LocalMiniPlayerScrollClearance

import androidx.compose.foundation.background

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor



private val SearchHeaderShape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)

private val EXPLORE_GENRES = listOf(
    "Pop", "Rock", "Hip-Hop", "Lo-Fi", "Electronic", "Indie",
    "R&B", "Bollywood", "Jazz", "Metal", "Acoustic", "Chill",
    "Anime", "Classical", "Synthwave",
)

/**
 * YouTube Music styled search experience with live auto-complete suggestions,
 * persistent search history, top result card, explore tags, and 4 tabs.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit = {},
    onOpenArtist: (name: String, browseId: String?) -> Unit = { _, _ -> },
    onOpenAlbum: (title: String, artist: String, browseId: String?) -> Unit = { _, _, _ -> },
    onOpenPlaylist: (playlistId: String) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val artworkViewModel: ArtworkViewModel = hiltViewModel()
    val musicPlayer = com.lastwave.app.ui.player.LocalMusicPlayer.current
    val playbackState by musicPlayer.listPlaybackState.collectAsStateWithLifecycle()
    var menuTarget by remember { mutableStateOf<TrackMenuTarget?>(null) }
    var menuPlayableTrack by remember { mutableStateOf<com.lastwave.app.playback.PlayableTrack?>(null) }
    // Long-press (deep press) on a TRACKS row opens the mini tray;
    // the overflow button keeps the full sheet.
    var miniTrayItem by remember { mutableStateOf<SearchResultItem?>(null) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val voiceSearchLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenQuery = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenQuery.isNullOrBlank()) {
                viewModel.executeSearch(spokenQuery)
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .adaptiveContentWidth(maxWidth = 860.dp),
        ) {
        Surface(
            shape = SearchHeaderShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                    )
                    .padding(horizontal = 16.dp)
                    .padding(top = 6.dp, bottom = 10.dp),
            ) {
                // Top Search Input Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HeaderActionIcon(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
                    Spacer(Modifier.width(10.dp))

                    val pillBg = MaterialTheme.colorScheme.surfaceContainerHighest


                    BasicTextField(
                        value = state.query,
                        onValueChange = viewModel::setQuery,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                                viewModel.searchNow()
                            },
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.searchNow()
                            },
                            onGo = {
                                focusManager.clearFocus()
                                viewModel.searchNow()
                            },
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .focusRequester(focusRequester),
                        decorationBox = { innerTextField ->
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(pillBg)
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = stringResource(R.string.search_cd),
                                    tint = if (state.query.isNotEmpty()) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
                                    },
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable(enabled = state.query.isNotBlank()) {
                                            focusManager.clearFocus()
                                            viewModel.searchNow()
                                        },
                                )
                                Spacer(Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    if (state.query.isEmpty()) {
                                        Text(
                                            text = "Search songs, albums, or artists",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.74f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    innerTextField()
                                }
                                if (state.query.isNotEmpty()) {
                                    IconButton(
                                        onClick = viewModel::clearQuery,
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Icon(
                                            Icons.Filled.Clear,
                                            contentDescription = stringResource(R.string.search_clear_cd),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        },
                    )

                    Spacer(Modifier.width(4.dp))

                    MusicRecognitionButton(key = "recognition_search")

                    IconButton(
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                                )
                                putExtra(
                                    RecognizerIntent.EXTRA_PROMPT,
                                    context.getString(R.string.search_voice_prompt),
                                )
                                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                            }
                            try {
                                voiceSearchLauncher.launch(intent)
                            } catch (_: ActivityNotFoundException) {
                                Toast.makeText(
                                    context,
                                    R.string.search_voice_unavailable,
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = stringResource(R.string.search_voice_cd),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Box(Modifier.fillMaxSize().safeHorizontalContentPadding()) {
            when {
                // 1. Live Auto-Complete Suggestions (while actively typing)
                state.isShowingSuggestions && (state.songSuggestions.isNotEmpty() || state.suggestions.isNotEmpty()) -> {
                    val matchingRecent = remember(state.query, state.recentSearches) {
                        val q = state.query.trim().lowercase()
                        if (q.isEmpty()) emptyList()
                        else state.recentSearches.filter { it.lowercase().contains(q) }.take(3)
                    }
                    val nonRecentSuggestions = remember(state.suggestions, matchingRecent) {
                        state.suggestions.filter { !matchingRecent.contains(it) }.distinct()
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(
                            top = 8.dp,
                            bottom = 24.dp + LocalMiniPlayerScrollClearance.current + safeDrawingBottomPadding(),
                        ),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(state.songSuggestions, key = { index, song -> "song_sugg_${song.videoId}_$index" }) { _, song ->
                            SuggestionRow(
                                fullText = song.name,
                                query = state.query,
                                isHistory = false,
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.playResult(song)
                                    viewModel.executeSearch(song.name)
                                },
                                onInsert = { viewModel.setQuery(song.name) },
                                song = song,
                            )
                        }

                        item(key = "explicit_direct_search") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        focusManager.clearFocus()
                                        viewModel.searchNow()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    text = "Search for \"${state.query.trim()}\"",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        if (matchingRecent.isNotEmpty()) {
                            itemsIndexed(matchingRecent.distinct(), key = { idx, recentText -> "recent_match_${recentText}_$idx" }) { _, recentText ->
                                SuggestionRow(
                                    fullText = recentText,
                                    query = state.query,
                                    isHistory = true,
                                    onClick = {
                                        focusManager.clearFocus()
                                        viewModel.executeSearch(recentText)
                                    },
                                    onInsert = { viewModel.setQuery(recentText) },
                                )
                            }
                        }

                        itemsIndexed(nonRecentSuggestions, key = { idx, suggestion -> "sugg_${suggestion}_$idx" }) { _, suggestion ->
                            SuggestionRow(
                                fullText = suggestion,
                                query = state.query,
                                isHistory = false,
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.executeSearch(suggestion)
                                },
                                onInsert = { viewModel.setQuery(suggestion) },
                            )
                        }
                    }
                }

                // 2. Empty Query State: Show Recent Search History & Explore Tags
                state.query.isBlank() -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            top = 12.dp,
                            bottom = 24.dp + LocalMiniPlayerScrollClearance.current + safeDrawingBottomPadding(),
                        ),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (state.recentSearches.isNotEmpty()) {
                            item(key = "history_header") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        stringResource(R.string.search_recent),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    TextButton(onClick = viewModel::clearRecentSearches) {
                                        Text(stringResource(R.string.search_clear_all), style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                            itemsIndexed(state.recentSearches.distinct(), key = { idx, recentQuery -> "hist_${recentQuery}_$idx" }) { _, recentQuery ->
                                RecentSearchRow(
                                    text = recentQuery,
                                    onClick = {
                                        focusManager.clearFocus()
                                        viewModel.executeSearch(recentQuery)
                                    },
                                    onDelete = { viewModel.removeRecentSearch(recentQuery) },
                                )
                            }
                            item(key = "history_spacer") {
                                Spacer(Modifier.height(16.dp))
                            }
                        }

                        item(key = "explore_section") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 12.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Explore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        stringResource(R.string.search_explore),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    EXPLORE_GENRES.forEach { genre ->
                                        AssistChip(
                                            onClick = {
                                                focusManager.clearFocus()
                                                viewModel.executeSearch(genre)
                                            },
                                            label = { Text(genre, style = MaterialTheme.typography.labelLarge) },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                labelColor = MaterialTheme.colorScheme.onSurface,
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Search Results State: Loading, Empty, Results
                else -> {
                    Crossfade(
                        targetState = state.status,
                        animationSpec = tween(ExpressiveMotion.Standard, easing = FastOutSlowInEasing),
                        label = "searchState",
                    ) { status ->
                        when (status) {
                            SearchStatus.IDLE -> Box(Modifier.fillMaxSize())
                            SearchStatus.LOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                com.lastwave.app.ui.common.ExpressiveLoadingIndicator()
                            }
                            SearchStatus.EMPTY -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Filled.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(44.dp),
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Text(stringResource(R.string.search_no_results, state.query), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            SearchStatus.RESULTS -> {
                                val topResult = state.results.firstOrNull()
                                val otherResults = if (state.results.size > 1) state.results.drop(1) else emptyList()

                                LazyColumn(
                                    contentPadding = PaddingValues(
                                        top = 8.dp,
                                        bottom = 24.dp + LocalMiniPlayerScrollClearance.current + safeDrawingBottomPadding(),
                                    ),
                                ) {
                                    if (topResult != null) {
                                        val isTopPlaying = topResult.type == SearchResultType.TRACK && playbackState.isPlaying &&
                                            playbackState.current?.title.equals(topResult.name, ignoreCase = true) &&
                                            (topResult.artist.isNullOrBlank() || playbackState.current?.artist.equals(topResult.artist, ignoreCase = true))

                                        item(key = "top_result_card", contentType = "top_card") {
                                            TopResultCard(
                                                item = topResult,
                                                isPlaying = isTopPlaying,
                                                onPlay = {
                                                    when (topResult.type) {
                                                        SearchResultType.ARTIST -> onOpenArtist(topResult.name, topResult.entityId)
                                                        SearchResultType.ALBUM -> viewModel.playResult(topResult)
                                                        SearchResultType.PLAYLIST -> topResult.entityId?.let(onOpenPlaylist)
                                                        SearchResultType.TRACK -> viewModel.playResult(topResult)
                                                    }
                                                },
                                                onLongClick = {
                                                    if (topResult.type == SearchResultType.TRACK) {
                                                        miniTrayItem = topResult
                                                    }
                                                },
                                            )
                                            Spacer(Modifier.height(8.dp))
                                        }

                                        if (topResult.type == SearchResultType.ALBUM && topResult.previewTracks.isNotEmpty()) {
                                            item(key = "album_inline_preview", contentType = "album_preview") {
                                                AlbumPreviewCard(
                                                    album = topResult,
                                                    currentVideoId = playbackState.current?.videoId,
                                                    isPlaying = playbackState.isPlaying,
                                                    onTrackClick = { idx -> viewModel.playAlbumPreviewTrack(topResult, idx) },
                                                    onOpenFullAlbum = { onOpenAlbum(topResult.name, topResult.artist.orEmpty(), topResult.entityId) },
                                                )
                                                Spacer(Modifier.height(10.dp))
                                            }
                                        }

                                        if (topResult.type == SearchResultType.ARTIST && otherResults.isNotEmpty()) {
                                            item(key = "artist_albums_header") {
                                                Text(
                                                    text = "Albums & Releases",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                                )
                                            }
                                        }
                                    }

                                    itemsIndexed(
                                        otherResults,
                                        key = { index, item -> "${item.entityId ?: item.url.ifBlank { "${item.name}_${item.artist.orEmpty()}" }}_$index" },
                                        contentType = { _, _ -> "search_result" },
                                    ) { _, item ->
                                        val isItemPlaying = item.type == SearchResultType.TRACK && playbackState.isPlaying &&
                                            playbackState.current?.title.equals(item.name, ignoreCase = true) &&
                                            (item.artist.isNullOrBlank() || playbackState.current?.artist.equals(item.artist, ignoreCase = true))

                                        SearchResultRow(
                                            item = item,
                                            isPlaying = isItemPlaying,
                                            onClick = {
                                                when (item.type) {
                                                    SearchResultType.ARTIST -> onOpenArtist(item.name, item.entityId)
                                                    SearchResultType.ALBUM -> onOpenAlbum(item.name, item.artist.orEmpty(), item.entityId)
                                                    SearchResultType.PLAYLIST -> item.entityId?.let(onOpenPlaylist)
                                                    SearchResultType.TRACK -> viewModel.playResult(item)
                                                }
                                            },
                                            onLongClick = {
                                                if (item.type == SearchResultType.TRACK) {
                                                    miniTrayItem = item
                                                }
                                            },
                                            onMenu = {
                                                menuPlayableTrack = if (item.type == SearchResultType.TRACK) {
                                                    item.toPlayableTrack(podcast = false)
                                                } else null
                                                menuTarget = when (item.type) {
                                                    SearchResultType.TRACK -> TrackMenuTarget.Track(item.name, item.artist.orEmpty(), item.url)
                                                    SearchResultType.ARTIST -> TrackMenuTarget.Artist(item.name, item.url)
                                                    SearchResultType.ALBUM -> TrackMenuTarget.Album(item.name, item.artist.orEmpty(), item.url)
                                                    SearchResultType.PLAYLIST -> null
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }

    menuTarget?.let { target ->
        TrackContextMenuSheet(
            target = target,
            playableTrack = menuPlayableTrack,
            capabilities = TrackMenuCapabilities(showDeleteScrobble = state.tab != SearchTab.PODCASTS),
            playbackSourceLabel = "Search",
            onRefreshArtwork = (target as? TrackMenuTarget.Track)?.let { trackTarget ->
                { artworkViewModel.refresh(trackTarget.name, trackTarget.artist) }
            },
            onDismiss = { menuTarget = null },
        )
    }

    miniTrayItem?.let { item ->
        TrackMiniTraySheet(
            data = TrackMiniTrayData(
                title = item.name,
                artist = item.artist.orEmpty(),
                album = item.subtitle,
                artworkUrl = item.artworkUrl,
                videoId = item.videoId,
                durationMs = item.durationMs,
                sourceLabel = "Search",
                onPlay = { viewModel.playResult(item) },
            ),
            onDismiss = { miniTrayItem = null },
        )
    }
}

/** Featured Top Result Card styled after YouTube Music hero cards. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun TopResultCard(
    item: SearchResultItem,
    isPlaying: Boolean = false,
    onPlay: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(20.dp)
    val containerColor = if (isPlaying) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f)
    }
    val cardContentColor = MaterialTheme.colorScheme.onSurface
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = cardContentColor,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .combinedClickable(
                onClick = onPlay,
                onLongClick = onLongClick?.let { action ->
                    {
                        haptics.performHapticFeedback(
                            androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress,
                        )
                        action()
                    }
                },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val fallback = when (item.type) {
                SearchResultType.TRACK -> if (isPlaying) Icons.Filled.GraphicEq else Icons.Filled.MusicNote
                SearchResultType.ARTIST -> Icons.Filled.Person
                SearchResultType.ALBUM -> Icons.Filled.Album
                SearchResultType.PLAYLIST -> Icons.AutoMirrored.Filled.QueueMusic
            }
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(if (item.type == SearchResultType.ARTIST) CircleShape else RoundedCornerShape(6.dp)),
            ) {
                ArtworkImage(
                    name = item.name,
                    artist = item.artist.orEmpty(),
                    embeddedUrl = item.artworkUrl,
                    fallbackIcon = fallback,
                    modifier = Modifier.fillMaxSize(),
                )
                if (isPlaying) {
                    com.lastwave.app.ui.player.PlayingWaveBars(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(3.dp).size(24.dp, 18.dp),
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.padding(bottom = 4.dp),
                ) {
                    Text(
                        when (item.type) {
                            SearchResultType.TRACK -> "TOP SONG"
                            SearchResultType.ARTIST -> "TOP ARTIST"
                            SearchResultType.ALBUM -> "TOP ALBUM"
                            SearchResultType.PLAYLIST -> "TOP PLAYLIST"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = when (item.type) {
                    SearchResultType.TRACK -> listOfNotNull(item.artist, item.subtitle).filter { it.isNotBlank() }.distinct().joinToString(" • ")
                    SearchResultType.ALBUM -> listOfNotNull(item.artist, item.subtitle).filter { it.isNotBlank() }.distinct().joinToString(" • ")
                    SearchResultType.PLAYLIST -> item.subtitle.orEmpty()
                    SearchResultType.ARTIST -> item.subtitle.orEmpty().ifBlank { "Artist" }
                }
                if (subtitle.isNotBlank()) {
                        Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = onPlay,
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.size(46.dp),
            ) {
                Icon(
                    if (item.type == SearchResultType.PLAYLIST || item.type == SearchResultType.ARTIST) Icons.AutoMirrored.Filled.ArrowForward else Icons.Filled.PlayArrow,
                    contentDescription = if (item.type == SearchResultType.PLAYLIST || item.type == SearchResultType.ARTIST) "Open" else "Play top result",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    fullText: String,
    query: String,
    isHistory: Boolean,
    onClick: () -> Unit,
    onInsert: () -> Unit,
    song: SearchResultItem? = null,
) {
    val baseColor = MaterialTheme.colorScheme.onSurface
    val highlightColor = MaterialTheme.colorScheme.primary
    val annotated = remember(fullText, query, baseColor, highlightColor) {
        buildHighlightedSuggestion(fullText, query, baseColor, highlightColor)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (song != null) {
            val thumbnailUrl = remember(song.artworkUrl) { suggestionThumbnailUrl(song.artworkUrl) }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else {
            Icon(
                if (isHistory) Icons.Filled.History else Icons.Filled.Search,
                contentDescription = null,
                tint = if (isHistory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = annotated,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
        )
        IconButton(
            onClick = onInsert,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.search_insert_cd),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer { rotationZ = 135f },
            )
        }
    }
}

private fun suggestionThumbnailUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    return when {
        url.contains("googleusercontent.com") || url.contains("ggpht.com") ->
            url.substringBeforeLast('=') + "=w120-h120-l90-rj"
        url.contains("ytimg.com") || url.contains("img.youtube.com/vi/") ->
            url.substringBeforeLast('/') + if (url.contains("/vi_webp/")) "/default.webp" else "/default.jpg"
        else -> url
    }
}

private fun buildHighlightedSuggestion(
    suggestion: String,
    query: String,
    baseColor: Color,
    highlightColor: Color,
): AnnotatedString {
    val q = query.trim()
    if (q.isEmpty()) return AnnotatedString(suggestion)
    val lowerSug = suggestion.lowercase()
    val lowerQ = q.lowercase()
    val idx = lowerSug.indexOf(lowerQ)
    if (idx < 0) return AnnotatedString(suggestion)

    return buildAnnotatedString {
        if (idx > 0) {
            withStyle(SpanStyle(color = baseColor, fontWeight = FontWeight.Normal)) {
                append(suggestion.substring(0, idx))
            }
        }
        withStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold)) {
            append(suggestion.substring(idx, idx + q.length))
        }
        if (idx + q.length < suggestion.length) {
            withStyle(SpanStyle(color = baseColor, fontWeight = FontWeight.SemiBold)) {
                append(suggestion.substring(idx + q.length))
            }
        }
    }
}

@Composable
private fun RecentSearchRow(
    text: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
        )
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.search_remove_cd),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun SearchResultRow(
    item: SearchResultItem,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = if (item.type == SearchResultType.TRACK) onLongClick else onMenu,
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fallback = when (item.type) {
            SearchResultType.TRACK -> if (isPlaying) Icons.Filled.GraphicEq else Icons.Filled.MusicNote
            SearchResultType.ARTIST -> Icons.Filled.Person
            SearchResultType.ALBUM -> Icons.Filled.Album
            SearchResultType.PLAYLIST -> Icons.AutoMirrored.Filled.QueueMusic
        }
        Box(modifier = Modifier.size(48.dp)) {
            ArtworkImage(
                name = item.name,
                artist = item.artist.orEmpty(),
                embeddedUrl = item.artworkUrl,
                fallbackIcon = fallback,
                modifier = Modifier.fillMaxSize().clip(if (item.type == SearchResultType.ARTIST) CircleShape else RoundedCornerShape(4.dp)),
            )
            if (isPlaying) {
                com.lastwave.app.ui.player.PlayingWaveBars(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp).size(24.dp, 18.dp),
                )
            }
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = when (item.type) {
                SearchResultType.TRACK -> listOfNotNull(item.artist, item.subtitle).filter { it.isNotBlank() }.joinToString(" \u2022 ")
                SearchResultType.ALBUM -> item.subtitle ?: item.artist.orEmpty()
                SearchResultType.PLAYLIST -> item.subtitle.orEmpty()
                SearchResultType.ARTIST -> item.subtitle.orEmpty()
            }
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (item.type != SearchResultType.PLAYLIST) {
            com.lastwave.app.ui.common.OverflowMenuButton(onClick = onMenu)
        }
    }
}

@Composable
private fun AlbumPreviewCard(
    album: SearchResultItem,
    currentVideoId: String?,
    isPlaying: Boolean,
    onTrackClick: (Int) -> Unit,
    onOpenFullAlbum: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            album.previewTracks.forEachIndexed { index, track ->
                val isTrackPlaying = isPlaying && currentVideoId != null && currentVideoId == track.videoId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTrackClick(index) }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${track.trackNumber ?: (index + 1)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isTrackPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(24.dp),
                    )
                    Text(
                        text = track.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isTrackPlaying) FontWeight.Bold else FontWeight.Medium,
                        color = if (isTrackPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    track.durationMs?.takeIf { it > 0 }?.let { ms ->
                        val totalSec = ms / 1000
                        val min = totalSec / 60
                        val sec = totalSec % 60
                        Text(
                            text = "%d:%02d".format(min, sec),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenFullAlbum)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = "Open full album",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
