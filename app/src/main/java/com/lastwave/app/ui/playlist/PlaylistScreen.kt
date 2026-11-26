package com.lastwave.app.ui.playlist

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.lastwave.app.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lastwave.app.data.generate.GeneratedTrack
import com.lastwave.app.data.playlist.SavedPlaylist
import com.lastwave.app.data.playlist.LIKED_SONGS_MODE
import com.lastwave.app.data.playlist.LOCAL_SONGS_MODE
import com.lastwave.app.data.playlist.PlaylistImportManager
import com.lastwave.app.data.playlist.isYouTubeOnly
import com.lastwave.app.playback.toPlayableTrack
import com.lastwave.app.ui.common.ArtworkImage
import com.lastwave.app.ui.common.ExpressiveHeader
import com.lastwave.app.ui.common.GroupPosition
import com.lastwave.app.ui.common.PlaylistCover
import com.lastwave.app.ui.common.TopCurvedSectionClipShape
import com.lastwave.app.ui.common.TrackContextMenuSheet
import com.lastwave.app.ui.common.TrackMenuCapabilities
import com.lastwave.app.ui.common.TrackMenuTarget
import com.lastwave.app.ui.common.adaptiveContentWidth
import com.lastwave.app.ui.shell.FloatingNavDefaults
import com.lastwave.app.ui.theme.ExpressivePillShape
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Faithful port of playlist.js's saved-playlists screen (§4): card list
 * backed by Room via PlaylistViewModel, expand/collapse with lazy track
 * rendering, the "just generated" regenerate bar, export bottom sheet
 * (CSV/M3U), Generate Similar, and delete — plus the shared track context
 * menu (§1.7) with Copy + Delete Scrobble enabled (this screen's full
 * capability set, matching the original's playlist.js menu exactly).
 */

private enum class PlaylistFilter(val label: String) {
    ALL("All"),
    LIKED("Liked"),
    GENERATED("Generated"),
    YTMUSIC("YT Music"),
    SPOTIFY("Spotify"),
    IMPORTED("Imported"),
    PINNED("Pinned"),
}

private val GENERATED_MODES = setOf(
    "discover", "generate", "recommendations", "mix", "top", "recent",
    "similar-tracks", "similar-artists", "tag", "never-heard", "library", "radio"
)

private fun heroPillShape(
    index: Int,
    count: Int,
    hasItemsBelow: Boolean,
): RoundedCornerShape {
    val outer = 24.dp
    val inner = 6.dp
    if (count == 1) {
        return RoundedCornerShape(
            topStart = outer,
            topEnd = outer,
            bottomStart = if (hasItemsBelow) inner else outer,
            bottomEnd = if (hasItemsBelow) inner else outer,
        )
    }
    return RoundedCornerShape(
        topStart = if (index == 0) outer else inner,
        topEnd = if (index == count - 1) outer else inner,
        bottomStart = if (index == 0 && !hasItemsBelow) outer else inner,
        bottomEnd = if (index == count - 1 && !hasItemsBelow) outer else inner,
    )
}

private fun connectedShape(index: Int, count: Int): RoundedCornerShape {
    val outer = 24.dp
    val inner = 6.dp
    return RoundedCornerShape(
        topStart = if (index == 0) outer else inner,
        bottomStart = if (index == 0) outer else inner,
        topEnd = if (index == count - 1) outer else inner,
        bottomEnd = if (index == count - 1) outer else inner,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    onOpenPlaylist: (Long) -> Unit = {},
    onOpenFeedPlaylist: (String) -> Unit = {},
    onOpenGenerator: () -> Unit = {},
    viewModel: PlaylistViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val musicPlayer = com.lastwave.app.ui.player.LocalMusicPlayer.current
    val playbackState by musicPlayer.listPlaybackState.collectAsStateWithLifecycle()
    val addToPlaylist = com.lastwave.app.ui.player.LocalAddToPlaylist.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf(PlaylistFilter.ALL) }
    var coverEditorPlaylistId by remember { mutableStateOf<Long?>(null) }
    var coverPickerPlaylistId by remember { mutableStateOf<Long?>(null) }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val playlistId = coverPickerPlaylistId
        if (uri != null && playlistId != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            viewModel.setCustomCover(playlistId, uri.toString())
        }
        coverPickerPlaylistId = null
    }

    // Re-reads from Room whenever this tab regains visibility — this is how
    // a playlist just saved by Generate shows up here without polling.
    LifecycleResumeEffect(Unit) {
        viewModel.load()
        onPauseOrDispose { }
    }
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            viewModel.ensureLibraryTracksIndexed()
        }
    }

    var menuTarget by remember { mutableStateOf<Pair<Long, GeneratedTrack>?>(null) }
    var selectedPlaylists by remember { mutableStateOf(setOf<Long>()) }
    var showMultipleDeleteConfirm by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    var isSearchBarVisible by remember { mutableStateOf(true) }
    val searchBarNestedScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -8f && searchQuery.isEmpty()) {
                    isSearchBarVisible = false
                } else if (available.y > 8f) {
                    isSearchBarVisible = true
                }
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) isSearchBarVisible = true
    }

    Box(modifier = Modifier.fillMaxSize().nestedScroll(searchBarNestedScroll)) {
        com.lastwave.app.ui.feed.FlowingArtworkTopGlow(
            artworkUrl = playbackState.current?.artworkUrl,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        Column(
            Modifier
                .fillMaxSize()
                .adaptiveContentWidth(maxWidth = 860.dp)
                .align(Alignment.TopCenter),
        ) {
            var sortMenuExpanded by remember { mutableStateOf(false) }
            ExpressiveHeader(
                title = if (selectedPlaylists.isNotEmpty()) stringResource(R.string.pl_selected_count, selectedPlaylists.size) else stringResource(R.string.pl_title),
                subtitle = if (selectedPlaylists.isNotEmpty()) {
                    stringResource(R.string.pl_select_more_hint)
                } else {
                    val totalPlaylistsCount = state.playlists.size + (if (state.isSpotifySyncEnabled) state.spotifyPlaylists.size else 0)
                    // A playlist whose remote count is still unknown adds 0 rather than hiding the whole total.
                    val totalTracks = state.playlists.sumOf { it.remoteTrackCount ?: it.tracks.size } +
                        (if (state.isSpotifySyncEnabled) state.spotifyPlaylists.sumOf { it.trackCount ?: 0 } else 0)
                    stringResource(R.string.pl_count_tracks, totalPlaylistsCount, totalTracks)
                },
                actions = {
                    if (selectedPlaylists.isNotEmpty()) {
                        val anyUnpinned = state.playlists.any { it.id in selectedPlaylists && !it.isPinned }
                        IconButton(
                            onClick = {
                                viewModel.setPinnedMultiple(selectedPlaylists, pin = anyUnpinned)
                                selectedPlaylists = setOf()
                            },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                Icons.Filled.PushPin,
                                contentDescription = if (anyUnpinned) "Pin to top" else "Unpin",
                                tint = if (anyUnpinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(
                            onClick = { showMultipleDeleteConfirm = true },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.pl_delete_selected_cd), tint = MaterialTheme.colorScheme.error)
                        }
                        IconButton(
                            onClick = { selectedPlaylists = setOf() },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.pl_clear_selection_cd))
                        }
                    } else {
                        IconButton(
                            onClick = viewModel::openCreateDialog,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.pl_create_cd))
                        }
                        Box {
                            Surface(
                                onClick = { sortMenuExpanded = true },
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                tonalElevation = 1.dp,
                                modifier = Modifier.heightIn(min = 34.dp),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = stringResource(R.string.pl_sort_cd),
                                        modifier = Modifier.size(15.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        stringResource(R.string.pl_sort_label),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false },
                                shape = RoundedCornerShape(22.dp),
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                tonalElevation = 4.dp,
                                shadowElevation = 10.dp,
                                modifier = Modifier.padding(vertical = 4.dp),
                            ) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.pl_sort_newest)) }, onClick = { viewModel.setSortMode(PlaylistSortMode.DATE_DESC); sortMenuExpanded = false })
                                DropdownMenuItem(text = { Text(stringResource(R.string.pl_sort_oldest)) }, onClick = { viewModel.setSortMode(PlaylistSortMode.DATE_ASC); sortMenuExpanded = false })
                                DropdownMenuItem(text = { Text(stringResource(R.string.pl_sort_name)) }, onClick = { viewModel.setSortMode(PlaylistSortMode.NAME); sortMenuExpanded = false })
                                DropdownMenuItem(text = { Text(stringResource(R.string.pl_sort_track_count)) }, onClick = { viewModel.setSortMode(PlaylistSortMode.TRACK_COUNT); sortMenuExpanded = false })
                            }
                        }
                    }
                },
            )

            // Compact low-profile translucent search pill (hides on scroll down, shows on scroll up)
            AnimatedVisibility(
                visible = isSearchBarVisible || searchQuery.isNotEmpty(),
                enter = expandVertically(
                    animationSpec = spring(dampingRatio = 0.9f, stiffness = 700f),
                    expandFrom = Alignment.Top,
                ) + fadeIn(animationSpec = tween(150)),
                exit = shrinkVertically(
                    animationSpec = spring(dampingRatio = 0.9f, stiffness = 700f),
                    shrinkTowards = Alignment.Top,
                ) + fadeOut(animationSpec = tween(120)),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .height(42.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    "Search songs, artists, albums, playlists...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            androidx.compose.foundation.text.BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                ),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }

            val localSongsPlaylist = remember(state.playlists, searchQuery, selectedFilter) {
                if (selectedFilter == PlaylistFilter.ALL || selectedFilter == PlaylistFilter.IMPORTED) {
                    state.playlists.firstOrNull {
                        it.mode == LOCAL_SONGS_MODE && it.matchesPlaylistSearch(searchQuery)
                    }
                } else null
            }

            val filteredPlaylists = remember(state.playlists, searchQuery, selectedFilter) {
                state.playlists
                    .filter { it.mode != LOCAL_SONGS_MODE && it.mode != LIKED_SONGS_MODE && !it.title.equals("Liked Songs", ignoreCase = true) && it.mode !in GENERATED_MODES }
                    .filter { it.matchesPlaylistSearch(searchQuery) }
                    .sortedByDescending { it.isPinned }
            }

            val filteredSpotifyPlaylists = remember(state.spotifyPlaylists, searchQuery, selectedFilter) {
                if (selectedFilter != PlaylistFilter.ALL && selectedFilter != PlaylistFilter.SPOTIFY && selectedFilter != PlaylistFilter.LIKED) {
                    emptyList()
                } else if (selectedFilter == PlaylistFilter.LIKED) {
                    state.spotifyPlaylists.filter { it.id == "liked" && (searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)) }
                } else if (searchQuery.isBlank()) {
                    state.spotifyPlaylists
                } else {
                    state.spotifyPlaylists.filter {
                        it.name.contains(searchQuery, ignoreCase = true)
                    }
                }
            }

            val effectiveSpotify = if (state.isSpotifySyncEnabled) filteredSpotifyPlaylists else emptyList()
            val downloadedCount by viewModel.downloadedCount.collectAsStateWithLifecycle()
            // Downloads lives here now (it left the Home/Stats headers): always
            // shown, except while a search for something else is typed.
            val showDownloadsCard = (searchQuery.isBlank() ||
                "downloads".contains(searchQuery.trim(), ignoreCase = true)) &&
                (selectedFilter == PlaylistFilter.ALL || selectedFilter == PlaylistFilter.IMPORTED)

            val hasHeroRow = false // Removed Local & Downloads hero cards
            val heroCardCount = (if (localSongsPlaylist != null) 1 else 0) + (if (showDownloadsCard) 1 else 0)
            val totalPlaylistCount = effectiveSpotify.size + filteredPlaylists.size
            val hasItemsBelow = totalPlaylistCount > 0
            val normalizedQuery = remember(searchQuery) { normalizeSearchText(searchQuery) }
            val matchingSongs = remember(state.playlists, normalizedQuery) {
                if (normalizedQuery.isBlank()) emptyList()
                else {
                    val tokens = normalizedQuery.split(Regex("\\s+")).filter { it.isNotBlank() }
                    if (tokens.isEmpty()) emptyList()
                    else state.playlists
                        .flatMap { pl ->
                            pl.tracks.mapNotNull { t ->
                                val nameNorm = normalizeSearchText(t.name)
                                val artistNorm = normalizeSearchText(t.artist)
                                val albumNorm = normalizeSearchText(t.album.orEmpty())
                                val plNorm = normalizeSearchText(pl.title)
                                val haystack = "$nameNorm $artistNorm $albumNorm $plNorm"
                                if (tokens.all { tok -> haystack.contains(tok) }) {
                                    val score = when {
                                        nameNorm.startsWith(normalizedQuery) || artistNorm.startsWith(normalizedQuery) -> 0
                                        nameNorm.contains(normalizedQuery) || artistNorm.contains(normalizedQuery) -> 1
                                        else -> 2
                                    }
                                    Triple(score, pl.title, t)
                                } else null
                            }
                        }
                        .sortedBy { it.first }
                        .distinctBy { (_, _, t) -> "${t.name.lowercase()}|${t.artist.lowercase()}" }
                        .map { (_, plTitle, t) -> plTitle to t }
                        .take(60)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            ) {
                when {
                    state.isLoading && state.playlists.isEmpty() && !state.isGenerating -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            repeat(2) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    repeat(2) {
                                        Column(Modifier.weight(1f)) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.35f)),
                                            )
                                            Spacer(Modifier.height(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(0.7f)
                                                    .height(14.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.25f)),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    searchQuery.isBlank() && !hasHeroRow && filteredPlaylists.isEmpty() && !state.isGenerating &&
                        effectiveSpotify.isEmpty() -> EmptyState()
                    else -> LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = FloatingNavDefaults.contentBottomPadding()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (hasHeroRow) {
                            item(key = "heroes_side_by_side", contentType = "heroes_side_by_side") {
                                Box(modifier = Modifier.animateItem()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (localSongsPlaylist != null) {
                                            Box(Modifier.weight(1f)) {
                                                LocalSongsHeroCard(
                                                    playlist = localSongsPlaylist,
                                                    isPlaying = playbackState.isPlaying,
                                                    playbackSource = playbackState.sourceLabel,
                                                    shape = heroPillShape(
                                                        index = 0,
                                                        count = heroCardCount,
                                                        hasItemsBelow = hasItemsBelow,
                                                    ),
                                                    onClick = { onOpenPlaylist(localSongsPlaylist.id) },
                                                    onPlay = {
                                                        musicPlayer.playQueue(
                                                            localSongsPlaylist.tracks.map(GeneratedTrack::toPlayableTrack),
                                                            startIndex = 0,
                                                            sourceLabel = localSongsPlaylist.title,
                                                            enableRadioContinuation = false,
                                                        )
                                                    },
                                                )
                                            }
                                        }
                                        if (showDownloadsCard) {
                                            Box(Modifier.weight(1f)) {
                                                DownloadsHeroCard(
                                                    downloadedCount = downloadedCount,
                                                    shape = heroPillShape(
                                                        index = if (localSongsPlaylist != null) 1 else 0,
                                                        count = heroCardCount,
                                                        hasItemsBelow = hasItemsBelow,
                                                    ),
                                                    onClick = viewModel::openDownloads,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (state.isGenerating) {
                            item(key = "generationProgress", contentType = "generationProgress") {
                                com.lastwave.app.ui.common.GenerationProgressCard(message = state.generatingMessage)
                            }
                        }

                        if (state.justSavedBannerVisible) {
                            item(key = "banner") {
                                LaunchedEffect(state.justSavedBannerVisible) {
                                    kotlinx.coroutines.delay(3000)
                                    viewModel.dismissJustSavedBanner()
                                }
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        "Playlist saved!",
                                        modifier = Modifier.padding(14.dp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }

                        fun playlistGroupPosition(index: Int): GroupPosition = when {
                            hasHeroRow -> if (index == totalPlaylistCount - 1) GroupPosition.BOTTOM else GroupPosition.MIDDLE
                            else -> com.lastwave.app.ui.common.groupPositionFor(index, totalPlaylistCount)
                        }

                        if (effectiveSpotify.isNotEmpty()) {
                            itemsIndexed(
                                items = effectiveSpotify,
                                key = { _, pl -> "sp_${pl.id}" },
                            ) { index, pl ->
                                Box(Modifier.animateItem()) {
                                    SpotifyPlaylistCard(
                                        playlist = pl,
                                        position = playlistGroupPosition(index),
                                        onClick = {
                                            val routeId = if (pl.id == "liked") "sp_liked" else "sp_${pl.id}"
                                            onOpenFeedPlaylist(routeId)
                                        },
                                    )
                                }
                            }
                        }

                        if (searchQuery.isNotBlank()) {
                            if (filteredPlaylists.isNotEmpty()) {
                                item(key = "search_playlists_header") {
                                    Text(
                                        text = "Playlists (${filteredPlaylists.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp, start = 2.dp, bottom = 2.dp),
                                    )
                                }
                                itemsIndexed(
                                    items = filteredPlaylists,
                                    key = { _, pl -> "search_pl_${pl.id}" },
                                ) { _, playlist ->
                                    val trackCount = playlist.remoteTrackCount ?: playlist.tracks.size
                                    Surface(
                                        onClick = { onOpenPlaylist(playlist.id) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .border(0.5.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
                                            ) {
                                                PlaylistCover(playlist = playlist, modifier = Modifier.fillMaxSize(), cornerRadius = 10.dp)
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    text = playlist.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                Text(
                                                    text = "$trackCount songs",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                        val chunkedPlaylists = filteredPlaylists.chunked(2)
                        itemsIndexed(
                            items = chunkedPlaylists,
                            key = { rowIndex, rowItems -> "pl_row_${rowIndex}_${rowItems.firstOrNull()?.id}" },
                        ) { _, rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth().animateItem(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                rowItems.forEach { playlist ->
                                    val isThisPlaylistPlaying = playbackState.isPlaying &&
                                        playbackState.sourceLabel == playlist.title &&
                                        playlist.tracks.any { t ->
                                            t.name.equals(playbackState.current?.title, ignoreCase = true) &&
                                                t.artist.equals(playbackState.current?.artist, ignoreCase = true)
                                        }
                                    val isSelected = selectedPlaylists.contains(playlist.id)
                                    val isRegenerating = state.regeneratingId == playlist.id
                                    val trackCount = playlist.remoteTrackCount ?: playlist.tracks.size
                                    Column(modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(14.dp))
                                                .border(
                                                    width = if (isSelected) 2.dp else 0.5.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.08f),
                                                    shape = RoundedCornerShape(14.dp),
                                                )
                                                .combinedClickable(
                                                    onClick = {
                                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.ContextClick)
                                                        if (selectedPlaylists.isNotEmpty()) {
                                                            selectedPlaylists = if (isSelected) selectedPlaylists - playlist.id else selectedPlaylists + playlist.id
                                                        } else {
                                                            onOpenPlaylist(playlist.id)
                                                        }
                                                    },
                                                    onLongClick = {
                                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                        selectedPlaylists = if (isSelected) selectedPlaylists - playlist.id else selectedPlaylists + playlist.id
                                                    },
                                                ),
                                        ) {
                                            PlaylistCover(
                                                playlist = playlist,
                                                modifier = Modifier.fillMaxSize(),
                                                cornerRadius = 14.dp,
                                            )
                                            if (isThisPlaylistPlaying) {
                                                com.lastwave.app.ui.player.PlayingWaveBars(
                                                    Modifier.align(Alignment.BottomEnd).padding(8.dp).size(26.dp, 20.dp),
                                                )
                                            } else if (playlist.tracks.isNotEmpty()) {
                                                Surface(
                                                    onClick = {
                                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.ContextClick)
                                                        musicPlayer.playQueue(
                                                            playlist.tracks.map(GeneratedTrack::toPlayableTrack),
                                                            startIndex = 0,
                                                            sourceLabel = playlist.title,
                                                        )
                                                    },
                                                    shape = CircleShape,
                                                    color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f),
                                                    contentColor = androidx.compose.ui.graphics.Color.White,
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .padding(8.dp)
                                                        .size(32.dp),
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Filled.PlayArrow,
                                                            contentDescription = "Play ${playlist.title}",
                                                            modifier = Modifier.size(18.dp),
                                                        )
                                                    }
                                                }
                                            }
                                            if (isRegenerating) {
                                                Surface(
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.62f),
                                                    modifier = Modifier.fillMaxSize(),
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        com.lastwave.app.ui.common.ExpressiveInlineLoadingIndicator(
                                                            size = 28.dp,
                                                            color = androidx.compose.ui.graphics.Color.White,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        var cardMenuExpanded by remember(playlist.id) { mutableStateOf(false) }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 0.dp),
                                        ) {
                                            if (playlist.isPinned) {
                                                Icon(
                                                    imageVector = Icons.Filled.PushPin,
                                                    contentDescription = "Pinned",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(12.dp).padding(end = 3.dp),
                                                )
                                            }
                                            Text(
                                                text = playlist.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f),
                                            )
                                            Box {
                                                IconButton(
                                                    onClick = { cardMenuExpanded = true },
                                                    modifier = Modifier.size(24.dp),
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.MoreVert,
                                                        contentDescription = "Playlist options",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                }
                                                DropdownMenu(
                                                    expanded = cardMenuExpanded,
                                                    onDismissRequest = { cardMenuExpanded = false },
                                                    shape = RoundedCornerShape(18.dp),
                                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text(if (playlist.isPinned) "Unpin from top" else "Pin to top") },
                                                        leadingIcon = {
                                                            Icon(
                                                                Icons.Filled.PushPin,
                                                                contentDescription = null,
                                                                tint = if (playlist.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                            )
                                                        },
                                                        onClick = {
                                                            cardMenuExpanded = false
                                                            viewModel.togglePinned(playlist.id)
                                                        },
                                                    )
                                                    if (playlist.tracks.isNotEmpty()) {
                                                        DropdownMenuItem(
                                                            text = { Text("Play") },
                                                            leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                                                            onClick = {
                                                                cardMenuExpanded = false
                                                                musicPlayer.playQueue(
                                                                    playlist.tracks.map(GeneratedTrack::toPlayableTrack),
                                                                    startIndex = 0,
                                                                    sourceLabel = playlist.title,
                                                                )
                                                            },
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text("Play next") },
                                                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
                                                            onClick = {
                                                                cardMenuExpanded = false
                                                                musicPlayer.playNext(playlist.tracks.map(GeneratedTrack::toPlayableTrack))
                                                            },
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text("Add to queue") },
                                                            leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                                                            onClick = {
                                                                cardMenuExpanded = false
                                                                musicPlayer.addToQueue(playlist.tracks.map(GeneratedTrack::toPlayableTrack))
                                                            },
                                                        )
                                                    }
                                                    DropdownMenuItem(
                                                        text = { Text("Rename") },
                                                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                                        onClick = {
                                                            cardMenuExpanded = false
                                                            viewModel.requestRename(playlist.id)
                                                        },
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Change cover") },
                                                        leadingIcon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) },
                                                        onClick = {
                                                            cardMenuExpanded = false
                                                            coverEditorPlaylistId = playlist.id
                                                        },
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                                        leadingIcon = {
                                                            Icon(
                                                                Icons.Filled.Delete,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.error,
                                                            )
                                                        },
                                                        onClick = {
                                                            cardMenuExpanded = false
                                                            viewModel.requestDelete(playlist.id)
                                                        },
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "$trackCount songs",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            modifier = Modifier.padding(start = 2.dp),
                                        )
                                    }
                                }
                                if (rowItems.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                        }

                        if (searchQuery.isNotBlank()) {
                            if (matchingSongs.isNotEmpty()) {
                                item(key = "library_matching_songs_header") {
                                    Text(
                                        text = "Songs in Library (${matchingSongs.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 8.dp, start = 4.dp, bottom = 2.dp),
                                    )
                                }
                                itemsIndexed(
                                    items = matchingSongs,
                                    key = { idx, (plTitle, t) -> "lib_song_${plTitle}_${t.name}_${t.artist}_$idx" },
                                ) { _, (plTitle, track) ->
                                    val playable = track.toPlayableTrack()
                                    com.lastwave.app.ui.player.SwipeableTrackRow(
                                        onSwipeRightPlayNext = { musicPlayer.playNext(playable) },
                                        onSwipeLeftAddToQueue = { musicPlayer.addToQueue(playable) },
                                    ) {
                                        Surface(
                                            onClick = {
                                                musicPlayer.play(playable, sourceLabel = plTitle, startRadio = true)
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                ArtworkImage(
                                                    name = track.name,
                                                    artist = track.artist,
                                                    embeddedUrl = track.artworkUrl,
                                                    fallbackIcon = Icons.Filled.MusicNote,
                                                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),
                                                )
                                                Spacer(Modifier.width(12.dp))
                                                Column(Modifier.weight(1f)) {
                                                    Text(
                                                        text = track.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                    Text(
                                                        text = listOfNotNull(track.artist, track.album?.takeIf { it.isNotBlank() }, plTitle).joinToString(" • "),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                }
                                                Icon(
                                                    imageVector = Icons.Filled.PlayArrow,
                                                    contentDescription = "Play",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (state.isIndexingTracks) {
                                item(key = "library_indexing_hint") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        com.lastwave.app.ui.common.ExpressiveInlineLoadingIndicator(size = 20.dp)
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = "Indexing songs across your playlists...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            } else if (matchingSongs.isEmpty() && filteredPlaylists.isEmpty() && effectiveSpotify.isEmpty()) {
                                item(key = "library_search_empty") {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text(
                                            text = "No matches for \"${searchQuery.trim()}\"",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "Try a different song, artist, album, or playlist name",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showMultipleDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showMultipleDeleteConfirm = false },
                title = { Text(stringResource(R.string.pl_delete_multi_title)) },
                text = { Text(stringResource(R.string.pl_delete_multi_text, selectedPlaylists.size)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteMultiple(selectedPlaylists)
                            selectedPlaylists = setOf()
                            showMultipleDeleteConfirm = false
                        }
                    ) {
                        Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showMultipleDeleteConfirm = false }) { Text(stringResource(R.string.common_cancel)) }
                },
            )
        }

        state.toastMessage?.let { msg ->
            LaunchedEffect(msg) {
                kotlinx.coroutines.delay(3000)
                viewModel.dismissToast()
            }
            Surface(
                shape = ExpressivePillShape,
                color = MaterialTheme.colorScheme.inverseSurface,
                modifier = Modifier.align(Alignment.BottomCenter)
                    .padding(bottom = FloatingNavDefaults.contentBottomPadding() + 12.dp),
            ) {
                Text(msg, color = MaterialTheme.colorScheme.inverseOnSurface, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
            }
        }
    }

    // ── Export bottom sheet (§4.6) ──
    state.exportSheetForPlaylistId?.let { id ->
        ExportBottomSheet(
            onDismiss = viewModel::dismissExportSheet,
            onSaveCsv = { viewModel.exportSave(id, ExportFormat.CSV) },
            onSaveM3u = { viewModel.exportSave(id, ExportFormat.M3U) },
            onShareCsv = { viewModel.exportShare(id, ExportFormat.CSV) },
            onShareM3u = { viewModel.exportShare(id, ExportFormat.M3U) },
        )
    }

    // ── Delete confirm dialog ──
    if (state.deleteConfirmForPlaylistId != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteConfirm,
            title = { Text(stringResource(R.string.pl_delete_single_title)) },
            text = { Text(stringResource(R.string.pl_delete_undone)) },
            confirmButton = { TextButton(onClick = viewModel::confirmDelete) { Text(stringResource(R.string.common_delete)) } },
            dismissButton = { TextButton(onClick = viewModel::dismissDeleteConfirm) { Text(stringResource(R.string.common_cancel)) } },
        )
    }

    // ── Delete-scrobble authorization-required dialog ──
    if (state.createDialogVisible) {
        var title by remember(state.createDialogVisible) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = viewModel::dismissCreateDialog,
            title = { Text(stringResource(R.string.pl_create_title)) },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.pl_name_label)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.createCustomPlaylist(title) }, enabled = title.isNotBlank()) { Text(stringResource(R.string.common_create)) }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissCreateDialog) { Text(stringResource(R.string.common_cancel)) } },
        )
    }

    state.renamePlaylistId?.let { playlistId ->
        val playlist = state.playlists.firstOrNull { it.id == playlistId }
        var title by remember(playlistId) { mutableStateOf(playlist?.title.orEmpty()) }
        AlertDialog(
            onDismissRequest = viewModel::dismissRename,
            title = { Text(stringResource(R.string.pl_rename_title)) },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.pl_name_label)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.renamePlaylist(title) }, enabled = title.isNotBlank()) { Text(stringResource(R.string.common_rename)) }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissRename) { Text(stringResource(R.string.common_cancel)) } },
        )
    }

    coverEditorPlaylistId?.let { playlistId ->
        state.playlists.firstOrNull { it.id == playlistId }?.let { playlist ->
            AlertDialog(
                onDismissRequest = { coverEditorPlaylistId = null },
                title = { Text(stringResource(R.string.pl_cover_title)) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        PlaylistCover(playlist = playlist, modifier = Modifier.size(132.dp), cornerRadius = 26.dp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (playlist.customCoverUri.isNullOrBlank()) {
                                stringResource(R.string.pl_cover_auto_desc)
                            } else {
                                stringResource(R.string.pl_cover_custom_desc)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            coverPickerPlaylistId = playlist.id
                            coverEditorPlaylistId = null
                            coverPicker.launch(arrayOf("image/*"))
                        },
                    ) {
                        Text(if (playlist.customCoverUri.isNullOrBlank()) stringResource(R.string.pl_choose_image) else stringResource(R.string.pl_change_image))
                    }
                },
                dismissButton = {
                    Row {
                        if (!playlist.customCoverUri.isNullOrBlank()) {
                            TextButton(
                                onClick = {
                                    viewModel.setCustomCover(playlist.id, null)
                                    coverEditorPlaylistId = null
                                },
                            ) { Text(stringResource(R.string.pl_use_automatic)) }
                        }
                        TextButton(onClick = { coverEditorPlaylistId = null }) { Text(stringResource(R.string.common_done)) }
                    }
                },
            )
        }
    }

    if (state.deleteScrobbleAuthRequired) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteScrobbleAuthRequired,
            title = { Text(stringResource(R.string.pl_scrobble_auth_title)) },
            text = { Text(stringResource(R.string.pl_scrobble_auth_text)) },
            confirmButton = { TextButton(onClick = viewModel::dismissDeleteScrobbleAuthRequired) { Text(stringResource(R.string.pl_scrobble_auth_ok)) } },
        )
    }

    // ── Shared track context menu (§1.7) ──
    menuTarget?.let { (playlistId, track) ->
        TrackContextMenuSheet(
            target = TrackMenuTarget.Track(track.name, track.artist, track.url),
            capabilities = TrackMenuCapabilities(showDeleteScrobble = true),
            playbackSourceLabel = state.playlists.firstOrNull { it.id == playlistId }?.title ?: "Playlists",
            onDismiss = { menuTarget = null },
            onDeleteScrobble = { name, artist -> viewModel.deleteScrobble(name, artist) },
            onRefreshArtwork = { viewModel.refreshArtwork(track.name, track.artist) },
        )
    }
}

private fun normalizeSearchText(text: String): String {
    if (text.isBlank()) return ""
    val decomposed = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
    return decomposed.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "").lowercase().trim()
}

private fun SavedPlaylist.matchesPlaylistSearch(query: String): Boolean {
    if (query.isBlank()) return true
    val normalizedQuery = normalizeSearchText(query)
    val tokens = normalizedQuery.split(Regex("\\s+")).filter { it.isNotBlank() }
    if (tokens.isEmpty()) return true
    val titleLower = normalizeSearchText(title)
    if (tokens.all { titleLower.contains(it) }) return true
    return tracks.any { t ->
        val haystack = normalizeSearchText("${t.name} ${t.artist} ${t.album.orEmpty()} $title")
        tokens.all { haystack.contains(it) }
    }
}
