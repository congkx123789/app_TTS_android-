package com.example.texttosound.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val coverUrl: String?,
    val epubPath: String? = null,
    val isLibrary: Boolean = false,
    val description: String? = null,
    val genre: String? = null,
    val rating: Float = 0.0f
)

@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey val bookId: String,
    val chapterIndex: Int,
    val blockIndex: Int,
    val lastReadAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val avatarUrl: String? = "android.resource://com.example.texttosound/drawable/ic_launcher_foreground",
    val bio: String? = "Chưa có giới thiệu",
    val daysOnPlatform: Int = 0,
    val badgeCount: Int = 0
)

@Entity(tableName = "social_posts")
data class SocialPostEntity(
    @PrimaryKey val id: Int,
    val authorName: String,
    val authorAvatarUrl: String?,
    val content: String,
    val likesCount: Int,
    val commentsCount: Int,
    val createdAt: String
)
