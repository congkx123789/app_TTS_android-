package com.example.texttosound.model

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val coverImage: ByteArray?,
    val chapters: List<Chapter>,
    val description: String? = null,
    val rating: Float = 0.0f
)

data class Chapter(
    val id: String,
    val title: String,
    val textBlocks: List<String> = emptyList() // Now immutable, defaults to empty
)
