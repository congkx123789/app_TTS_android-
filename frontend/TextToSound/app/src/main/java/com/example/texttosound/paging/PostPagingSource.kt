package com.example.texttosound.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.texttosound.api.AppApiService
import com.example.texttosound.api.Post

class PostPagingSource(
    private val apiService: AppApiService
) : PagingSource<Int, Post>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Post> {
        val position = params.key ?: 1
        return try {
            val response = apiService.getPosts() // Currently API doesn't support pagination, but we wrap it
            val posts = if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
            
            // Simulating pagination for now as the backend returns all posts
            LoadResult.Page(
                data = posts,
                prevKey = if (position == 1) null else position - 1,
                nextKey = if (posts.isEmpty()) null else position + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Post>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
