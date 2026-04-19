package com.example.texttosound.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.texttosound.player.AudioPlayer

import com.example.texttosound.database.*

class BookViewModelFactory(
    private val audioPlayer: AudioPlayer,
    private val bookDao: BookDao,
    private val progressDao: ReadingProgressDao,
    private val userDao: UserDao,
    private val postDao: SocialPostDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BookViewModel(audioPlayer, bookDao, progressDao, userDao, postDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
