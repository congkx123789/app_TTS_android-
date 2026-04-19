package com.example.texttosound

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.texttosound.database.AppDatabase
import com.example.texttosound.player.AudioPlayer
import com.example.texttosound.ui.AudiobookPlayerScreen
import com.example.texttosound.ui.SearchScreen
import com.example.texttosound.ui.EpubReaderScreen
import com.example.texttosound.ui.MainScreen
import com.example.texttosound.viewmodel.BookViewModel
import com.example.texttosound.viewmodel.BookViewModelFactory

class MainActivity : ComponentActivity() {
    private lateinit var audioPlayer: AudioPlayer
    private lateinit var viewModel: BookViewModel

    // Launcher to select the EPUB file
    private val selectEpubLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            Log.d("MainActivity", "Selected EPUB: $it")
            viewModel.loadEpub(this, it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        audioPlayer = AudioPlayer(this)
        val db = AppDatabase.getDatabase(this)
        val factory = BookViewModelFactory(
            audioPlayer,
            db.bookDao(),
            db.readingProgressDao(),
            db.userDao(),
            db.socialPostDao()
        )
        viewModel = ViewModelProvider(this, factory)[BookViewModel::class.java]
        viewModel.preloadBooks(this)

        // We now launch the file picker from the Library Screen instead of on startup.

        setContent {
            MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "main") {
                        composable("main") {
                            MainScreen(
                                viewModel = viewModel,
                                onPickEpub = { selectEpubLauncher.launch("application/epub+zip") },
                                onNavigateToPlayer = { navController.navigate("audiobook") },
                                onNavigateToSearch = { 
                                    Log.d("Navigation", "Navigating to search")
                                    Toast.makeText(this@MainActivity, "Navigating to Search...", Toast.LENGTH_SHORT).show()
                                    navController.navigate("search") 
                                }
                            )
                        }
                        composable("search") {
                            SearchScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("audiobook") {
                            AudiobookPlayerScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToReader = { navController.navigate("reader") }
                            )
                        }
                        composable("reader") {
                            EpubReaderScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayer.stop()
    }
}
