package com.prod.singles_date.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.prod.singles_date.model.AppNotification
import com.prod.singles_date.model.BlockedUser
import com.prod.singles_date.model.CityMood
import com.prod.singles_date.model.PostType
import com.prod.singles_date.model.Thought
import com.prod.singles_date.model.ThoughtLoadState
import com.prod.singles_date.model.Comment
import com.prod.singles_date.repository.ThoughtRepository
import com.prod.singles_date.util.AnalyticsEvents
import com.prod.singles_date.util.FeedRanking
import com.prod.singles_date.util.FeedSortMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ThoughtViewModel @Inject constructor(
    private val repository: ThoughtRepository,
    private val analytics: FirebaseAnalytics,
) : ViewModel() {

    sealed class UiEvent {
        data class Message(val text: String) : UiEvent()
        data class Error(val text: String) : UiEvent()
    }

    private val _uiEvents = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvents = _uiEvents.receiveAsFlow()

    private suspend fun notifyError(error: Throwable) {
        _uiEvents.send(UiEvent.Error(humanizePostError(error)))
    }

    private suspend fun notifyMessage(text: String) {
        _uiEvents.send(UiEvent.Message(text))
    }

    private val _selectedCity = MutableStateFlow("")
    val selectedCity: StateFlow<String> = _selectedCity.asStateFlow()

    fun setSelectedCity(city: String) {
        _selectedCity.value = city
    }

    /** Optional feed scope; blank = all cities (early-audience default). */
    private val _feedCityFilter = MutableStateFlow("")
    val feedCityFilter: StateFlow<String> = _feedCityFilter.asStateFlow()

    fun setFeedCityFilter(city: String) {
        val cityChanged = _feedCityFilter.value != city
        _feedCityFilter.value = city
        if (city.isBlank() || cityChanged) {
            _feedLocalityFilter.value = ""
        }
        _displayLimit.value = repository.pageSize()
    }

    private val _selectedLocality = MutableStateFlow("")
    val selectedLocality: StateFlow<String> = _selectedLocality.asStateFlow()

    fun setSelectedLocality(locality: String) {
        _selectedLocality.value = locality
    }

    private val _feedLocalityFilter = MutableStateFlow("")
    val feedLocalityFilter: StateFlow<String> = _feedLocalityFilter.asStateFlow()

    fun setFeedLocalityFilter(locality: String) {
        _feedLocalityFilter.value = locality
        _displayLimit.value = repository.pageSize()
    }

    private val _feedCategoryFilter = MutableStateFlow("")
    val feedCategoryFilter: StateFlow<String> = _feedCategoryFilter.asStateFlow()

    fun setFeedCategoryFilter(category: String) {
        _feedCategoryFilter.value = category
        _displayLimit.value = repository.pageSize()
    }

    private val _feedSortMode = MutableStateFlow(FeedSortMode.NEW)
    val feedSortMode: StateFlow<FeedSortMode> = _feedSortMode.asStateFlow()

    fun setFeedSortMode(mode: FeedSortMode) {
        _feedSortMode.value = mode
    }

    private val _snapsOnlyFilter = MutableStateFlow(false)
    val snapsOnlyFilter: StateFlow<Boolean> = _snapsOnlyFilter.asStateFlow()

    fun setSnapsOnlyFilter(enabled: Boolean) {
        _snapsOnlyFilter.value = enabled
    }

    private val _workOnlyFilter = MutableStateFlow(false)
    val workOnlyFilter: StateFlow<Boolean> = _workOnlyFilter.asStateFlow()

    fun setWorkOnlyFilter(enabled: Boolean) {
        _workOnlyFilter.value = enabled
        if (enabled) _feedCategoryFilter.value = "work"
        else if (_feedCategoryFilter.value == "work") _feedCategoryFilter.value = ""
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query.trim()
    }

    private val _refreshNonce = MutableStateFlow(0)
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refreshFeed() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _refreshNonce.value += 1
            _displayLimit.value = repository.pageSize()
            delay(400)
            _isRefreshing.value = false
        }
    }

    private val _displayLimit = MutableStateFlow(repository.pageSize())
    val displayLimit: StateFlow<Int> = _displayLimit.asStateFlow()

    fun loadMoreFeed() {
        _displayLimit.value += repository.pageSize()
    }

    private val _isPosting = MutableStateFlow(false)
    val isPosting: StateFlow<Boolean> = _isPosting.asStateFlow()

    private val _postError = MutableStateFlow<String?>(null)
    val postError: StateFlow<String?> = _postError.asStateFlow()

    fun clearPostError() {
        _postError.value = null
    }

    private data class FeedQuery(val city: String, val locality: String, val category: String, val nonce: Int)

    val thoughts: StateFlow<List<Thought>> =
        combine(_feedCityFilter, _feedLocalityFilter, _feedCategoryFilter, _refreshNonce) { city, locality, category, nonce ->
            FeedQuery(city, locality, category, nonce)
        }
            .flatMapLatest { q -> repository.thoughtsFlow(q.city, q.locality, q.category) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val cityMood: StateFlow<CityMood?> =
        _selectedCity
            .flatMapLatest { city -> repository.cityMoodFlow(city) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _pendingPostPrompt = MutableStateFlow<String?>(null)
    val pendingPostPrompt: StateFlow<String?> = _pendingPostPrompt.asStateFlow()

    fun setPendingPostPrompt(prompt: String?) {
        _pendingPostPrompt.value = prompt
    }

    fun clearPendingPostPrompt() {
        _pendingPostPrompt.value = null
    }

    private val _pendingOpenPostId = MutableStateFlow<String?>(null)
    val pendingOpenPostId: StateFlow<String?> = _pendingOpenPostId.asStateFlow()

    fun setPendingOpenPostId(thoughtId: String?) {
        _pendingOpenPostId.value = thoughtId?.takeIf { it.isNotBlank() }
    }

    fun clearPendingOpenPostId() {
        _pendingOpenPostId.value = null
    }

    private val _requestOpenComposer = MutableStateFlow(false)
    val requestOpenComposer: StateFlow<Boolean> = _requestOpenComposer.asStateFlow()

    fun requestOpenComposer() {
        _requestOpenComposer.value = true
    }

    fun clearOpenComposerRequest() {
        _requestOpenComposer.value = false
    }

    private val _activeDetailThoughtId = MutableStateFlow("")
    val detailLoadState: StateFlow<ThoughtLoadState> =
        _activeDetailThoughtId
            .flatMapLatest { id ->
                if (id.isBlank()) kotlinx.coroutines.flow.flowOf(ThoughtLoadState.NotFound)
                else repository.thoughtDetailFlow(id)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThoughtLoadState.Loading)

    val detailComments: StateFlow<List<Comment>> =
        _activeDetailThoughtId
            .flatMapLatest { id ->
                if (id.isBlank()) kotlinx.coroutines.flow.flowOf(emptyList())
                else repository.commentsFlow(id)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setActiveDetailThoughtId(thoughtId: String) {
        _activeDetailThoughtId.value = thoughtId
    }

    fun clearActiveDetailThoughtId() {
        _activeDetailThoughtId.value = ""
    }

    private val _currentUid = MutableStateFlow<String?>(null)
    /** Immediate UI overrides until Firestore listener catches up. */
    private val _feelOverrides = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    /** Optimistic feel/comment count deltas until Firestore catches up. */
    private val _feelCountAdjustments = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _commentCountAdjustments = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _feelCountBase = mutableMapOf<String, Int>()

    private val thoughtsWithLiveCounts: StateFlow<List<Thought>> =
        combine(thoughts, _feelCountAdjustments, _commentCountAdjustments) { list, feelAdj, commentAdj ->
            list.map { applyCountAdjustments(it, feelAdj, commentAdj) }
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val detailThought: StateFlow<Thought?> =
        combine(
            detailLoadState.map { state -> (state as? ThoughtLoadState.Ready)?.thought },
            _feelCountAdjustments,
            _commentCountAdjustments,
        ) { thought, feelAdj, commentAdj ->
            thought?.let { applyCountAdjustments(it, feelAdj, commentAdj) }
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            thoughts.collect { list ->
                _feelCountAdjustments.update { adj ->
                    if (adj.isEmpty()) adj
                    else adj.filter { (id, delta) ->
                        val base = _feelCountBase[id]
                        val server = list.find { it.id == id }?.feelCount
                        if (base != null && server != null && server >= base + delta) {
                            _feelCountBase.remove(id)
                            false
                        } else {
                            true
                        }
                    }
                }
            }
        }
    }

    fun setCurrentUid(uid: String?) {
        _currentUid.value = uid
        if (uid.isNullOrBlank()) {
            _feelOverrides.value = emptyMap()
            _feelCountAdjustments.value = emptyMap()
            _commentCountAdjustments.value = emptyMap()
            _feelCountBase.clear()
        }
    }

    val feeledThoughtIds: StateFlow<Set<String>> =
        combine(
            _currentUid.flatMapLatest { uid ->
                if (uid.isNullOrBlank()) kotlinx.coroutines.flow.flowOf(emptySet())
                else repository.feeledThoughtIdsFlow(uid)
            },
            _feelOverrides,
        ) { fromServer, overrides ->
            buildSet {
                addAll(fromServer)
                overrides.forEach { (thoughtId, felt) ->
                    if (felt) add(thoughtId) else remove(thoughtId)
                }
            }
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val savedThoughtIds: StateFlow<Set<String>> =
        _currentUid
            .flatMapLatest { uid ->
                if (uid.isNullOrBlank()) kotlinx.coroutines.flow.flowOf(emptySet())
                else repository.savedThoughtIdsFlow(uid)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val notifications: StateFlow<List<AppNotification>> =
        _currentUid
            .flatMapLatest { uid ->
                if (uid.isNullOrBlank()) kotlinx.coroutines.flow.flowOf(emptyList())
                else repository.notificationsFlow(uid)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unreadNotificationCount: StateFlow<Int> =
        notifications
            .map { list -> list.count { !it.read } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _publicProfileUid = MutableStateFlow("")
    val publicProfileThoughts: StateFlow<List<Thought>> =
        _publicProfileUid
            .flatMapLatest { uid ->
                if (uid.isBlank()) kotlinx.coroutines.flow.flowOf(emptyList())
                else repository.authorThoughtsFlow(uid)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setPublicProfileUid(uid: String) {
        _publicProfileUid.value = uid
    }

    fun clearPublicProfileUid() {
        _publicProfileUid.value = ""
    }

    private val hiddenThoughtIds: StateFlow<Set<String>> =
        _currentUid
            .flatMapLatest { uid ->
                if (uid.isNullOrBlank()) kotlinx.coroutines.flow.flowOf(emptySet())
                else repository.hiddenThoughtIdsFlow(uid)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val blockedUids: StateFlow<Set<String>> =
        _currentUid
            .flatMapLatest { uid ->
                if (uid.isNullOrBlank()) kotlinx.coroutines.flow.flowOf(emptySet())
                else repository.blockedUidsFlow(uid)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val blockedUsers: StateFlow<List<BlockedUser>> =
        blockedUids
            .flatMapLatest { uids ->
                flow {
                    val list = withContext(Dispatchers.IO) {
                        uids.map { blockedUid ->
                            val name = runCatching { repository.getUserDisplayName(blockedUid) }
                                .getOrElse { "User" }
                            BlockedUser(blockedUid, name)
                        }
                    }
                    emit(list)
                }
            }
            .flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val rankedThoughtsRaw: StateFlow<List<Thought>> =
        thoughtsWithLiveCounts
            .combine(hiddenThoughtIds) { thoughtList, hidden ->
                FeedRankingContext(
                    thoughts = thoughtList,
                    hidden = hidden,
                    blocked = emptySet(),
                    city = "",
                    userLocality = "",
                    sortMode = FeedSortMode.NEW,
                )
            }
            .combine(blockedUids) { ctx, blocked -> ctx.copy(blocked = blocked) }
            .combine(_feedCityFilter) { ctx, city -> ctx.copy(city = city) }
            .combine(_selectedLocality) { ctx, userLocality -> ctx.copy(userLocality = userLocality) }
            .combine(_feedSortMode) { ctx, sortMode -> ctx.copy(sortMode = sortMode) }
            .map { ctx -> rankVisibleThoughts(ctx) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val canLoadMore: StateFlow<Boolean> =
        combine(_displayLimit, rankedThoughtsRaw) { limit, all -> all.size > limit }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val visibleThoughts: StateFlow<List<Thought>> =
        rankedThoughtsRaw
            .combine(_snapsOnlyFilter) { thoughts, snapsOnly ->
                if (snapsOnly) thoughts.filter { it.imageUrls.isNotEmpty() } else thoughts
            }
            .combine(_workOnlyFilter) { thoughts, workOnly ->
                if (workOnly) thoughts.filter { it.category == "work" || it.category == "startup" } else thoughts
            }
            .combine(_searchQuery) { thoughts, query ->
                if (query.isBlank()) thoughts
                else thoughts.filter { thought ->
                    thought.text.contains(query, ignoreCase = true) ||
                        thought.authorName.contains(query, ignoreCase = true) ||
                        thought.locality.contains(query, ignoreCase = true)
                }
            }
            .combine(_displayLimit) { thoughts, limit -> thoughts.take(limit) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _activeCommentsThoughtId = MutableStateFlow<String?>(null)
    val activeCommentsThoughtId: StateFlow<String?> = _activeCommentsThoughtId.asStateFlow()

    val activeComments: StateFlow<List<Comment>> =
        activeCommentsThoughtId
            .flatMapLatest { thoughtId ->
                if (thoughtId.isNullOrBlank()) kotlinx.coroutines.flow.flowOf(emptyList())
                else repository.commentsFlow(thoughtId)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setCommentsTarget(thoughtId: String?) {
        _activeCommentsThoughtId.value = thoughtId
    }

    fun onFeelThis(thoughtId: String, uid: String) {
        if (uid.isBlank() || thoughtId.isBlank()) return
        viewModelScope.launch {
            val currentlyFeeled = thoughtId in feeledThoughtIds.value
            val targetFeeled = !currentlyFeeled
            val delta = if (targetFeeled) 1 else -1
            val serverFeelCount = thoughts.value.find { it.id == thoughtId }?.feelCount ?: 0
            _feelCountBase[thoughtId] = serverFeelCount
            _feelOverrides.update { it + (thoughtId to targetFeeled) }
            _feelCountAdjustments.update { adjustments ->
                adjustments + (thoughtId to ((adjustments[thoughtId] ?: 0) + delta))
            }
            runCatching { repository.toggleFeel(thoughtId, uid) }
                .onSuccess { actualFeeled ->
                    if (actualFeeled != targetFeeled) {
                        _feelOverrides.update { it + (thoughtId to actualFeeled) }
                        val correction = if (actualFeeled) 1 else -1
                        _feelCountAdjustments.update { adjustments ->
                            adjustments + (thoughtId to ((adjustments[thoughtId] ?: 0) - delta + correction))
                        }
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "toggleFeel failed thoughtId=$thoughtId", error)
                    _feelOverrides.update { it - thoughtId }
                    _feelCountAdjustments.update { adjustments ->
                        adjustments + (thoughtId to ((adjustments[thoughtId] ?: 0) - delta))
                    }
                    _feelCountBase.remove(thoughtId)
                }
        }
    }

    fun toggleSaveThought(uid: String, thoughtId: String, save: Boolean) {
        viewModelScope.launch { runCatching { repository.toggleSaveThought(uid, thoughtId, save) } }
    }

    fun markNotificationRead(uid: String, notificationId: String) {
        viewModelScope.launch { runCatching { repository.markNotificationRead(uid, notificationId) } }
    }

    fun submitSponsorLead(
        uid: String,
        businessName: String,
        email: String,
        city: String,
        budget: String,
        message: String,
        onDone: (Boolean) -> Unit = {},
    ) {
        viewModelScope.launch {
            val result = runCatching {
                repository.submitSponsorLead(uid, businessName, email, city, budget, message)
            }
            onDone(result.isSuccess)
        }
    }

    fun postThought(
        rawText: String,
        authorId: String,
        authorName: String,
        authorPhotoUrl: String = "",
        city: String,
        locality: String,
        category: String,
        postType: String = PostType.SNAP,
        context: Context,
        imageUris: List<Uri> = emptyList(),
    ) {
        val maxLen = PostType.maxLength(postType)
        val text = rawText.trim().take(maxLen)
        if ((text.isBlank() && imageUris.isEmpty()) || city.isBlank()) return
        viewModelScope.launch {
            _isPosting.value = true
            _postError.value = null
            runCatching {
                repository.postThought(
                    text = text,
                    authorId = authorId,
                    authorName = authorName,
                    authorPhotoUrl = authorPhotoUrl,
                    city = city,
                    locality = locality,
                    category = category,
                    postType = postType,
                    context = context,
                    imageUris = imageUris,
                )
            }.onSuccess { thoughtId ->
                if (thoughtId.isNotBlank()) {
                    setPendingOpenPostId(thoughtId)
                }
                AnalyticsEvents.logPostCreated(analytics, city, imageUris.isNotEmpty())
            }.onFailure { error ->
                _postError.value = humanizePostError(error)
            }
            _isPosting.value = false
        }
    }

    fun incrementShareCount(thoughtId: String) {
        viewModelScope.launch { runCatching { repository.incrementShareCount(thoughtId) } }
    }

    fun reportThought(thoughtId: String, reporterUid: String, reason: String = "Inappropriate") {
        if (reporterUid.isBlank()) return
        viewModelScope.launch { runCatching { repository.reportThought(thoughtId, reporterUid, reason) } }
    }

    fun blockAuthor(blockerUid: String, blockedUid: String) {
        if (blockerUid.isBlank() || blockedUid.isBlank()) return
        viewModelScope.launch { runCatching { repository.blockUser(blockerUid, blockedUid) } }
    }

    fun unblockAuthor(blockerUid: String, blockedUid: String) {
        if (blockerUid.isBlank() || blockedUid.isBlank()) return
        viewModelScope.launch { runCatching { repository.unblockUser(blockerUid, blockedUid) } }
    }

    fun reportComment(thoughtId: String, commentId: String, reporterUid: String, reason: String) {
        if (reporterUid.isBlank()) return
        viewModelScope.launch {
            runCatching { repository.reportComment(thoughtId, commentId, reporterUid, reason) }
        }
    }

    fun updateThought(thoughtId: String, rawText: String, allowEmpty: Boolean = false) {
        val text = rawText.trim().take(MAX_THOUGHT_LENGTH)
        if (text.isBlank() && !allowEmpty) return
        viewModelScope.launch { runCatching { repository.updateThought(thoughtId, text) } }
    }

    fun deleteThought(thoughtId: String, authorId: String) {
        if (thoughtId.isBlank() || authorId.isBlank()) return
        viewModelScope.launch {
            runCatching { repository.deleteThought(thoughtId, authorId) }
                .onFailure { notifyError(it) }
        }
    }

    fun addComment(thoughtId: String, userId: String, userName: String, rawText: String) {
        val body = rawText.trim().take(MAX_COMMENT_LENGTH)
        if (body.isBlank()) return
        viewModelScope.launch {
            _commentCountAdjustments.update { adjustments ->
                adjustments + (thoughtId to ((adjustments[thoughtId] ?: 0) + 1))
            }
            runCatching { repository.addComment(thoughtId, userId, userName, body) }
                .onFailure { error ->
                    _commentCountAdjustments.update { adjustments ->
                        adjustments + (thoughtId to ((adjustments[thoughtId] ?: 0) - 1))
                    }
                    notifyError(error)
                }
        }
    }

    fun updateComment(thoughtId: String, commentId: String, rawText: String) {
        val body = rawText.trim().take(MAX_COMMENT_LENGTH)
        if (body.isBlank() || thoughtId.isBlank() || commentId.isBlank()) return
        viewModelScope.launch {
            runCatching { repository.updateComment(thoughtId, commentId, body) }
                .onSuccess { notifyMessage("Comment updated") }
                .onFailure { notifyError(it) }
        }
    }

    fun deleteComment(thoughtId: String, commentId: String) {
        if (thoughtId.isBlank() || commentId.isBlank()) return
        viewModelScope.launch {
            _commentCountAdjustments.update { adjustments ->
                adjustments + (thoughtId to ((adjustments[thoughtId] ?: 0) - 1))
            }
            runCatching { repository.deleteComment(thoughtId, commentId) }
                .onSuccess { notifyMessage("Comment deleted") }
                .onFailure { error ->
                    _commentCountAdjustments.update { adjustments ->
                        adjustments + (thoughtId to ((adjustments[thoughtId] ?: 0) + 1))
                    }
                    notifyError(error)
                }
        }
    }

    suspend fun fetchUserProfile(uid: String) = repository.getUserProfile(uid)

    private fun applyCountAdjustments(
        thought: Thought,
        feelAdj: Map<String, Int>,
        commentAdj: Map<String, Int>,
    ): Thought {
        val feelDelta = feelAdj[thought.id] ?: 0
        val commentDelta = commentAdj[thought.id] ?: 0
        if (feelDelta == 0 && commentDelta == 0) return thought
        return thought.copy(
            feelCount = (thought.feelCount + feelDelta).coerceAtLeast(0),
            commentCount = (thought.commentCount + commentDelta).coerceAtLeast(0),
        )
    }

    companion object {
        private const val TAG = "ThoughtViewModel"
        const val MAX_THOUGHT_LENGTH = PostType.SNAP_MAX_LENGTH
        const val MAX_NOTE_LENGTH = PostType.NOTE_MAX_LENGTH
        const val MAX_COMMENT_LENGTH = 500
    }
}

private fun humanizePostError(error: Throwable): String {
    val raw = error.message?.trim().orEmpty()
    val msg = raw.lowercase()
    // Prefer the real server/network message so upload issues are diagnosable.
    if (raw.isNotBlank() &&
        !msg.contains("category") &&
        !msg.contains("50 mb") &&
        !msg.contains("too large") &&
        !msg.contains("could not read image")
    ) {
        return raw
    }
    return when {
        msg.contains("category") -> "Pick a category for Local Notes."
        msg.contains("50 mb") || msg.contains("too large") ->
            "Each photo must be 50 MB or smaller. Remove oversized images and try again."
        msg.contains("could not read image") ->
            "Couldn't read one of the photos. Try picking it again from your gallery."
        msg.contains("network") || msg.contains("unavailable") || msg.contains("timeout") ||
            msg.contains("unable to resolve host") ->
            "No internet connection. Check your network and try again."
        msg.contains("permission_denied") || msg.contains("missing or insufficient permissions") ->
            "Post blocked by Firestore rules. Deploy latest firestore.rules, then try again."
        else -> raw.ifBlank { "Couldn't post right now. Please try again." }
    }
}

private data class FeedRankingContext(
    val thoughts: List<Thought>,
    val hidden: Set<String>,
    val blocked: Set<String>,
    val city: String,
    val userLocality: String,
    val sortMode: FeedSortMode,
)

private fun rankVisibleThoughts(ctx: FeedRankingContext): List<Thought> {
    val now = System.currentTimeMillis()
    val filtered = ctx.thoughts.filter { thought ->
        thought.id !in ctx.hidden &&
            thought.authorId !in ctx.blocked &&
            (ctx.city.isBlank() || thought.city.isBlank() || thought.city == ctx.city)
    }
    return if (ctx.userLocality.isNotBlank() && ctx.sortMode == FeedSortMode.HOT) {
        FeedRanking.mixLocalAndCity(filtered, ctx.userLocality, ctx.sortMode, now)
    } else {
        FeedRanking.sortThoughts(filtered, ctx.sortMode, now)
    }
}
