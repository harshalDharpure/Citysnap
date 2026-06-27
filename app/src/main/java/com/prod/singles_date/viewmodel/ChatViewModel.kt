package com.prod.singles_date.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prod.singles_date.model.ChatMessage
import com.prod.singles_date.model.ConversationMeta
import com.prod.singles_date.model.User
import com.prod.singles_date.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
) : ViewModel() {

    private val _currentUid = MutableStateFlow("")
    private val _activeConversationId = MutableStateFlow("")
    private val _activeOtherUid = MutableStateFlow("")
    private val _otherUser = MutableStateFlow<User?>(null)
    private val _isSending = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    private val _isSearching = MutableStateFlow(false)

    val error: StateFlow<String?> = _error.asStateFlow()
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()
    val otherUser: StateFlow<User?> = _otherUser.asStateFlow()
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    val inbox: StateFlow<List<ConversationMeta>> =
        _currentUid
            .flatMapLatest { uid ->
                if (uid.isBlank()) kotlinx.coroutines.flow.flowOf(emptyList())
                else repository.inboxFlow(uid)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalUnreadCount: StateFlow<Int> =
        inbox
            .map { list -> list.sumOf { it.unreadCount } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val messages: StateFlow<List<ChatMessage>> =
        _activeConversationId
            .flatMapLatest { id ->
                if (id.isBlank()) kotlinx.coroutines.flow.flowOf(emptyList())
                else repository.messagesFlow(id)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _profileCache = MutableStateFlow<Map<String, User>>(emptyMap())
    val profileCache: StateFlow<Map<String, User>> = _profileCache.asStateFlow()

    init {
        viewModelScope.launch {
            inbox.collect { list ->
                list.forEach { meta ->
                    val otherId = meta.otherUserId
                    if (otherId.isNotBlank() && !_profileCache.value.containsKey(otherId)) {
                        repository.getPublicProfile(otherId)?.let { user ->
                            _profileCache.value = _profileCache.value + (otherId to user)
                        }
                    }
                }
            }
        }
    }

    fun setCurrentUid(uid: String?) {
        _currentUid.value = uid.orEmpty()
        if (uid.isNullOrBlank()) {
            clearActiveChat()
        }
    }

    fun clearError() {
        _error.value = null
    }

    suspend fun canMessage(otherUid: String): Boolean {
        val myUid = _currentUid.value
        if (myUid.isBlank() || otherUid.isBlank() || myUid == otherUid) return false
        return !repository.isBlocked(myUid, otherUid)
    }

    fun openChatWith(otherUid: String, onReady: (conversationId: String) -> Unit) {
        val myUid = _currentUid.value
        if (myUid.isBlank() || otherUid.isBlank() || myUid == otherUid) return
        viewModelScope.launch {
            runCatching {
                if (repository.isBlocked(myUid, otherUid)) {
                    _error.value = "You can't message this user"
                    return@launch
                }
                val conversationId = repository.getOrCreateConversation(myUid, otherUid)
                _activeConversationId.value = conversationId
                _activeOtherUid.value = otherUid
                _otherUser.value = repository.getPublicProfile(otherUid)
                repository.markConversationRead(myUid, conversationId)
                onReady(conversationId)
            }.onFailure {
                _error.value = it.message ?: "Could not open chat"
            }
        }
    }

    fun setActiveChat(conversationId: String, otherUid: String) {
        _activeConversationId.value = conversationId
        _activeOtherUid.value = otherUid
        val myUid = _currentUid.value
        viewModelScope.launch {
            _otherUser.value = repository.getPublicProfile(otherUid)
            if (myUid.isNotBlank()) {
                repository.markConversationRead(myUid, conversationId)
            }
        }
    }

    fun clearActiveChat() {
        _activeConversationId.value = ""
        _activeOtherUid.value = ""
        _otherUser.value = null
    }

    fun sendMessage(text: String) {
        val myUid = _currentUid.value
        val conversationId = _activeConversationId.value
        val body = text.trim()
        if (myUid.isBlank() || conversationId.isBlank() || body.isBlank()) return
        viewModelScope.launch {
            _isSending.value = true
            runCatching { repository.sendMessage(conversationId, myUid, body) }
                .onFailure { _error.value = it.message ?: "Failed to send message" }
            _isSending.value = false
        }
    }

    fun reportMessage(messageId: String, reason: String) {
        val myUid = _currentUid.value
        val conversationId = _activeConversationId.value
        val reportedUid = _activeOtherUid.value
        if (myUid.isBlank() || conversationId.isBlank() || messageId.isBlank()) return
        viewModelScope.launch {
            runCatching {
                repository.reportMessage(myUid, conversationId, messageId, reportedUid, reason)
            }.onFailure {
                _error.value = it.message ?: "Failed to report message"
            }
        }
    }

    fun searchUsers(city: String, query: String) {
        val myUid = _currentUid.value
        if (city.isBlank() || query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            _searchResults.value = runCatching {
                repository.searchUsersInCity(city, query, myUid)
            }.getOrElse { emptyList() }
            _isSearching.value = false
        }
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
    }
}
