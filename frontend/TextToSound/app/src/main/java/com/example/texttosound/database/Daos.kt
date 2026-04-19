package com.example.texttosound.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books WHERE isLibrary = 1")
    fun getLibraryBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE isLibrary = 0")
    fun getStoreBooks(): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Query("UPDATE books SET isLibrary = :isLibrary WHERE id = :bookId")
    suspend fun updateLibraryStatus(bookId: String, isLibrary: Int)

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteBook(bookId: String)

    @Query("SELECT * FROM books WHERE title = :title AND author = :author AND genre = 'Local' LIMIT 1")
    suspend fun findLocalBook(title: String, author: String): BookEntity?

    @Query("SELECT * FROM books")
    suspend fun getAllBooks(): List<BookEntity>
}

@Dao
interface ReadingProgressDao {
    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId")
    fun getProgress(bookId: String): Flow<ReadingProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: ReadingProgressEntity)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUser(userId: Int): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
}

@Dao
interface SocialPostDao {
    @Query("SELECT * FROM social_posts ORDER BY createdAt DESC")
    fun getPosts(): Flow<List<SocialPostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<SocialPostEntity>)

    @Query("DELETE FROM social_posts")
    suspend fun clearPosts()
}
