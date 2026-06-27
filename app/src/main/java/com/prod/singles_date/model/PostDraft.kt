package com.prod.singles_date.model

data class PostDraft(
    val text: String = "",
    val category: String = "",
    val postType: String = PostType.SNAP,
    val imageUris: List<String> = emptyList(),
) {
    fun hasContent(): Boolean = text.isNotBlank() || imageUris.isNotEmpty()
}
