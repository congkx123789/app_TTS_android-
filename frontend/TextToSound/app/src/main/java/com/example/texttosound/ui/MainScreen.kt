package com.example.texttosound.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.texttosound.ui.components.FloatingMiniPlayer
import com.example.texttosound.viewmodel.BookViewModel

@Composable
fun MainScreen(
    viewModel: BookViewModel,
    onPickEpub: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToSearch: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val bookState by viewModel.bookState.collectAsState()
    val isSynthesizing by viewModel.isSynthesizing.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val context = LocalContext.current

    // Show error as a Toast
    LaunchedEffect(loadError) {
        loadError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.consumeError()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF191A22))) {
        // Tab Content
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> LibraryScreen(
                        viewModel = viewModel,
                        onPickEpub = onPickEpub,
                        onNavigateToPlayer = onNavigateToPlayer,
                        onNavigateToSearch = onNavigateToSearch
                     )
                1 -> StoreScreen(
                        viewModel = viewModel,
                        onNavigateToSearch = onNavigateToSearch
                     )
                2 -> ExploreScreen(viewModel = viewModel) // Used as placeholder for Explore
                3 -> GroupScreen(viewModel = viewModel)
                4 -> ProfileScreen(viewModel = viewModel)
            }
        }

        // Bottom Overlay (Player + Nav)
        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            // Floating Mini Player Overlay (Only show if a book is loaded)
            if (bookState != null) {
                FloatingMiniPlayer(
                    viewModel = viewModel,
                    bookCover = bookState!!.coverImage,
                    title = bookState!!.title,
                    isSynthesizing = isSynthesizing,
                    isPlaying = isPlaying,
                    onPlayPauseClick = { 
                        if (isPlaying) viewModel.stopAudio() else viewModel.playCurrentBlock(context, "vn", 1.0f) 
                    },
                    onCloseClick = { viewModel.closeBook() },
                    onClick = onNavigateToPlayer,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            // Bottom Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(Color(0xFF191A22))
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(icon = Icons.Filled.Book, label = "Giá sách", isSelected = selectedTab == 0, onClick = { selectedTab = 0 }) // Shelf
                BottomNavItem(icon = Icons.Filled.Store, label = "Thư viện", isSelected = selectedTab == 1, onClick = { selectedTab = 1 }) // Store
                BottomNavItem(icon = Icons.Filled.Explore, label = "Khám phá", isSelected = selectedTab == 2, onClick = { selectedTab = 2 }) // Discover
                BottomNavItem(icon = Icons.Filled.Group, label = "Cộng đồng", isSelected = selectedTab == 3, onClick = { selectedTab = 3 }) // Group
                BottomNavItem(icon = Icons.Filled.Person, label = "Của tôi", isSelected = selectedTab == 4, onClick = { selectedTab = 4 }) // Me
            }
        }
    }
}

@Composable
fun BottomNavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (isSelected) Color(0xFF00A8FF) else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (isSelected) Color(0xFF00A8FF) else Color.Gray,
            fontSize = 10.sp
        )
    }
}
