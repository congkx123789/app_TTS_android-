package com.example.texttosound.api

import com.google.gson.annotations.SerializedName

data class User(
    val id: Int,
    val name: String,
    @SerializedName("avatar_url") val avatarUrl: String?,
    val bio: String?,
    @SerializedName("days_on_platform") val daysOnPlatform: Int,
    @SerializedName("badge_count") val badgeCount: Int
)

data class Book(
    val id: Int,
    val title: String,
    val author: String,
    @SerializedName("cover_url") val coverUrl: String?,
    val epubPath: String? = null,
    val genre: String,
    val rating: Float,
    @SerializedName("chapter_count") val chapterCount: Int,
    val description: String?,
    @SerializedName("reader_count_info") val readerCountInfo: String?
)

data class LibraryBook(
    val id: Int,
    val title: String,
    val author: String,
    @SerializedName("cover_url") val coverUrl: String?,
    val epubPath: String? = null,
    val genre: String,
    val rating: Float,
    @SerializedName("chapter_count") val chapterCount: Int,
    val description: String?,
    @SerializedName("reader_count_info") val readerCountInfo: String?,
    @SerializedName("progress_percent") val progressPercent: Float,
    @SerializedName("last_read_at") val lastReadAt: String,
    @SerializedName("local_id") val localId: String? = null
)

data class Post(
    val id: Int,
    @SerializedName("author_id") val authorId: Int,
    val content: String,
    @SerializedName("likes_count") val likesCount: Int,
    @SerializedName("comments_count") val commentsCount: Int,
    @SerializedName("created_at") val createdAt: String,
    val author: User
)

data class Group(
    val id: Int,
    val name: String,
    @SerializedName("cover_url") val coverUrl: String?,
    @SerializedName("member_count") val memberCount: String?,
    @SerializedName("activity_info") val activityInfo: String?
)

data class GroupPost(
    val id: Int,
    @SerializedName("group_id") val groupId: Int,
    @SerializedName("author_id") val authorId: Int,
    val content: String,
    @SerializedName("likes_count") val likesCount: Int,
    @SerializedName("comments_count") val commentsCount: Int,
    @SerializedName("created_at") val createdAt: String
)
