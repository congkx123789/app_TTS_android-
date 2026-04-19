package com.example.texttosound.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import coil.compose.rememberAsyncImagePainter
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.texttosound.viewmodel.BookViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(viewModel: BookViewModel) {
    val groups by viewModel.groups.collectAsState()
    var selectedGroupId by remember { mutableStateOf<String?>(null) }
    var selectedUserId by remember { mutableStateOf<String?>(null) }
    val posts = viewModel.postsPagingFlow.collectAsLazyPagingItems()
    
    if (selectedUserId != null) {
        UserProfileScreen(
            userName = selectedUserId!!,
            onBackClick = { selectedUserId = null }
        )
    } else if (selectedGroupId != null) {
        GroupDetailScreen(
            groupName = selectedGroupId!!,
            onBackClick = { selectedGroupId = null },
            onUserClick = { selectedUserId = it }
        )
    } else {
        Column(
            modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF18191A))
        ) {
            // Facebook-style Top App Bar
            TopAppBar(
                title = { Text("Groups", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF18191A)),
                actions = {
                    IconButton(onClick = { /* Search */ }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.White)
                    }
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // Horizontal Groups Row
                item {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(groups) { group ->
                            GroupCircleItem(
                                name = group.name,
                                image = group.coverUrl,
                                onClick = { selectedGroupId = group.name }
                            )
                        }
                    }
                    Divider(color = Color(0xFF3E4042), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                }

                // Suggested Groups section
                item {
                    Text("Gợi ý cho bạn", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                    
                    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        groups.forEach { group ->
                            GroupCard(
                                name = group.name,
                                members = group.memberCount ?: "",
                                postsPerDay = group.activityInfo ?: "",
                                image = group.coverUrl,
                                onClick = { selectedGroupId = group.name }
                            )
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Divider(color = Color(0xFF3E4042), thickness = 8.dp)
                }

                // Group Posts Feed Title
                item {
                     Text(
                        "From your groups", 
                        color = Color.White, 
                        fontSize = 18.sp, 
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                // Paging Group Posts Feed
                items(posts.itemCount) { index ->
                    val post = posts[index] ?: return@items
                    FBPostCard(
                        groupName = "Cộng đồng đọc sách",
                        authorName = post.author.name,
                        timeAgo = "Vừa xong",
                        postText = post.content,
                        imageUrls = if (index % 3 == 0) listOf("https://picsum.photos/id/${100+index}/400/300") else emptyList(),
                        likesCount = post.likesCount.toString(),
                        commentsCount = post.commentsCount.toString(),
                        onGroupClick = { selectedGroupId = "Cộng đồng đọc sách" },
                        onUserClick = { selectedUserId = post.author.name }
                    )
                    Divider(color = Color(0xFF3E4042), thickness = 8.dp)
                }
            }
        }
    }
}

@Composable
fun GroupCircleItem(name: String, image: String? = null, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp).clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF3A3B3C))
        ) {
            if (image != null) {
                AsyncImage(
                    model = image,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            name, 
            color = Color.White, 
            fontSize = 12.sp, 
            maxLines = 1, 
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun GroupCard(name: String, members: String, postsPerDay: String, image: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF242526))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF3A3B3C))
        ) {
            if (image != null) {
                AsyncImage(
                    model = image,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(members, color = Color.Gray, fontSize = 14.sp)
            Text(postsPerDay, color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A3B3C)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(36.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Join Group", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun FBPostCard(groupName: String, authorName: String, timeAgo: String, postText: String, imageUrls: List<String>, likesCount: String, commentsCount: String, onGroupClick: () -> Unit = {}, onUserClick: () -> Unit = {}) {
    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF242526))) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clickable { onGroupClick() }) {
                Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color.DarkGray)
                        .border(1.dp, Color(0xFF242526), CircleShape)
                        .clickable { onUserClick() }
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                 Text(
                    text = groupName,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { onGroupClick() }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(authorName, color = Color(0xFFB0B3B8), fontSize = 13.sp, modifier = Modifier.clickable { onUserClick() })
                    Text(" · $timeAgo · ", color = Color(0xFFB0B3B8), fontSize = 13.sp)
                    Icon(Icons.Filled.Public, contentDescription = "Public", tint = Color(0xFFB0B3B8), modifier = Modifier.size(12.dp))
                }
            }
            IconButton(onClick = {}) {
                Icon(Icons.Filled.MoreHoriz, contentDescription = "More", tint = Color(0xFFB0B3B8))
            }
        }

        Text(
            text = postText,
            color = Color.White,
            fontSize = 15.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (imageUrls.isNotEmpty()) {
            AsyncImage(
                model = imageUrls[0],
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(250.dp),
                contentScale = ContentScale.Crop
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(Color(0xFF2E89FF)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.ThumbUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(likesCount, color = Color(0xFFB0B3B8), fontSize = 14.sp)
            }
            Text("$commentsCount comments", color = Color(0xFFB0B3B8), fontSize = 14.sp)
        }

        Divider(color = Color(0xFF3E4042), thickness = 1.dp, modifier = Modifier.padding(horizontal = 12.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FBActionButton(Icons.Outlined.ThumbUp, "Like")
            FBActionButton(Icons.Outlined.ChatBubbleOutline, "Comment")
            FBActionButton(Icons.Outlined.Share, "Share")
        }
    }
}

@Composable
fun FBActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, tint: Color = Color(0xFFB0B3B8)) {
    Row(
        modifier = Modifier.padding(12.dp).clickable { },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = text, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = Color(0xFFB0B3B8), fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(groupName: String, onBackClick: () -> Unit, onUserClick: (String) -> Unit = {}) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFF18191A)),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Gray))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
            }
        }
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(groupName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Public group · 1.7K members", color = Color(0xFFB0B3B8), fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun FBPillItem(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF3A3B3C))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(userName: String, onBackClick: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFF18191A)),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            TopAppBar(
                title = { Text(userName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF18191A)),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
            )
        }
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Trang cá nhân của $userName", color = Color.White, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(16.dp))
                // Basic placeholder for now to satisfy the compiler
            }
        }
    }
}
