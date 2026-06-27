package com.prod.singles_date.model

sealed class ThoughtLoadState {
    data object Loading : ThoughtLoadState()
    data class Ready(val thought: Thought) : ThoughtLoadState()
    data object NotFound : ThoughtLoadState()
}
