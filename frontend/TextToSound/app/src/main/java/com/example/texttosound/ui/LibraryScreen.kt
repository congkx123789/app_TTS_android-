package com.example.texttosound.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asImageBitmap
import coil.compose.AsyncImage
import com.example.texttosound.viewmodel.BookViewModel

@Composable
fun LibraryScreen(
    viewModel: BookViewModel,
    onPickEpub: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToSearch: () -> Unit
) {
    val bookState by viewModel.bookState.collectAsState()
    val libraryBooks by viewModel.libraryBooks.collectAsState()
    val isSynthesizing by viewModel.isSynthesizing.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF191A22))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Giá sách", 
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Row {
                    IconButton(
                        onClick = {
                            android.util.Log.d("LibraryScreen", "Search icon clicked with large area")
                            onNavigateToSearch()
                        },
                        modifier = Modifier
                            .size(56.dp) // Larger touch area
                            .background(Color.Red.copy(alpha = 0.4f)) // Visual debug
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = Color.White)
                    }
                }
            }
            
            // Stats Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2A2B36))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("57", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(" Phút", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                        }
                        Text("Đã đọc tuần này", color = Color.Gray, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF3D3F4A))
                            .clickable { onPickEpub() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Thêm Sách (EPUB)", color = Color(0xFF00A8FF), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Icon(Icons.Filled.Add, contentDescription = null, tint = Color(0xFF00A8FF), modifier = Modifier.size(16.dp).padding(start = 4.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Book Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                // Slot for "Add Book"
                item {
                    BookGridItem(
                        title = "Thêm sách mới",
                        subtitle = "Nhấn để chọn EPUB",
                        onClick = onPickEpub,
                        isPlaceholder = true
                    )
                }

                // Currently loaded EPUB (local)
                if (bookState != null && libraryBooks.none { it.title == bookState!!.title }) {
                    item {
                        BookGridItem(
                            title = bookState!!.title,
                            subtitle = "Đang đọc",
                            onClick = onNavigateToPlayer,
                            isPlaying = isSynthesizing,
                            coverByteArray = bookState!!.coverImage
                        )
                    }
                }

                // Library Books (local + api)
                items(libraryBooks.size, key = { index -> libraryBooks[index].localId ?: libraryBooks[index].title }) { index ->
                    val book = libraryBooks[index]
                    BookGridItem(
                        title = book.title,
                        subtitle = "${book.progressPercent.toInt()}% · ${book.author}",
                        onClick = { 
                            if (book.title == bookState?.title) {
                                onNavigateToPlayer()
                            } else if (book.genre == "Local" && book.localId != null && (book.epubPath != null || book.coverUrl != null)) {
                                viewModel.loadLocalEpub(context, book.epubPath ?: book.coverUrl ?: "", book.localId)
                                onNavigateToPlayer()
                            } else {
                                viewModel.loadApiBook(book)
                                onNavigateToPlayer()
                            }
                        },
                        isPlaying = (book.title == bookState?.title) && (isPlaying || isSynthesizing),
                        coverByteArray = if (book.title == bookState?.title) bookState?.coverImage else null,
                        remoteCoverUrl = book.coverUrl
                    )
                }
            }
        }
    }
}

@Composable
fun BookGridItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isPlaying: Boolean = false,
    isPlaceholder: Boolean = false,
    coverByteArray: ByteArray? = null,
    remoteCoverUrl: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isPlaceholder) Color(0xFF2A2B36) else Color.DarkGray)
        ) {
            if (isPlaceholder) {
                Icon(
                    Icons.Filled.Add, 
                    contentDescription = null, 
                    tint = Color.Gray, 
                    modifier = Modifier.align(Alignment.Center).size(32.dp)
                )
            } else if (coverByteArray != null) {
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(coverByteArray, 0, coverByteArray.size)?.asImageBitmap()
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            } else if (remoteCoverUrl != null) {
                AsyncImage(
                    model = remoteCoverUrl,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF3A4B5C)))
            }
            
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(4.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                
                Text(
                    text = "Đang phát",
                    color = Color(0xFF00E5FF),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 6.dp, bottom = 6.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.height(2.dp))
        
        Text(
            text = subtitle,
            color = Color.Gray,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
