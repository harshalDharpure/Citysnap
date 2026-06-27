package com.prod.singles_date.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prod.singles_date.model.Thought
import com.prod.singles_date.model.User
import com.prod.singles_date.repository.ProfileRepository
import com.prod.singles_date.repository.ThoughtRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val thoughtRepository: ThoughtRepository,
) : ViewModel() {

    private val _uid = MutableStateFlow<String?>(null)
    val uid: StateFlow<String?> = _uid.asStateFlow()

    fun setUid(uid: String?) {
        _uid.value = uid
    }

    val profile: StateFlow<User?> =
        _uid.flatMapLatest { u ->
            if (u.isNullOrBlank()) {
                kotlinx.coroutines.flow.flowOf(null)
            } else {
                profileRepository.userProfileFlow(u)
            }
        }
            .catch { emit(null) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val myThoughts: StateFlow<List<Thought>> =
        _uid.flatMapLatest { u ->
            if (u.isNullOrBlank()) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            } else {
                profileRepository.myThoughtsFlow(u)
            }
        }
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val referralCount: StateFlow<Int> =
        _uid.flatMapLatest { u ->
            if (u.isNullOrBlank()) {
                kotlinx.coroutines.flow.flowOf(0)
            } else {
                profileRepository.referralCountFlow(u)
            }
        }
            .catch { emit(0) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun deleteThought(thoughtId: String, authorId: String) {
        if (thoughtId.isBlank() || authorId.isBlank()) return
        viewModelScope.launch {
            runCatching { thoughtRepository.deleteThought(thoughtId, authorId) }
        }
    }
}

