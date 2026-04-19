package com.example.texttosound.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.texttosound.viewmodel.BookViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudiobookPlayerScreen(
    viewModel: BookViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToReader: () -> Unit
) {
    val bookState by viewModel.bookState.collectAsState()
    val chapterIndex by viewModel.currentChapterIndex.collectAsState()
    val blockIndex by viewModel.currentBlockIndex.collectAsState()
    val isSynthesizing by viewModel.isSynthesizing.collectAsState()

    val context = LocalContext.current
    var speed by remember { mutableFloatStateOf(1.0f) }
    
    // TOC Modal State
    var showToc by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    if (bookState == null) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF191A22)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF00A8FF))
        }
        return
    }

    val book = bookState!!
    val currentChapter = book.chapters.getOrNull(chapterIndex)
    val totalBlocks = currentChapter?.textBlocks?.size ?: 1

    Scaffold(
        containerColor = Color(0xFF191A22), // Deep dark background
        topBar = {
            TopAppBar(
                title = { 
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "AI Đọc", 
                            color = Color.White, 
                            fontSize = 18.sp, 
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Back", tint = Color.LightGray)
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Color.LightGray)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = Color.LightGray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Cover Image Card
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .aspectRatio(1f) // Square
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF2E2E36))
            ) {
                val bitmap = book.coverImage?.let {
                    BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
                }
                if (bitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap,
                        contentDescription = "Cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Gradient overlay for bottom half
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                                startY = 300f
                            )
                        )
                )

                // Title and Chapter on Cover
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = book.title,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = currentChapter?.title ?: "Chương Không Xác Định",
                        color = Color(0xFFBBBBBB),
                        fontSize = 14.sp
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Now Playing Text Preview
                    val currentText = currentChapter?.textBlocks?.getOrNull(blockIndex) ?: ""
                    Text(
                        text = if (currentText.length > 80) "${currentText.take(80)}..." else currentText,
                        color = Color(0xFF888888),
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Membership Badge Text
            Text(
                text = "Hội viên nghe sách này miễn phí",
                color = Color.Gray,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Progress Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2A2A30))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Đoạn ${blockIndex + 1} / $totalBlocks  ·  03:19/08:20",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar (Slider)
            Slider(
                value = if (totalBlocks > 0) blockIndex.toFloat() / totalBlocks else 0f,
                onValueChange = {},
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent, 
                    activeTrackColor = Color.LightGray,
                    inactiveTrackColor = Color.DarkGray
                ),
                modifier = Modifier.fillMaxWidth().height(16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* -15s */ }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "-15s", tint = Color.Gray, modifier = Modifier.size(28.dp))
                }
                
                IconButton(onClick = { viewModel.previousBlock(context, "vn", speed) }) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Prev", tint = Color(0xFF00A8FF), modifier = Modifier.size(36.dp))
                }

                // Play / Pause Circle Button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00A8FF))
                        .clickable { if(!isSynthesizing) viewModel.playCurrentBlock(context, "vn", speed) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSynthesizing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                IconButton(onClick = { viewModel.nextBlock(context, "vn", speed) }) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color(0xFF00A8FF), modifier = Modifier.size(36.dp))
                }
                
                IconButton(onClick = { /* +15s */ }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "+15s", tint = Color.Gray, modifier = Modifier.size(28.dp)) 
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Tools Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ToolButton(icon = Icons.Filled.List, label = "Trong Kệ")
                ToolButton(icon = Icons.Filled.DateRange, label = "Hẹn Giờ")
                ToolButton(icon = Icons.Filled.PlayArrow, label = "${speed}X", onClick = { 
                    speed = when (speed) {
                        0.5f -> 0.75f
                        0.75f -> 1.0f
                        1.0f -> 1.25f
                        1.25f -> 1.5f
                        1.5f -> 2.0f
                        2.0f -> 2.5f
                        2.5f -> 3.0f
                        else -> 1.0f
                    }
                    viewModel.playCurrentBlock(context, "vn", speed)
                })
                ToolButton(icon = Icons.Filled.KeyboardArrowDown, label = "Tải Xuống")
                ToolButton(icon = Icons.Filled.Menu, label = "${book.chapters.size} Chương", onClick = { showToc = true })
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Bottom Nav Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF2A2A30))
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Người Đọc - Chiêu Quân >", color = Color.LightGray, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF2A2A30))
                        .clickable { onNavigateToReader() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Xem Nguyên Bản", color = Color.LightGray, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Book Introduction Section
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Sách giới thiệu",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = book.description ?: "Chưa có giới thiệu cho cuốn sách này. Đây là một tác phẩm hấp dẫn dành cho bạn khám phá.",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }

            // Recommended Books Section
            val libraryBooks by viewModel.libraryBooks.collectAsState()
            if (libraryBooks.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Những sách khác",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Xem thêm >",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(libraryBooks.take(5)) { _, libBook ->
                            Column(
                                modifier = Modifier.width(100.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp, 140.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF2E2E36))
                                ) {
                                    // Local mapping of library cover would go here, 
                                    // for now just placeholder or AsyncImage if available.
                                    if (libBook.coverUrl != null) {
                                        androidx.compose.foundation.Image(
                                            painter = painterResource(id = android.R.drawable.ic_menu_gallery),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = libBook.title,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    text = libBook.author,
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // TOC Bottom Sheet
        if (showToc) {
            ModalBottomSheet(
                onDismissRequest = { showToc = false },
                sheetState = sheetState,
                containerColor = Color(0xFF1E1E24), // Dark grey for TOC background
                modifier = Modifier.fillMaxHeight(0.85f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hoàn Thành Tổng Cộng ${book.chapters.size} Chương",
                            color = Color(0xFFBBBBBB),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Đảo Ngược ↓",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                    
                    Divider(color = Color(0xFF2A2A30), thickness = 0.5.dp)
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        itemsIndexed(book.chapters) { index, chapter ->
                            val isCurrent = index == chapterIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.jumpToChapter(index)
                                        showToc = false
                                        onNavigateToReader()
                                    }
                                    .padding(horizontal = 24.dp, vertical = 18.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = chapter.title,
                                            color = if (isCurrent) Color(0xFF00A8FF) else Color(0xFFBBBBBB),
                                            fontSize = 15.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                        )
                                        
                                        val previewText = chapter.textBlocks.firstOrNull() ?: ""
                                        if (previewText.isNotEmpty()) {
                                            Text(
                                                text = if (previewText.length > 50) "${previewText.take(50)}..." else previewText,
                                                color = Color.Gray,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                
                                if (!isCurrent) {
                                    Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = Color(0xFF333333), modifier = Modifier.size(16.dp))
                                }
                            }
                            
                            Divider(color = Color(0xFF2A2A30), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToolButton(icon: ImageVector, label: String, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.padding(8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = Color.Gray, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = Color.Gray, fontSize = 11.sp)
    }
}
