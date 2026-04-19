package com.example.texttosound.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.texttosound.viewmodel.BookViewModel

@Composable
fun ExploreScreen(viewModel: BookViewModel) {
    val posts = viewModel.postsPagingFlow.collectAsLazyPagingItems()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF191A22))
    ) {
        // Top Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Theo dõi", color = Color.Gray, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text("Quảng trường", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Truyện ngắn", color = Color.Gray, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(posts.itemCount) { index ->
                val post = posts[index] ?: return@items
                ExplorePostItem(
                    authorName = post.author.name,
                    authorAvatar = post.author.avatarUrl,
                    timeAgo = "Vừa xong",
                    content = post.content,
                    likesCount = post.likesCount.toString(),
                    commentsCount = post.commentsCount.toString()
                )
            }
        }
    }
}

@Composable
fun ExplorePostItem(
    authorName: String,
    authorAvatar: String? = null,
    timeAgo: String,
    content: String,
    likesCount: String = "0",
    commentsCount: String = "0",
    isImageGrid: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Author Row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
            ) {
                if (authorAvatar != null) {
                    AsyncImage(
                        model = authorAvatar,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(authorName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(timeAgo, color = Color.Gray, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { }) {
                Icon(Icons.Filled.MoreVert, contentDescription = null, tint = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Content
        Text(
            text = content,
            color = Color.White,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis
        )

        if (isImageGrid) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF2A2B36))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.FavoriteBorder, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(likesCount, color = Color.Gray, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.width(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(commentsCount, color = Color.Gray, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Filled.Share, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = Color(0xFF2A2B36), thickness = 1.dp)
    }
}
