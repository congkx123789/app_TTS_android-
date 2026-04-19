package com.example.texttosound.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.texttosound.ui.components.FloatingMiniPlayer
import com.example.texttosound.viewmodel.BookViewModel
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EpubReaderScreen(
    viewModel: BookViewModel,
    onNavigateBack: () -> Unit
) {
    var showTocModal by remember { mutableStateOf(false) }
    var showSettingsModal by remember { mutableStateOf(false) }
    
    // Settings state
    var fontSizeSp by remember { mutableStateOf(18) }
    var bgColor by remember { mutableStateOf(Color(0xFF191A22)) }
    var textColor by remember { mutableStateOf(Color(0xFFBBBBBB)) }
    var activeBgColor by remember { mutableStateOf(Color(0xFF2A4B6B)) }
    var activeTextColor by remember { mutableStateOf(Color(0xFF8DB2D6)) }
    val bookState by viewModel.bookState.collectAsState()
    val chapterIndex by viewModel.currentChapterIndex.collectAsState()
    val blockIndex by viewModel.currentBlockIndex.collectAsState()
    val isSynthesizing by viewModel.isSynthesizing.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Save scroll position when the app goes to the background
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                // Determine current visible element
                val firstVisibleIndex = listState.firstVisibleItemIndex
                viewModel.setBlockIndex(firstVisibleIndex)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (bookState == null || bookState!!.chapters.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF191A22)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF00A8FF))
        }
        return
    }

    val book = bookState!!
    val currentChapter = book.chapters.getOrNull(chapterIndex) ?: return

    // Auto-scroll logic
    LaunchedEffect(blockIndex) {
        if (blockIndex in currentChapter.textBlocks.indices) {
            listState.animateScrollToItem(blockIndex)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Minimal Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, bottom = 16.dp, start = 12.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.Gray)
                }
                
                Text(
                    text = "〈 ${currentChapter.title} 〉",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val pagerState = rememberPagerState(
                initialPage = chapterIndex,
                pageCount = { book.chapters.size }
            )

            // React to pager swipes
            LaunchedEffect(pagerState.currentPage) {
                if (pagerState.currentPage != chapterIndex) {
                    viewModel.jumpToChapter(pagerState.currentPage)
                }
            }

            // Sync pager with external chapter jumps
            LaunchedEffect(chapterIndex) {
                if (pagerState.currentPage != chapterIndex) {
                    pagerState.animateScrollToPage(chapterIndex)
                }
            }

            // Reader Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val pageChapter = book.chapters.getOrNull(page)
                
                if (pageChapter != null && pageChapter.textBlocks.isNotEmpty()) {
                    LazyColumn(
                        state = if (page == chapterIndex) listState else rememberLazyListState(),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                    ) {
                        itemsIndexed(pageChapter.textBlocks) { index, text ->
                            val isActive = (page == chapterIndex) && (index == blockIndex)
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isActive) activeBgColor else Color.Transparent)
                                    .clickable { 
                                        if (page == chapterIndex) viewModel.setBlockIndex(index) 
                                    }
                                    .padding(4.dp)
                            ) {
                                Text(
                                    text = "\u3000\u3000$text", // Add paragraph indent
                                    fontSize = fontSizeSp.sp,
                                    lineHeight = (fontSizeSp * 1.5).sp,
                                    color = if (isActive) activeTextColor else textColor,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(100.dp)) // Padding for bottom player
                        }
                    }
                } else if (page == chapterIndex && isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                         CircularProgressIndicator(color = Color(0xFF00A8FF))
                    }
                } else {
                     Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                         Text("Vuốt để tải chương này...", color = Color.Gray)
                     }
                }
            }
        }

        // Bottom Overlay (Mini Player + Nav Bar)
        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val currentTitle = currentChapter.title
            FloatingMiniPlayer(
                viewModel = viewModel,
                bookCover = book.coverImage,
                title = currentTitle,
                isSynthesizing = isSynthesizing,
                isPlaying = isPlaying,
                onPlayPauseClick = { 
                    if (isPlaying) viewModel.stopAudio() else viewModel.playCurrentBlock(context, "vn", 1.0f) 
                },
                onCloseClick = { viewModel.stopAudio() },
                onClick = { onNavigateBack() }, // Navigates back to player screen on tap
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Bottom Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(Color(0xFF22232E))
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItemReader(icon = Icons.Filled.List, label = "Mục lục", onClick = { showTocModal = true }) // Catalog
                BottomNavItemReader(icon = Icons.Filled.DataSaverOn, label = "Tiến độ", onClick = {}) // Progress
                BottomNavItemReader(icon = Icons.Filled.Settings, label = "Cài đặt", onClick = { showSettingsModal = true }) // Settings
                BottomNavItemReader(icon = Icons.Filled.ChatBubbleOutline, label = "Thư hữu", onClick = {}) // Community
            }
        }

        // Floating Back to Player Button (Right Edge)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 24.dp) // Push it partially off-screen
                .clip(CircleShape)
                .background(Color(0xFF333333).copy(alpha = 0.8f))
                .clickable { onNavigateBack() }
                .padding(16.dp)
        ) {
            Icon(
                Icons.Filled.ArrowBack, 
                contentDescription = "Back to Player", 
                tint = Color.LightGray, 
                modifier = Modifier.padding(end = 16.dp) // shift icon left a bit
            )
        }

        // --- MODALS ---

        // TOC Modal
        if (showTocModal) {
            ModalBottomSheet(
                onDismissRequest = { showTocModal = false },
                containerColor = Color(0xFF22232E),
                modifier = Modifier.fillMaxHeight(0.7f)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    Text(
                        text = "Mục lục (${book.chapters.size} chương)",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    LazyColumn {
                        itemsIndexed(book.chapters) { index, chapter ->
                            val isCurrent = index == chapterIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.jumpToChapter(index)
                                        showTocModal = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = chapter.title,
                                    color = if (isCurrent) Color(0xFF00A8FF) else Color.LightGray,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Divider(color = Color(0xFF333333))
                        }
                    }
                }
            }
        }

        // Settings Modal
        if (showSettingsModal) {
            ModalBottomSheet(
                onDismissRequest = { showSettingsModal = false },
                containerColor = Color(0xFF22232E)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    
                    // Font Size Row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Cỡ chữ", color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(24.dp))
                        
                        Row(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(Color(0xFF333333)),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { if (fontSizeSp > 12) fontSizeSp -= 2 }) {
                                Text("A-", color = Color.White, fontSize = 16.sp)
                            }
                            Text("$fontSizeSp", color = Color.White, fontSize = 16.sp)
                            TextButton(onClick = { if (fontSizeSp < 32) fontSizeSp += 2 }) {
                                Text("A+", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    // Theme Row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                         ThemeCircle(
                            color = Color(0xFFFFFFFF),
                            isSelected = bgColor == Color(0xFFFFFFFF),
                            onClick = {
                                bgColor = Color(0xFFFFFFFF)
                                textColor = Color(0xFF333333)
                                activeBgColor = Color(0xFFE0E0E0)
                                activeTextColor = Color(0xFF000000)
                            }
                        )
                        ThemeCircle(
                            color = Color(0xFFF4E4D0), // Sepia
                            isSelected = bgColor == Color(0xFFF4E4D0),
                            onClick = {
                                bgColor = Color(0xFFF4E4D0)
                                textColor = Color(0xFF5C4033)
                                activeBgColor = Color(0xFFE6D2B5)
                                activeTextColor = Color(0xFF3E2723)
                            }
                        )
                        ThemeCircle(
                            color = Color(0xFF191A22), // Default Dark
                            isSelected = bgColor == Color(0xFF191A22),
                            onClick = {
                                bgColor = Color(0xFF191A22)
                                textColor = Color(0xFFBBBBBB)
                                activeBgColor = Color(0xFF2A4B6B)
                                activeTextColor = Color(0xFF8DB2D6)
                            }
                        )
                         ThemeCircle(
                            color = Color(0xFFE0F7FA), // Light Blue
                            isSelected = bgColor == Color(0xFFE0F7FA),
                            onClick = {
                                bgColor = Color(0xFFE0F7FA)
                                textColor = Color(0xFF006064)
                                activeBgColor = Color(0xFFB2EBF2)
                                activeTextColor = Color(0xFF004D40)
                            }
                        )
                        ThemeCircle(
                            color = Color(0xFFE8F5E9), // Light Green
                            isSelected = bgColor == Color(0xFFE8F5E9),
                            onClick = {
                                bgColor = Color(0xFFE8F5E9)
                                textColor = Color(0xFF1B5E20)
                                activeBgColor = Color(0xFFC8E6C9)
                                activeTextColor = Color(0xFF004D40)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeCircle(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color)
            .clickable { onClick() }
            // Add a border if it's selected to show it's active
            .then(
                if (isSelected) Modifier.padding(2.dp).background(color, CircleShape).padding(2.dp) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
         if (isSelected) {
            Icon(Icons.Filled.DataSaverOn, contentDescription = "Selected", tint = if(color == Color(0xFF191A22)) Color.White else Color.Black, modifier = Modifier.size(24.dp))
         }
    }
}

@Composable
fun BottomNavItemReader(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
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
            tint = Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 10.sp
        )
    }
}
