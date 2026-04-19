package com.example.texttosound.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface AppApiService {
    @GET("health")
    suspend fun checkHealth(): Response<Void>

    @GET("api/v1/books")
    suspend fun getBooks(): Response<List<Book>>

    @GET("api/v1/books/{id}")
    suspend fun getBook(@Path("id") id: Int): Response<Book>

    @GET("api/v1/users/{userId}/library")
    suspend fun getUserLibrary(@Path("userId") userId: Int): Response<List<LibraryBook>>

    @GET("api/v1/posts")
    suspend fun getPosts(): Response<List<Post>>

    @GET("api/v1/groups")
    suspend fun getGroups(): Response<List<Group>>

    @GET("api/v1/groups/{groupId}/posts")
    suspend fun getGroupPosts(@Path("groupId") groupId: Int): Response<List<GroupPost>>

    @GET("api/v1/users/{id}")
    suspend fun getUser(@Path("id") id: Int): Response<User>
}
