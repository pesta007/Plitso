package com.loki.plitso.data.remote.bookmark

import kotlinx.coroutines.flow.Flow

interface BookmarkApi {
    val bookmarks: Flow<List<String>>

    suspend fun saveBookmark(recipeId: String)

    suspend fun deleteBookmark(recipeId: String)

    suspend fun isBookmarked(recipeId: String): Boolean
}
